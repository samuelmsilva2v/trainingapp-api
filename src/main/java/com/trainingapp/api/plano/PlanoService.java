package com.trainingapp.api.plano;

import com.trainingapp.api.config.RecursoNaoEncontradoException;
import com.trainingapp.api.config.RegraDeNegocioException;
import com.trainingapp.api.exercicio.Exercicio;
import com.trainingapp.api.exercicio.ExercicioRepository;
import com.trainingapp.api.usuario.Usuario;
import com.trainingapp.api.usuario.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlanoService {

	private final PlanoRepository planos;
	private final ExercicioRepository exercicios;
	private final UsuarioAtual usuarioAtual;

	public PlanoService(PlanoRepository planos, ExercicioRepository exercicios, UsuarioAtual usuarioAtual) {
		this.planos = planos;
		this.exercicios = exercicios;
		this.usuarioAtual = usuarioAtual;
	}

	@Transactional(readOnly = true)
	public List<PlanoResumoResponse> listar() {
		return planos.findByUsuarioIdOrderByCriadoEmDesc(usuarioAtual.obter().getId()).stream()
				.map(PlanoResumoResponse::de)
				.toList();
	}

	@Transactional(readOnly = true)
	public PlanoResponse obter(UUID id) {
		return PlanoResponse.de(buscar(id));
	}

	public PlanoResponse criar(PlanoRequest request) {
		Usuario usuario = usuarioAtual.obter();
		Plano plano = new Plano();
		plano.setUsuario(usuario);
		// O primeiro plano do usuario ja nasce ativo.
		plano.setAtivo(!planos.existsByUsuarioIdAndAtivoTrue(usuario.getId()));
		aplicar(plano, request, usuario);
		return PlanoResponse.de(planos.save(plano));
	}

	public PlanoResponse atualizar(UUID id, PlanoRequest request) {
		Plano plano = buscar(id);
		plano.getDias().clear();
		aplicar(plano, request, usuarioAtual.obter());
		return PlanoResponse.de(plano);
	}

	public void excluir(UUID id) {
		planos.delete(buscar(id));
	}

	public PlanoResponse ativar(UUID id) {
		Plano alvo = buscar(id);
		planos.findByUsuarioIdOrderByCriadoEmDesc(alvo.getUsuario().getId())
				.forEach(p -> p.setAtivo(p.getId().equals(alvo.getId())));
		return PlanoResponse.de(alvo);
	}

	private Plano buscar(UUID id) {
		return planos.findByIdAndUsuarioId(id, usuarioAtual.obter().getId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Plano nao encontrado: " + id));
	}

	private void aplicar(Plano plano, PlanoRequest request, Usuario usuario) {
		plano.setNome(request.nome().trim());
		plano.setObjetivo(request.objetivo());

		Map<UUID, Exercicio> disponiveis = carregarExercicios(request, usuario);

		for (int d = 0; d < request.dias().size(); d++) {
			PlanoRequest.DiaRequest diaReq = request.dias().get(d);
			DiaTreino dia = new DiaTreino();
			dia.setPlano(plano);
			dia.setNome(diaReq.nome().trim());
			dia.setOrdem(d);

			for (int i = 0; i < diaReq.exercicios().size(); i++) {
				PlanoRequest.ItemRequest itemReq = diaReq.exercicios().get(i);
				if (itemReq.repsMin() > itemReq.repsMax()) {
					throw new RegraDeNegocioException(
							"repsMin nao pode ser maior que repsMax (dia '" + dia.getNome() + "', exercicio " + (i + 1) + ")");
				}
				ExercicioPlano item = new ExercicioPlano();
				item.setDia(dia);
				item.setExercicio(disponiveis.get(itemReq.exercicioId()));
				item.setOrdem(i);
				item.setSeries(itemReq.series());
				item.setRepsMin(itemReq.repsMin());
				item.setRepsMax(itemReq.repsMax());
				item.setCargaAlvo(itemReq.cargaAlvo());
				item.setDescansoSegundos(itemReq.descansoSegundos());
				dia.getExercicios().add(item);
			}
			plano.getDias().add(dia);
		}
	}

	/** Carrega os exercicios citados e garante que sao do catalogo ou do proprio usuario. */
	private Map<UUID, Exercicio> carregarExercicios(PlanoRequest request, Usuario usuario) {
		List<UUID> ids = request.dias().stream()
				.flatMap(d -> d.exercicios().stream())
				.map(PlanoRequest.ItemRequest::exercicioId)
				.distinct()
				.toList();
		Map<UUID, Exercicio> encontrados = exercicios.findAllById(ids).stream()
				.filter(e -> e.getUsuario() == null || e.getUsuario().getId().equals(usuario.getId()))
				.collect(Collectors.toMap(Exercicio::getId, Function.identity()));
		for (UUID id : ids) {
			if (!encontrados.containsKey(id)) {
				throw new RegraDeNegocioException("Exercicio nao encontrado: " + id);
			}
		}
		return encontrados;
	}
}
