package com.trainingapp.api.dto;

import com.trainingapp.api.model.Objetivo;
import com.trainingapp.api.model.Plano;
import java.util.UUID;

public record PlanoResumoResponse(UUID id, String nome, Objetivo objetivo, boolean ativo, int quantidadeDias) {

	public static PlanoResumoResponse de(Plano plano) {
		return new PlanoResumoResponse(plano.getId(), plano.getNome(), plano.getObjetivo(), plano.isAtivo(),
				plano.getDias().size());
	}
}
