package com.trainingapp.api.repository;

import java.time.Instant;
import java.util.UUID;

/** Dados de um treino concluido usados pelas conquistas da fatia 2 (rotacao e horarios). */
public record TreinoDaConta(UUID id, Instant finalizadaEm, UUID planoId, int diaOrdem) {
}
