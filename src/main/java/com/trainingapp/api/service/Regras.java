package com.trainingapp.api.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class Regras {

	public static final int VOLUME_POR_XP = 40;
	public static final int XP_MINIMO_SERIE = 5;
	public static final int XP_MAXIMO_SERIE = 100;
	public static final int XP_RECORDE = 25;
	public static final int SERIES_MINIMAS = 3;

	public static final int FOLGAS_TOLERADAS = 2;

	public static final int TREINOS_SEMANA_FIRME = 3;

	public static final int DIAS_PARADO_DE_VOLTA = 14;

	public static final int RECORDES_DIA_INSPIRADO = 3;

	public static final int XP_POR_DIA_DE_SEQUENCIA = 5;
	public static final int XP_MAXIMO_SEQUENCIA = 50;

	private Regras() {
	}

	public static int xpDaConquista(int faixa, boolean faixaUnica) {
		if (faixaUnica) {
			return 50;
		}
		return switch (faixa) {
			case 1 -> 25;
			case 2 -> 50;
			case 3 -> 100;
			case 4 -> 150;
			default -> 200;
		};
	}

	public static int xpDaSequencia(int dias) {
		return Math.min(XP_MAXIMO_SEQUENCIA, XP_POR_DIA_DE_SEQUENCIA * Math.max(0, dias - 1));
	}

	public static int xpDoBloco(List<BigDecimal> cargas, List<Integer> reps) {
		double volume = 0;
		int totalReps = 0;
		for (int i = 0; i < reps.size(); i++) {
			int r = reps.get(i) == null ? 0 : reps.get(i);
			volume += (cargas.get(i) == null ? 0 : cargas.get(i).doubleValue()) * r;
			totalReps += r;
		}
		return xpDoVolume(volume, totalReps);
	}

	public static int xpDoBlocoPesoCorporal(List<Integer> reps, double fator) {
		int total = reps.stream().mapToInt(r -> r == null ? 0 : r).sum();
		if (total < 1) {
			return 0;
		}
		return (int) Math.min(XP_MAXIMO_SERIE, Math.max(XP_MINIMO_SERIE, Math.round(total * fator)));
	}

	private static int xpDoVolume(double volume, int reps) {
		if (reps < 1) {
			return 0;
		}
		long xp = Math.round(volume / VOLUME_POR_XP);
		return (int) Math.min(XP_MAXIMO_SERIE, Math.max(XP_MINIMO_SERIE, xp));
	}

	public static long xpParaNivel(int nivel) {
		return (long) Math.ceil(100 * Math.pow(nivel, 1.5));
	}

	public static String titulo(int nivel) {
		if (nivel >= 50) {
			return "Lenda";
		}
		if (nivel >= 30) {
			return "Elite";
		}
		if (nivel >= 20) {
			return "Veterano";
		}
		if (nivel >= 10) {
			return "Atleta";
		}
		return nivel >= 5 ? "Dedicado" : "Iniciante";
	}

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

	public static long xpInicioDoNivel(int nivel) {
		return nivel <= 1 ? 0 : xpParaNivel(nivel);
	}

	public static final int MAXIMO_CONGELAMENTOS = 2;
	public static final int DIAS_POR_CONGELAMENTO = 2;
	private static final int[] CUSTO_CONGELAMENTO = {7, 14};

	public record Streak(int atual, int melhor, int congelamentos, boolean congelada) {
	}

	public static Streak streak(Collection<LocalDate> diasDeTreino, LocalDate hoje) {
		List<LocalDate> dias = List.copyOf(new TreeSet<>(diasDeTreino));
		int melhor = 0;
		int corrente = 0;
		int congelamentos = 0;
		int progresso = 0;
		LocalDate anterior = null;
		for (LocalDate dia : dias) {
			if (anterior != null) {
				long usados = congelamentosNecessarios(anterior, dia);
				if (usados > congelamentos) {
					corrente = 0;
					congelamentos = 0;
					progresso = 0;
				} else if (usados > 0) {
					congelamentos -= (int) usados;
					progresso = 0;
				}
			}
			corrente++;
			melhor = Math.max(melhor, corrente);
			if (congelamentos < MAXIMO_CONGELAMENTOS && ++progresso == CUSTO_CONGELAMENTO[congelamentos]) {
				congelamentos++;
				progresso = 0;
			}
			anterior = dia;
		}
		if (anterior == null) {
			return new Streak(0, 0, 0, false);
		}
		long usados = congelamentosNecessarios(anterior, hoje);
		return usados > congelamentos ? new Streak(0, melhor, 0, false)
				: new Streak(corrente, melhor, congelamentos - (int) usados, usados > 0);
	}

	private static long congelamentosNecessarios(LocalDate anterior, LocalDate dia) {
		long alem = ChronoUnit.DAYS.between(anterior, dia) - (FOLGAS_TOLERADAS + 1);
		return alem <= 0 ? 0 : (alem + DIAS_POR_CONGELAMENTO - 1) / DIAS_POR_CONGELAMENTO;
	}

	public static int melhorSemanasFirmes(Collection<LocalDate> diasDeTreino) {
		TreeSet<LocalDate> firmes = new TreeSet<>(diasDeTreino).stream()
				.collect(Collectors.groupingBy(d -> d.with(DayOfWeek.MONDAY), Collectors.counting()))
				.entrySet().stream().filter(s -> s.getValue() >= TREINOS_SEMANA_FIRME).map(java.util.Map.Entry::getKey)
				.collect(Collectors.toCollection(TreeSet::new));
		int melhor = 0;
		int corrente = 0;
		LocalDate anterior = null;
		for (LocalDate segunda : firmes) {
			corrente = segunda.minusWeeks(1).equals(anterior) ? corrente + 1 : 1;
			melhor = Math.max(melhor, corrente);
			anterior = segunda;
		}
		return melhor;
	}

	public static boolean voltouDepoisDePausa(Collection<LocalDate> diasDeTreino) {
		LocalDate anterior = null;
		for (LocalDate dia : new TreeSet<>(diasDeTreino)) {
			if (anterior != null && ChronoUnit.DAYS.between(anterior, dia) > DIAS_PARADO_DE_VOLTA) {
				return true;
			}
			anterior = dia;
		}
		return false;
	}
}
