package com.trainingapp.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class Tokens {

	private static final SecureRandom ALEATORIO = new SecureRandom();

	private Tokens() {
	}

	static String novo() {
		return novo(32);
	}

	/** Token aleatorio em base64url: {@code bytes} bytes viram {@code ceil(bytes * 4 / 3)} caracteres. */
	static String novo(int tamanhoEmBytes) {
		byte[] bytes = new byte[tamanhoEmBytes];
		ALEATORIO.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
