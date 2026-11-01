package com.trainingapp.api.repository;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.GrupoMuscular;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Um exercicio de um treino, ja somado no banco: so as series concluidas com pelo menos 1 repeticao (drops incluidos).
 * Usado pelas conquistas da fatia 2, que leem o historico inteiro sem carregar serie por serie.
 */
public record ExercicioDoTreino(UUID sessaoId, UUID exercicioId, GrupoMuscular grupo, Equipamento equipamento,
		BigDecimal volume, BigDecimal maiorCarga, Long reps, Integer maisReps) {
}
