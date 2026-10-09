package com.trainingapp.api.exercicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExercicioApiTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	CatalogoImportador importador;

	@BeforeEach
	void carregarCatalogo() {
		importador.importar();
	}

	@Test
	void buscaIgnoraAcentoECaixa() throws Exception {
		mvc.perform(get("/api/exercicios").param("busca", "LIBERACAO miofascial do quadriceps"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nome").value("Liberação miofascial do quadríceps"))
				.andExpect(jsonPath("$[0].personalizado").value(false));
	}

	@Test
	void filtraPorGrupoMuscular() throws Exception {
		mvc.perform(get("/api/exercicios").param("grupo", "CARDIO"))
				.andExpect(jsonPath("$.length()").value(14));
	}

	@Test
	void exercicioPersonalizadoApareceNaListagem() throws Exception {
		mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Sled push\",\"grupoMuscular\":\"CORPO_INTEIRO\",\"equipamento\":\"OUTRO\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.personalizado").value(true));

		mvc.perform(get("/api/exercicios").param("busca", "sled push"))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void exercicioSemNomeRetorna400() throws Exception {
		mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\" \",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}"))
				.andExpect(status().isBadRequest());
	}
}
