package com.trainingapp.api.exercicio;

import java.util.UUID;

public record ExercicioResponse(
		UUID id,
		String nome,
		GrupoMuscular grupoMuscular,
		Equipamento equipamento,
		boolean personalizado) {

	static ExercicioResponse de(Exercicio e) {
		return new ExercicioResponse(e.getId(), e.getNome(), e.getGrupoMuscular(), e.getEquipamento(),
				e.getUsuario() != null);
	}
}
