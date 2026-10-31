package com.trainingapp.api.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Set;

/**
 * Desafios semanais fixos (api#38): os mesmos toda semana ISO (segunda a domingo), contando so treinos validos. O XP vai
 * num evento DESAFIO com o codigo {@code <desafio>:<ano>-W<semana>}, que a chave unica de xp_event credita uma vez.
 */
final class Desafios {

	private Desafios() {
	}

	record Desafio(String codigo, String nome, int meta, int xp) {
	}

	static final List<Desafio> TODOS = List.of(
			new Desafio("TRES_TREINOS", "3 treinos na semana", 3, 50),
			new Desafio("UM_RECORDE", "Bater 1 recorde", 1, 30),
			new Desafio("DOIS_DIAS_SEGUIDOS", "Treinar 2 dias seguidos", 2, 20));

	static LocalDate inicioDaSemana(LocalDate dia) {
		return dia.with(DayOfWeek.MONDAY);
	}

	static LocalDate fimDaSemana(LocalDate dia) {
		return dia.with(DayOfWeek.SUNDAY);
	}

	static boolean naSemana(LocalDate data, LocalDate dia) {
		return !data.isBefore(inicioDaSemana(dia)) && !data.isAfter(fimDaSemana(dia));
	}

	/** Codigo do evento: o do desafio e a semana ISO, ex.: TRES_TREINOS:2026-W44. */
	static String codigo(Desafio d, LocalDate dia) {
		return "%s:%d-W%02d".formatted(d.codigo(), dia.get(IsoFields.WEEK_BASED_YEAR), dia.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
	}

	/** O desafio de um codigo de evento (a parte antes da semana); null se nao for de desafio. */
	static Desafio deCodigo(String codigoDoEvento) {
		String base = codigoDoEvento.split(":")[0];
		return TODOS.stream().filter(d -> d.codigo().equals(base)).findFirst().orElse(null);
	}

	/**
	 * Progresso do desafio na semana de {@code dia}, ate a meta. "2 dias seguidos" sao dias de calendario consecutivos com
	 * os dois dentro da semana: domingo e a segunda seguinte nao contam juntos.
	 */
	static int progresso(Desafio d, Set<LocalDate> diasDeTreino, int recordesNaSemana, LocalDate dia) {
		int valor = switch (d.codigo()) {
			case "TRES_TREINOS" -> (int) diasDeTreino.stream().filter(t -> naSemana(t, dia)).count();
			case "UM_RECORDE" -> recordesNaSemana;
			case "DOIS_DIAS_SEGUIDOS" -> maiorSequenciaNaSemana(diasDeTreino, dia);
			default -> 0;
		};
		return Math.max(0, Math.min(valor, d.meta()));
	}

	private static int maiorSequenciaNaSemana(Set<LocalDate> diasDeTreino, LocalDate dia) {
		int melhor = 0;
		int corrente = 0;
		for (LocalDate d = inicioDaSemana(dia); !d.isAfter(fimDaSemana(dia)); d = d.plusDays(1)) {
			corrente = diasDeTreino.contains(d) ? corrente + 1 : 0;
			melhor = Math.max(melhor, corrente);
		}
		return melhor;
	}
}
