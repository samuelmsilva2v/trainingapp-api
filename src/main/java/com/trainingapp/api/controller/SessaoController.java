package com.trainingapp.api.controller;

import com.trainingapp.api.dto.HojeResponse;
import com.trainingapp.api.dto.SessaoRequest;
import com.trainingapp.api.dto.SessaoResponse;
import com.trainingapp.api.dto.SessaoResumoResponse;
import com.trainingapp.api.service.SessaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Validated
public class SessaoController {

	private final SessaoService service;

	public SessaoController(SessaoService service) {
		this.service = service;
	}

	@GetMapping("/hoje")
	public HojeResponse hoje() {
		return service.hoje();
	}

	@PostMapping("/hoje/pular")
	public HojeResponse pular() {
		return service.pular();
	}

	@GetMapping("/sessoes")
	public List<SessaoResumoResponse> historico(@RequestParam(defaultValue = "0") @Min(0) int pagina,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
		return service.historico(pagina, tamanho);
	}

	@GetMapping("/sessoes/{id}")
	public SessaoResponse obter(@PathVariable UUID id) {
		return service.obter(id);
	}

	@DeleteMapping("/sessoes/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable UUID id) {
		service.excluir(id);
	}

	@PutMapping("/sessoes/{id}")
	public SessaoResponse salvar(@PathVariable UUID id, @Valid @RequestBody SessaoRequest request) {
		return service.salvar(id, request);
	}
}
