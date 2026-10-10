package com.trainingapp.api.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RegrasTest {

	private static final LocalDate D = LocalDate.of(2026, 10, 1);

	@Test
	void serieRendeVolumeDivididoPor40() {
		assertThat(Regras.xpDaSerie(new BigDecimal("60"), 10)).isEqualTo(15);
		assertThat(Regras.xpDaSerie(new BigDecimal("100"), 5)).isEqualTo(13); // 12,5 arredonda para cima
		assertThat(Regras.xpDaSerie(new BigDecimal("32.5"), 12)).isEqualTo(10);
	}

	@Test
	void quantoMaisPesadaELongaMaisXp() {
		assertThat(Regras.xpDaSerie(new BigDecimal("80"), 10)).isGreaterThan(Regras.xpDaSerie(new BigDecimal("60"), 10));
		assertThat(Regras.xpDaSerie(new BigDecimal("60"), 12)).isGreaterThan(Regras.xpDaSerie(new BigDecimal("60"), 8));
	}

	@Test
	void serieLeveOuSemCargaRendeOMinimoENuncaPassaDoMaximo() {
		assertThat(Regras.xpDaSerie(new BigDecimal("10"), 8)).isEqualTo(5);
		assertThat(Regras.xpDaSerie(null, 20)).isEqualTo(5);
		assertThat(Regras.xpDaSerie(new BigDecimal("300"), 30)).isEqualTo(100);
	}

	@Test
	void serieSemRepeticaoNaoRendeXp() {
		assertThat(Regras.xpDaSerie(new BigDecimal("60"), 0)).isZero();
		assertThat(Regras.xpDaSerie(new BigDecimal("60"), null)).isZero();
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
