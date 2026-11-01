package com.trainingapp.api.model;

public enum CategoriaFeedback {
	BUG("Bug"), MELHORIA("Melhoria"), FUNCIONALIDADE("Funcionalidade"), OUTRO("Outro");

	private final String rotulo;

	CategoriaFeedback(String rotulo) {
		this.rotulo = rotulo;
	}

	public String rotulo() {
		return rotulo;
	}
}
