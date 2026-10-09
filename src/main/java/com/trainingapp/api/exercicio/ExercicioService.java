package com.trainingapp.api.exercicio;

import com.trainingapp.api.usuario.Usuario;
import com.trainingapp.api.usuario.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ExercicioService {

	private final ExercicioRepository repository;
	private final UsuarioAtual usuarioAtual;

	public ExercicioService(ExercicioRepository repository, UsuarioAtual usuarioAtual) {
		this.repository = repository;
		this.usuarioAtual = usuarioAtual;
	}

	@Transactional(readOnly = true)
	public List<ExercicioResponse> listar(GrupoMuscular grupo, String busca) {
		String termo = busca == null || busca.isBlank() ? null : normalizar(busca);
		return repository.findDisponiveis(usuarioAtual.obter().getId()).stream()
				.filter(e -> grupo == null || e.getGrupoMuscular() == grupo)
				.filter(e -> termo == null || normalizar(e.getNome()).contains(termo))
				.map(ExercicioResponse::de)
				.toList();
	}

	public ExercicioResponse criar(ExercicioRequest request) {
		Usuario usuario = usuarioAtual.obter();
		Exercicio exercicio = new Exercicio();
		exercicio.setNome(request.nome().trim());
		exercicio.setGrupoMuscular(request.grupoMuscular());
		exercicio.setEquipamento(request.equipamento());
		exercicio.setUsuario(usuario);
		return ExercicioResponse.de(repository.save(exercicio));
	}

	// Busca ignora acento e caixa: "agachamento" acha "Agachamento", "triceps" acha "Tríceps".
	private static String normalizar(String texto) {
		return Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.trim();
	}
}
