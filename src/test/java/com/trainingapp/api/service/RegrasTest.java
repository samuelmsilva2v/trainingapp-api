package com.trainingapp.api.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RegrasTest {

	private static final LocalDate D = LocalDate.of(2026, 10, 1);

	/** Serie sem drop: um bloco de uma etapa. */
	private static int serie(String carga, Integer reps) {
		return Regras.xpDoBloco(Collections.singletonList(carga == null ? null : new BigDecimal(carga)),
				Collections.singletonList(reps));
	}

	@Test
	void serieRendeVolumeDivididoPor40() {
		assertThat(serie("60", 10)).isEqualTo(15);
		assertThat(serie("100", 5)).isEqualTo(13); // 12,5 arredonda para cima
		assertThat(serie("32.5", 12)).isEqualTo(10);
	}

	@Test
	void dropSetValeOVolumeSomadoComoUmaSerieSo() {
		// (600 + 50 x 8 + 40 x 6) / 40 = 31; o teto de 100 vale para o bloco inteiro.
		assertThat(Regras.xpDoBloco(List.of(new BigDecimal("60"), new BigDecimal("50"), new BigDecimal("40")), List.of(10, 8, 6)))
				.isEqualTo(31);
		assertThat(Regras.xpDoBloco(List.of(new BigDecimal("200"), new BigDecimal("160")), List.of(15, 12))).isEqualTo(100);
	}

	@Test
	void pesoCorporalRendeRepsVezesOFatorEntre5E100() {
		assertThat(Regras.xpDoBlocoPesoCorporal(List.of(10), 2.0)).isEqualTo(20); // barra fixa
		assertThat(Regras.xpDoBlocoPesoCorporal(List.of(15), 0.5)).isEqualTo(8); // 7,5 arredonda para cima
		assertThat(Regras.xpDoBlocoPesoCorporal(List.of(6), 0.5)).isEqualTo(5); // minimo
		assertThat(Regras.xpDoBlocoPesoCorporal(List.of(60), 2.0)).isEqualTo(100); // maximo
		assertThat(Regras.xpDoBlocoPesoCorporal(List.of(8, 6), 1.0)).isEqualTo(14); // drop antigo: reps somadas
		assertThat(Regras.xpDoBlocoPesoCorporal(List.of(0), 1.0)).isZero();
	}

	@Test
	void quantoMaisPesadaELongaMaisXp() {
		assertThat(serie("80", 10)).isGreaterThan(serie("60", 10));
		assertThat(serie("60", 12)).isGreaterThan(serie("60", 8));
	}

	@Test
	void serieLeveOuSemCargaRendeOMinimoENuncaPassaDoMaximo() {
		assertThat(serie("10", 8)).isEqualTo(5);
		assertThat(serie(null, 20)).isEqualTo(5);
		assertThat(serie("300", 30)).isEqualTo(100);
	}

	@Test
	void conquistaValePelaFaixaEFaixaUnicaVale50() {
		assertThat(Regras.xpDaConquista(1, false)).isEqualTo(25);
		assertThat(Regras.xpDaConquista(2, false)).isEqualTo(50);
		assertThat(Regras.xpDaConquista(3, false)).isEqualTo(100);
		assertThat(Regras.xpDaConquista(4, false)).isEqualTo(150);
		assertThat(Regras.xpDaConquista(5, false)).isEqualTo(200);
		assertThat(Regras.xpDaConquista(1, true)).isEqualTo(50);
	}

	@Test
	void tituloMudaNosLimitesDeCadaFaixaDeNivel() {
		assertThat(Regras.titulo(1)).isEqualTo("Iniciante");
		assertThat(Regras.titulo(4)).isEqualTo("Iniciante");
		assertThat(Regras.titulo(5)).isEqualTo("Dedicado");
		assertThat(Regras.titulo(9)).isEqualTo("Dedicado");
		assertThat(Regras.titulo(10)).isEqualTo("Atleta");
		assertThat(Regras.titulo(19)).isEqualTo("Atleta");
		assertThat(Regras.titulo(20)).isEqualTo("Veterano");
		assertThat(Regras.titulo(29)).isEqualTo("Veterano");
		assertThat(Regras.titulo(30)).isEqualTo("Elite");
		assertThat(Regras.titulo(49)).isEqualTo("Elite");
		assertThat(Regras.titulo(50)).isEqualTo("Lenda");
		assertThat(Regras.titulo(80)).isEqualTo("Lenda");
	}

	@Test
	void bonusDeSequenciaComecaNoSegundoDiaETemTeto() {
		assertThat(Regras.xpDaSequencia(0)).isZero();
		assertThat(Regras.xpDaSequencia(1)).isZero();
		assertThat(Regras.xpDaSequencia(2)).isEqualTo(5);
		assertThat(Regras.xpDaSequencia(5)).isEqualTo(20);
		assertThat(Regras.xpDaSequencia(10)).isEqualTo(45);
		assertThat(Regras.xpDaSequencia(11)).isEqualTo(50);
		assertThat(Regras.xpDaSequencia(30)).isEqualTo(50);
	}

	@Test
	void serieSemRepeticaoNaoRendeXp() {
		assertThat(serie("60", 0)).isZero();
		assertThat(serie("60", null)).isZero();
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
		assertThat(Regras.streak(List.of(), D)).isEqualTo(new Regras.Streak(0, 0, 0, false));
	}

	@Test
	void diasSeguidosFormamUmaSequencia() {
		var s = Regras.streak(List.of(D, D.plusDays(1), D.plusDays(2)), D.plusDays(2));
		assertThat(s).isEqualTo(new Regras.Streak(3, 3, 0, false));
	}

	@Test
	void doisDiasDeFolgaAindaMantemASequencia() {
		var s = Regras.streak(List.of(D, D.plusDays(3)), D.plusDays(3));
		assertThat(s).isEqualTo(new Regras.Streak(2, 2, 0, false));
	}

	@Test
	void tresDiasDeFolgaQuebramASequencia() {
		var s = Regras.streak(List.of(D, D.plusDays(1), D.plusDays(5)), D.plusDays(5));
		assertThat(s).isEqualTo(new Regras.Streak(1, 2, 0, false));
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
		assertThat(Regras.streak(List.of(D, D, D), D)).isEqualTo(new Regras.Streak(1, 1, 0, false));
	}

	/** {@code n} dias de treino seguidos a partir de {@code inicio}. */
	private static List<LocalDate> seguidos(LocalDate inicio, int n) {
		return inicio.datesUntil(inicio.plusDays(n)).toList();
	}

	private static List<LocalDate> juntos(List<LocalDate> a, List<LocalDate> b) {
		return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
	}

	/** Congelamentos guardados no dia do ultimo treino. */
	private static int congelamentos(List<LocalDate> treinos) {
		return Regras.streak(treinos, treinos.getLast()).congelamentos();
	}

	@Test
	void congelamentoChegaNoSetimoENoVigesimoPrimeiroDia() {
		assertThat(congelamentos(seguidos(D, 6))).isZero();
		assertThat(congelamentos(seguidos(D, 7))).isEqualTo(1);
		assertThat(congelamentos(seguidos(D, 20))).isEqualTo(1);
		assertThat(congelamentos(seguidos(D, 21))).isEqualTo(2);
	}

	@Test
	void guardaNoMaximoDoisCongelamentos() {
		assertThat(congelamentos(seguidos(D, 60))).isEqualTo(2);
	}

	@Test
	void congelamentoEUsadoSozinhoDepoisDaFolga() {
		var treinos = seguidos(D, 7);
		var ultimo = treinos.getLast();
		assertThat(Regras.streak(treinos, ultimo.plusDays(3))).isEqualTo(new Regras.Streak(7, 7, 1, false)); // ainda folga
		assertThat(Regras.streak(treinos, ultimo.plusDays(4))).isEqualTo(new Regras.Streak(7, 7, 0, true));
		assertThat(Regras.streak(treinos, ultimo.plusDays(5))).isEqualTo(new Regras.Streak(7, 7, 0, true));
		assertThat(Regras.streak(treinos, ultimo.plusDays(6))).isEqualTo(new Regras.Streak(0, 7, 0, false));
	}

	@Test
	void treinarNumDiaCongeladoSomaNaSequenciaEGastaOCongelamento() {
		var treinos = seguidos(D, 7);
		var dia = treinos.getLast().plusDays(4);
		assertThat(Regras.streak(juntos(treinos, List.of(dia)), dia)).isEqualTo(new Regras.Streak(8, 8, 0, false));
	}

	@Test
	void doisCongelamentosSeguidos() {
		var treinos = seguidos(D, 21);
		var ultimo = treinos.getLast();
		assertThat(Regras.streak(treinos, ultimo.plusDays(5))).isEqualTo(new Regras.Streak(21, 21, 1, true));
		assertThat(Regras.streak(treinos, ultimo.plusDays(7))).isEqualTo(new Regras.Streak(21, 21, 0, true));
		assertThat(Regras.streak(treinos, ultimo.plusDays(8)).atual()).isZero();
		var volta = ultimo.plusDays(7);
		assertThat(Regras.streak(juntos(treinos, List.of(volta)), volta)).isEqualTo(new Regras.Streak(22, 22, 0, false));
	}

	@Test
	void progressoRecomecaDepoisDoUso() {
		// 21 dias: 2 guardados; usa 1 e precisa de mais 14 para voltar a 2.
		var antes = seguidos(D, 21);
		var depois = antes.getLast().plusDays(4);
		assertThat(congelamentos(juntos(antes, seguidos(depois, 13)))).isEqualTo(1);
		assertThat(congelamentos(juntos(antes, seguidos(depois, 14)))).isEqualTo(2);
		// 7 dias: 1 guardado; usa e precisa de mais 7.
		var semana = seguidos(D, 7);
		var volta = semana.getLast().plusDays(4);
		assertThat(congelamentos(juntos(semana, seguidos(volta, 6)))).isZero();
		assertThat(congelamentos(juntos(semana, seguidos(volta, 7)))).isEqualTo(1);
	}

	@Test
	void quebraSemCongelamentoSuficienteZeraSequenciaEProgresso() {
		var semana = seguidos(D, 7); // 1 congelamento, mas a folga pede 2
		var volta = semana.getLast().plusDays(6);
		var depois = juntos(semana, seguidos(volta, 6));
		assertThat(Regras.streak(depois, depois.getLast())).isEqualTo(new Regras.Streak(6, 7, 0, false));
		assertThat(congelamentos(juntos(semana, seguidos(volta, 7)))).isEqualTo(1);
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
