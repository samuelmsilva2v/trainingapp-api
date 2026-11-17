package com.trainingapp.api.service;

public interface VerificadorGoogle {

	record Identidade(String sub, String email, boolean emailVerificado, String nome, String foto) {

		public Identidade(String sub, String email, boolean emailVerificado, String nome) {
			this(sub, email, emailVerificado, nome, null);
		}
	}

	Identidade verificarCodigo(String codigo);

	Identidade verificarIdToken(String idToken);
}
