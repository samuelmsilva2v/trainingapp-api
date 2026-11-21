package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TemaRequest(@NotBlank String tema) {
}
