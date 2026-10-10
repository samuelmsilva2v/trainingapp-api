package com.trainingapp.api.dto;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.GrupoMuscular;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExercicioRequest(
		@NotBlank @Size(max = 150) String nome,
		@NotNull GrupoMuscular grupoMuscular,
		@NotNull Equipamento equipamento) {
}
