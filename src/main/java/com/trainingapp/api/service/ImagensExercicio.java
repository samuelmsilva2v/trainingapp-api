package com.trainingapp.api.service;

import com.trainingapp.api.model.Exercicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * URLs das fotos de um exercicio do catalogo (posicao inicial e final). O free-exercise-db guarda as fotos em
 * {@code <slug>/0.jpg} e {@code <slug>/1.jpg}; a base fica em configuracao para trocar de hospedagem sem mexer no codigo.
 * Exercicio do usuario (sem slug) nao tem foto.
 */
@Component
public class ImagensExercicio {

	private final String base;

	public ImagensExercicio(@Value("${trainingapp.catalogo.imagens-url}") String base) {
		this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	public List<String> de(Exercicio exercicio) {
		String slug = exercicio.getSlug();
		return slug == null ? List.of() : List.of(base + "/" + slug + "/0.jpg", base + "/" + slug + "/1.jpg");
	}
}
