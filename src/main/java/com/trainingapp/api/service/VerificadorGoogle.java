package com.trainingapp.api.service;

/** Descobre quem e o dono de um login com Google. Lanca 401 se o login nao vale e 503 se o Google nao esta configurado ou fora do ar. */
public interface VerificadorGoogle {

	record Identidade(String sub, String email, boolean emailVerificado, String nome) {
	}

	/** Troca o codigo de autorizacao que o popup do Google entregou ao front pelos tokens e valida o ID token. */
	Identidade verificarCodigo(String codigo);
}
