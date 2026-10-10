package com.trainingapp.api.dto;


import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.Objetivo;
import com.trainingapp.api.model.Plano;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlanoResponse(
		UUID id,
		String nome,
		Objetivo objetivo,
		boolean ativo,
		List<DiaResponse> dias) {

	public record DiaResponse(UUID id, String nome, int ordem, List<ItemResponse> exercicios) {
	}

	public record ItemResponse(
			UUID id,
			UUID exercicioId,
			String exercicioNome,
			GrupoMuscular grupoMuscular,
			Equipamento equipamento,
			int ordem,
			int series,
			int repsMin,
			int repsMax,
			BigDecimal cargaAlvo,
			Integer descansoSegundos) {
	}

	public static PlanoResponse de(Plano plano) {
		List<DiaResponse> dias = plano.getDias().stream()
				.map(dia -> new DiaResponse(dia.getId(), dia.getNome(), dia.getOrdem(),
						dia.getExercicios().stream()
								.map(i -> new ItemResponse(i.getId(), i.getExercicio().getId(),
										i.getExercicio().getNome(), i.getExercicio().getGrupoMuscular(),
										i.getExercicio().getEquipamento(), i.getOrdem(), i.getSeries(),
										i.getRepsMin(), i.getRepsMax(), i.getCargaAlvo(), i.getDescansoSegundos()))
								.toList()))
				.toList();
		return new PlanoResponse(plano.getId(), plano.getNome(), plano.getObjetivo(), plano.isAtivo(), dias);
	}
}
