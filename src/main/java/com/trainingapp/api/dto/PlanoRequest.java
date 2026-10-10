package com.trainingapp.api.dto;

import com.trainingapp.api.model.Objetivo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Documento completo do plano: criar e atualizar enviam a arvore inteira. A ordem das listas e a ordem de execucao. */
public record PlanoRequest(
		@NotBlank @Size(max = 100) String nome,
		@NotNull Objetivo objetivo,
		@NotEmpty @Valid List<DiaRequest> dias) {

	public record DiaRequest(
			@NotBlank @Size(max = 100) String nome,
			@NotNull @Valid List<ItemRequest> exercicios) {
	}

	public record ItemRequest(
			@NotNull UUID exercicioId,
			@Min(1) @Max(20) int series,
			@Min(1) @Max(100) int repsMin,
			@Min(1) @Max(100) int repsMax,
			@DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal cargaAlvo,
			@Min(0) @Max(1800) Integer descansoSegundos) {
	}
}
