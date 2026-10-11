package com.trainingapp.api;

import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.UsuarioRepository;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * O usuario que os testes de API usam via {@code @WithMockUser(username = UsuarioDeTeste.ID)}.
 * Criado na subida do contexto, so no perfil de teste (o app nao tem mais usuario padrao).
 */
@Component
@Profile("test")
public class UsuarioDeTeste implements SmartInitializingSingleton {

	public static final String ID = "00000000-0000-0000-0000-000000000001";
	public static final String EMAIL = "atleta@teste.com";

	private final UsuarioRepository repository;

	public UsuarioDeTeste(UsuarioRepository repository) {
		this.repository = repository;
	}

	@Override
	public void afterSingletonsInstantiated() {
		UUID id = UUID.fromString(ID);
		if (repository.existsById(id)) {
			return;
		}
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setNome("Atleta");
		usuario.setEmail(EMAIL);
		usuario.setEmailVerificado(true);
		repository.save(usuario);
	}
}
