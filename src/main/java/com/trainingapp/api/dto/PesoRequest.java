package com.trainingapp.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PesoRequest(@NotNull @DecimalMin("20") @DecimalMax("400") @Digits(integer = 3, fraction = 2) BigDecimal kg) {
}
