package com.trainingapp.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Token de acesso (Bearer) e quantos segundos ele ainda vale. Na web o refresh vai so no cookie HttpOnly; no app
 * Android (header {@code X-Cliente: android}) o login devolve o refresh aqui, porque o cookie nao vale fora do navegador.
 */
public record AcessoResponse(String acesso, long expiraEmSegundos, @JsonInclude(JsonInclude.Include.NON_NULL) String refresh) {

	public AcessoResponse(String acesso, long expiraEmSegundos) {
		this(acesso, expiraEmSegundos, null);
	}

	public AcessoResponse comRefresh(String refresh) {
		return new AcessoResponse(acesso, expiraEmSegundos, refresh);
	}
}
