package com.trainingapp.api.config;

import com.trainingapp.api.dto.SessaoResponse;

/**
 * Escrita de uma sessao em andamento mais antiga que a salva (outro aparelho escreveu depois, ou um reenvio atrasado).
 * Vira 409 com a sessao atual no corpo, para o cliente adotar a versao do servidor em vez de achar que salvou.
 */
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
