package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ID token (JWT) que o Credential Manager do Android entrega ao app depois do login com Google. */
public record GoogleTokenRequest(@NotBlank @Size(max = 4096) String idToken) {
}
