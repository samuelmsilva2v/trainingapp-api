package com.trainingapp.api.service;

import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.UsuarioRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UsuarioPadraoAtual implements UsuarioAtual {

	public static final UUID ID_USUARIO_PADRAO = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final UsuarioRepository repository;

	public UsuarioPadraoAtual(UsuarioRepository repository) {
		this.repository = repository;
	}

	@Override
	public Usuario obter() {
		return repository.findById(ID_USUARIO_PADRAO)
				.orElseThrow(() -> new IllegalStateException("Usuario padrao nao foi criado na inicializacao"));
	}
}
