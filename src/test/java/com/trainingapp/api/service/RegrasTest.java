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
	void dropSetValeOVolumeSomadoComoUmaSerieSo() {
		// (600 + 50 x 8 + 40 x 6) / 40 = 31; sem drop, igual a serie; o teto de 100 vale para o bloco inteiro.
		assertThat(Regras.xpDoBloco(List.of(new BigDecimal("60"), new BigDecimal("50"), new BigDecimal("40")), List.of(10, 8, 6)))
				.isEqualTo(31);
		assertThat(Regras.xpDoBloco(List.of(new BigDecimal("60")), List.of(10))).isEqualTo(Regras.xpDaSerie(new BigDecimal("60"), 10));
		assertThat(Regras.xpDoBloco(List.of(new BigDecimal("200"), new BigDecimal("160")), List.of(15, 12))).isEqualTo(100);
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

	@Test
	void semanasFirmesContamSemanasSeguidasCom3Treinos() {
		LocalDate seg = LocalDate.of(2026, 10, 5); // segunda
		var tresPorSemana = List.of(seg, seg.plusDays(2), seg.plusDays(6), // domingo ainda e a mesma semana
				seg.plusWeeks(1), seg.plusWeeks(1).plusDays(1), seg.plusWeeks(1).plusDays(3));
		assertThat(Regras.melhorSemanasFirmes(tresPorSemana)).isEqualTo(2);
		assertThat(Regras.melhorSemanasFirmes(List.of(seg, seg, seg, seg.plusDays(1)))).isZero(); // dias repetidos contam uma vez
		assertThat(Regras.melhorSemanasFirmes(List.of())).isZero();
	}

	@Test
	void semanaFracaNoMeioQuebraAsSemanasFirmes() {
		LocalDate seg = LocalDate.of(2026, 10, 5);
		var dias = new java.util.ArrayList<LocalDate>();
		for (int semana : new int[] { 0, 2, 3, 4 }) {
			for (int d = 0; d < 3; d++) {
				dias.add(seg.plusWeeks(semana).plusDays(d));
			}
		}
		dias.add(seg.plusWeeks(1)); // so 1 treino na semana 1
		assertThat(Regras.melhorSemanasFirmes(dias)).isEqualTo(3);
	}

	@Test
	void deVoltaExige14DiasSemTreinoEntreDoisTreinos() {
		assertThat(Regras.voltouDepoisDePausa(List.of(D, D.plusDays(15)))).isTrue(); // 14 dias parado entre eles
		assertThat(Regras.voltouDepoisDePausa(List.of(D, D.plusDays(14)))).isFalse(); // 13 dias parado
		assertThat(Regras.voltouDepoisDePausa(List.of(D))).isFalse();
	}
}
