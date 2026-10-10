package com.trainingapp.api.controller;

import com.trainingapp.api.model.Plano;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
class GamificacaoApiTest {

	// Com 25h de diferenca, estes fusos nunca estao no mesmo dia: simulam treinos em dias distintos
	// sem precisar de registro retroativo.
	private static final String FUSO_ADIANTADO = "Pacific/Kiritimati";
	private static final String FUSO_ATRASADO = "Pacific/Pago_Pago";

	@Autowired
	MockMvc mvc;

	String supino;
	// Cada sessao comeca alguns segundos depois da anterior, para a ordem do historico ser estavel.
	int deslocamento;

	@BeforeEach
	void criarExercicio() throws Exception {
		supino = JsonPath.read(mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}"))
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	@Test
	void sessaoValidaRende50MaisCincoPorSerieEApareceNoResumo() throws Exception {
		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(65)).andExpect(jsonPath("$.recordes.length()").value(0));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpTotal").value(65))
				.andExpect(jsonPath("$.nivel").value(1))
				.andExpect(jsonPath("$.xpInicioDoNivel").value(0))
				.andExpect(jsonPath("$.xpProximoNivel").value(283))
				.andExpect(jsonPath("$.streakAtual").value(1))
				.andExpect(jsonPath("$.treinos").value(1))
				.andExpect(jsonPath("$.treinouHoje").value(true))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='PRIMEIRO_TREINO')].conquistada").value(true))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='TREINOS_10')].conquistada").value(false));
	}

	@Test
	void usuarioNovoTemTudoZerado() throws Exception {
		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(0))
				.andExpect(jsonPath("$.nivel").value(1))
				.andExpect(jsonPath("$.streakAtual").value(0))
				.andExpect(jsonPath("$.treinouHoje").value(false));
	}

	@Test
	void menosDeTresSeriesNaoRendeXp() throws Exception {
		concluir(2, "60").andExpect(jsonPath("$.xpGanho").value(0));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(0))
				.andExpect(jsonPath("$.streakAtual").value(0));
	}

	@Test
	void soAPrimeiraSessaoDoDiaConta() throws Exception {
		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(65));
		concluir(5, "80").andExpect(jsonPath("$.xpGanho").value(0));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(65))
				.andExpect(jsonPath("$.treinos").value(1))
				.andExpect(jsonPath("$.recordes").value(0));
	}

	@Test
	void recordeExigeMarcaAnteriorEMaiorCarga() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		// Primeira vez no exercicio: nao ha marca para superar.
		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(65)).andExpect(jsonPath("$.recordes.length()").value(0));

		definirFuso(FUSO_ATRASADO);
		concluir(3, "70")
				.andExpect(jsonPath("$.xpGanho").value(90))
				.andExpect(jsonPath("$.recordes[0]").value(supino));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(155))
				.andExpect(jsonPath("$.recordes").value(1))
				.andExpect(jsonPath("$.treinos").value(2))
				.andExpect(jsonPath("$.melhorStreak").value(2))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='PRIMEIRO_RECORDE')].conquistada").value(true));
	}

	@Test
	void mesmoExercicioDuasVezesNoDiaPagaUmRecordeSo() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		concluir(3, "60");
		definirFuso(FUSO_ATRASADO);

		Instant inicio = agora().plusSeconds(100);
		String bloco = """
				{\"id\":\"%s\",\"exercicioId\":\"%s\",\"metaSeries\":3,\"metaRepsMin\":8,\"metaRepsMax\":12,
				 \"metaCarga\":null,\"metaDescansoSegundos\":null,\"series\":[%s]}""";
		String series = "{\"id\":\"%s\",\"reps\":8,\"carga\":70,\"concluida\":true}";
		String corpo = """
				{\"planoId\":null,\"planoNome\":\"Plano\",\"diaNome\":\"Treino A\",\"diaOrdem\":0,\"estado\":\"CONCLUIDA\",
				 \"iniciadaEm\":\"%s\",\"finalizadaEm\":\"%s\",\"atualizadoEm\":\"%s\",\"exercicios\":[%s,%s]}"""
				.formatted(inicio, inicio.plusSeconds(60), inicio.plusSeconds(60),
						bloco.formatted(UUID.randomUUID(), supino,
								series.formatted(UUID.randomUUID()) + "," + series.formatted(UUID.randomUUID())),
						bloco.formatted(UUID.randomUUID(), supino,
								series.formatted(UUID.randomUUID()) + "," + series.formatted(UUID.randomUUID())));

		// 50 + 4 series x 5 + um unico recorde (25).
		mvc.perform(put("/api/sessoes/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpGanho").value(95))
				.andExpect(jsonPath("$.recordes.length()").value(1));
	}

	@Test
	void cargaIgualOuMenorQueAMarcaNaoEhRecorde() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		concluir(3, "60");
		definirFuso(FUSO_ATRASADO);

		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(65)).andExpect(jsonPath("$.recordes.length()").value(0));
	}

	@Test
	void historicoTrazXpEQuantidadeDeRecordes() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		concluir(3, "60");
		definirFuso(FUSO_ATRASADO);
		concluir(4, "70");

		mvc.perform(get("/api/sessoes"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].xpGanho").value(95))
				.andExpect(jsonPath("$[0].recordes").value(1))
				.andExpect(jsonPath("$[1].xpGanho").value(65))
				.andExpect(jsonPath("$[1].recordes").value(0));
	}

	@Test
	void apagarTreinoEstornaXpTreinosStreakERecorde() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		concluir(3, "60");
		definirFuso(FUSO_ATRASADO);
		String recorde = idDe(concluir(3, "70"));

		mvc.perform(delete("/api/sessoes/" + recorde)).andExpect(status().isNoContent());

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(65))
				.andExpect(jsonPath("$.treinos").value(1))
				.andExpect(jsonPath("$.recordes").value(0))
				.andExpect(jsonPath("$.melhorStreak").value(1));
		mvc.perform(get("/api/sessoes")).andExpect(jsonPath("$.length()").value(1));
		mvc.perform(delete("/api/sessoes/" + recorde)).andExpect(status().isNotFound());
	}

	@Test
	void apagarOTreinoDoDiaLiberaOUsoDoDiaParaRenderXpDeNovo() throws Exception {
		String id = idDe(concluir(3, "60"));
		mvc.perform(delete("/api/sessoes/" + id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(0))
				.andExpect(jsonPath("$.treinouHoje").value(false));

		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(65));
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(65));
	}

	@Test
	void naoApagaTreinoEmAndamento() throws Exception {
		Instant inicio = agora();
		UUID id = UUID.randomUUID();
		mvc.perform(put("/api/sessoes/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("EM_ANDAMENTO", inicio, null, 1, "60").replace("\"finalizadaEm\":\"null\"", "\"finalizadaEm\":null")
				.replace("\"atualizadoEm\":\"null\"", "\"atualizadoEm\":\"" + inicio + "\""))).andExpect(status().isOk());

		mvc.perform(delete("/api/sessoes/" + id)).andExpect(status().isUnprocessableContent());
	}

	@Test
	void sessaoAbandonadaNaoRendeXp() throws Exception {
		Instant inicio = agora();
		mvc.perform(put("/api/sessoes/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("ABANDONADA", inicio, inicio.plusSeconds(60), 5, "60")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpGanho").value(0));

		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(0));
	}

	// --- helpers ---

	private static String idDe(org.springframework.test.web.servlet.ResultActions r) throws Exception {
		return com.jayway.jsonpath.JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private org.springframework.test.web.servlet.ResultActions concluir(int series, String carga) throws Exception {
		Instant inicio = agora().plusSeconds(++deslocamento * 10L);
		return mvc.perform(put("/api/sessoes/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("CONCLUIDA", inicio, inicio.plusSeconds(60), series, carga)))
				.andExpect(status().isOk());
	}

	private void definirFuso(String fuso) throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Teste\",\"fusoHorario\":\"%s\"}".formatted(fuso)))
				.andExpect(status().isOk());
	}

	private String corpo(String estado, Instant inicio, Instant fim, int series, String carga) {
		List<String> lista = new ArrayList<>();
		for (int i = 0; i < series; i++) {
			lista.add("{\"id\":\"%s\",\"reps\":8,\"carga\":%s,\"concluida\":true}".formatted(UUID.randomUUID(), carga));
		}
		return """
				{"planoId":null,"planoNome":"Plano","diaNome":"Treino A","diaOrdem":0,"estado":"%s",
				 "iniciadaEm":"%s","finalizadaEm":"%s","atualizadoEm":"%s",
				 "exercicios":[{"id":"%s","exercicioId":"%s","metaSeries":3,"metaRepsMin":8,"metaRepsMax":12,
				   "metaCarga":null,"metaDescansoSegundos":null,"series":[%s]}]}
				""".formatted(estado, inicio, fim, fim, UUID.randomUUID(), supino, String.join(",", lista));
	}

	private static Instant agora() {
		return Instant.now().truncatedTo(ChronoUnit.SECONDS);
	}
}
