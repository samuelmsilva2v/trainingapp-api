package com.trainingapp.api.dto;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.EstadoSessao;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.Sessao;
import com.trainingapp.api.service.GamificacaoService;
import com.trainingapp.api.service.ImagensExercicio;
import com.trainingapp.api.service.Regras;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessaoResponse(
		UUID id,
		UUID planoId,
		String planoNome,
		String diaNome,
		int diaOrdem,
		EstadoSessao estado,
		Instant iniciadaEm,
		Instant finalizadaEm,
		Instant atualizadoEm,
		List<ExercicioResponse> exercicios,
		int xpGanho,
		int xpSeries,
		int xpRecordes,
		List<UUID> recordes) {

	public record ExercicioResponse(
			UUID id,
			UUID exercicioId,
			String nome,
			GrupoMuscular grupoMuscular,
			Equipamento equipamento,
			List<String> imagens,
			int ordem,
			int metaSeries,
			int metaRepsMin,
			int metaRepsMax,
			BigDecimal metaCarga,
			Integer metaDescansoSegundos,
			List<SerieResponse> series,
			/** XP das series concluidas deste exercicio (0 se o treino nao rendeu XP). */
			int xp) {
	}

	public record SerieResponse(UUID id, int ordem, Integer reps, BigDecimal carga, boolean concluida) {
	}

	public static SessaoResponse de(Sessao s, GamificacaoService.Ganho ganho, ImagensExercicio imagens) {
		return new SessaoResponse(s.getId(), s.getPlanoId(), s.getPlanoNome(), s.getDiaNome(), s.getDiaOrdem(),
				s.getEstado(), s.getIniciadaEm(), s.getFinalizadaEm(), s.getAtualizadoEm(),
				s.getExercicios().stream()
						.map(e -> new ExercicioResponse(e.getId(), e.getExercicio().getId(), e.getNome(),
								e.getExercicio().getGrupoMuscular(), e.getExercicio().getEquipamento(), imagens.de(e.getExercicio()), e.getOrdem(),
								e.getMetaSeries(), e.getMetaRepsMin(), e.getMetaRepsMax(), e.getMetaCarga(),
								e.getMetaDescansoSegundos(),
								e.getSeries().stream()
										.map(r -> new SerieResponse(r.getId(), r.getOrdem(), r.getReps(), r.getCarga(),
												r.isConcluida()))
										.toList(),
								ganho.xpSeries() > 0 ? e.getSeries().stream().filter(r -> r.isConcluida())
										.mapToInt(r -> Regras.xpDaSerie(r.getCarga(), r.getReps())).sum() : 0))
						.toList(),
				ganho.xp(), ganho.xpSeries(), ganho.xpRecordes(), ganho.recordes());
	}
}
