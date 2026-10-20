package com.trainingapp.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** O perfil inteiro: campo ausente vira nulo (lembrete de pesagem desligado). */
public record PerfilRequest(@NotBlank @Size(max = 100) String nome, Integer lembretePesoDias) {

	@AssertTrue(message = "lembretePesoDias deve ser 7, 15 ou 30")
	boolean isLembreteValido() {
		return lembretePesoDias == null || Set.of(7, 15, 30).contains(lembretePesoDias);
	}
}
