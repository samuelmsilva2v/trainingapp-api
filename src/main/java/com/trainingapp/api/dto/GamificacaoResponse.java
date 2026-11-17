package com.trainingapp.api.dto;

import java.time.LocalDate;
import java.util.List;

public record GamificacaoResponse(
		long xpTotal,
		int nivel,
		String titulo,
		long xpInicioDoNivel,
		long xpProximoNivel,
		int streakAtual,
		int melhorStreak,
		int congelamentos,
		boolean congelada,
		int treinos,
		int recordes,
		boolean treinouHoje,
		List<ConquistaResponse> conquistas,
		List<DesafioResponse> desafios,
		LocalDate semanaTerminaEm) {
}
