package com.trainingapp.api.config;

import com.trainingapp.api.dto.SessaoResponse;

public class SessaoDesatualizadaException extends RuntimeException {

	private final transient SessaoResponse atual;

	public SessaoDesatualizadaException(SessaoResponse atual) {
		super("A sessao foi alterada depois desta escrita");
		this.atual = atual;
	}

	public SessaoResponse atual() {
		return atual;
	}
}
