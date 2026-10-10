package com.trainingapp.api.service;

import com.trainingapp.api.dto.GamificacaoResponse;
import com.trainingapp.api.model.ExercicioSessao;
import com.trainingapp.api.model.SerieSessao;
import com.trainingapp.api.model.Sessao;
import com.trainingapp.api.model.TipoXp;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.model.XpEvent;
import com.trainingapp.api.repository.SessaoRepository;
import com.trainingapp.api.repository.XpEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificacaoService {

	private final XpEventRepository eventos;
	private final SessaoRepository sessoes;
	private final UsuarioAtual usuarioAtual;

	public GamificacaoService(XpEventRepository eventos, SessaoRepository sessoes, UsuarioAtual usuarioAtual) {
		this.eventos = eventos;
		this.sessoes = sessoes;
		this.usuarioAtual = usuarioAtual;
	}

	/**
	 * Credita o XP de uma sessao recem-concluida. So conta a primeira sessao valida do dia (no fuso do
	 * perfil) e com pelo menos 3 series concluidas; o chamador garante que isto roda uma vez por sessao.
	 */
	public void registrarSessaoConcluida(Sessao sessao) {
		if (eventos.existsBySessaoId(sessao.getId())) {
			return;
		}
		Usuario usuario = sessao.getUsuario();
		LocalDate dia = diaDoTreino(sessao.getFinalizadaEm(), usuario);
		// Saldo > 0: o dia ja rendeu XP e nao foi estornado (apagar o treino libera o dia).
		if (eventos.findByUsuarioIdAndTipoAndDia(usuario.getId(), TipoXp.SERIES, dia).stream()
				.mapToInt(XpEvent::getPontos).sum() > 0) {
			return;
		}
		List<SerieSessao> concluidas = sessao.getExercicios().stream()
				.flatMap(e -> e.getSeries().stream())
				.filter(SerieSessao::isConcluida)
				.toList();
		if (concluidas.size() < Regras.SERIES_MINIMAS) {
			return;
		}

		List<XpEvent> novos = new ArrayList<>();
		// Um unico evento com o XP de todas as series; ele tambem marca o dia de treino (dia_unico).
		int xpSeries = concluidas.stream().mapToInt(s -> Regras.xpDaSerie(s.getCarga(), s.getReps())).sum();
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
		eventos.saveAllAndFlush(novos);
	}

	/**
	 * Estorna o XP de uma sessao apagada com eventos de pontos negativos (o registro e append-only).
	 * O evento SERIES original so perde o dia_unico, para que o dia possa render XP de novo.
	 */
	public void estornar(UUID sessaoId) {
		List<XpEvent> estornos = new ArrayList<>();
		for (XpEvent original : eventos.findBySessaoIdIn(List.of(sessaoId))) {
			if (original.getPontos() <= 0) {
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

	private static LocalDate diaDoTreino(Instant instante, Usuario usuario) {
		return instante.atZone(ZoneId.of(usuario.getFusoHorario())).toLocalDate();
	}

	@Transactional(readOnly = true)
	public GamificacaoResponse resumo() {
		Usuario usuario = usuarioAtual.obter();
		List<XpEvent> todos = eventos.findByUsuarioId(usuario.getId());

		long xp = todos.stream().mapToLong(XpEvent::getPontos).sum();
		// Estornos tem pontos negativos: um dia so conta com saldo positivo, e cada recorde estornado desconta um.
		Set<LocalDate> dias = todos.stream()
				.filter(e -> e.getTipo() == TipoXp.SERIES)
				.collect(Collectors.groupingBy(XpEvent::getDia, Collectors.summingInt(XpEvent::getPontos)))
				.entrySet().stream().filter(d -> d.getValue() > 0).map(java.util.Map.Entry::getKey)
				.collect(Collectors.toSet());
		int recordes = todos.stream().filter(e -> e.getTipo() == TipoXp.RECORDE).mapToInt(e -> Integer.signum(e.getPontos())).sum();
		LocalDate hoje = LocalDate.now(ZoneId.of(usuario.getFusoHorario()));
		Regras.Streak streak = Regras.streak(dias, hoje);
		int nivel = Regras.nivel(xp);

		return new GamificacaoResponse(xp, nivel, Regras.xpInicioDoNivel(nivel), Regras.xpParaNivel(nivel + 1),
				streak.atual(), streak.melhor(), dias.size(), recordes, dias.contains(hoje),
				Conquistas.avaliar(dias.size(), recordes, streak.melhor(), nivel));
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

	/** XP total da sessao, o detalhe por tipo e os exercicios com recorde. */
	public record Ganho(int xp, int xpSeries, int xpRecordes, List<UUID> recordes) {

		public static final Ganho NENHUM = new Ganho(0, 0, 0, List.of());

		static Ganho de(List<XpEvent> eventos) {
			int series = pontos(eventos, TipoXp.SERIES);
			int recordes = pontos(eventos, TipoXp.RECORDE);
			return new Ganho(series + recordes, series, recordes,
					eventos.stream().filter(e -> e.getTipo() == TipoXp.RECORDE).map(XpEvent::getExercicioId).toList());
		}

		private static int pontos(List<XpEvent> eventos, TipoXp tipo) {
			return eventos.stream().filter(e -> e.getTipo() == tipo).mapToInt(XpEvent::getPontos).sum();
		}
	}
}
