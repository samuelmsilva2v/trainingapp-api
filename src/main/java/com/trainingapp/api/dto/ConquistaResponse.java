package com.trainingapp.api.dto;

public record ConquistaResponse(String codigo, String nome, String descricao, boolean conquistada, String familia,
		int faixa, int progresso, int meta, boolean oculta) {
}
