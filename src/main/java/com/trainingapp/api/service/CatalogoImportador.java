package com.trainingapp.api.service;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.repository.ExercicioRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Importa o catalogo empacotado em catalogo/exercicios.json. Idempotente: o slug
 * identifica cada exercicio, entao reexecutar atualiza em vez de duplicar, e os
 * exercicios criados pelo usuario (sem slug) nunca sao tocados.
 */
@Component
public class CatalogoImportador {

	static final String ARQUIVO = "catalogo/exercicios.json";

	record ItemCatalogo(String slug, String nome, GrupoMuscular grupoMuscular, Equipamento equipamento) {
	}

	private final ExercicioRepository repository;
	private final JsonMapper jsonMapper;

	public CatalogoImportador(ExercicioRepository repository, JsonMapper jsonMapper) {
		this.repository = repository;
		this.jsonMapper = jsonMapper;
	}

	@Transactional
	public int importar() {
		List<ItemCatalogo> itens = ler();
		for (ItemCatalogo item : itens) {
			Exercicio exercicio = repository.findBySlug(item.slug()).orElseGet(Exercicio::new);
			exercicio.setSlug(item.slug());
			exercicio.setNome(item.nome());
			exercicio.setGrupoMuscular(item.grupoMuscular());
			exercicio.setEquipamento(item.equipamento());
			repository.save(exercicio);
		}
		return itens.size();
	}

	private List<ItemCatalogo> ler() {
		try (InputStream in = new ClassPathResource(ARQUIVO).getInputStream()) {
			return jsonMapper.readValue(in, new TypeReference<List<ItemCatalogo>>() {
			});
		}
		catch (IOException e) {
			throw new UncheckedIOException("Falha ao ler " + ARQUIVO, e);
		}
	}
}
