package com.trainingapp.api.controller;

import com.trainingapp.api.UsuarioDeTeste;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sem TRAININGAPP_FEEDBACK_EMAIL o feedback fica desligado. */
@SpringBootTest(properties = "trainingapp.feedback.email=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
class FeedbackDesligadoApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void perfilDizDesligadoEOPostDa404() throws Exception {
		mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.feedbackAtivo").value(false));
		mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Atleta\",\"categoria\":\"BUG\",\"mensagem\":\"O app travou no treino\"}"))
				.andExpect(status().isNotFound());
	}
}
