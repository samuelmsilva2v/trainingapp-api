package com.trainingapp.api.controller;

import com.trainingapp.api.model.Plano;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlanoApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void primeiroPlanoNasceAtivoEPreservaOrdemDeDiasEExercicios() throws Exception {
		String supino = criarExercicio("Supino");
		String agachamento = criarExercicio("Agachamento");

		String corpo = plano("ABC", """
				{"nome":"Treino A","exercicios":[%s,%s]},
				{"nome":"Treino B","exercicios":[%s]}
				""".formatted(item(supino, 4, 8, 12, "60.50"), item(agachamento, 3, 5, 5, null),
				item(agachamento, 3, 10, 12, null)));

		mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ativo").value(true))
				.andExpect(jsonPath("$.dias[0].nome").value("Treino A"))
				.andExpect(jsonPath("$.dias[0].ordem").value(0))
				.andExpect(jsonPath("$.dias[1].ordem").value(1))
				.andExpect(jsonPath("$.dias[0].exercicios[0].exercicioNome").value("Supino"))
				.andExpect(jsonPath("$.dias[0].exercicios[0].cargaAlvo").value(60.5))
				.andExpect(jsonPath("$.dias[0].exercicios[1].exercicioNome").value("Agachamento"))
				.andExpect(jsonPath("$.dias[0].exercicios[1].ordem").value(1));
	}

	@Test
	void segundoPlanoNaoEhAtivoEAtivarTrocaOPlanoAtivo() throws Exception {
		String ex = criarExercicio("Remada");
		String primeiro = criarPlano("Plano 1", ex);
		String segundo = criarPlano("Plano 2", ex);

		mvc.perform(get("/api/planos/" + segundo)).andExpect(jsonPath("$.ativo").value(false));

		mvc.perform(post("/api/planos/" + segundo + "/ativar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativo").value(true));
		mvc.perform(get("/api/planos/" + primeiro)).andExpect(jsonPath("$.ativo").value(false));
		mvc.perform(get("/api/planos")).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void atualizarSubstituiOsDias() throws Exception {
		String ex = criarExercicio("Rosca");
		String id = criarPlano("Antes", ex);

		String novo = plano("Depois", """
				{"nome":"Novo A","exercicios":[%s]},
				{"nome":"Novo B","exercicios":[]},
				{"nome":"Novo C","exercicios":[]}
				""".formatted(item(ex, 3, 10, 15, null)));

		mvc.perform(put("/api/planos/" + id).contentType(MediaType.APPLICATION_JSON).content(novo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.dias.length()").value(3))
				.andExpect(jsonPath("$.dias[2].nome").value("Novo C"));

		mvc.perform(get("/api/planos/" + id)).andExpect(jsonPath("$.dias.length()").value(3));
	}

	@Test
	void repsMinMaiorQueRepsMaxRetorna422() throws Exception {
		String ex = criarExercicio("Stiff");
		String corpo = plano("X", "{\"nome\":\"A\",\"exercicios\":[%s]}".formatted(item(ex, 3, 12, 8, null)));

		mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void exercicioInexistenteRetorna422() throws Exception {
		String corpo = plano("X", "{\"nome\":\"A\",\"exercicios\":[%s]}"
				.formatted(item("11111111-1111-1111-1111-111111111111", 3, 8, 12, null)));

		mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void corpoInvalidoRetorna400() throws Exception {
		mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"\",\"objetivo\":\"FORCA\",\"dias\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void planoInexistenteRetorna404() throws Exception {
		mvc.perform(get("/api/planos/11111111-1111-1111-1111-111111111111")).andExpect(status().isNotFound());
	}

	@Test
	void excluirRemoveOPlano() throws Exception {
		String id = criarPlano("Apagar", criarExercicio("Prancha"));

		mvc.perform(delete("/api/planos/" + id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/planos/" + id)).andExpect(status().isNotFound());
	}

	// --- helpers ---

	private String criarExercicio(String nome) throws Exception {
		String resposta = mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"%s\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}".formatted(nome)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(resposta, "$.id");
	}

	private String criarPlano(String nome, String exercicioId) throws Exception {
		String corpo = plano(nome, "{\"nome\":\"A\",\"exercicios\":[%s]}".formatted(item(exercicioId, 3, 8, 12, null)));
		String resposta = mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(resposta, "$.id");
	}

	private static String plano(String nome, String dias) {
		return "{\"nome\":\"%s\",\"objetivo\":\"HIPERTROFIA\",\"dias\":[%s]}".formatted(nome, dias);
	}

	private static String item(String exercicioId, int series, int repsMin, int repsMax, String carga) {
		return "{\"exercicioId\":\"%s\",\"series\":%d,\"repsMin\":%d,\"repsMax\":%d,\"cargaAlvo\":%s}"
				.formatted(exercicioId, series, repsMin, repsMax, carga);
	}
}
