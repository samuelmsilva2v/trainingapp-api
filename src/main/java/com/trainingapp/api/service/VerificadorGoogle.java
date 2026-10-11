package com.trainingapp.api.service;

/** Valida o ID token que o front recebeu do Google e devolve quem e o dono. Lanca 401 se o token nao vale. */
public interface VerificadorGoogle {

	record Identidade(String sub, String email, boolean emailVerificado, String nome) {
	}

	Identidade verificar(String credencial);
}
