package com.trainingapp.api.dto;

import com.trainingapp.api.model.ExercicioSessao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Séries concluídas do exercício no último treino concluído em que ele apareceu (coluna "Anterior" da execução). */
public record AnteriorResponse(UUID exercicioId, UUID sessaoId, Instant iniciadaEm, List<SerieAnterior> series) {

	public record SerieAnterior(int ordem, Integer reps, BigDecimal carga) {
	}

	public static AnteriorResponse de(ExercicioSessao e) {
		return new AnteriorResponse(e.getExercicio().getId(), e.getSessao().getId(), e.getSessao().getIniciadaEm(),
				e.getSeries().stream()
						.filter(s -> s.isConcluida())
						.map(s -> new SerieAnterior(s.getOrdem(), s.getReps(), s.getCarga()))
						.toList());
	}
}
