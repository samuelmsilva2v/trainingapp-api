package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// O token tem 43 caracteres (256 bits em base64url); o teto so barra lixo enorme.
public record TokenRequest(@NotBlank @Size(max = 200) String token) {
}
