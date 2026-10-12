package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// O e-mail entra na chave do limite de tentativas, que fica na memoria: sem teto, um e-mail enorme vira consumo de memoria.
public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank String senha) {
}
