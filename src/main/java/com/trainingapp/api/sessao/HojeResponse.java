package com.trainingapp.api.sessao;

import com.trainingapp.api.plano.PlanoResponse;
import java.util.UUID;

/**
 * O que a tela "Hoje" precisa: o plano ativo, o proximo dia da rotacao e, se houver, a sessao em
 * andamento. Sem plano ativo (ou sem dias), planoId e proximoDia vem nulos.
 */
public record HojeResponse(
		UUID planoId,
		String planoNome,
		PlanoResponse.DiaResponse proximoDia,
		SessaoResponse sessaoEmAndamento) {
}
