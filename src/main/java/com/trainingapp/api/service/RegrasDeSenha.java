package com.trainingapp.api.service;

import java.nio.charset.StandardCharsets;

final class RegrasDeSenha {

	static final int MINIMO = 8;
	static final int MAXIMO_BYTES = 72;
	static final String MENSAGEM = "A senha precisa ter 8 ou mais caracteres, com letra maiúscula, letra minúscula, número e símbolo";

	private RegrasDeSenha() {
	}

	static String problema(String senha) {
		if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
			return "A senha pode ter no máximo 72 bytes (menos caracteres se tiver acentos)";
		}
		return forte(senha) ? null : MENSAGEM;
	}

	static boolean forte(String senha) {
		boolean minuscula = false;
		boolean maiuscula = false;
		boolean numero = false;
		boolean simbolo = false;
		for (int c : senha.codePoints().toArray()) {
			if (Character.isLowerCase(c)) {
				minuscula = true;
			}
			else if (Character.isUpperCase(c)) {
				maiuscula = true;
			}
			else if (Character.isDigit(c)) {
				numero = true;
			}
			else if (!Character.isLetter(c)) {
				simbolo = true;
			}
		}
		return senha.codePointCount(0, senha.length()) >= MINIMO && minuscula && maiuscula && numero && simbolo;
	}
}
