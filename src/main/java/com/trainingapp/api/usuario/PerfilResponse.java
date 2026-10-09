package com.trainingapp.api.usuario;

public record PerfilResponse(String nome, String fusoHorario) {

	static PerfilResponse de(Usuario usuario) {
		return new PerfilResponse(usuario.getNome(), usuario.getFusoHorario());
	}
}
