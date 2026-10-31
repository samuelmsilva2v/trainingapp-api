package com.trainingapp.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.trainingapp.api.UsuarioDeTeste;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterio de desempenho da api#36: com 1.000 treinos de 30 series (uns 4 anos de uso), o {@code GET /api/gamificacao},
 * que agora le o historico inteiro para a fatia 2 das conquistas, responde em menos de 200 ms num Postgres real. Se um dia
 * passar disso, a fatia 2 passa a ser gravada ao concluir. Ignorado sem Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = { "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate" })
class GamificacaoDesempenhoPostgresTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	private static final int TREINOS = 1000;
	private static final int EXERCICIOS_POR_TREINO = 5;
	private static final int SERIES_POR_EXERCICIO = 6;
	private static final long LIMITE_MS = 200;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void resumoComMilTreinosDe30SeriesRespondeEmMenosDe200ms() throws Exception {
		List<UUID> exercicios = new ArrayList<>();
		for (int i = 0; i < EXERCICIOS_POR_TREINO; i++) {
			exercicios.add(UUID.fromString(JsonPath.read(mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
					.content("{\"nome\":\"Exercicio %d\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}".formatted(i)))
					.andReturn().getResponse().getContentAsString(), "$.id")));
		}
		semear(exercicios);

		mvc.perform(get("/api/gamificacao")).andExpect(status().isOk()); // aquece (JIT, cache de consultas)
		mvc.perform(get("/api/gamificacao")).andExpect(status().isOk());
		long melhor = Long.MAX_VALUE;
		for (int i = 0; i < 5; i++) {
			long inicio = System.nanoTime();
			mvc.perform(get("/api/gamificacao")).andExpect(status().isOk());
			melhor = Math.min(melhor, (System.nanoTime() - inicio) / 1_000_000);
		}
		System.out.printf("GET /api/gamificacao com %d treinos de %d series: %d ms%n", TREINOS,
				EXERCICIOS_POR_TREINO * SERIES_POR_EXERCICIO, melhor);
		assertThat(melhor).isLessThan(LIMITE_MS);
	}

	/** 1.000 treinos concluidos em dias seguidos, cada um com o evento SERIES que o torna valido. */
	private void semear(List<UUID> exercicios) {
		UUID usuario = UUID.fromString(UsuarioDeTeste.ID);
		Instant base = Instant.now().minus(TREINOS + 1, ChronoUnit.DAYS);
		List<Object[]> sessoes = new ArrayList<>();
		List<Object[]> exerciciosSessao = new ArrayList<>();
		List<Object[]> series = new ArrayList<>();
		List<Object[]> eventos = new ArrayList<>();
		for (int t = 0; t < TREINOS; t++) {
			UUID sessao = UUID.randomUUID();
			Timestamp fim = Timestamp.from(base.plus(t, ChronoUnit.DAYS));
			sessoes.add(new Object[] { sessao, usuario, "Plano", "A", 0, "CONCLUIDA", fim, fim, fim });
			for (int e = 0; e < EXERCICIOS_POR_TREINO; e++) {
				UUID exSessao = UUID.randomUUID();
				exerciciosSessao.add(new Object[] { exSessao, sessao, exercicios.get(e), "Exercicio " + e, e });
				for (int s = 0; s < SERIES_POR_EXERCICIO; s++) {
					series.add(new Object[] { UUID.randomUUID(), exSessao, s, 10, 60 + e, true });
				}
			}
			LocalDate dia = base.plus(t, ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toLocalDate();
			eventos.add(new Object[] { UUID.randomUUID(), usuario, sessao, 300, dia, dia, fim });
		}
		jdbc.batchUpdate("""
				insert into sessao (id, usuario_id, plano_nome, dia_nome, dia_ordem, estado, iniciada_em, finalizada_em, atualizado_em)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?)""", sessoes);
		jdbc.batchUpdate("""
				insert into exercicio_sessao (id, sessao_id, exercicio_id, nome, ordem, meta_series, meta_reps_min, meta_reps_max)
				values (?, ?, ?, ?, ?, 6, 8, 12)""", exerciciosSessao);
		jdbc.batchUpdate("insert into serie_sessao (id, exercicio_sessao_id, ordem, reps, carga, concluida) values (?, ?, ?, ?, ?, ?)",
				series);
		jdbc.batchUpdate("""
				insert into xp_event (id, usuario_id, sessao_id, tipo, pontos, dia, dia_unico, criado_em)
				values (?, ?, ?, 'SERIES', ?, ?, ?, ?)""", eventos);
	}
}
