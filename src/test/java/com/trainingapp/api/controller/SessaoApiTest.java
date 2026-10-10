package com.trainingapp.api.controller;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
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
class SessaoApiTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	EntityManager em;

	String exercicio;
	String plano;

	@BeforeEach
	void prepararPlanoComDoisDias() throws Exception {
		exercicio = JsonPath.read(mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}"))
				.andReturn().getResponse().getContentAsString(), "$.id");
		String item = "{\"exercicioId\":\"%s\",\"series\":3,\"repsMin\":8,\"repsMax\":12,\"cargaAlvo\":60}"
				.formatted(exercicio);
		String corpo = """
				{"nome":"ABC","objetivo":"HIPERTROFIA","dias":[
				  {"nome":"Treino A","exercicios":[%s]},
				  {"nome":"Treino B","exercicios":[%s]}]}
				""".formatted(item, item);
		plano = JsonPath.read(mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	@Test
	void historicoNaoFazUmaQueryPorTreino() throws Exception {
		for (int i = 0; i < 10; i++) {
			concluir(UUID.randomUUID(), 0, "Treino A");
		}
		em.flush();
		em.clear();
		var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		stats.clear();

		mvc.perform(get("/api/sessoes")).andExpect(status().isOk());

		// Sem batch fetch sao 1 + 10 (exercicios) + 10 (series) queries.
		org.junit.jupiter.api.Assertions.assertTrue(stats.getPrepareStatementCount() < 10,
				"queries: " + stats.getPrepareStatementCount());
	}

	@Test
	void hojeSemPlanoAtivoVemVazio() throws Exception {
		mvc.perform(delete("/api/planos/" + plano)).andExpect(status().isNoContent());

		mvc.perform(get("/api/hoje"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planoId").doesNotExist())
				.andExpect(jsonPath("$.proximoDia").doesNotExist());
	}

	@Test
	void hojeMostraOPrimeiroDiaEConcluirAvancaARotacaoEDaAVolta() throws Exception {
		mvc.perform(get("/api/hoje"))
				.andExpect(jsonPath("$.planoNome").value("ABC"))
				.andExpect(jsonPath("$.totalDias").value(2))
				.andExpect(jsonPath("$.proximoDia.nome").value("Treino A"))
				.andExpect(jsonPath("$.sessaoEmAndamento").doesNotExist());

		concluir(UUID.randomUUID(), 0, "Treino A");
		mvc.perform(get("/api/hoje")).andExpect(jsonPath("$.proximoDia.nome").value("Treino B"));

		concluir(UUID.randomUUID(), 1, "Treino B");
		mvc.perform(get("/api/hoje")).andExpect(jsonPath("$.proximoDia.nome").value("Treino A"));
	}

	@Test
	void abandonarNaoAvancaARotacao() throws Exception {
		salvar(UUID.randomUUID(), "ABANDONADA", agora(), agora(), serie(UUID.randomUUID(), 8, "60", false))
				.andExpect(status().isOk());

		mvc.perform(get("/api/hoje")).andExpect(jsonPath("$.proximoDia.nome").value("Treino A"));
	}

	@Test
	void pularAvancaARotacaoSemRegistrarTreino() throws Exception {
		mvc.perform(post("/api/hoje/pular"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.proximoDia.nome").value("Treino B"));
		mvc.perform(get("/api/sessoes")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void naoPulaComTreinoEmAndamento() throws Exception {
		salvar(UUID.randomUUID(), "EM_ANDAMENTO", agora(), null, serie(UUID.randomUUID(), null, null, false))
				.andExpect(status().isOk());

		mvc.perform(post("/api/hoje/pular")).andExpect(status().isUnprocessableContent());
	}

	@Test
	void salvarEhIdempotenteEAceitaReenvioDaSessaoInteiraComOsMesmosIds() throws Exception {
		UUID sessao = UUID.randomUUID();
		UUID exSessao = UUID.randomUUID();
		UUID serie1 = UUID.randomUUID();
		UUID serie2 = UUID.randomUUID();
		Instant inicio = agora();

		salvar(sessao, exSessao, "EM_ANDAMENTO", inicio, inicio, null, serie(serie1, null, null, false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercicios[0].series.length()").value(1));

		// Reenvio igual: continua uma sessao com uma serie.
		salvar(sessao, exSessao, "EM_ANDAMENTO", inicio, inicio, null, serie(serie1, null, null, false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercicios[0].series.length()").value(1));

		// Reenvio com a serie preenchida e uma nova, reaproveitando ids.
		salvar(sessao, exSessao, "EM_ANDAMENTO", inicio, inicio.plusSeconds(60), null,
				serie(serie1, 10, "62.5", true), serie(serie2, null, null, false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercicios[0].series.length()").value(2))
				.andExpect(jsonPath("$.exercicios[0].series[0].reps").value(10))
				.andExpect(jsonPath("$.exercicios[0].series[0].carga").value(62.5))
				.andExpect(jsonPath("$.exercicios[0].series[0].concluida").value(true));

		mvc.perform(get("/api/sessoes/" + sessao))
				.andExpect(jsonPath("$.exercicios[0].series[1].id").value(serie2.toString()));
	}

	@Test
	void escritaMaisAntigaQueASalvaEhIgnorada() throws Exception {
		UUID sessao = UUID.randomUUID();
		UUID exSessao = UUID.randomUUID();
		UUID s = UUID.randomUUID();
		Instant inicio = agora();

		salvar(sessao, exSessao, "EM_ANDAMENTO", inicio, inicio.plusSeconds(120), null, serie(s, 12, "70", true))
				.andExpect(status().isOk());
		salvar(sessao, exSessao, "EM_ANDAMENTO", inicio, inicio.plusSeconds(30), null, serie(s, 5, "20", true))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercicios[0].series[0].reps").value(12));
	}

	@Test
	void sessaoConcluidaNaoMudaMaisEReenvioNaoAvancaARotacaoDeNovo() throws Exception {
		UUID sessao = UUID.randomUUID();
		concluir(sessao, 0, "Treino A");

		salvar(sessao, "ABANDONADA", agora(), agora(), serie(UUID.randomUUID(), 8, "60", true))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CONCLUIDA"));
		concluir(sessao, 0, "Treino A");

		mvc.perform(get("/api/hoje")).andExpect(jsonPath("$.proximoDia.nome").value("Treino B"));
	}

	@Test
	void soUmaSessaoEmAndamentoPorVez() throws Exception {
		salvar(UUID.randomUUID(), "EM_ANDAMENTO", agora(), null, serie(UUID.randomUUID(), null, null, false))
				.andExpect(status().isOk());

		salvar(UUID.randomUUID(), "EM_ANDAMENTO", agora(), null, serie(UUID.randomUUID(), null, null, false))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void naoRegistraTreinoRetroativoNemSessaoInconsistente() throws Exception {
		Instant ontem = agora().minus(2, ChronoUnit.DAYS);
		salvar(UUID.randomUUID(), "CONCLUIDA", ontem, ontem.plusSeconds(3600), serie(UUID.randomUUID(), 8, "60", true))
				.andExpect(status().isUnprocessableContent());

		// Fim no futuro (XP farmavel em dias futuros).
		salvar(UUID.randomUUID(), "CONCLUIDA", agora(), agora().plus(2, ChronoUnit.DAYS), serie(UUID.randomUUID(), 8, "60", true))
				.andExpect(status().isUnprocessableContent());

		// atualizadoEm no futuro (travaria as proximas escritas).
		salvar(UUID.randomUUID(), UUID.randomUUID(), "EM_ANDAMENTO", agora(), agora().plus(2, ChronoUnit.DAYS), null,
				serie(UUID.randomUUID(), 8, "60", true)).andExpect(status().isUnprocessableContent());

		// Concluida sem finalizadaEm.
		salvar(UUID.randomUUID(), "CONCLUIDA", agora(), null, serie(UUID.randomUUID(), 8, "60", true))
				.andExpect(status().isUnprocessableContent());

		// Serie concluida sem repeticao.
		salvar(UUID.randomUUID(), "EM_ANDAMENTO", agora(), null, serie(UUID.randomUUID(), null, "60", true))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void historicoListaSoEncerradasComTotais() throws Exception {
		salvar(UUID.randomUUID(), "CONCLUIDA", agora(), agora().plusSeconds(60),
				serie(UUID.randomUUID(), 10, "50", true), serie(UUID.randomUUID(), 8, "60", true),
				serie(UUID.randomUUID(), null, null, false))
				.andExpect(status().isOk());
		salvar(UUID.randomUUID(), "EM_ANDAMENTO", agora(), null, serie(UUID.randomUUID(), null, null, false))
				.andExpect(status().isOk());

		mvc.perform(get("/api/sessoes"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].estado").value("CONCLUIDA"))
				.andExpect(jsonPath("$[0].seriesConcluidas").value(2))
				.andExpect(jsonPath("$[0].volumeKg").value(980.0));
	}

	@Test
	void sessaoGuardaSnapshotEExcluirOPlanoNaoApagaOHistorico() throws Exception {
		UUID sessao = UUID.randomUUID();
		concluir(sessao, 0, "Treino A");

		mvc.perform(delete("/api/planos/" + plano)).andExpect(status().isNoContent());

		mvc.perform(get("/api/sessoes/" + sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planoNome").value("ABC"))
				.andExpect(jsonPath("$.diaNome").value("Treino A"))
				.andExpect(jsonPath("$.exercicios[0].nome").value("Supino"));
	}

	@Test
	void rejeitaExercicioInexistenteESessaoDesconhecidaDa404() throws Exception {
		mvc.perform(put("/api/sessoes/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(UUID.randomUUID(), UUID.randomUUID(), "EM_ANDAMENTO", agora(), agora(), null,
						UUID.randomUUID(), serie(UUID.randomUUID(), null, null, false))))
				.andExpect(status().isUnprocessableContent());

		mvc.perform(get("/api/sessoes/" + UUID.randomUUID())).andExpect(status().isNotFound());
	}

	// --- helpers ---

	private void concluir(UUID sessao, int diaOrdem, String diaNome) throws Exception {
		Instant inicio = agora();
		mvc.perform(put("/api/sessoes/" + sessao).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(sessao, UUID.randomUUID(), "CONCLUIDA", inicio, inicio.plusSeconds(60),
						inicio.plusSeconds(60), UUID.fromString(exercicio), diaOrdem, diaNome,
						serie(UUID.randomUUID(), 10, "60", true))))
				.andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions salvar(UUID sessao, String estado, Instant inicio,
			Instant fim, String... series) throws Exception {
		return salvar(sessao, UUID.randomUUID(), estado, inicio, inicio, fim, series);
	}

	private org.springframework.test.web.servlet.ResultActions salvar(UUID sessao, UUID exSessao, String estado,
			Instant inicio, Instant atualizado, Instant fim, String... series) throws Exception {
		return mvc.perform(put("/api/sessoes/" + sessao).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(sessao, exSessao, estado, inicio, atualizado, fim, UUID.fromString(exercicio), series)));
	}

	private String corpo(UUID sessao, UUID exSessao, String estado, Instant inicio, Instant atualizado, Instant fim,
			UUID exercicioId, String... series) {
		return corpo(sessao, exSessao, estado, inicio, atualizado, fim, exercicioId, 0, "Treino A", series);
	}

	private String corpo(UUID sessao, UUID exSessao, String estado, Instant inicio, Instant atualizado, Instant fim,
			UUID exercicioId, int diaOrdem, String diaNome, String... series) {
		return """
				{"planoId":"%s","planoNome":"ABC","diaNome":"%s","diaOrdem":%d,"estado":"%s",
				 "iniciadaEm":"%s","finalizadaEm":%s,"atualizadoEm":"%s",
				 "exercicios":[{"id":"%s","exercicioId":"%s","metaSeries":3,"metaRepsMin":8,"metaRepsMax":12,
				   "metaCarga":60,"metaDescansoSegundos":90,"series":[%s]}]}
				""".formatted(plano, diaNome, diaOrdem, estado, inicio, fim == null ? "null" : "\"" + fim + "\"",
				atualizado, exSessao, exercicioId, String.join(",", series));
	}

	private static String serie(UUID id, Integer reps, String carga, boolean concluida) {
		return "{\"id\":\"%s\",\"reps\":%s,\"carga\":%s,\"concluida\":%s}".formatted(id, reps, carga, concluida);
	}

	private static Instant agora() {
		return Instant.now().truncatedTo(ChronoUnit.SECONDS);
	}
}
