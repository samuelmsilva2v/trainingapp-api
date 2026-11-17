package com.trainingapp.api.dto;

import jakarta.validation.constraints.Size;

public record ExcluirContaRequest(@Size(max = 72) String senha, @Size(max = 2048) String codigo, @Size(max = 4096) String idToken) {
}
