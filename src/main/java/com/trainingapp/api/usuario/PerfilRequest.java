package com.trainingapp.api.usuario;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.ZoneId;

public record PerfilRequest(
		@NotBlank @Size(max = 100) String nome,
		@NotBlank String fusoHorario) {

	@AssertTrue(message = "fusoHorario invalido")
	boolean isFusoHorarioValido() {
		return fusoHorario == null || ZoneId.getAvailableZoneIds().contains(fusoHorario);
	}
}
