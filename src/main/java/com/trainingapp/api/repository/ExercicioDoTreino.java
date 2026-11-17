package com.trainingapp.api.repository;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.GrupoMuscular;

import java.math.BigDecimal;
import java.util.UUID;

public record ExercicioDoTreino(UUID sessaoId, UUID exercicioId, GrupoMuscular grupo, Equipamento equipamento,
		BigDecimal volume, BigDecimal maiorCarga, Long reps, Integer maisReps) {
}
