package com.trainingapp.api.dto;

import jakarta.validation.constraints.Size;

public record RefreshRequest(@Size(max = 200) String refresh) {
}
