package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleRequest(@NotBlank @Size(max = 2048) String codigo) {
}
