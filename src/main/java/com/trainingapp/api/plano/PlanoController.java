package com.trainingapp.api.plano;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/planos")
public class PlanoController {

	private final PlanoService service;

	public PlanoController(PlanoService service) {
		this.service = service;
	}

	@GetMapping
	public List<PlanoResumoResponse> listar() {
		return service.listar();
	}

	@GetMapping("/{id}")
	public PlanoResponse obter(@PathVariable UUID id) {
		return service.obter(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PlanoResponse criar(@Valid @RequestBody PlanoRequest request) {
		return service.criar(request);
	}

	@PutMapping("/{id}")
	public PlanoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PlanoRequest request) {
		return service.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable UUID id) {
		service.excluir(id);
	}

	@PostMapping("/{id}/ativar")
	public PlanoResponse ativar(@PathVariable UUID id) {
		return service.ativar(id);
	}
}
