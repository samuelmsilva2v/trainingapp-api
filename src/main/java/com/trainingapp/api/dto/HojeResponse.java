package com.trainingapp.api.dto;

import java.util.UUID;

public record HojeResponse(
		UUID planoId,
		String planoNome,
		int totalDias,
		PlanoResponse.DiaResponse proximoDia,
		SessaoResponse sessaoEmAndamento) {
}
