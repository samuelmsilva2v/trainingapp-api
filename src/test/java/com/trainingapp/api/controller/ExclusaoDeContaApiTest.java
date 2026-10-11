package com.trainingapp.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.trainingapp.api.UsuarioDeTeste;
import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.EmailPendente;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.repository.EmailPendenteRepository;
import com.trainingapp.api.repository.ExercicioRepository;
import com.trainingapp.api.repository.PlanoRepository;
import com.trainingapp.api.repository.RefreshTokenRepository;
import com.trainingapp.api.repository.SessaoRepository;
import com.trainingapp.api.repository.TokenEmailRepository;
import com.trainingapp.api.repository.UsuarioRepository;
import com.trainingapp.api.repository.XpEventRepository;
import com.trainingapp.api.service.VerificadorGoogle;
import com.trainingapp.api.service.VerificadorGoogle.Identidade;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/** Excluir a conta apaga tudo do usuario (e so dele), exige provar quem pede e derruba os logins. Sem @WithMockUser: tokens reais. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExclusaoDeContaApiTest {

	static final String SENHA = "senha-forte-123";
	static final String EMAIL = "maria@teste.com"; // esta na lista de permitidos (application-test.properties)
	static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
	// O freio de tentativas por IP vive entre os testes: cada requisicao usa um IP novo.
	static final AtomicInteger IPS = new AtomicInteger(1000);

	@Autowired
	MockMvc mvc;
	@Autowired
	EmailPendenteRepository emails;
	@Autowired
	UsuarioRepository usuarios;
	@Autowired
	PlanoRepository planos;
	@Autowired
	SessaoRepository sessoes;
	@Autowired
	XpEventRepository xp;
	@Autowired
	ExercicioRepository exercicios;
	@Autowired
	RefreshTokenRepository refreshTokens;
	@Autowired
	TokenEmailRepository tokensEmail;

	@MockitoBean
	VerificadorGoogle google;

	record Sessao(String acesso, String refresh, UUID usuarioId) {
	}

	@Test
	void apagaTudoDoUsuarioSemTocarNoCatalogoNemEmOutrosUsuarios() throws Exception {
		Exercicio global = exercicioDe(null, "Agachamento do catalogo");
		Exercicio deOutro = exercicioDe(usuarios.findById(UUID.fromString(UsuarioDeTeste.ID)).orElseThrow(), "Exercicio de outro usuario");
		Sessao maria = criarContaComSenha(EMAIL);
		popular(maria);
		assertThat(planos.findByUsuarioIdOrderByCriadoEmDesc(maria.usuarioId())).hasSize(1);
		assertThat(sessoes.findByUsuarioId(maria.usuarioId())).hasSize(1);
		assertThat(xp.findByUsuarioId(maria.usuarioId())).isNotEmpty();
		assertThat(exercicios.findByUsuarioId(maria.usuarioId())).hasSize(1);
		assertThat(refreshTokens.findAll()).isNotEmpty();

		excluir(maria, "{\"senha\":\"" + SENHA + "\"}").andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

		assertThat(usuarios.findById(maria.usuarioId())).isEmpty();
		assertThat(planos.findByUsuarioIdOrderByCriadoEmDesc(maria.usuarioId())).isEmpty();
		assertThat(sessoes.findByUsuarioId(maria.usuarioId())).isEmpty();
		assertThat(xp.findByUsuarioId(maria.usuarioId())).isEmpty();
		assertThat(exercicios.findByUsuarioId(maria.usuarioId())).isEmpty();
		assertThat(refreshTokens.findAll()).isEmpty();
		assertThat(tokensEmail.findAll()).isEmpty();
		assertThat(emails.findAll().stream().filter(e -> e.getDestinatario().equals(EMAIL))).isEmpty();
		// O que nao e dele continua la.
		assertThat(exercicios.findById(global.getId())).isPresent();
		assertThat(exercicios.findById(deOutro.getId())).isPresent();
		assertThat(usuarios.findById(UUID.fromString(UsuarioDeTeste.ID))).isPresent();
	}

	@Test
	void depoisDeExcluirOLoginAntigoNaoValeMaisEOEmailFicaLivre() throws Exception {
		Sessao maria = criarContaComSenha(EMAIL);
		excluir(maria, "{\"senha\":\"" + SENHA + "\"}").andExpect(status().isNoContent());

		// O access token ainda nao venceu, mas o dono nao existe mais.
		mvc.perform(get("/api/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + maria.acesso())).andExpect(status().isUnauthorized());
		mvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh").cookie(new Cookie("refresh_token", maria.refresh())))
				.andExpect(status().isUnauthorized());
		post("/api/auth/login", "{\"email\":\"" + EMAIL + "\",\"senha\":\"" + SENHA + "\"}").andExpect(status().isUnauthorized());
		// Quem excluiu pode se cadastrar de novo.
		post("/api/auth/cadastro", "{\"nome\":\"Maria\",\"email\":\"" + EMAIL + "\",\"senha\":\"" + SENHA + "\"}").andExpect(status().isCreated());
	}

	@Test
	void senhaErradaOuAusenteNaoApagaNada() throws Exception {
		Sessao maria = criarContaComSenha(EMAIL);
		popular(maria);

		excluir(maria, "{\"senha\":\"errada-errada\"}").andExpect(status().isUnprocessableContent());
		excluir(maria, "{}").andExpect(status().isUnprocessableContent());
		excluir(maria, "{\"senha\":\"\",\"codigo\":\"\"}").andExpect(status().isUnprocessableContent());

		assertThat(usuarios.findById(maria.usuarioId())).isPresent();
		assertThat(planos.findByUsuarioIdOrderByCriadoEmDesc(maria.usuarioId())).hasSize(1);
		assertThat(sessoes.findByUsuarioId(maria.usuarioId())).hasSize(1);
	}

	@Test
	void cincoSenhasErradasBloqueiamATentativa() throws Exception {
		Sessao maria = criarContaComSenha(EMAIL);

		for (int i = 0; i < 5; i++) {
			excluir(maria, "{\"senha\":\"errada-errada\"}").andExpect(status().isUnprocessableContent());
		}
		// Bloqueado: nem a senha certa passa, e a conta continua la.
		excluir(maria, "{\"senha\":\"" + SENHA + "\"}").andExpect(status().isTooManyRequests());
		assertThat(usuarios.findById(maria.usuarioId())).isPresent();
	}

	@Test
	void exigeEstarLogado() throws Exception {
		post("/api/auth/excluir-conta", "{\"senha\":\"" + SENHA + "\"}").andExpect(status().isUnauthorized());
	}

	@Test
	void contaSoDoGoogleConfirmaComUmLoginNovoNoGoogleDaMesmaConta() throws Exception {
		when(google.verificarCodigo("entrada")).thenReturn(new Identidade("sub-1", EMAIL, true, "Maria"));
		when(google.verificarCodigo("outra-conta")).thenReturn(new Identidade("sub-2", "maria@teste.com", true, "Outra"));
		when(google.verificarCodigo("mesma-conta")).thenReturn(new Identidade("sub-1", EMAIL, true, "Maria"));
		MvcResult entrou = post("/api/auth/google", "{\"codigo\":\"entrada\"}").andExpect(status().isOk()).andReturn();
		Sessao maria = new Sessao(JsonPath.read(entrou.getResponse().getContentAsString(), "$.acesso"),
				entrou.getResponse().getCookie("refresh_token").getValue(), usuarios.findByEmail(EMAIL).orElseThrow().getId());

		// Conta sem senha: a senha nao serve, e o Google de outra conta tambem nao.
		excluir(maria, "{\"senha\":\"" + SENHA + "\"}").andExpect(status().isUnprocessableContent());
		excluir(maria, "{\"codigo\":\"outra-conta\"}").andExpect(status().isForbidden());
		assertThat(usuarios.findById(maria.usuarioId())).isPresent();

		excluir(maria, "{\"codigo\":\"mesma-conta\"}").andExpect(status().isNoContent());
		assertThat(usuarios.findById(maria.usuarioId())).isEmpty();
	}

	// --- apoio ---

	/** Cria a conta pelo cadastro de verdade, confirma o e-mail pelo link da fila e entra. */
	Sessao criarContaComSenha(String email) throws Exception {
		post("/api/auth/cadastro", "{\"nome\":\"Maria\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}").andExpect(status().isCreated());
		EmailPendente link = emails.findAll().stream().filter(e -> e.getDestinatario().equals(email))
				.max(Comparator.comparing(EmailPendente::getCriadoEm)).orElseThrow();
		Matcher m = TOKEN.matcher(link.getCorpo());
		assertThat(m.find()).isTrue();
		post("/api/auth/confirmar-email", "{\"token\":\"" + m.group(1) + "\"}").andExpect(status().isNoContent());
		MvcResult entrou = post("/api/auth/login", "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}").andExpect(status().isOk()).andReturn();
		return new Sessao(JsonPath.read(entrou.getResponse().getContentAsString(), "$.acesso"),
				entrou.getResponse().getCookie("refresh_token").getValue(), usuarios.findByEmail(email).orElseThrow().getId());
	}

	/** Um plano usando um exercicio proprio e um treino concluido com 3 series (gera XP), tudo pela API. */
	void popular(Sessao s) throws Exception {
		String exercicio = JsonPath.read(comToken(s, MockMvcRequestBuilders.post("/api/exercicios"),
				"{\"nome\":\"Supino proprio\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}").andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");
		String item = "{\"exercicioId\":\"%s\",\"series\":3,\"repsMin\":8,\"repsMax\":12,\"cargaAlvo\":60}".formatted(exercicio);
		String plano = JsonPath.read(comToken(s, MockMvcRequestBuilders.post("/api/planos"),
				"{\"nome\":\"ABC\",\"objetivo\":\"HIPERTROFIA\",\"dias\":[{\"nome\":\"Treino A\",\"exercicios\":[%s]}]}".formatted(item))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
		Instant inicio = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
		UUID sessao = UUID.randomUUID();
		String series = String.join(",", serie(), serie(), serie());
		String corpo = """
				{"planoId":"%s","planoNome":"ABC","diaNome":"Treino A","diaOrdem":0,"estado":"CONCLUIDA",
				 "iniciadaEm":"%s","finalizadaEm":"%s","atualizadoEm":"%s",
				 "exercicios":[{"id":"%s","exercicioId":"%s","metaSeries":3,"metaRepsMin":8,"metaRepsMax":12,
				   "metaCarga":60,"metaDescansoSegundos":90,"series":[%s]}]}
				""".formatted(plano, inicio, inicio.plusSeconds(60), inicio, UUID.randomUUID(), exercicio, series);
		comToken(s, MockMvcRequestBuilders.put("/api/sessoes/" + sessao), corpo).andExpect(status().isOk());
	}

	static String serie() {
		return "{\"id\":\"%s\",\"reps\":8,\"carga\":60,\"concluida\":true}".formatted(UUID.randomUUID());
	}

	Exercicio exercicioDe(com.trainingapp.api.model.Usuario dono, String nome) {
		Exercicio e = new Exercicio();
		e.setNome(nome);
		e.setGrupoMuscular(GrupoMuscular.PEITO);
		e.setEquipamento(Equipamento.BARRA);
		e.setUsuario(dono);
		return exercicios.saveAndFlush(e);
	}

	ResultActions excluir(Sessao s, String corpo) throws Exception {
		return comToken(s, MockMvcRequestBuilders.post("/api/auth/excluir-conta"), corpo);
	}

	ResultActions comToken(Sessao s, MockHttpServletRequestBuilder req, String corpo) throws Exception {
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, "Bearer " + s.acesso()).contentType(MediaType.APPLICATION_JSON).content(corpo));
	}

	ResultActions post(String caminho, String corpo) throws Exception {
		String ip = "10.8.%d.%d".formatted(IPS.incrementAndGet() / 250, IPS.get() % 250);
		return mvc.perform(MockMvcRequestBuilders.post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo)
				.with(r -> {
					r.setRemoteAddr(ip);
					return r;
				}));
	}
}
