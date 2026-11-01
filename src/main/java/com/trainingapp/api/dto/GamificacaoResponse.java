package com.trainingapp.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Situacao do usuario. O nivel atual vai de {@code xpInicioDoNivel} ate {@code xpProximoNivel}
 * (XP total). {@code treinos} conta dias de treino valido (a primeira sessao do dia, com 3+ series). {@code titulo} e o
 * nome da faixa do nivel (Iniciante, Dedicado, Atleta...).
 */
public record GamificacaoResponse(
		long xpTotal,
		int nivel,
		String titulo,
		long xpInicioDoNivel,
		long xpProximoNivel,
		int streakAtual,
		int melhorStreak,
		/** Congelamentos guardados (0 a 2) e se hoje a sequencia esta coberta por um. */
		int congelamentos,
		boolean congelada,
		int treinos,
		int recordes,
		boolean treinouHoje,
		List<ConquistaResponse> conquistas,
		/** Desafios da semana atual (segunda a domingo) e o domingo em que ela termina. */
		List<DesafioResponse> desafios,
		LocalDate semanaTerminaEm) {
}
