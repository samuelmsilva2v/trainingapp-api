package com.trainingapp.api.dto;

import com.trainingapp.api.model.Usuario;

public record PerfilResponse(String nome, String email, boolean temSenha, Integer lembretePesoDias, boolean feedbackAtivo) {

	public static PerfilResponse de(Usuario usuario, boolean feedbackAtivo) {
		return new PerfilResponse(usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash() != null, usuario.getLembretePesoDias(),
				feedbackAtivo);
	}
}
