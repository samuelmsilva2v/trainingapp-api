package com.trainingapp.api.config;

import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.UsuarioRepository;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import static com.trainingapp.api.service.UsuarioPadraoAtual.ID_USUARIO_PADRAO;

@Component
public class UsuarioPadraoInicializador implements SmartInitializingSingleton {

	private final UsuarioRepository repository;

	public UsuarioPadraoInicializador(UsuarioRepository repository) {
		this.repository = repository;
	}

	// Roda antes de o servidor abrir a porta; um ApplicationRunner deixaria uma janela de 500.
	@Override
	public void afterSingletonsInstantiated() {
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
