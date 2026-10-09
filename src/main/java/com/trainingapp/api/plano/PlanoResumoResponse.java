package com.trainingapp.api.plano;

import java.util.UUID;

public record PlanoResumoResponse(UUID id, String nome, Objetivo objetivo, boolean ativo, int quantidadeDias) {

	static PlanoResumoResponse de(Plano plano) {
		return new PlanoResumoResponse(plano.getId(), plano.getNome(), plano.getObjetivo(), plano.isAtivo(),
				plano.getDias().size());
	}
}
