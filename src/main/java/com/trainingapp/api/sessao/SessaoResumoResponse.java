package com.trainingapp.api.sessao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Linha do historico: totais calculados so sobre as series concluidas. */
public record SessaoResumoResponse(
		UUID id,
		String planoNome,
		String diaNome,
		EstadoSessao estado,
		Instant iniciadaEm,
		Instant finalizadaEm,
		int exercicios,
		int seriesConcluidas,
		BigDecimal volumeKg) {

	static SessaoResumoResponse de(Sessao s) {
		int series = 0;
		BigDecimal volume = BigDecimal.ZERO;
		for (ExercicioSessao e : s.getExercicios()) {
			for (SerieSessao r : e.getSeries()) {
				if (!r.isConcluida()) {
					continue;
				}
				series++;
				if (r.getCarga() != null && r.getReps() != null) {
					volume = volume.add(r.getCarga().multiply(BigDecimal.valueOf(r.getReps())));
				}
			}
		}
		return new SessaoResumoResponse(s.getId(), s.getPlanoNome(), s.getDiaNome(), s.getEstado(), s.getIniciadaEm(),
				s.getFinalizadaEm(), s.getExercicios().size(), series, volume);
	}
}
