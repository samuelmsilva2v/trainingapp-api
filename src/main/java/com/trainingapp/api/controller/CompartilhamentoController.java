package com.trainingapp.api.controller;

import com.trainingapp.api.dto.PlanoCompartilhadoResponse;
import com.trainingapp.api.dto.PlanoResponse;
import com.trainingapp.api.service.CompartilhamentoDePlano;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CompartilhamentoController {

	private final CompartilhamentoDePlano service;

	public CompartilhamentoController(CompartilhamentoDePlano service) {
		this.service = service;
	}

	@PostMapping("/api/planos/{id}/compartilhar")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanoCompartilhadoResponse.LinkResponse compartilhar(@PathVariable UUID id) {
		return service.compartilhar(id);
	}

	@GetMapping("/api/compartilhados/{token}")
	public PlanoCompartilhadoResponse previa(@PathVariable String token) {
		return service.previa(token);
	}

	@PostMapping("/api/compartilhados/{token}/salvar")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanoResponse salvar(@PathVariable String token, @RequestParam(defaultValue = "false") boolean ativar) {
		return service.salvar(token, ativar);
	}
}
