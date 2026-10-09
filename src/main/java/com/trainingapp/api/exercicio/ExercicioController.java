package com.trainingapp.api.exercicio;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exercicios")
public class ExercicioController {

	private final ExercicioService service;

	public ExercicioController(ExercicioService service) {
		this.service = service;
	}

	@GetMapping
	public List<ExercicioResponse> listar(@RequestParam(required = false) GrupoMuscular grupo,
			@RequestParam(required = false) String busca) {
		return service.listar(grupo, busca);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ExercicioResponse criar(@Valid @RequestBody ExercicioRequest request) {
		return service.criar(request);
	}
}
