package com.trainingapp.api.config;

import org.springframework.http.HttpStatus;

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
