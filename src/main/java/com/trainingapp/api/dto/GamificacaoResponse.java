package com.trainingapp.api.dto;

import java.util.List;

/**
 * Situacao do usuario. O nivel atual vai de {@code xpInicioDoNivel} ate {@code xpProximoNivel}
 * (XP total). {@code treinos} conta dias de treino valido (a primeira sessao do dia, com 3+ series).
 */
public record GamificacaoResponse(
		long xpTotal,
		int nivel,
		long xpInicioDoNivel,
		long xpProximoNivel,
		int streakAtual,
		int melhorStreak,
		int treinos,
		int recordes,
		boolean treinouHoje,
		List<ConquistaResponse> conquistas) {
}
