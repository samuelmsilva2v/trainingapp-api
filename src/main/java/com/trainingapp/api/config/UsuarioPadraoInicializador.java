package com.trainingapp.api.config;

import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static com.trainingapp.api.service.UsuarioPadraoAtual.ID_USUARIO_PADRAO;

@Component
public class UsuarioPadraoInicializador implements ApplicationRunner {

	private final UsuarioRepository repository;

	public UsuarioPadraoInicializador(UsuarioRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (repository.existsById(ID_USUARIO_PADRAO)) {
			return;
		}
		Usuario usuario = new Usuario();
		usuario.setId(ID_USUARIO_PADRAO);
		usuario.setNome("Atleta");
		usuario.setFusoHorario("America/Sao_Paulo");
		repository.save(usuario);
	}
}
