package com.trainingapp.api.dto;

import com.trainingapp.api.model.Usuario;

public record PerfilResponse(String nome, String fusoHorario) {

	public static PerfilResponse de(Usuario usuario) {
		return new PerfilResponse(usuario.getNome(), usuario.getFusoHorario());
	}
}
