package com.trainingapp.api.config;

import org.springframework.http.HttpStatus;

/** Falha de autenticacao/autorizacao com o status HTTP que a resposta deve ter (401, 403, 409, 429). */
public class ErroDeAutenticacaoException extends RuntimeException {

	private final HttpStatus status;

	public ErroDeAutenticacaoException(HttpStatus status, String mensagem) {
		super(mensagem);
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}
}
