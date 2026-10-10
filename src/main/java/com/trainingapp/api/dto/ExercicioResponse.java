package com.trainingapp.api.dto;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import java.util.UUID;

public record ExercicioResponse(
		UUID id,
		String nome,
		GrupoMuscular grupoMuscular,
		Equipamento equipamento,
		boolean personalizado) {

	public static ExercicioResponse de(Exercicio e) {
		return new ExercicioResponse(e.getId(), e.getNome(), e.getGrupoMuscular(), e.getEquipamento(),
				e.getUsuario() != null);
	}
}
