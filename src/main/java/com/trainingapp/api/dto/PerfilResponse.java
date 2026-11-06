package com.trainingapp.api.dto;

import com.trainingapp.api.model.FotoTipo;
import com.trainingapp.api.model.Usuario;

public record PerfilResponse(String nome, String email, boolean temSenha, Integer lembretePesoDias, boolean feedbackAtivo,
		FotoTipo fotoTipo, String fotoGoogleUrl) {

	public static PerfilResponse de(Usuario usuario, boolean feedbackAtivo) {
		return new PerfilResponse(usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash() != null, usuario.getLembretePesoDias(),
				feedbackAtivo, usuario.getFotoTipo(), usuario.getFotoGoogleUrl());
	}
}
