package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;

/** O codigo de autorizacao de uso unico que o popup do Google entrega ao front (campo "code"). */
public record GoogleRequest(@NotBlank String codigo) {
}
