package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(@NotBlank String senhaAtual, @NotBlank @Size(min = 8, max = 72) String novaSenha) {
}
