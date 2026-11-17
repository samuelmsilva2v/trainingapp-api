package com.trainingapp.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record AcessoResponse(String acesso, long expiraEmSegundos, @JsonInclude(JsonInclude.Include.NON_NULL) String refresh) {

	public AcessoResponse(String acesso, long expiraEmSegundos) {
		this(acesso, expiraEmSegundos, null);
	}

	public AcessoResponse comRefresh(String refresh) {
		return new AcessoResponse(acesso, expiraEmSegundos, refresh);
	}
}
