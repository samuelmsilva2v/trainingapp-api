package com.trainingapp.api.repository;

import java.time.Instant;
import java.util.UUID;

public record TreinoDaConta(UUID id, Instant finalizadaEm, UUID planoId, int diaOrdem) {
}
