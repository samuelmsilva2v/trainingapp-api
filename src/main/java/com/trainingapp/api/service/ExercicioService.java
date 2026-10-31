package com.trainingapp.api.service;

import com.trainingapp.api.config.RegraDeNegocioException;
import com.trainingapp.api.dto.ExercicioRequest;
import com.trainingapp.api.dto.ExercicioResponse;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.ExercicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExercicioService {

	/** Teto de exercicios proprios por conta: com o cadastro aberto, um POST em laco nao incha o banco. */
	public static final int LIMITE_POR_USUARIO = 200;

	private final ExercicioRepository repository;
	private final UsuarioAtual usuarioAtual;
	private final ImagensExercicio imagens;

	public ExercicioService(ExercicioRepository repository, UsuarioAtual usuarioAtual, ImagensExercicio imagens) {
		this.repository = repository;
		this.usuarioAtual = usuarioAtual;
		this.imagens = imagens;
	}

	@Transactional(readOnly = true)
	public List<ExercicioResponse> listar(GrupoMuscular grupo, String busca) {
		String termo = busca == null || busca.isBlank() ? null : normalizar(busca);
		return repository.findDisponiveis(usuarioAtual.obter().getId()).stream()
				.filter(e -> grupo == null || e.getGrupoMuscular() == grupo)
				.filter(e -> termo == null || normalizar(e.getNome()).contains(termo))
				.map(e -> ExercicioResponse.de(e, imagens))
				.toList();
	}

	public ExercicioResponse criar(ExercicioRequest request) {
		Usuario usuario = usuarioAtual.obter();
		if (repository.countByUsuarioId(usuario.getId()) >= LIMITE_POR_USUARIO) {
			throw new RegraDeNegocioException("Limite de " + LIMITE_POR_USUARIO + " exercícios próprios atingido");
		}
		Exercicio exercicio = new Exercicio();
		exercicio.setNome(request.nome().trim());
		exercicio.setGrupoMuscular(request.grupoMuscular());
		exercicio.setEquipamento(request.equipamento());
		exercicio.setUsuario(usuario);
		return ExercicioResponse.de(repository.save(exercicio), imagens);
	}

	/**
	 * Os exercicios citados, garantindo que cada um e do catalogo ou do proprio usuario (nunca o exercicio proprio de
	 * outra conta). Usado por planos e sessoes: e esta checagem que isola os exercicios entre usuarios.
	 */
	@Transactional(readOnly = true)
	public Map<UUID, Exercicio> disponiveis(Collection<UUID> ids, Usuario usuario) {
		Map<UUID, Exercicio> encontrados = repository.findAllById(ids).stream()
				.filter(e -> e.getUsuario() == null || e.getUsuario().getId().equals(usuario.getId()))
				.collect(Collectors.toMap(Exercicio::getId, Function.identity()));
		for (UUID id : ids) {
			if (!encontrados.containsKey(id)) {
				throw new RegraDeNegocioException("Exercicio nao encontrado: " + id);
			}
		}
		return encontrados;
	}

	// Busca ignora acento e caixa: "agachamento" acha "Agachamento", "triceps" acha "Tríceps".
	private static String normalizar(String texto) {
		return Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.trim();
	}
}
