package com.trainingapp.api.dto;

import java.util.UUID;

/**
 * O que a tela "Hoje" precisa: o plano ativo, o proximo dia da rotacao e, se houver, a sessao em
 * andamento. Sem plano ativo (ou sem dias), planoId e proximoDia vem nulos.
 */
public record HojeResponse(
		UUID planoId,
		String planoNome,
		int totalDias,
		PlanoResponse.DiaResponse proximoDia,
		SessaoResponse sessaoEmAndamento) {
}
