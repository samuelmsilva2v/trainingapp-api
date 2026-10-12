package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O codigo de autorizacao de uso unico que o popup do Google entrega ao front (campo "code"). */
public record GoogleRequest(@NotBlank @Size(max = 2048) String codigo) {
}
