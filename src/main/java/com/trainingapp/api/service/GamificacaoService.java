package com.trainingapp.api.service;

import com.trainingapp.api.dto.ConquistaResponse;
import com.trainingapp.api.dto.DesafioResponse;
import com.trainingapp.api.dto.GamificacaoResponse;
import com.trainingapp.api.model.ExercicioSessao;
import com.trainingapp.api.model.SerieSessao;
import com.trainingapp.api.model.Sessao;
import com.trainingapp.api.model.TipoXp;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.model.XpEvent;
import com.trainingapp.api.repository.PesoRegistroRepository;
import com.trainingapp.api.repository.PlanoRepository;
import com.trainingapp.api.repository.SessaoRepository;
import com.trainingapp.api.repository.TreinoDaConta;
import com.trainingapp.api.repository.XpEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificacaoService {

	/** Id que nenhuma sessao tem: o JPQL nao aceita parametro nulo no Postgres. */
	private static final UUID SEM_TREINO_ATUAL = new UUID(0, 0);

	private final XpEventRepository eventos;
	private final SessaoRepository sessoes;
	private final PlanoRepository planos;
	private final PesoRegistroRepository pesos;
	private final UsuarioAtual usuarioAtual;
	private final Clock relogio;

	public GamificacaoService(XpEventRepository eventos, SessaoRepository sessoes, PlanoRepository planos, PesoRegistroRepository pesos,
			UsuarioAtual usuarioAtual, Clock relogio) {
		this.planos = planos;
		this.pesos = pesos;
		this.eventos = eventos;
		this.sessoes = sessoes;
		this.usuarioAtual = usuarioAtual;
		this.relogio = relogio;
	}

	/**
	 * Credita o XP de uma sessao recem-concluida. So conta a primeira sessao valida do dia (no fuso do
	 * sistema) e com pelo menos 3 series concluidas; o chamador garante que isto roda uma vez por sessao.
	 */
	public void registrarSessaoConcluida(Sessao sessao) {
		if (eventos.existsBySessaoId(sessao.getId())) {
			return;
		}
		eventos.saveAllAndFlush(calcularEventos(sessao, sessao.getFinalizadaEm()));
	}

	/**
	 * Previa exata do XP de um treino em andamento: sao os mesmos eventos que seriam gravados se ele fosse
	 * concluido agora (limite de um treino por dia, minimo de series e recordes incluidos), sem gravar nada.
	 */
	@Transactional(readOnly = true)
	public Ganho previa(Sessao sessao) {
		return Ganho.de(calcularEventos(sessao, relogio.instant()));
	}

	/** Eventos de XP que a sessao rende se terminar em `fim`: os do treino e as conquistas que ele libera. */
	private List<XpEvent> calcularEventos(Sessao sessao, Instant fim) {
		LocalDate dia = diaDoTreino(fim);
		List<XpEvent> novos = new ArrayList<>(eventosDoTreino(sessao, fim));
		List<XpEvent> todos = new ArrayList<>(eventos.findByUsuarioId(sessao.getUsuario().getId()));
		todos.addAll(novos);
		// Desafios antes das conquistas: o XP deles tambem pode subir o nivel e liberar uma conquista de nivel.
		List<XpEvent> desafios = desafiosNovos(sessao, dia, todos);
		novos.addAll(desafios);
		todos.addAll(desafios);
		novos.addAll(conquistasNovas(sessao, dia, todos, historico(sessao.getUsuario().getId(), sessao.getId(), fim, todos)));
		return novos;
	}

	/**
	 * Desafios da semana do treino que passam a valer e ainda nao foram creditados nesta semana. Roda em toda conclusao,
	 * entao os treinos da semana feitos antes do deploy tambem contam.
	 */
	private List<XpEvent> desafiosNovos(Sessao sessao, LocalDate dia, List<XpEvent> todos) {
		Set<LocalDate> dias = diasDeTreino(todos);
		int recordes = recordesNaSemana(todos, dia);
		Set<String> creditados = codigosCreditados(todos);
		List<XpEvent> novos = new ArrayList<>();
		for (Desafios.Desafio d : Desafios.TODOS) {
			String codigo = Desafios.codigo(d, dia);
			if (Desafios.progresso(d, dias, recordes, dia) >= d.meta() && creditados.add(codigo)) {
				XpEvent e = evento(sessao.getUsuario(), sessao, TipoXp.DESAFIO, d.xp(), null, dia);
				e.setCodigo(codigo);
				novos.add(e);
			}
		}
		return novos;
	}

	/** Recordes que valem (descontando estornos) em treinos da semana de {@code dia}. */
	private static int recordesNaSemana(List<XpEvent> todos, LocalDate dia) {
		return todos.stream().filter(e -> e.getTipo() == TipoXp.RECORDE && Desafios.naSemana(e.getDia(), dia))
				.mapToInt(e -> Integer.signum(e.getPontos())).sum();
	}

	/**
	 * Conquistas que valem agora e ainda nao foram creditadas, em cascata: o XP de uma conquista pode subir o nivel e
	 * liberar outra. Roda em toda conclusao, mesmo sem XP de treino, entao a primeira conclusao depois do deploy credita
	 * as conquistas que ja valiam antes (retroativo decidido na #37).
	 */
	private List<XpEvent> conquistasNovas(Sessao sessao, LocalDate dia, List<XpEvent> todos, HistoricoDeTreinos.Totais historico) {
		Usuario usuario = sessao.getUsuario();
		Set<String> creditadas = codigosCreditados(todos);
		List<XpEvent> novas = new ArrayList<>();
		while (true) {
			List<ConquistaResponse> lista = Conquistas.avaliar(totais(todos), historico);
			java.util.Map<String, Long> faixasPorFamilia = lista.stream()
					.collect(Collectors.groupingBy(ConquistaResponse::familia, Collectors.counting()));
			List<XpEvent> rodada = new ArrayList<>();
			for (ConquistaResponse c : lista) {
				if (c.conquistada() && creditadas.add(c.codigo())) {
					XpEvent e = evento(usuario, sessao, TipoXp.CONQUISTA,
							Regras.xpDaConquista(c.faixa(), faixasPorFamilia.get(c.familia()) == 1), null, dia);
					e.setCodigo(c.codigo());
					rodada.add(e);
				}
			}
			if (rodada.isEmpty()) {
				return novas;
			}
			novas.addAll(rodada);
			todos.addAll(rodada);
		}
	}

	/** Codigos ja creditados: conquistas (CONQUISTA) e desafios da semana (DESAFIO, com a semana no codigo). */
	private static Set<String> codigosCreditados(List<XpEvent> eventos) {
		return eventos.stream().filter(e -> e.getCodigo() != null)
				.map(XpEvent::getCodigo).collect(Collectors.toCollection(HashSet::new));
	}

	/** XP do treino em si (series, recordes e sequencia); vazio quando o dia ja rendeu ou ha menos de 3 series. */
	private List<XpEvent> eventosDoTreino(Sessao sessao, Instant fim) {
		Usuario usuario = sessao.getUsuario();
		LocalDate dia = diaDoTreino(fim);
		// Saldo > 0: o dia ja rendeu XP e nao foi estornado (apagar o treino libera o dia).
		if (eventos.findByUsuarioIdAndTipoAndDia(usuario.getId(), TipoXp.SERIES, dia).stream()
				.mapToInt(XpEvent::getPontos).sum() > 0) {
			return List.of();
		}
		// O drop-set conta como uma serie so (um bloco), nas series minimas e no XP.
		int concluidas = sessao.getExercicios().stream().mapToInt(e -> Blocos.concluidos(e.getSeries())).sum();
		if (concluidas < Regras.SERIES_MINIMAS) {
			return List.of();
		}

		List<XpEvent> novos = new ArrayList<>();
		// Um unico evento com o XP de todas as series; ele tambem marca o dia de treino (dia_unico).
		int xpSeries = sessao.getExercicios().stream().mapToInt(Blocos::xp).sum();
		XpEvent series = evento(usuario, sessao, TipoXp.SERIES, xpSeries, null, dia);
		series.setDiaUnico(dia);
		novos.add(series);
		Set<UUID> premiados = new HashSet<>();
		for (ExercicioSessao exercicio : sessao.getExercicios()) {
			// Um plano pode repetir o exercicio no dia; o recorde vale uma vez por exercicio.
			if (!premiados.contains(exercicio.getExercicio().getId()) && bateuRecorde(usuario, sessao, exercicio)) {
				premiados.add(exercicio.getExercicio().getId());
				novos.add(evento(usuario, sessao, TipoXp.RECORDE, Regras.XP_RECORDE,
						exercicio.getExercicio().getId(), dia));
			}
		}
		// Bonus pela sequencia que este treino estende; dias depois dele (treino sincronizado com atraso) nao contam.
		Set<LocalDate> dias = diasDeTreino(eventos.findByUsuarioIdAndTipo(usuario.getId(), TipoXp.SERIES));
		dias.removeIf(d -> d.isAfter(dia));
		dias.add(dia);
		int xpSequencia = Regras.xpDaSequencia(Regras.streak(dias, dia).atual());
		if (xpSequencia > 0) {
			novos.add(evento(usuario, sessao, TipoXp.STREAK, xpSequencia, null, dia));
		}
		return novos;
	}

	/** Dias com treino valido: saldo de SERIES positivo no dia (apagar o treino estorna e libera o dia). */
	private static Set<LocalDate> diasDeTreino(List<XpEvent> eventos) {
		return eventos.stream()
				.filter(e -> e.getTipo() == TipoXp.SERIES)
				.collect(Collectors.groupingBy(XpEvent::getDia, Collectors.summingInt(XpEvent::getPontos)))
				.entrySet().stream().filter(d -> d.getValue() > 0).map(java.util.Map.Entry::getKey)
				.collect(Collectors.toCollection(HashSet::new));
	}

	/**
	 * Estorna o XP de uma sessao apagada com eventos de pontos negativos (o registro e append-only).
	 * O evento SERIES original so perde o dia_unico, para que o dia possa render XP de novo.
	 */
	public void estornar(UUID sessaoId) {
		List<XpEvent> estornos = new ArrayList<>();
		for (XpEvent original : eventos.findBySessaoIdIn(List.of(sessaoId))) {
			// Conquista e desafio creditados permanecem (#37 e #38): apagar um treino nao os desfaz.
			if (original.getPontos() <= 0 || original.getTipo() == TipoXp.CONQUISTA || original.getTipo() == TipoXp.DESAFIO) {
				continue;
			}
			XpEvent estorno = evento(original.getUsuario(), original.getSessaoId(), original.getTipo(),
					-original.getPontos(), original.getExercicioId(), original.getDia());
			estornos.add(estorno);
			original.setDiaUnico(null);
		}
		eventos.saveAll(estornos);
	}

	/**
	 * Recorde = carga maior que a melhor ja registrada no exercicio. A primeira vez que o exercicio
	 * aparece no historico nao e recorde: nao ha marca anterior para superar.
	 */
	private boolean bateuRecorde(Usuario usuario, Sessao sessao, ExercicioSessao exercicio) {
		// Peso corporal (api#43): recorde = mais reps numa serie (ou etapa); a carga e ignorada.
		if (exercicio.getExercicio().isPesoCorporal()) {
			int maisRepsAgora = exercicio.getSeries().stream()
					.filter(s -> s.isConcluida() && s.getReps() != null)
					.mapToInt(SerieSessao::getReps).max().orElse(0);
			Integer anterior = sessoes.maisRepsAnterior(usuario.getId(), exercicio.getExercicio().getId(), sessao.getId());
			return maisRepsAgora >= 1 && anterior != null && maisRepsAgora > anterior;
		}
		BigDecimal melhorAgora = exercicio.getSeries().stream()
				.filter(s -> s.isConcluida() && s.getReps() != null && s.getReps() >= 1 && s.getCarga() != null)
				.map(SerieSessao::getCarga)
				.max(BigDecimal::compareTo)
				.orElse(null);
		if (melhorAgora == null || melhorAgora.signum() <= 0) {
			return false;
		}
		BigDecimal anterior = sessoes.maiorCargaAnterior(usuario.getId(), exercicio.getExercicio().getId(),
				sessao.getId());
		return anterior != null && melhorAgora.compareTo(anterior) > 0;
	}

	private XpEvent evento(Usuario usuario, Sessao sessao, TipoXp tipo, int pontos, UUID exercicioId, LocalDate dia) {
		return evento(usuario, sessao.getId(), tipo, pontos, exercicioId, dia);
	}

	private XpEvent evento(Usuario usuario, UUID sessaoId, TipoXp tipo, int pontos, UUID exercicioId, LocalDate dia) {
		XpEvent e = new XpEvent();
		e.setUsuario(usuario);
		e.setSessaoId(sessaoId);
		e.setTipo(tipo);
		e.setPontos(pontos);
		e.setExercicioId(exercicioId);
		e.setDia(dia);
		return e;
	}

	private LocalDate diaDoTreino(Instant instante) {
		return instante.atZone(relogio.getZone()).toLocalDate();
	}

	@Transactional(readOnly = true)
	public GamificacaoResponse resumo() {
		Usuario usuario = usuarioAtual.obter();
		List<XpEvent> todos = eventos.findByUsuarioId(usuario.getId());

		long xp = todos.stream().mapToLong(XpEvent::getPontos).sum();
		Set<LocalDate> dias = diasDeTreino(todos);
		LocalDate hoje = LocalDate.now(relogio);
		Regras.Streak streak = Regras.streak(dias, hoje);
		Conquistas.Totais totais = totais(todos);
		int nivel = totais.nivel();
		// Conquistada = a regra da agora ou ja foi creditada (a conquista gravada nunca "desconquista").
		Set<String> creditadas = codigosCreditados(todos);
		List<ConquistaResponse> conquistas = Conquistas.avaliar(totais, historico(usuario.getId(), null, null, todos)).stream()
				.map(c -> c.conquistada() || !creditadas.contains(c.codigo()) ? c
						: new ConquistaResponse(c.codigo(), c.nome(), c.descricao(), true, c.familia(), c.faixa(), c.progresso(),
								c.meta(), c.oculta()))
				.toList();

		// Desafios da semana de hoje; cumprido tambem se o XP ja foi creditado (apagar o treino nao tira).
		int recordesSemana = recordesNaSemana(todos, hoje);
		List<DesafioResponse> desafios = Desafios.TODOS.stream()
				.map(d -> {
					int progresso = Desafios.progresso(d, dias, recordesSemana, hoje);
					return new DesafioResponse(d.codigo(), d.nome(), progresso, d.meta(), d.xp(),
							progresso >= d.meta() || creditadas.contains(Desafios.codigo(d, hoje)));
				})
				.toList();

		return new GamificacaoResponse(xp, nivel, Regras.titulo(nivel), Regras.xpInicioDoNivel(nivel), Regras.xpParaNivel(nivel + 1),
				streak.atual(), streak.melhor(), streak.congelamentos(), streak.congelada(), dias.size(), totais.recordes(), dias.contains(hoje), conquistas, desafios,
				Desafios.fimDaSemana(hoje));
	}

	/**
	 * Totais da fatia 2 das conquistas: o banco soma por exercicio de cada treino (sem carregar serie por serie) e o resto
	 * e feito em memoria. {@code atual} e o treino sendo concluido ou na previa (null no resumo), que entra com o {@code fim}
	 * dado; os validos vem do saldo de SERIES em {@code todos}, que ja inclui o deste treino.
	 */
	private HistoricoDeTreinos.Totais historico(UUID usuarioId, UUID atual, Instant fim, List<XpEvent> todos) {
		UUID idAtual = atual == null ? SEM_TREINO_ATUAL : atual;
		List<TreinoDaConta> treinos = sessoes.treinosDaConta(usuarioId, idAtual).stream()
				.map(t -> t.id().equals(atual) ? new TreinoDaConta(t.id(), fim, t.planoId(), t.diaOrdem()) : t)
				.toList();
		Set<UUID> validos = HistoricoDeTreinos.validos(todos.stream().filter(e -> e.getTipo() == TipoXp.SERIES)
				.collect(Collectors.groupingBy(XpEvent::getSessaoId, Collectors.summingInt(XpEvent::getPontos))));
		java.util.NavigableMap<LocalDate, java.math.BigDecimal> pesoAte = new java.util.TreeMap<>();
		pesos.findByUsuarioIdOrderByDataAsc(usuarioId).forEach(p -> pesoAte.put(p.getData(), p.getKg()));
		java.util.Map<UUID, Integer> diasPorPlano = new java.util.HashMap<>();
		planos.diasPorPlano(usuarioId).forEach(l -> diasPorPlano.put((UUID) l[0], ((Number) l[1]).intValue()));
		return HistoricoDeTreinos.de(treinos, validos, sessoes.exerciciosDaConta(usuarioId, idAtual),
				HistoricoDeTreinos.blocosComDrop(sessoes.seriesComDrop(usuarioId)), pesoAte, diasPorPlano, relogio.getZone());
	}

	/** Totais das conquistas a partir dos eventos (estornos tem pontos negativos e descontam). */
	private static Conquistas.Totais totais(List<XpEvent> todos) {
		long xp = todos.stream().mapToLong(XpEvent::getPontos).sum();
		Set<LocalDate> dias = diasDeTreino(todos);
		int recordes = todos.stream().filter(e -> e.getTipo() == TipoXp.RECORDE).mapToInt(e -> Integer.signum(e.getPontos())).sum();

		// Recordes que valem (saldo positivo por sessao e exercicio, descontando estornos).
		Set<UUID> exerciciosComRecorde = new HashSet<>();
		boolean diaInspirado = false;
		for (java.util.Map<UUID, Integer> porExercicio : todos.stream()
				.filter(e -> e.getTipo() == TipoXp.RECORDE)
				.collect(Collectors.groupingBy(XpEvent::getSessaoId, Collectors.groupingBy(XpEvent::getExercicioId,
						Collectors.summingInt(e -> Integer.signum(e.getPontos())))))
				.values()) {
			List<UUID> valendo = porExercicio.entrySet().stream().filter(r -> r.getValue() > 0)
					.map(java.util.Map.Entry::getKey).toList();
			exerciciosComRecorde.addAll(valendo);
			diaInspirado |= valendo.size() >= Regras.RECORDES_DIA_INSPIRADO;
		}
		// A melhor sequencia nao depende de "hoje".
		int melhorStreak = Regras.streak(dias, LocalDate.MAX).melhor();
		return new Conquistas.Totais(dias.size(), melhorStreak, Regras.melhorSemanasFirmes(dias), recordes,
				exerciciosComRecorde.size(), Regras.nivel(xp), Regras.voltouDepoisDePausa(dias), diaInspirado);
	}

	/** XP ganho e exercicios com recorde, por sessao (para as telas de treino e historico). */
	@Transactional(readOnly = true)
	public java.util.Map<UUID, Ganho> ganhos(java.util.Collection<UUID> sessaoIds) {
		if (sessaoIds.isEmpty()) {
			return java.util.Map.of();
		}
		return eventos.findBySessaoIdIn(sessaoIds).stream()
				.collect(Collectors.groupingBy(XpEvent::getSessaoId,
						Collectors.collectingAndThen(Collectors.toList(), Ganho::de)));
	}

	/** Conquista creditada num treino, com o XP que rendeu. */
	public record ConquistaGanha(String codigo, String nome, int xp) {
	}

	/** Desafio da semana cumprido num treino, com o XP que rendeu. */
	public record DesafioGanho(String codigo, String nome, int xp) {
	}

	/** XP total da sessao, o detalhe por tipo, os exercicios com recorde e as conquistas creditadas. */
	public record Ganho(int xp, int xpSeries, int xpRecordes, int xpStreak, int xpConquistas, int xpDesafios,
			List<UUID> recordes, List<ConquistaGanha> conquistas, List<DesafioGanho> desafios) {

		public static final Ganho NENHUM = new Ganho(0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of());

		static Ganho de(List<XpEvent> eventos) {
			int series = pontos(eventos, TipoXp.SERIES);
			int recordes = pontos(eventos, TipoXp.RECORDE);
			int streak = pontos(eventos, TipoXp.STREAK);
			int conquistas = pontos(eventos, TipoXp.CONQUISTA);
			int desafios = pontos(eventos, TipoXp.DESAFIO);
			return new Ganho(series + recordes + streak + conquistas + desafios, series, recordes, streak, conquistas, desafios,
					eventos.stream().filter(e -> e.getTipo() == TipoXp.RECORDE).map(XpEvent::getExercicioId).toList(),
					eventos.stream().filter(e -> e.getTipo() == TipoXp.CONQUISTA && e.getPontos() > 0)
							.map(e -> new ConquistaGanha(e.getCodigo(), Conquistas.NOMES.getOrDefault(e.getCodigo(), e.getCodigo()),
									e.getPontos()))
							.toList(),
					eventos.stream().filter(e -> e.getTipo() == TipoXp.DESAFIO && e.getPontos() > 0)
							.map(e -> {
								Desafios.Desafio d = Desafios.deCodigo(e.getCodigo());
								return new DesafioGanho(d == null ? e.getCodigo() : d.codigo(), d == null ? e.getCodigo() : d.nome(),
										e.getPontos());
							})
							.toList());
		}

		private static int pontos(List<XpEvent> eventos, TipoXp tipo) {
			return eventos.stream().filter(e -> e.getTipo() == tipo).mapToInt(XpEvent::getPontos).sum();
		}
	}
}
