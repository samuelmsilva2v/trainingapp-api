package com.trainingapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleTokenRequest(@NotBlank @Size(max = 4096) String idToken) {
}
