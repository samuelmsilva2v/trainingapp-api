package com.trainingapp.api.dto;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.Objetivo;

import java.util.List;

/** A previa do plano que chegou por link; {@code compartilhadoPor} e o nome do perfil de quem gerou (nulo sem nome). */
public record PlanoCompartilhadoResponse(String nome, Objetivo objetivo, String compartilhadoPor, List<DiaResponse> dias) {

	public record DiaResponse(String nome, List<ItemResponse> exercicios) {
	}

	public record ItemResponse(
			String exercicioNome,
			GrupoMuscular grupoMuscular,
			Equipamento equipamento,
			List<String> imagens,
			int series,
			int repsMin,
			int repsMax,
			Integer descansoSegundos,
			int drops) {
	}

	public record LinkResponse(String link) {
	}
}
