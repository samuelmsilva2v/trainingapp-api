package com.trainingapp.api.controller;

import com.trainingapp.api.dto.GamificacaoResponse;
import com.trainingapp.api.service.GamificacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamificacao")
public class GamificacaoController {

	private final GamificacaoService service;

	public GamificacaoController(GamificacaoService service) {
		this.service = service;
	}

	@GetMapping
	public GamificacaoResponse resumo() {
		return service.resumo();
	}
}
