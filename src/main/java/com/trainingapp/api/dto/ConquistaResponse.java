package com.trainingapp.api.dto;

/** Faixa de uma familia de conquistas: `faixa` comeca em 1 e `progresso` vai de 0 ate `meta`. */
public record ConquistaResponse(String codigo, String nome, String descricao, boolean conquistada, String familia,
		int faixa, int progresso, int meta) {
}
