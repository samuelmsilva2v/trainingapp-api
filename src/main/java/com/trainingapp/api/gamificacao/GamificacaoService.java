package com.trainingapp.api.gamificacao;

import com.trainingapp.api.sessao.ExercicioSessao;
import com.trainingapp.api.sessao.Sessao;
import com.trainingapp.api.sessao.SessaoRepository;
import com.trainingapp.api.sessao.SerieSessao;
import com.trainingapp.api.usuario.Usuario;
import com.trainingapp.api.usuario.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
		if (eventos.existsByUsuarioIdAndTipoAndDia(usuario.getId(), TipoXp.SESSAO, dia)) {
			return;
		}
		int series = (int) sessao.getExercicios().stream()
				.flatMap(e -> e.getSeries().stream())
				.filter(SerieSessao::isConcluida)
				.count();
		if (series < Regras.SERIES_MINIMAS) {
			return;
		}

		List<XpEvent> novos = new ArrayList<>();
		novos.add(evento(usuario, sessao, TipoXp.SESSAO, Regras.XP_SESSAO, null, dia));
		novos.add(evento(usuario, sessao, TipoXp.SERIES,
				Regras.xpDaSessao(series) - Regras.XP_SESSAO, null, dia));
		for (ExercicioSessao exercicio : sessao.getExercicios()) {
			if (bateuRecorde(usuario, sessao, exercicio)) {
				novos.add(evento(usuario, sessao, TipoXp.RECORDE, Regras.XP_RECORDE,
						exercicio.getExercicio().getId(), dia));
			}
		}
		eventos.saveAll(novos);
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
		XpEvent e = new XpEvent();
		e.setUsuario(usuario);
		e.setSessaoId(sessao.getId());
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
		Set<LocalDate> dias = todos.stream()
				.filter(e -> e.getTipo() == TipoXp.SESSAO)
				.map(XpEvent::getDia)
				.collect(Collectors.toSet());
		int recordes = (int) todos.stream().filter(e -> e.getTipo() == TipoXp.RECORDE).count();
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

	public record Ganho(int xp, List<UUID> recordes) {

		public static final Ganho NENHUM = new Ganho(0, List.of());

		static Ganho de(List<XpEvent> eventos) {
			return new Ganho(
					eventos.stream().mapToInt(XpEvent::getPontos).sum(),
					eventos.stream().filter(e -> e.getTipo() == TipoXp.RECORDE).map(XpEvent::getExercicioId).toList());
		}
	}
}
