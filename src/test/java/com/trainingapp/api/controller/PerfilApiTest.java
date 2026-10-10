package com.trainingapp.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PerfilApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void usuarioPadraoExisteSemFusoHorario() throws Exception {
		mvc.perform(get("/api/perfil"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Atleta"))
				.andExpect(jsonPath("$.fusoHorario").doesNotExist());
	}

	@Test
	void atualizaNome() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Samuel\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Samuel"));
	}

	@Test
	void clienteAntigoQueAindaEnviaFusoNaoQuebra() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Samuel\",\"fusoHorario\":\"America/Manaus\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fusoHorario").doesNotExist());
	}

	@Test
	void nomeEmBrancoRetorna400() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\" \"}"))
				.andExpect(status().isBadRequest());
	}
}
