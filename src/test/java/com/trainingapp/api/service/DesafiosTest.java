package com.trainingapp.api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DesafiosTest {

	// Semana ISO 44 de 2026: segunda 26/10 a domingo 01/11.
	private static final LocalDate QUARTA = LocalDate.of(2026, 10, 28);
	private static final Desafios.Desafio TRES = Desafios.TODOS.get(0);
	private static final Desafios.Desafio RECORDE = Desafios.TODOS.get(1);
	private static final Desafios.Desafio SEGUIDOS = Desafios.TODOS.get(2);

	@Test
	void codigoLevaASemanaIso() {
		assertThat(Desafios.codigo(TRES, QUARTA)).isEqualTo("TRES_TREINOS:2026-W44");
		assertThat(Desafios.deCodigo("TRES_TREINOS:2026-W44")).isEqualTo(TRES);
		assertThat(Desafios.fimDaSemana(QUARTA)).isEqualTo(LocalDate.of(2026, 11, 1));
	}

	@Test
	void tresTreinosContaSoOsDiasDaSemanaELimitaNaMeta() {
		Set<LocalDate> dias = Set.of(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 28));
		assertThat(Desafios.progresso(TRES, dias, 0, QUARTA)).isEqualTo(2); // o domingo 25 e da semana anterior
		Set<LocalDate> cinco = Set.of(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28),
				LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 30));
		assertThat(Desafios.progresso(TRES, cinco, 0, QUARTA)).isEqualTo(3);
	}

	@Test
	void doisDiasSeguidosPrecisaDosDoisDentroDaSemana() {
		// Domingo 01/11 e segunda 02/11 sao seguidos, mas de semanas diferentes.
		assertThat(Desafios.progresso(SEGUIDOS, Set.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)), 0, QUARTA))
				.isEqualTo(1);
		assertThat(Desafios.progresso(SEGUIDOS, Set.of(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 28)), 0, QUARTA))
				.isEqualTo(1); // com folga no meio
		assertThat(Desafios.progresso(SEGUIDOS, Set.of(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28)), 0, QUARTA))
				.isEqualTo(2);
	}

	@Test
	void recordeUsaOsRecordesDaSemana() {
		assertThat(Desafios.progresso(RECORDE, Set.of(), 0, QUARTA)).isZero();
		assertThat(Desafios.progresso(RECORDE, Set.of(), 3, QUARTA)).isEqualTo(1);
	}
}
