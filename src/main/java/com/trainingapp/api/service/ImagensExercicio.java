package com.trainingapp.api.service;

import com.trainingapp.api.model.Exercicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

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
