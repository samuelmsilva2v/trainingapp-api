package com.trainingapp.api.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RegrasTest {

	private static final LocalDate D = LocalDate.of(2026, 10, 1);

	@Test
	void sessaoSemSeriesSuficientesNaoRendeXp() {
		assertThat(Regras.xpDaSessao(0)).isZero();
		assertThat(Regras.xpDaSessao(2)).isZero();
	}

	@Test
	void sessaoRende50MaisCincoPorSerieAteVinte() {
		assertThat(Regras.xpDaSessao(3)).isEqualTo(65);
		assertThat(Regras.xpDaSessao(20)).isEqualTo(150);
		assertThat(Regras.xpDaSessao(35)).isEqualTo(150);
	}

	@Test
	void nivelSegueCemVezesNElevadoA1Virgula5() {
		assertThat(Regras.xpParaNivel(2)).isEqualTo(283);
		assertThat(Regras.xpParaNivel(4)).isEqualTo(800);
		assertThat(Regras.nivel(0)).isEqualTo(1);
		assertThat(Regras.nivel(282)).isEqualTo(1);
		assertThat(Regras.nivel(283)).isEqualTo(2);
		assertThat(Regras.nivel(799)).isEqualTo(3);
		assertThat(Regras.nivel(800)).isEqualTo(4);
		assertThat(Regras.nivel(100_000)).isEqualTo(100);
	}

	@Test
	void nivelUmComecaEmZeroEOsDemaisNoLimiar() {
		assertThat(Regras.xpInicioDoNivel(1)).isZero();
		assertThat(Regras.xpInicioDoNivel(3)).isEqualTo(Regras.xpParaNivel(3));
	}

	@Test
	void semTreinosNaoHaStreak() {
		assertThat(Regras.streak(List.of(), D)).isEqualTo(new Regras.Streak(0, 0));
	}

	@Test
	void diasSeguidosFormamUmaSequencia() {
		var s = Regras.streak(List.of(D, D.plusDays(1), D.plusDays(2)), D.plusDays(2));
		assertThat(s).isEqualTo(new Regras.Streak(3, 3));
	}

	@Test
	void doisDiasDeFolgaAindaMantemASequencia() {
		var s = Regras.streak(List.of(D, D.plusDays(3)), D.plusDays(3));
		assertThat(s).isEqualTo(new Regras.Streak(2, 2));
	}

	@Test
	void tresDiasDeFolgaQuebramASequencia() {
		var s = Regras.streak(List.of(D, D.plusDays(1), D.plusDays(5)), D.plusDays(5));
		assertThat(s).isEqualTo(new Regras.Streak(1, 2));
	}

	@Test
	void sequenciaAtualMorreQuandoOUltimoTreinoPassouDaTolerancia() {
		var treinos = List.of(D, D.plusDays(1));
		assertThat(Regras.streak(treinos, D.plusDays(4)).atual()).isEqualTo(2); // 2 folgas ate hoje
		assertThat(Regras.streak(treinos, D.plusDays(5)).atual()).isZero();
		assertThat(Regras.streak(treinos, D.plusDays(5)).melhor()).isEqualTo(2);
	}

	@Test
	void diasRepetidosContamUmaVez() {
		assertThat(Regras.streak(List.of(D, D, D), D)).isEqualTo(new Regras.Streak(1, 1));
	}
}
