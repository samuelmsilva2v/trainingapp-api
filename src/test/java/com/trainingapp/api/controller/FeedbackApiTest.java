package com.trainingapp.api.controller;

import com.trainingapp.api.UsuarioDeTeste;
import com.trainingapp.api.model.CategoriaFeedback;
import com.trainingapp.api.model.EmailPendente;
import com.trainingapp.api.model.Feedback;
import com.trainingapp.api.repository.EmailPendenteRepository;
import com.trainingapp.api.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
class FeedbackApiTest {

	static final String MENSAGEM = "Ao concluir o treino a tela ficou branca e o resumo nao apareceu, tive que fechar o app.";

	@Autowired
	MockMvc mvc;
	@Autowired
	FeedbackRepository feedbacks;
	@Autowired
	EmailPendenteRepository emails;

	@Test
	void gravaORelatoEEnfileiraOEmailComReplyToEContexto() throws Exception {
		enviar("""
				{"nome":"Atleta","categoria":"BUG","mensagem":"%s","plataforma":"android","versaoApp":"0.3.0",
				 "tela":"/treino","userAgent":"Mozilla/5.0 (Linux; Android 14)"}""".formatted(MENSAGEM)).andExpect(status().isNoContent());

		Feedback f = feedbacks.findAll().getFirst();
		assertThat(f.getCategoria()).isEqualTo(CategoriaFeedback.BUG);
		assertThat(f.getVersaoApp()).isEqualTo("0.3.0");
		assertThat(f.getTela()).isEqualTo("/treino");

		EmailPendente email = emails.findAll().getFirst();
		assertThat(email.getDestinatario()).isEqualTo("suporte@teste.com");
		assertThat(email.getResponderPara()).isEqualTo(UsuarioDeTeste.EMAIL);
		assertThat(email.getAssunto()).isEqualTo("[Bug] Ao concluir o treino a tela ficou branca e o resumo nao apar…");
		assertThat(email.getCorpo()).contains("Nome: Atleta", "E-mail da conta: " + UsuarioDeTeste.EMAIL, MENSAGEM,
				"Plataforma: android", "Versão do app: 0.3.0", "Tela: /treino", "User agent: Mozilla/5.0 (Linux; Android 14)");
	}

	@Test
	void semContextoOCorpoMostraTraco() throws Exception {
		enviar("{\"nome\":\"Atleta\",\"categoria\":\"OUTRO\",\"mensagem\":\"Mensagem curta\\nem duas linhas\"}")
				.andExpect(status().isNoContent());

		EmailPendente email = emails.findAll().getFirst();
		assertThat(email.getAssunto()).isEqualTo("[Outro] Mensagem curta em duas linhas");
		assertThat(email.getCorpo()).contains("Versão do app: -", "Tela: -");
	}

	@Test
	void camposInvalidosDao400ENaoGravam() throws Exception {
		String valido = "\"nome\":\"Atleta\",\"categoria\":\"MELHORIA\",\"mensagem\":\"Uma sugestao qualquer\"";
		enviar("{\"nome\":\" \",\"categoria\":\"MELHORIA\",\"mensagem\":\"Uma sugestao qualquer\"}").andExpect(status().isBadRequest());
		enviar("{\"nome\":\"" + "a".repeat(101) + "\",\"categoria\":\"MELHORIA\",\"mensagem\":\"Uma sugestao qualquer\"}")
				.andExpect(status().isBadRequest());
		enviar("{\"nome\":\"Atleta\",\"mensagem\":\"Uma sugestao qualquer\"}").andExpect(status().isBadRequest());
		enviar("{\"nome\":\"Atleta\",\"categoria\":\"ELOGIO\",\"mensagem\":\"Uma sugestao qualquer\"}").andExpect(status().isBadRequest());
		enviar("{\"nome\":\"Atleta\",\"categoria\":\"MELHORIA\",\"mensagem\":\"curta\"}").andExpect(status().isBadRequest());
		enviar("{\"nome\":\"Atleta\",\"categoria\":\"MELHORIA\",\"mensagem\":\"" + "a".repeat(2001) + "\"}").andExpect(status().isBadRequest());
		enviar("{" + valido + ",\"plataforma\":\"ios\"}").andExpect(status().isBadRequest());
		enviar("{" + valido + ",\"versaoApp\":\"" + "1".repeat(51) + "\"}").andExpect(status().isBadRequest());
		enviar("{" + valido + ",\"tela\":\"/" + "a".repeat(200) + "\"}").andExpect(status().isBadRequest());
		enviar("{" + valido + ",\"userAgent\":\"" + "a".repeat(501) + "\"}").andExpect(status().isBadRequest());

		assertThat(feedbacks.findAll()).isEmpty();
		assertThat(emails.findAll()).isEmpty();
	}

	@Test
	void sextoRelatoDoDiaDa429() throws Exception {
		String corpo = "{\"nome\":\"Atleta\",\"categoria\":\"FUNCIONALIDADE\",\"mensagem\":\"Queria exportar os treinos\"}";
		for (int i = 0; i < 5; i++) {
			enviar(corpo).andExpect(status().isNoContent());
		}
		enviar(corpo).andExpect(status().isTooManyRequests());

		assertThat(feedbacks.findAll()).hasSize(5);
		assertThat(emails.findAll()).hasSize(5);
	}

	@Test
	void perfilDizQueOFeedbackEstaAtivo() throws Exception {
		mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.feedbackAtivo").value(true));
	}

	private ResultActions enviar(String corpo) throws Exception {
		return mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON).content(corpo));
	}
}
