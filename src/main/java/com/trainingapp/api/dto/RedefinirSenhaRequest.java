package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(@NotBlank @Size(max = 200) String token,@NotBlank @Size(max = 72) String senha) {
}
