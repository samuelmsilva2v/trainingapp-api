package com.trainingapp.api.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Regras de gamificacao puras (sem banco): faceis de testar e de ajustar num lugar so. */
public final class Regras {

	/** Volume (carga x reps) que vale 1 XP: 60 kg x 10 reps = 600 = 15 XP. */
	public static final int VOLUME_POR_XP = 40;
	public static final int XP_MINIMO_SERIE = 5;
	public static final int XP_MAXIMO_SERIE = 100;
	public static final int XP_RECORDE = 25;
	public static final int SERIES_MINIMAS = 3;

	/** O streak quebra apos mais de 2 dias sem treinar: entre dois treinos cabem no maximo 2 dias de folga. */
	public static final int FOLGAS_TOLERADAS = 2;

	/** Semana firme: semana (segunda a domingo) com pelo menos 3 treinos. */
	public static final int TREINOS_SEMANA_FIRME = 3;

	/** "De volta": treinar depois de 14 dias ou mais sem treino. */
	public static final int DIAS_PARADO_DE_VOLTA = 14;

	/** "Dia inspirado": recordes no mesmo treino. */
	public static final int RECORDES_DIA_INSPIRADO = 3;

	/** Bonus por dia de sequencia alem do primeiro, e o teto do bonus por treino. */
	public static final int XP_POR_DIA_DE_SEQUENCIA = 5;
	public static final int XP_MAXIMO_SEQUENCIA = 50;

	private Regras() {
	}

	/** XP de uma conquista: pela faixa (1 = 25, 2 = 50, 3 = 100, 4 = 150, 5 em diante = 200); conquista de faixa unica vale 50. */
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

	/** Bonus de um treino no dia N da sequencia: 5 XP x (N - 1), ate 50. O dia 1 nao rende bonus. */
	public static int xpDaSequencia(int dias) {
		return Math.min(XP_MAXIMO_SEQUENCIA, XP_POR_DIA_DE_SEQUENCIA * Math.max(0, dias - 1));
	}

	/**
	 * XP de um bloco (serie normal + drops): e uma serie so, entao vale o volume somado das etapas (carga x reps) / 40,
	 * entre 5 e 100. Quanto mais pesada e longa, mais XP; sem carga vale o minimo; zero se nao houve
	 * repeticao. Serie sem drop e um bloco de uma etapa. As listas tem a carga e as reps de cada etapa concluida.
	 */
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

	/** XP de um bloco de peso corporal (api#43): reps somadas das etapas x fator, entre 5 e 100; zero sem repeticao. */
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

	/** XP total necessario para alcancar o nivel n: 100 x n^1,5. */
	public static long xpParaNivel(int nivel) {
		return (long) Math.ceil(100 * Math.pow(nivel, 1.5));
	}

	/** Titulo da faixa de nivel: Iniciante (1-4), Dedicado (5-9), Atleta (10-19), Veterano (20-29), Elite (30-49), Lenda (50+). */
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

	/** Congelamentos guardados no maximo, os dias de calendario que cada um cobre e o custo (dias de treino) do 1o e do 2o. */
	public static final int MAXIMO_CONGELAMENTOS = 2;
	public static final int DIAS_POR_CONGELAMENTO = 2;
	private static final int[] CUSTO_CONGELAMENTO = {7, 14};

	/** {@code congelamentos} guardados agora; {@code congelada}: hoje esta coberto por um congelamento (e nao treinou). */
	public record Streak(int atual, int melhor, int congelamentos, boolean congelada) {
	}

	/**
	 * Sequencias de dias de treino. Dias seguidos, ou com ate 2 dias de folga entre eles, formam uma
	 * sequencia; a atual so vale enquanto o ultimo treino nao ficou para tras alem da tolerancia. Quando ia quebrar, um
	 * congelamento cobre os 2 dias seguintes (e depois o segundo, se houver). Dias congelados nao somam. O 1o congelamento
	 * custa 7 dias de treino e o 2o mais 14; o progresso recomeca a cada ganho, a cada uso e na quebra.
	 */
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

	/** Congelamentos que um intervalo entre treinos (ou ate hoje) consome: cada um estica a tolerancia em 2 dias. */
	private static long congelamentosNecessarios(LocalDate anterior, LocalDate dia) {
		long alem = ChronoUnit.DAYS.between(anterior, dia) - (FOLGAS_TOLERADAS + 1);
		return alem <= 0 ? 0 : (alem + DIAS_POR_CONGELAMENTO - 1) / DIAS_POR_CONGELAMENTO;
	}

	/** Maior numero de semanas seguidas (segunda a domingo) com pelo menos 3 dias de treino cada. */
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

	/** Houve um treino depois de 14 dias ou mais sem treino (o primeiro treino da vida nao conta). */
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
