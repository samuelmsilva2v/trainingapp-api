package com.trainingapp.api.service;

import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** O nome da autenticacao e o id do usuario (claim "sub" do JWT). */
@Component
public class UsuarioAutenticado implements UsuarioAtual {

	private final UsuarioRepository repository;

	public UsuarioAutenticado(UsuarioRepository repository) {
		this.repository = repository;
	}

	@Override
	public Usuario obter() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		try {
			return repository.findById(UUID.fromString(auth.getName()))
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		}
		catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
	}
}
