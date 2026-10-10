package com.trainingapp.api.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/** Regras de gamificacao puras (sem banco): faceis de testar e de ajustar num lugar so. */
public final class Regras {

	public static final int XP_SESSAO = 50;
	public static final int XP_POR_SERIE = 5;
	public static final int SERIES_COM_XP = 20;
	public static final int XP_RECORDE = 25;
	public static final int SERIES_MINIMAS = 3;

	/** O streak quebra apos mais de 2 dias sem treinar: entre dois treinos cabem no maximo 2 dias de folga. */
	public static final int FOLGAS_TOLERADAS = 2;

	private Regras() {
	}

	/** XP da sessao concluida (sem recordes): 50 + 5 por serie, ate 20 series. Zero se nao houver series suficientes. */
	public static int xpDaSessao(int seriesConcluidas) {
		if (seriesConcluidas < SERIES_MINIMAS) {
			return 0;
		}
		return XP_SESSAO + XP_POR_SERIE * Math.min(seriesConcluidas, SERIES_COM_XP);
	}

	/** XP total necessario para alcancar o nivel n: 100 x n^1,5. */
	public static long xpParaNivel(int nivel) {
		return (long) Math.ceil(100 * Math.pow(nivel, 1.5));
	}

	/** Nivel de quem tem este XP total; todo mundo comeca no nivel 1. */
	public static int nivel(long xpTotal) {
		int nivel = Math.max(1, (int) Math.floor(Math.pow(xpTotal / 100.0, 2.0 / 3.0)));
		while (xpParaNivel(nivel + 1) <= xpTotal) {
			nivel++;
		}
		while (nivel > 1 && xpParaNivel(nivel) > xpTotal) {
			nivel--;
		}
		return nivel;
	}

	/** XP total em que o nivel comeca (o nivel 1 comeca em zero). */
	public static long xpInicioDoNivel(int nivel) {
		return nivel <= 1 ? 0 : xpParaNivel(nivel);
	}

	public record Streak(int atual, int melhor) {
	}

	/**
	 * Sequencias de dias de treino. Dias seguidos, ou com ate 2 dias de folga entre eles, formam uma
	 * sequencia; a atual so vale enquanto o ultimo treino nao ficou para tras alem da tolerancia.
	 */
	public static Streak streak(Collection<LocalDate> diasDeTreino, LocalDate hoje) {
		List<LocalDate> dias = List.copyOf(new TreeSet<>(diasDeTreino));
		int melhor = 0;
		int corrente = 0;
		LocalDate anterior = null;
		for (LocalDate dia : dias) {
			boolean continua = anterior != null && ChronoUnit.DAYS.between(anterior, dia) <= FOLGAS_TOLERADAS + 1;
			corrente = continua ? corrente + 1 : 1;
			melhor = Math.max(melhor, corrente);
			anterior = dia;
		}
		boolean viva = anterior != null && ChronoUnit.DAYS.between(anterior, hoje) <= FOLGAS_TOLERADAS + 1;
		return new Streak(viva ? corrente : 0, melhor);
	}
}
