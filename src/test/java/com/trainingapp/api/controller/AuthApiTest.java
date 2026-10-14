package com.trainingapp.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.trainingapp.api.config.ErroDeAutenticacaoException;
import com.trainingapp.api.model.EmailPendente;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.EmailPendenteRepository;
import com.trainingapp.api.repository.RefreshTokenRepository;
import com.trainingapp.api.repository.TokenEmailRepository;
import com.trainingapp.api.repository.UsuarioRepository;
import com.trainingapp.api.service.AuthService;
import com.trainingapp.api.service.VerificadorGoogle;
import com.trainingapp.api.service.VerificadorGoogle.Identidade;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sem @WithMockUser: aqui o login e os tokens sao de verdade. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiTest {

	static final String SENHA = "Senha-Forte-123!";
	static final String EMAIL = "maria@teste.com"; // esta na lista de permitidos (application-test.properties)
	static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
	// O freio de tentativas e por IP e vive entre os testes: cada requisicao usa um IP novo, salvo quando o teste quer bater no freio.
	static final AtomicInteger IPS = new AtomicInteger();

	@Autowired
	MockMvc mvc;

	@Autowired
	EmailPendenteRepository emails;

	@Autowired
	RefreshTokenRepository refreshTokens;

	@Autowired
	TokenEmailRepository tokensEmail;

	@Autowired
	UsuarioRepository usuarios;

	@MockitoBean
	VerificadorGoogle google;

	// --- acesso protegido ---

	@Test
	void apiExigeToken() throws Exception {
		mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer lixo")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/auth/config")).andExpect(status().isOk())
				.andExpect(jsonPath("$.googleClientId").value("cliente-de-teste.apps.googleusercontent.com"));
	}

	// --- cadastro, confirmacao e login com senha ---

	@Test
	void cadastroConfirmacaoELoginComSenha() throws Exception {
		post("/api/auth/cadastro", cadastro("  Maria ", "Maria@Teste.com")).andExpect(status().isCreated());

		// Antes de confirmar o e-mail nao entra.
		login(EMAIL, SENHA).andExpect(status().isForbidden());

		EmailPendente email = ultimoEmailPara(EMAIL);
		assertThat(email.getAssunto()).contains("Confirme");
		assertThat(email.getCorpo()).contains("http://localhost:4200/confirmar-email?token=");
		post("/api/auth/confirmar-email", "{\"token\":\"%s\"}".formatted(token(email))).andExpect(status().isNoContent());
		// Link de uso unico.
		post("/api/auth/confirmar-email", "{\"token\":\"%s\"}".formatted(token(email))).andExpect(status().isBadRequest());

		MvcResult entrou = login(EMAIL, SENHA).andExpect(status().isOk())
				.andExpect(jsonPath("$.expiraEmSegundos").value(900))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
				.andReturn();

		// O access token abre a API como a Maria (nome "Maria", e-mail em minusculas).
		mvc.perform(get("/api/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + acesso(entrou)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Maria"))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.temSenha").value(true));
	}

	@Test
	void cadastroRespeitaListaDeEmailsESenha() throws Exception {
		post("/api/auth/cadastro", cadastro("Intruso", "intruso@fora.com")).andExpect(status().isForbidden());
		// Senha fraca: 422 com a mensagem da regra (falta de tamanho, maiuscula, minuscula, numero ou simbolo).
		for (String fraca : new String[] { "Curt@1", "sem-maiuscula-1!", "SEM-MINUSCULA-1!", "Sem-Numero-Aqui!", "SemSimbolo123" }) {
			post("/api/auth/cadastro", cadastro("Maria", EMAIL).replace(SENHA, fraca)).andExpect(status().isUnprocessableContent())
					.andExpect(jsonPath("$.detail").value(containsString("8 ou mais caracteres")));
		}

		post("/api/auth/cadastro", cadastro("Maria", EMAIL)).andExpect(status().isCreated());
	}

	@Test
	void cadastroComEmailQueJaTemContaRespondeIgualEAvisaODono() throws Exception {
		post("/api/auth/cadastro", cadastro("Maria", EMAIL)).andExpect(status().isCreated());
		String senhaHash = usuarios.findByEmail(EMAIL).orElseThrow().getSenhaHash();

		// Mesma resposta de um cadastro novo: quem testa e-mails nao descobre quem tem conta.
		post("/api/auth/cadastro", cadastro("Outra Maria", EMAIL).replace(SENHA, "Outra-Senha-9")).andExpect(status().isCreated());

		assertThat(usuarios.findAll().stream().filter(u -> u.getEmail().equals(EMAIL))).hasSize(1);
		assertThat(usuarios.findByEmail(EMAIL).orElseThrow().getSenhaHash()).isEqualTo(senhaHash);
		assertThat(emails.findAll().stream().filter(e -> e.getDestinatario().equals(EMAIL)).map(EmailPendente::getAssunto))
				.containsExactlyInAnyOrder("Confirme seu e-mail no TrainingApp", "Você já tem conta no TrainingApp");
	}

	@Test
	void linkDeConfirmacaoVencidoNaoFunciona() throws Exception {
		post("/api/auth/cadastro", cadastro("Maria", EMAIL)).andExpect(status().isCreated());
		String token = token(ultimoEmailPara(EMAIL));
		tokensEmail.findAll().forEach(t -> t.setExpiraEm(Instant.now().minusSeconds(1)));

		post("/api/auth/confirmar-email", "{\"token\":\"%s\"}".formatted(token)).andExpect(status().isBadRequest());
	}

	@Test
	void senhaErradaDaMensagemIgualParaContaInexistenteEBloqueiaNaQuintaTentativa() throws Exception {
		criarContaConfirmada(EMAIL);
		String ip = ipNovo();
		login(EMAIL, "errada-errada", ip).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos"));
		login("ninguem@teste.com", "errada-errada", ip).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos"));

		for (int i = 0; i < 4; i++) {
			login(EMAIL, "errada-errada", ip);
		}
		// Bloqueado: nem a senha certa passa, ate o tempo de espera acabar.
		login(EMAIL, SENHA, ip).andExpect(status().isTooManyRequests());
		// Outro IP nao e afetado.
		login(EMAIL, SENHA).andExpect(status().isOk());
	}

	@Test
	void umIpNaoTentaEmailsSemFimNemGoogleSemFim() throws Exception {
		// O freio por e-mail+IP nao basta: trocando de e-mail o mesmo IP nunca o atingiria, e cada tentativa custa um bcrypt.
		String ip = ipNovo();
		for (int i = 0; i < AuthService.LIMITE_POR_IP; i++) {
			login("ninguem" + i + "@teste.com", "errada-errada", ip).andExpect(status().isUnauthorized());
		}
		login("ninguem-a-mais@teste.com", "errada-errada", ip).andExpect(status().isTooManyRequests());

		String ipGoogle = ipNovo();
		when(google.verificarCodigo("ruim")).thenThrow(new ErroDeAutenticacaoException(HttpStatus.UNAUTHORIZED, "recusado"));
		for (int i = 0; i < AuthService.LIMITE_POR_IP; i++) {
			mvc.perform(requisicao("/api/auth/google", "{\"codigo\":\"ruim\"}", ipGoogle)).andExpect(status().isUnauthorized());
		}
		mvc.perform(requisicao("/api/auth/google", "{\"codigo\":\"ruim\"}", ipGoogle)).andExpect(status().isTooManyRequests());
	}

	@Test
	void emailEnormeNoLoginOuNoEsqueciSenhaDa400() throws Exception {
		// O e-mail do login vira chave do freio de tentativas, que fica na memoria: tem de ter teto.
		String enorme = "a".repeat(255) + "@teste.com";
		login(enorme, "qualquer").andExpect(status().isBadRequest());
		post("/api/auth/esqueci-senha", "{\"email\":\"%s\"}".formatted(enorme)).andExpect(status().isBadRequest());
	}

	// --- sessao longa ---

	@Test
	void refreshRenovaOAcessoEEmpurraOVencimento() throws Exception {
		criarContaConfirmada(EMAIL);
		String refresh = refresh(login(EMAIL, SENHA).andReturn());
		refreshTokens.findAll().forEach(t -> t.setUltimoUsoEm(Instant.now().minus(300, ChronoUnit.DAYS)));

		MvcResult renovou = renovar(refresh).andExpect(status().isOk())
				.andExpect(jsonPath("$.acesso").isNotEmpty())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=31536000")))
				.andReturn();
		mvc.perform(get("/api/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + acesso(renovou))).andExpect(status().isOk());

		// 300 dias parados nao venceram; a renovacao zerou a contagem.
		assertThat(refreshTokens.findAll().get(0).getUltimoUsoEm()).isAfter(Instant.now().minusSeconds(60));
	}

	@Test
	void refreshVencidoOuDesconhecidoDa401() throws Exception {
		renovar(null).andExpect(status().isUnauthorized());
		renovar("token-que-nao-existe").andExpect(status().isUnauthorized());

		criarContaConfirmada(EMAIL);
		String refresh = refresh(login(EMAIL, SENHA).andReturn());
		refreshTokens.findAll().forEach(t -> t.setUltimoUsoEm(Instant.now().minus(366, ChronoUnit.DAYS)));
		renovar(refresh).andExpect(status().isUnauthorized());
	}

	@Test
	void sairApagaSoOAparelhoESairDeTodosApagaTudo() throws Exception {
		criarContaConfirmada(EMAIL);
		MvcResult a = login(EMAIL, SENHA).andReturn();
		MvcResult b = login(EMAIL, SENHA).andReturn();

		mvc.perform(MockMvcRequestBuilders.post("/api/auth/sair").cookie(new Cookie("refresh_token", refresh(a))))
				.andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
		renovar(refresh(a)).andExpect(status().isUnauthorized());
		renovar(refresh(b)).andExpect(status().isOk());

		mvc.perform(MockMvcRequestBuilders.post("/api/auth/sair-todos")).andExpect(status().isUnauthorized());
		mvc.perform(MockMvcRequestBuilders.post("/api/auth/sair-todos").header(HttpHeaders.AUTHORIZATION, "Bearer " + acesso(b)))
				.andExpect(status().isNoContent());
		renovar(refresh(b)).andExpect(status().isUnauthorized());
	}

	// --- senha ---

	@Test
	void esqueciSenhaNaoRevelaContasERedefinirDerrubaOsAparelhos() throws Exception {
		criarContaConfirmada(EMAIL);
		String refresh = refresh(login(EMAIL, SENHA).andReturn());
		long antes = emails.count();

		post("/api/auth/esqueci-senha", "{\"email\":\"ninguem@teste.com\"}").andExpect(status().isAccepted());
		post("/api/auth/esqueci-senha", "{\"email\":\"intruso@fora.com\"}").andExpect(status().isAccepted());
		assertThat(emails.count()).isEqualTo(antes);

		post("/api/auth/esqueci-senha", "{\"email\":\"MARIA@teste.com\"}").andExpect(status().isAccepted());
		EmailPendente email = ultimoEmailPara(EMAIL);
		assertThat(email.getCorpo()).contains("http://localhost:4200/redefinir-senha?token=");

		String corpo = "{\"token\":\"%s\",\"senha\":\"Outra-Senha-456!\"}".formatted(token(email));
		post("/api/auth/redefinir-senha", corpo).andExpect(status().isNoContent());
		post("/api/auth/redefinir-senha", corpo).andExpect(status().isBadRequest());

		renovar(refresh).andExpect(status().isUnauthorized());
		login(EMAIL, SENHA).andExpect(status().isUnauthorized());
		login(EMAIL, "Outra-Senha-456!").andExpect(status().isOk());
	}

	@Test
	void redefinirSenhaConfirmaOEmailDeQuemNuncaConfirmou() throws Exception {
		post("/api/auth/cadastro", cadastro("Maria", EMAIL)).andExpect(status().isCreated());
		post("/api/auth/esqueci-senha", "{\"email\":\"%s\"}".formatted(EMAIL)).andExpect(status().isAccepted());
		EmailPendente email = ultimoEmailPara(EMAIL);
		assertThat(email.getAssunto()).contains("Redefinir");

		post("/api/auth/redefinir-senha", "{\"token\":\"%s\",\"senha\":\"Outra-Senha-456!\"}".formatted(token(email)))
				.andExpect(status().isNoContent());
		login(EMAIL, "Outra-Senha-456!").andExpect(status().isOk());
	}

	@Test
	void senhaFracaTambemERecusadaNaTrocaENaRedefinicaoSemGastarOLink() throws Exception {
		criarContaConfirmada(EMAIL);
		MvcResult a = login(EMAIL, SENHA).andReturn();
		trocarSenha(a, SENHA, "fraca123").andExpect(status().isUnprocessableContent());
		login(EMAIL, SENHA).andExpect(status().isOk()); // a senha antiga continua valendo

		post("/api/auth/esqueci-senha", "{\"email\":\"%s\"}".formatted(EMAIL)).andExpect(status().isAccepted());
		String token = token(ultimoEmailPara(EMAIL));
		post("/api/auth/redefinir-senha", "{\"token\":\"%s\",\"senha\":\"fraca123\"}".formatted(token)).andExpect(status().isUnprocessableContent());
		// O link nao foi gasto: com uma senha forte ele ainda vale.
		post("/api/auth/redefinir-senha", "{\"token\":\"%s\",\"senha\":\"Outra-Senha-456!\"}".formatted(token)).andExpect(status().isNoContent());
	}

	@Test
	void trocarSenhaExigeASenhaAtualEMantemSoOAparelhoAtual() throws Exception {
		criarContaConfirmada(EMAIL);
		MvcResult a = login(EMAIL, SENHA).andReturn();
		MvcResult b = login(EMAIL, SENHA).andReturn();

		trocarSenha(a, "errada-errada", "Nova-Senha-789!").andExpect(status().isUnprocessableContent());
		trocarSenha(a, SENHA, "Nova-Senha-789!").andExpect(status().isNoContent());

		renovar(refresh(a)).andExpect(status().isOk());
		renovar(refresh(b)).andExpect(status().isUnauthorized());
		login(EMAIL, SENHA).andExpect(status().isUnauthorized());
		login(EMAIL, "Nova-Senha-789!").andExpect(status().isOk());
	}

	// --- Google ---

	@Test
	void googleCriaContaNovaEReaproveitaNoSegundoLogin() throws Exception {
		when(google.verificarCodigo("cred")).thenReturn(new Identidade("sub-1", "Maria@Teste.com", true, "Maria Silva"));

		MvcResult primeiro = google("cred").andExpect(status().isOk()).andReturn();
		google("cred").andExpect(status().isOk());

		assertThat(usuarios.findByGoogleSub("sub-1")).isPresent();
		assertThat(usuarios.findByEmail(EMAIL)).get().satisfies(u -> {
			assertThat(u.getNome()).isEqualTo("Maria Silva");
			assertThat(u.isEmailVerificado()).isTrue();
			assertThat(u.getSenhaHash()).isNull();
		});
		mvc.perform(get("/api/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + acesso(primeiro)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.temSenha").value(false));
	}

	@Test
	void googleRespeitaListaEEmailVerificado() throws Exception {
		when(google.verificarCodigo("fora")).thenReturn(new Identidade("sub-2", "intruso@fora.com", true, "Intruso"));
		when(google.verificarCodigo("naoverificado")).thenReturn(new Identidade("sub-3", EMAIL, false, "Maria"));

		google("fora").andExpect(status().isForbidden());
		google("naoverificado").andExpect(status().isForbidden());
		assertThat(usuarios.findByEmail("intruso@fora.com")).isEmpty();
		assertThat(usuarios.findByEmail(EMAIL)).isEmpty();
	}

	@Test
	void googleVinculaContaDeSenhaJaConfirmadaEMantemASenha() throws Exception {
		criarContaConfirmada(EMAIL);
		when(google.verificarCodigo("cred")).thenReturn(new Identidade("sub-1", EMAIL, true, "Maria"));

		google("cred").andExpect(status().isOk());

		assertThat(usuarios.findAll().stream().filter(u -> u.getEmail().equals(EMAIL))).hasSize(1);
		login(EMAIL, SENHA).andExpect(status().isOk());
	}

	@Test
	void googleDescartaASenhaDeContaQueNuncaConfirmouOEmail() throws Exception {
		// Alguem se cadastrou com o e-mail da Maria sem provar que e dela; quando a Maria entra pelo Google, a senha dele some.
		post("/api/auth/cadastro", cadastro("Impostor", EMAIL)).andExpect(status().isCreated());
		when(google.verificarCodigo("cred")).thenReturn(new Identidade("sub-1", EMAIL, true, "Maria"));

		google("cred").andExpect(status().isOk());

		login(EMAIL, SENHA).andExpect(status().isUnauthorized());
		Usuario u = usuarios.findByEmail(EMAIL).orElseThrow();
		assertThat(u.isEmailVerificado()).isTrue();
		assertThat(u.getSenhaHash()).isNull();
	}

	// --- apoio ---

	String cadastro(String nome, String email) {
		return "{\"nome\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(nome, email, SENHA);
	}

	void criarContaConfirmada(String email) throws Exception {
		post("/api/auth/cadastro", cadastro("Maria", email)).andExpect(status().isCreated());
		post("/api/auth/confirmar-email", "{\"token\":\"%s\"}".formatted(token(ultimoEmailPara(email)))).andExpect(status().isNoContent());
	}

	ResultActions login(String email, String senha) throws Exception {
		return login(email, senha, ipNovo());
	}

	ResultActions login(String email, String senha, String ip) throws Exception {
		return mvc.perform(requisicao("/api/auth/login", "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha), ip));
	}

	ResultActions google(String codigo) throws Exception {
		return post("/api/auth/google", "{\"codigo\":\"%s\"}".formatted(codigo));
	}

	ResultActions renovar(String refresh) throws Exception {
		MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post("/api/auth/refresh");
		return mvc.perform(refresh == null ? req : req.cookie(new Cookie("refresh_token", refresh)));
	}

	ResultActions trocarSenha(MvcResult sessao, String atual, String nova) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.post("/api/auth/trocar-senha")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + acesso(sessao))
				.cookie(new Cookie("refresh_token", refresh(sessao)))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"senhaAtual\":\"%s\",\"novaSenha\":\"%s\"}".formatted(atual, nova)));
	}

	ResultActions post(String caminho, String corpo) throws Exception {
		return mvc.perform(requisicao(caminho, corpo, ipNovo()));
	}

	MockHttpServletRequestBuilder requisicao(String caminho, String corpo, String ip) {
		return MockMvcRequestBuilders.post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo)
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				});
	}

	static String ipNovo() {
		return "10.9.%d.%d".formatted(IPS.incrementAndGet() / 250, IPS.get() % 250);
	}

	static String acesso(MvcResult r) throws Exception {
		return JsonPath.read(r.getResponse().getContentAsString(), "$.acesso");
	}

	static String refresh(MvcResult r) {
		return r.getResponse().getCookie("refresh_token").getValue();
	}

	EmailPendente ultimoEmailPara(String destinatario) {
		return emails.findAll().stream().filter(e -> e.getDestinatario().equals(destinatario))
				.max(Comparator.comparing(EmailPendente::getCriadoEm)).orElseThrow();
	}

	static String token(EmailPendente email) {
		Matcher m = TOKEN.matcher(email.getCorpo());
		assertThat(m.find()).as("link com token no e-mail").isTrue();
		return m.group(1);
	}
}
