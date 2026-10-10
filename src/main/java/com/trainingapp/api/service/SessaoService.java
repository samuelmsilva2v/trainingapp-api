package com.trainingapp.api.service;

import com.trainingapp.api.config.RecursoNaoEncontradoException;
import com.trainingapp.api.config.RegraDeNegocioException;
import com.trainingapp.api.dto.ExercicioRequest;
import com.trainingapp.api.dto.HojeResponse;
import com.trainingapp.api.dto.PlanoResponse;
import com.trainingapp.api.dto.SessaoRequest;
import com.trainingapp.api.dto.SessaoResponse;
import com.trainingapp.api.dto.SessaoResumoResponse;
import com.trainingapp.api.model.EstadoSessao;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.ExercicioSessao;
import com.trainingapp.api.model.Plano;
import com.trainingapp.api.model.SerieSessao;
import com.trainingapp.api.model.Sessao;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.ExercicioRepository;
import com.trainingapp.api.repository.PlanoRepository;
import com.trainingapp.api.repository.SessaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SessaoService {

	// Sem registro retroativo: a sessao precisa ter comecado recentemente.
	private static final Duration JANELA_INICIO = Duration.ofHours(24);
	private static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(5);

	private final SessaoRepository sessoes;
	private final PlanoRepository planos;
	private final ExercicioRepository exercicios;
	private final GamificacaoService gamificacao;
	private final UsuarioAtual usuarioAtual;

	public SessaoService(SessaoRepository sessoes, PlanoRepository planos, ExercicioRepository exercicios,
			GamificacaoService gamificacao, UsuarioAtual usuarioAtual) {
		this.gamificacao = gamificacao;
		this.sessoes = sessoes;
		this.planos = planos;
		this.exercicios = exercicios;
		this.usuarioAtual = usuarioAtual;
	}

	@Transactional(readOnly = true)
	public HojeResponse hoje() {
		UUID usuarioId = usuarioAtual.obter().getId();
		SessaoResponse emAndamento = sessoes.findFirstByUsuarioIdAndEstado(usuarioId, EstadoSessao.EM_ANDAMENTO)
				.map(this::resposta)
				.orElse(null);
		Optional<Plano> ativo = planos.findFirstByUsuarioIdAndAtivoTrue(usuarioId);
		if (ativo.isEmpty()) {
			return new HojeResponse(null, null, 0, null, emAndamento);
		}
		Plano plano = ativo.get();
		PlanoResponse.DiaResponse proximo = plano.getDias().isEmpty()
				? null
				: PlanoResponse.de(plano).dias().get(plano.indiceProximoDia());
		return new HojeResponse(plano.getId(), plano.getNome(), plano.getDias().size(), proximo, emAndamento);
	}

	/** Pula o dia da rotacao sem registrar treino. */
	public HojeResponse pular() {
		UUID usuarioId = usuarioAtual.obter().getId();
		if (sessoes.findFirstByUsuarioIdAndEstado(usuarioId, EstadoSessao.EM_ANDAMENTO).isPresent()) {
			throw new RegraDeNegocioException("Conclua ou abandone o treino em andamento antes de pular o dia");
		}
		Plano plano = planos.findFirstByUsuarioIdAndAtivoTrue(usuarioId)
				.orElseThrow(() -> new RegraDeNegocioException("Nao ha plano ativo"));
		if (plano.getDias().isEmpty()) {
			throw new RegraDeNegocioException("O plano ativo nao tem dias de treino");
		}
		plano.avancarApos(plano.indiceProximoDia());
		return hoje();
	}

	@Transactional(readOnly = true)
	public List<SessaoResumoResponse> historico(int pagina, int tamanho) {
		List<Sessao> sessoesDaPagina = sessoes.findByUsuarioIdAndEstadoInOrderByIniciadaEmDesc(usuarioAtual.obter().getId(),
				EnumSet.of(EstadoSessao.CONCLUIDA, EstadoSessao.ABANDONADA), PageRequest.of(pagina, tamanho));
		Map<UUID, GamificacaoService.Ganho> ganhos = gamificacao.ganhos(sessoesDaPagina.stream().map(Sessao::getId).toList());
		return sessoesDaPagina.stream()
				.map(s -> SessaoResumoResponse.de(s, ganhos.getOrDefault(s.getId(), GamificacaoService.Ganho.NENHUM)))
				.toList();
	}

	@Transactional(readOnly = true)
	public SessaoResponse obter(UUID id) {
		return resposta(sessoes.findByIdAndUsuarioId(id, usuarioAtual.obter().getId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Sessao nao encontrada: " + id)));
	}

	/** Apaga um treino encerrado e estorna o XP e os recordes dele; a rotacao nao volta atras. */
	public void excluir(UUID id) {
		Sessao sessao = sessoes.findByIdAndUsuarioId(id, usuarioAtual.obter().getId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Sessao nao encontrada: " + id));
		if (sessao.getEstado() == EstadoSessao.EM_ANDAMENTO) {
			throw new RegraDeNegocioException("Abandone o treino em andamento antes de apagar");
		}
		gamificacao.estornar(id);
		sessoes.delete(sessao);
	}

	/**
	 * Upsert idempotente. Sessao encerrada nao muda mais (reenvios devolvem o que ja esta salvo) e,
	 * em andamento, uma escrita mais antiga que a salva e ignorada: o ultimo a escrever vence.
	 */
	public SessaoResponse salvar(UUID id, SessaoRequest request) {
		Usuario usuario = usuarioAtual.obter();
		Sessao existente = sessoes.findByIdAndUsuarioId(id, usuario.getId()).orElse(null);
		if (existente == null && sessoes.existsById(id)) {
			throw new RegraDeNegocioException("Id de sessao ja utilizado: " + id);
		}
		if (existente != null) {
			boolean encerrada = existente.getEstado() != EstadoSessao.EM_ANDAMENTO;
			if (encerrada || request.atualizadoEm().isBefore(existente.getAtualizadoEm())) {
				return resposta(existente);
			}
		}

		validar(request, existente == null);
		validarUnicaEmAndamento(id, request, usuario);

		Sessao sessao = existente != null ? existente : nova(id, usuario);
		boolean concluindo = request.estado() == EstadoSessao.CONCLUIDA;

		sessao.setPlanoId(request.planoId());
		sessao.setPlanoNome(request.planoNome().trim());
		sessao.setDiaNome(request.diaNome().trim());
		sessao.setDiaOrdem(request.diaOrdem());
		sessao.setEstado(request.estado());
		sessao.setAndamentoUsuarioId(request.estado() == EstadoSessao.EM_ANDAMENTO ? usuario.getId() : null);
		sessao.setIniciadaEm(request.iniciadaEm());
		sessao.setFinalizadaEm(request.finalizadaEm());
		sessao.setAtualizadoEm(request.atualizadoEm());
		reconciliar(sessao, request, usuario);

		if (concluindo && request.planoId() != null) {
			planos.findByIdAndUsuarioId(request.planoId(), usuario.getId())
					.ifPresent(plano -> plano.avancarApos(request.diaOrdem()));
		}
		Sessao salva = sessoes.saveAndFlush(sessao);
		if (concluindo) {
			gamificacao.registrarSessaoConcluida(salva);
		}
		return resposta(salva);
	}

	private SessaoResponse resposta(Sessao sessao) {
		return SessaoResponse.de(sessao,
				gamificacao.ganhos(List.of(sessao.getId())).getOrDefault(sessao.getId(), GamificacaoService.Ganho.NENHUM));
	}

	private Sessao nova(UUID id, Usuario usuario) {
		Sessao sessao = new Sessao();
		sessao.setId(id);
		sessao.setUsuario(usuario);
		return sessao;
	}

	private void validar(SessaoRequest request, boolean criacao) {
		boolean encerrada = request.estado() != EstadoSessao.EM_ANDAMENTO;
		if (encerrada && request.finalizadaEm() == null) {
			throw new RegraDeNegocioException("finalizadaEm e obrigatorio em sessao concluida ou abandonada");
		}
		if (!encerrada && request.finalizadaEm() != null) {
			throw new RegraDeNegocioException("finalizadaEm so vale para sessao concluida ou abandonada");
		}
		if (request.finalizadaEm() != null && request.finalizadaEm().isBefore(request.iniciadaEm())) {
			throw new RegraDeNegocioException("finalizadaEm nao pode ser anterior a iniciadaEm");
		}
		Instant agora = Instant.now();
		if (request.iniciadaEm().isAfter(agora.plus(TOLERANCIA_RELOGIO))) {
			throw new RegraDeNegocioException("iniciadaEm nao pode estar no futuro");
		}
		if (request.atualizadoEm().isAfter(agora.plus(TOLERANCIA_RELOGIO))) {
			throw new RegraDeNegocioException("atualizadoEm nao pode estar no futuro");
		}
		if (request.finalizadaEm() != null && request.finalizadaEm().isAfter(agora.plus(TOLERANCIA_RELOGIO))) {
			throw new RegraDeNegocioException("finalizadaEm nao pode estar no futuro");
		}
		if (criacao && request.iniciadaEm().isBefore(agora.minus(JANELA_INICIO))) {
			throw new RegraDeNegocioException("Nao e possivel registrar treino retroativo");
		}
		Set<UUID> ids = new HashSet<>();
		for (SessaoRequest.ExercicioRequest e : request.exercicios()) {
			if (!ids.add(e.id())) {
				throw new RegraDeNegocioException("Id repetido na sessao: " + e.id());
			}
			if (e.metaRepsMin() > e.metaRepsMax()) {
				throw new RegraDeNegocioException("metaRepsMin nao pode ser maior que metaRepsMax");
			}
			for (SessaoRequest.SerieRequest s : e.series()) {
				if (!ids.add(s.id())) {
					throw new RegraDeNegocioException("Id repetido na sessao: " + s.id());
				}
				if (s.concluida() && (s.reps() == null || s.reps() < 1)) {
					throw new RegraDeNegocioException("Serie concluida precisa de pelo menos 1 repeticao");
				}
			}
		}
	}

	private void validarUnicaEmAndamento(UUID id, SessaoRequest request, Usuario usuario) {
		if (request.estado() != EstadoSessao.EM_ANDAMENTO) {
			return;
		}
		sessoes.findFirstByUsuarioIdAndEstado(usuario.getId(), EstadoSessao.EM_ANDAMENTO)
				.filter(outra -> !outra.getId().equals(id))
				.ifPresent(outra -> {
					throw new RegraDeNegocioException("Ja existe um treino em andamento");
				});
	}

	/**
	 * Atualiza as colecoes no lugar, casando por id. Recriar tudo faria o Hibernate inserir antes
	 * de apagar e estourar a chave primaria dos ids reaproveitados pelo cliente.
	 */
	private void reconciliar(Sessao sessao, SessaoRequest request, Usuario usuario) {
		Map<UUID, Exercicio> catalogo = carregarExercicios(request, usuario);
		Map<UUID, ExercicioSessao> atuais = sessao.getExercicios().stream()
				.collect(Collectors.toMap(ExercicioSessao::getId, Function.identity()));

		List<ExercicioSessao> resultado = new ArrayList<>();
		for (int i = 0; i < request.exercicios().size(); i++) {
			SessaoRequest.ExercicioRequest req = request.exercicios().get(i);
			ExercicioSessao ex = atuais.get(req.id());
			if (ex == null) {
				ex = new ExercicioSessao();
				ex.setId(req.id());
				ex.setSessao(sessao);
			}
			Exercicio exercicio = catalogo.get(req.exercicioId());
			ex.setExercicio(exercicio);
			ex.setNome(exercicio.getNome());
			ex.setOrdem(i);
			ex.setMetaSeries(req.metaSeries());
			ex.setMetaRepsMin(req.metaRepsMin());
			ex.setMetaRepsMax(req.metaRepsMax());
			ex.setMetaCarga(req.metaCarga());
			ex.setMetaDescansoSegundos(req.metaDescansoSegundos());
			reconciliarSeries(ex, req);
			resultado.add(ex);
		}
		sessao.getExercicios().clear();
		sessao.getExercicios().addAll(resultado);
	}

	private void reconciliarSeries(ExercicioSessao ex, SessaoRequest.ExercicioRequest req) {
		Map<UUID, SerieSessao> atuais = ex.getSeries().stream()
				.collect(Collectors.toMap(SerieSessao::getId, Function.identity(), (a, b) -> a, HashMap::new));
		List<SerieSessao> resultado = new ArrayList<>();
		for (int i = 0; i < req.series().size(); i++) {
			SessaoRequest.SerieRequest s = req.series().get(i);
			SerieSessao serie = atuais.get(s.id());
			if (serie == null) {
				serie = new SerieSessao();
				serie.setId(s.id());
				serie.setExercicioSessao(ex);
			}
			serie.setOrdem(i);
			serie.setReps(s.reps());
			serie.setCarga(s.carga());
			serie.setConcluida(s.concluida());
			resultado.add(serie);
		}
		ex.getSeries().clear();
		ex.getSeries().addAll(resultado);
	}

	/** Garante que os exercicios sao do catalogo ou do proprio usuario. */
	private Map<UUID, Exercicio> carregarExercicios(SessaoRequest request, Usuario usuario) {
		List<UUID> ids = request.exercicios().stream()
				.map(SessaoRequest.ExercicioRequest::exercicioId)
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
