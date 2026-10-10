package com.trainingapp.api.dto;

import com.trainingapp.api.model.EstadoSessao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Documento completo da sessao: o cliente gera os ids (UUID) e reenvia a sessao inteira a cada
 * alteracao. A ordem das listas e a ordem de execucao.
 */
public record SessaoRequest(
		UUID planoId,
		@NotBlank @Size(max = 100) String planoNome,
		@NotBlank @Size(max = 100) String diaNome,
		@Min(0) int diaOrdem,
		@NotNull EstadoSessao estado,
		@NotNull Instant iniciadaEm,
		Instant finalizadaEm,
		@NotNull Instant atualizadoEm,
		@NotNull @Size(max = 30) @Valid List<ExercicioRequest> exercicios) {

	public record ExercicioRequest(
			@NotNull UUID id,
			@NotNull UUID exercicioId,
			@Min(1) @Max(20) int metaSeries,
			@Min(1) @Max(100) int metaRepsMin,
			@Min(1) @Max(100) int metaRepsMax,
			@DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal metaCarga,
			@Min(0) @Max(1800) Integer metaDescansoSegundos,
			@NotNull @Size(max = 50) @Valid List<SerieRequest> series) {
	}

	public record SerieRequest(
			@NotNull UUID id,
			@Min(0) @Max(1000) Integer reps,
			@DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal carga,
			boolean concluida) {
	}
}
