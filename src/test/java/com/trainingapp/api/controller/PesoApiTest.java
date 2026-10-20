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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
class PesoApiTest {

	@Autowired
	MockMvc mvc;

	LocalDate hoje = LocalDate.now();

	@Test
	void registrarDeNovoNoMesmoDiaSubstituiEListaEmOrdemDeData() throws Exception {
		registrar(hoje, "80.5").andExpect(status().isOk()).andExpect(jsonPath("$.kg").value(80.5));
		registrar(hoje, "79.95").andExpect(status().isOk());
		registrar(hoje.minusDays(10), "82").andExpect(status().isOk());

		mvc.perform(get("/api/peso"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].data").value(hoje.minusDays(10).toString()))
				.andExpect(jsonPath("$[0].kg").value(82))
				.andExpect(jsonPath("$[1].data").value(hoje.toString()))
				.andExpect(jsonPath("$[1].kg").value(79.95));
	}

	@Test
	void dataFuturaDa422() throws Exception {
		registrar(hoje.plusDays(1), "80").andExpect(status().isUnprocessableContent());
	}

	@Test
	void pesoForaDaFaixaOuComMuitasCasasDa400() throws Exception {
		registrar(hoje, "19.99").andExpect(status().isBadRequest());
		registrar(hoje, "400.01").andExpect(status().isBadRequest());
		registrar(hoje, "80.123").andExpect(status().isBadRequest());
		mvc.perform(put("/api/peso/" + hoje).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/peso")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void excluiOdiaEExcluirDeNovoNaoFalha() throws Exception {
		registrar(hoje, "80").andExpect(status().isOk());

		mvc.perform(delete("/api/peso/" + hoje)).andExpect(status().isNoContent());
		mvc.perform(delete("/api/peso/" + hoje)).andExpect(status().isNoContent());
		mvc.perform(get("/api/peso")).andExpect(jsonPath("$.length()").value(0));
	}

	private ResultActions registrar(LocalDate data, String kg) throws Exception {
		return mvc.perform(put("/api/peso/" + data).contentType(MediaType.APPLICATION_JSON).content("{\"kg\":" + kg + "}"));
	}
}
