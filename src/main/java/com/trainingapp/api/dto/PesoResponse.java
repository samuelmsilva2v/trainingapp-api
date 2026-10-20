package com.trainingapp.api.dto;

import com.trainingapp.api.model.PesoRegistro;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PesoResponse(LocalDate data, BigDecimal kg) {

	public static PesoResponse de(PesoRegistro registro) {
		return new PesoResponse(registro.getData(), registro.getKg());
	}
}
