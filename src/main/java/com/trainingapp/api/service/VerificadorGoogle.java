package com.trainingapp.api.service;

/** Descobre quem e o dono de um login com Google. Lanca 401 se o login nao vale e 503 se o Google nao esta configurado ou fora do ar. */
public interface VerificadorGoogle {

	/** foto: claim "picture" (URL da foto da conta Google), quando houver. */
	record Identidade(String sub, String email, boolean emailVerificado, String nome, String foto) {

		public Identidade(String sub, String email, boolean emailVerificado, String nome) {
			this(sub, email, emailVerificado, nome, null);
		}
	}

	/** Troca o codigo de autorizacao que o popup do Google entregou ao front pelos tokens e valida o ID token. */
	Identidade verificarCodigo(String codigo);

	/** Valida um ID token que o app Android recebeu do Credential Manager (destinatario = nosso client id web). */
	Identidade verificarIdToken(String idToken);
}
