package com.trainingapp.api.controller;

import com.trainingapp.api.dto.PerfilRequest;
import com.trainingapp.api.dto.PerfilResponse;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.UsuarioRepository;
import com.trainingapp.api.service.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

	private final UsuarioAtual usuarioAtual;
	private final UsuarioRepository repository;

	public PerfilController(UsuarioAtual usuarioAtual, UsuarioRepository repository) {
		this.usuarioAtual = usuarioAtual;
		this.repository = repository;
	}

	@GetMapping
	public PerfilResponse obter() {
		return PerfilResponse.de(usuarioAtual.obter());
	}

	@PutMapping
	@Transactional
	public PerfilResponse atualizar(@Valid @RequestBody PerfilRequest request) {
		Usuario usuario = usuarioAtual.obter();
		usuario.setNome(request.nome());
		return PerfilResponse.de(repository.save(usuario));
	}
}
