package com.trainingapp.api.dto;

import com.trainingapp.api.model.Usuario;

public record PerfilResponse(String nome, String email, boolean temSenha) {

	public static PerfilResponse de(Usuario usuario) {
		return new PerfilResponse(usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash() != null);
	}
}
