package com.trainingapp.api.controller;

import com.trainingapp.api.dto.FeedbackRequest;
import com.trainingapp.api.service.FeedbackService;
import com.trainingapp.api.service.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Relato de bug ou sugestao: 404 com o feedback desligado, 429 acima do limite diario. */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

	private final UsuarioAtual usuarioAtual;
	private final FeedbackService service;

	public FeedbackController(UsuarioAtual usuarioAtual, FeedbackService service) {
		this.usuarioAtual = usuarioAtual;
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void enviar(@Valid @RequestBody FeedbackRequest request) {
		service.enviar(usuarioAtual.obter(), request);
	}
}
