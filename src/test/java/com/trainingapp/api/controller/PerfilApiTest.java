package com.trainingapp.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import com.trainingapp.api.UsuarioDeTeste;
import org.springframework.security.test.context.support.WithMockUser;
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
@WithMockUser(username = UsuarioDeTeste.ID)
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
	void guardaOLembreteDePesagemEOPerfilInteiroSubstitui() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Samuel\",\"lembretePesoDias\":15}"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.lembretePesoDias").value(15));

		// Campo ausente vira nulo: lembrete desligado.
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Samuel\"}"))
				.andExpect(jsonPath("$.lembretePesoDias").isEmpty());
	}

	@Test
	void lembreteForaDe7_15_30Retorna400() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"S\",\"lembretePesoDias\":10}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nomeEmBrancoRetorna400() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\" \"}"))
				.andExpect(status().isBadRequest());
	}
}
