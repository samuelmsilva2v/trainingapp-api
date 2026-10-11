package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;

/** O ID token que o Google entrega ao front (campo "credential" do Google Identity Services). */
public record GoogleRequest(@NotBlank String credencial) {
}
