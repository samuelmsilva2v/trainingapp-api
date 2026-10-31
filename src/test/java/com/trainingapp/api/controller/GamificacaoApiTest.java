package com.trainingapp.api.controller;

import com.trainingapp.api.model.Plano;
import com.trainingapp.api.model.TipoXp;
import com.trainingapp.api.model.XpEvent;
import com.trainingapp.api.repository.UsuarioRepository;
import com.trainingapp.api.repository.XpEventRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import com.trainingapp.api.UsuarioDeTeste;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
@Import(GamificacaoApiTest.RelogioDeTeste.class)
class GamificacaoApiTest {

	// Com 25h de diferenca, estes fusos nunca estao no mesmo dia: simulam treinos em dias distintos
	// sem precisar de registro retroativo. O fuso do sistema vem do Clock da API; aqui o teste o troca.
	private static final String FUSO_ADIANTADO = "Pacific/Kiritimati";
	private static final String FUSO_ATRASADO = "Pacific/Pago_Pago";

	/**
	 * Relogio da API parado numa quarta-feira (28/10/2026 15:00 UTC), com o fuso trocavel: os desafios semanais dependem do
	 * dia da semana, e com o relogio real o mesmo teste daria outro XP conforme o dia em que roda.
	 */
	static class RelogioMutavel extends Clock {
		static final Instant AGORA = Instant.parse("2026-10-28T15:00:00Z");

		private volatile ZoneId zona = ZoneId.systemDefault();

		void definirZona(ZoneId nova) {
			zona = nova;
		}

		@Override
		public ZoneId getZone() {
			return zona;
		}

		@Override
		public Clock withZone(ZoneId nova) {
			return Clock.fixed(AGORA, nova);
		}

		@Override
		public Instant instant() {
			return AGORA;
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class RelogioDeTeste {
		@Bean
		@Primary
		RelogioMutavel relogioDeTeste() {
			return new RelogioMutavel();
		}
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	RelogioMutavel relogio;

	@Autowired
	XpEventRepository eventos;

	@Autowired
	UsuarioRepository usuarios;

	String supino;
	// Cada sessao comeca alguns segundos depois da anterior, para a ordem do historico ser estavel.
	int deslocamento;

	@BeforeEach
	void criarExercicio() throws Exception {
		relogio.definirZona(ZoneId.systemDefault()); // o relogio e compartilhado entre os testes
		supino = JsonPath.read(mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}"))
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	@Test
	void sessaoValidaRendeXpPorSerieEApareceNoResumo() throws Exception {
		// 36 das series + 25 da conquista "Primeiro treino", creditada ao concluir.
		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(61)).andExpect(jsonPath("$.recordes.length()").value(0))
				.andExpect(jsonPath("$.xpSeries").value(36))
				.andExpect(jsonPath("$.xpConquistas").value(25))
				.andExpect(jsonPath("$.conquistas[0].codigo").value("PRIMEIRO_TREINO"))
				.andExpect(jsonPath("$.conquistas[0].nome").value("Primeiro treino"))
				.andExpect(jsonPath("$.conquistas[0].xp").value(25))
				.andExpect(jsonPath("$.exercicios[0].xp").value(36))
				.andExpect(jsonPath("$.xpRecordes").value(0));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpTotal").value(61))
				.andExpect(jsonPath("$.nivel").value(1))
				.andExpect(jsonPath("$.titulo").value("Iniciante"))
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
		concluir(2, "60").andExpect(jsonPath("$.xpGanho").value(0))
				.andExpect(jsonPath("$.exercicios[0].xp").value(0));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(0))
				.andExpect(jsonPath("$.streakAtual").value(0));
	}

	@Test
	void soAPrimeiraSessaoDoDiaConta() throws Exception {
		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(61)); // 36 + 25 de "Primeiro treino"
		concluir(5, "80").andExpect(jsonPath("$.xpGanho").value(0));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(61))
				.andExpect(jsonPath("$.treinos").value(1))
				.andExpect(jsonPath("$.recordes").value(0));
	}

	@Test
	void recordeExigeMarcaAnteriorEMaiorCarga() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		// Primeira vez no exercicio: nao ha marca para superar.
		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(61)).andExpect(jsonPath("$.recordes.length()").value(0));

		definirFuso(FUSO_ATRASADO);
		concluir(3, "70")
				// 3 x 14 + 25 do recorde + 25 de "Novo recorde" + desafios: 2 dias seguidos (20) e 1 recorde (30).
				.andExpect(jsonPath("$.xpGanho").value(142))
				.andExpect(jsonPath("$.xpDesafios").value(50))
				.andExpect(jsonPath("$.recordes[0]").value(supino));

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.xpTotal").value(203))
				.andExpect(jsonPath("$.recordes").value(1))
				.andExpect(jsonPath("$.treinos").value(2))
				.andExpect(jsonPath("$.melhorStreak").value(2))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='PRIMEIRO_RECORDE')].conquistada").value(true))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='RECORDES_VARIADOS_5')].progresso").value(1));
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

		// 4 series de 70 kg x 8 reps (14 XP cada) + um unico recorde (25) + a conquista "Novo recorde" (25)
		// + desafios: 2 dias seguidos (20) e 1 recorde (30).
		mvc.perform(put("/api/sessoes/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpGanho").value(156))
				.andExpect(jsonPath("$.recordes.length()").value(1));
	}

	@Test
	void cargaIgualOuMenorQueAMarcaNaoEhRecorde() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		concluir(3, "60");
		definirFuso(FUSO_ATRASADO);

		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(56)).andExpect(jsonPath("$.recordes.length()").value(0)); // + 2 dias seguidos
	}

	@Test
	void historicoTrazXpEQuantidadeDeRecordes() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		concluir(3, "60");
		definirFuso(FUSO_ATRASADO);
		concluir(4, "70");

		mvc.perform(get("/api/sessoes"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].xpGanho").value(156))
				.andExpect(jsonPath("$[0].xpDesafios").value(50))
				.andExpect(jsonPath("$[0].xpConquistas").value(25))
				.andExpect(jsonPath("$[0].recordes").value(1))
				.andExpect(jsonPath("$[1].xpGanho").value(61))
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
				.andExpect(jsonPath("$.xpTotal").value(136)) // 61 do 1o treino + "Novo recorde" (25) e os desafios (50), que permanecem
				.andExpect(jsonPath("$.treinos").value(1))
				.andExpect(jsonPath("$.recordes").value(0))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='PRIMEIRO_RECORDE')].conquistada").value(true))
				.andExpect(jsonPath("$.melhorStreak").value(1))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='RECORDES_VARIADOS_5')].progresso").value(0));
		mvc.perform(get("/api/sessoes")).andExpect(jsonPath("$.length()").value(1));
		mvc.perform(delete("/api/sessoes/" + recorde)).andExpect(status().isNotFound());
	}

	@Test
	void apagarOTreinoDoDiaLiberaOUsoDoDiaParaRenderXpDeNovo() throws Exception {
		String id = idDe(concluir(3, "60"));
		mvc.perform(delete("/api/sessoes/" + id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(25)) // a conquista permanece
				.andExpect(jsonPath("$.treinouHoje").value(false));

		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(36)).andExpect(jsonPath("$.xpConquistas").value(0)); // ja creditada
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(61));
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
	void treinoEmAndamentoTemPreviaExataDoXpSemGravarNada() throws Exception {
		UUID id = emAndamento(3, "60");

		mvc.perform(get("/api/sessoes/" + id))
				.andExpect(jsonPath("$.xpGanho").value(61)) // a previa inclui a conquista que o treino libera
				.andExpect(jsonPath("$.xpConquistas").value(25))
				.andExpect(jsonPath("$.exercicios[0].xp").value(36))
				.andExpect(jsonPath("$.recordes.length()").value(0));
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(0));
	}

	@Test
	void previaIncluiORecordeEZeraQuandoOTreinoDoDiaJaRendeu() throws Exception {
		definirFuso(FUSO_ADIANTADO);
		concluir(3, "60");
		definirFuso(FUSO_ATRASADO);

		UUID comRecorde = emAndamento(3, "70");
		mvc.perform(get("/api/sessoes/" + comRecorde))
				.andExpect(jsonPath("$.xpGanho").value(142)) // 3 x 14 + 25 do recorde + 25 de "Novo recorde" + desafios (50)
				.andExpect(jsonPath("$.recordes[0]").value(supino));
	}

	@Test
	void previaEhZeroQuandoOUsuarioJaTreinouHoje() throws Exception {
		concluir(3, "60");
		UUID id = emAndamento(3, "60");

		mvc.perform(get("/api/sessoes/" + id))
				.andExpect(jsonPath("$.xpGanho").value(0))
				.andExpect(jsonPath("$.exercicios[0].xp").value(0));
	}

	@Test
	void primeiroDiaDaSequenciaNaoRendeBonus() throws Exception {
		concluir(3, "60").andExpect(jsonPath("$.xpGanho").value(61)).andExpect(jsonPath("$.xpStreak").value(0));
	}

	@Test
	void bonusDeSequenciaCresceComOsDiasSeguidosEVemNaPreviaNaRespostaENoHistorico() throws Exception {
		LocalDate hoje = LocalDate.now(relogio);
		treinouEm(hoje.minusDays(3), hoje.minusDays(2), hoje.minusDays(1)); // hoje e o 4o dia: 5 x 3 = 15

		UUID id = emAndamento(3, "60");
		// 36 + 15 da sequencia + 25 de "Primeiro treino" (ja valia pelos dias gravados, credito retroativo)
		// + desafios da semana (segunda, terca e quarta): 3 treinos (50) e 2 dias seguidos (20).
		mvc.perform(get("/api/sessoes/" + id)).andExpect(jsonPath("$.xpGanho").value(146)).andExpect(jsonPath("$.xpStreak").value(15));
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(90)); // a previa nao grava nada

		concluir(id, 3, "60").andExpect(jsonPath("$.xpGanho").value(146)).andExpect(jsonPath("$.xpSeries").value(36))
				.andExpect(jsonPath("$.xpStreak").value(15));
		mvc.perform(get("/api/sessoes")).andExpect(jsonPath("$[0].xpGanho").value(146)).andExpect(jsonPath("$[0].xpStreak").value(15));
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(236)).andExpect(jsonPath("$.streakAtual").value(4))
				.andExpect(jsonPath("$.treinos").value(4)); // o bonus nao conta como dia de treino
	}

	@Test
	void folgaDeDoisDiasMantemASequenciaETresQuebram() throws Exception {
		LocalDate hoje = LocalDate.now(relogio);
		treinouEm(hoje.minusDays(3)); // 2 dias de folga: hoje e o 2o dia
		concluir(3, "60").andExpect(jsonPath("$.xpStreak").value(5));
	}

	@Test
	void folgaDeTresDiasQuebraASequencia() throws Exception {
		LocalDate hoje = LocalDate.now(relogio);
		treinouEm(hoje.minusDays(5), hoje.minusDays(4)); // 3 dias de folga: hoje recomeca no dia 1
		concluir(3, "60").andExpect(jsonPath("$.xpStreak").value(0));
	}

	@Test
	void bonusDeSequenciaTemTetoDe50() throws Exception {
		LocalDate hoje = LocalDate.now(relogio);
		treinouEm(java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(hoje::minusDays).toArray(LocalDate[]::new));
		concluir(3, "60").andExpect(jsonPath("$.xpStreak").value(50));
	}

	@Test
	void apagarOTreinoEstornaOBonusDeSequencia() throws Exception {
		LocalDate hoje = LocalDate.now(relogio);
		treinouEm(hoje.minusDays(1));
		String id = idDe(concluir(3, "60").andExpect(jsonPath("$.xpStreak").value(5)));

		mvc.perform(delete("/api/sessoes/" + id)).andExpect(status().isNoContent());
		// O dia gravado antes (30) + "Primeiro treino" (25) e o desafio "2 dias seguidos" (20), que permanecem.
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.xpTotal").value(75))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='DOIS_DIAS_SEGUIDOS')].cumprido").value(true));
	}

	@Test
	void conquistaQueSobeONivelLiberaAConquistaDeNivelEmCascata() throws Exception {
		LocalDate hoje = LocalDate.now(relogio);
		treinouEm(1070, hoje.minusDays(5)); // 1070 + 36 = 1106 (< 1118, nivel 5); com os 25 de "Primeiro treino", 1131

		concluir(3, "60")
				.andExpect(jsonPath("$.xpConquistas").value(50))
				.andExpect(jsonPath("$.xpGanho").value(86)) // 36 + 25 (Primeiro treino) + 25 (Nivel 5)
				.andExpect(jsonPath("$.conquistas[*].codigo").value(org.hamcrest.Matchers.containsInAnyOrder("PRIMEIRO_TREINO", "NIVEL_5")));
		mvc.perform(get("/api/gamificacao")).andExpect(jsonPath("$.nivel").value(5))
				.andExpect(jsonPath("$.conquistas[?(@.codigo=='NIVEL_5')].conquistada").value(true));
	}

	@Test
	void conquistasQueJaValiamSaoCreditadasNaPrimeiraConclusaoMesmoSemXpDeTreino() throws Exception {
		LocalDate hoje = LocalDate.now(relogio);
		treinouEm(java.util.stream.IntStream.rangeClosed(11, 20).mapToObj(hoje::minusDays).toArray(LocalDate[]::new));

		// 2 series nao rendem XP de treino, mas a conclusao credita o que ja valia: 25 + 50 (10 treinos) + 25 (7 dias).
		concluir(2, "60")
				.andExpect(jsonPath("$.xpSeries").value(0))
				.andExpect(jsonPath("$.xpGanho").value(100))
				.andExpect(jsonPath("$.conquistas[*].codigo")
						.value(org.hamcrest.Matchers.containsInAnyOrder("PRIMEIRO_TREINO", "TREINOS_10", "STREAK_7")));
		concluir(2, "60").andExpect(jsonPath("$.xpGanho").value(0)); // credito unico
	}

	@Test
	void desafiosDaSemanaCreditamUmaVezEAparecemNoResumo() throws Exception {
		LocalDate quarta = LocalDate.now(relogio);
		treinouEm(quarta.minusDays(2), quarta.minusDays(1)); // segunda e terca da mesma semana

		concluir(3, "60")
				.andExpect(jsonPath("$.xpDesafios").value(70)) // 3 treinos (50) + 2 dias seguidos (20)
				.andExpect(jsonPath("$.desafios[*].codigo").value(org.hamcrest.Matchers.containsInAnyOrder("TRES_TREINOS", "DOIS_DIAS_SEGUIDOS")))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='TRES_TREINOS')].nome").value("3 treinos na semana"));
		concluir(3, "60").andExpect(jsonPath("$.xpDesafios").value(0)); // uma vez por semana

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.semanaTerminaEm").value("2026-11-01"))
				.andExpect(jsonPath("$.desafios.length()").value(3))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='TRES_TREINOS')].progresso").value(3))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='TRES_TREINOS')].cumprido").value(true))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='UM_RECORDE')].progresso").value(0))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='UM_RECORDE')].cumprido").value(false));
	}

	@Test
	void recordeNaSemanaCumpreODesafio() throws Exception {
		LocalDate quarta = LocalDate.now(relogio);
		XpEvent recorde = new XpEvent(); // um recorde valido na segunda desta semana
		recorde.setUsuario(usuarios.getReferenceById(UUID.fromString(UsuarioDeTeste.ID)));
		recorde.setSessaoId(UUID.randomUUID());
		recorde.setTipo(TipoXp.RECORDE);
		recorde.setPontos(25);
		recorde.setExercicioId(UUID.fromString(supino));
		recorde.setDia(quarta.minusDays(2));
		eventos.save(recorde);

		concluir(3, "60").andExpect(jsonPath("$.desafios[*].codigo").value(org.hamcrest.Matchers.hasItem("UM_RECORDE")));
	}

	@Test
	void treinosDaSemanaPassadaNaoContamNosDesafios() throws Exception {
		LocalDate quarta = LocalDate.now(relogio);
		treinouEm(quarta.minusDays(5), quarta.minusDays(4), quarta.minusDays(3)); // sexta, sabado e domingo passados

		mvc.perform(get("/api/gamificacao"))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='TRES_TREINOS')].progresso").value(0))
				.andExpect(jsonPath("$.desafios[?(@.codigo=='DOIS_DIAS_SEGUIDOS')].progresso").value(0));
		concluir(3, "60").andExpect(jsonPath("$.xpDesafios").value(0));
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

	private UUID emAndamento(int series, String carga) throws Exception {
		Instant inicio = agora().plusSeconds(++deslocamento * 10L);
		UUID id = UUID.randomUUID();
		mvc.perform(put("/api/sessoes/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("EM_ANDAMENTO", inicio, null, series, carga).replace("\"finalizadaEm\":\"null\"", "\"finalizadaEm\":null")
				.replace("\"atualizadoEm\":\"null\"", "\"atualizadoEm\":\"" + inicio + "\""))).andExpect(status().isOk());
		return id;
	}

	private static String idDe(org.springframework.test.web.servlet.ResultActions r) throws Exception {
		return com.jayway.jsonpath.JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private org.springframework.test.web.servlet.ResultActions concluir(int series, String carga) throws Exception {
		return concluir(UUID.randomUUID(), series, carga);
	}

	private org.springframework.test.web.servlet.ResultActions concluir(UUID id, int series, String carga) throws Exception {
		Instant inicio = agora().plusSeconds(++deslocamento * 10L);
		return mvc.perform(put("/api/sessoes/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("CONCLUIDA", inicio, inicio.plusSeconds(60), series, carga)))
				.andExpect(status().isOk());
	}

	/** Dias de treino validos anteriores (30 XP cada), gravados direto: o fuso do relogio so separa dois dias. */
	private void treinouEm(LocalDate... dias) {
		treinouEm(30, dias);
	}

	private void treinouEm(int pontos, LocalDate... dias) {
		for (LocalDate dia : dias) {
			XpEvent e = new XpEvent();
			e.setUsuario(usuarios.getReferenceById(UUID.fromString(UsuarioDeTeste.ID)));
			e.setSessaoId(UUID.randomUUID());
			e.setTipo(TipoXp.SERIES);
			e.setPontos(pontos);
			e.setDia(dia);
			e.setDiaUnico(dia);
			eventos.save(e);
		}
	}

	private void definirFuso(String fuso) {
		relogio.definirZona(ZoneId.of(fuso));
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
		return RelogioMutavel.AGORA;
	}
}
