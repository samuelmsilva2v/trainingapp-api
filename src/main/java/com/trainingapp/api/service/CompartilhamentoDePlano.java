package com.trainingapp.api.service;

import com.trainingapp.api.config.RecursoNaoEncontradoException;
import com.trainingapp.api.dto.ExercicioRequest;
import com.trainingapp.api.dto.PlanoCompartilhadoResponse;
import com.trainingapp.api.dto.PlanoRequest;
import com.trainingapp.api.dto.PlanoResponse;
import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.Objetivo;
import com.trainingapp.api.model.PlanoCompartilhado;
import com.trainingapp.api.repository.ExercicioRepository;
import com.trainingapp.api.repository.PlanoCompartilhadoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compartilhar plano por link: gerar guarda uma copia do plano (sem carga alvo) sob um token aleatorio; quem abre o link
 * ve a previa e pode salvar a copia como um plano novo seu.
 */
@Service
@Transactional
public class CompartilhamentoDePlano {

	/** O que fica guardado em {@code plano_compartilhado.dados}. {@code exercicioId} so vem em exercicio do catalogo. */
	record Dados(String nome, Objetivo objetivo, List<Dia> dias) {
		record Dia(String nome, List<Item> exercicios) {
		}

		record Item(UUID exercicioId, String nome, GrupoMuscular grupoMuscular, Equipamento equipamento, int series,
				int repsMin, int repsMax, Integer descansoSegundos, int drops) {
		}
	}

	private final PlanoCompartilhadoRepository compartilhados;
	private final PlanoService planos;
	private final ExercicioService exercicioService;
	private final ExercicioRepository exercicios;
	private final UsuarioAtual usuarioAtual;
	private final ImagensExercicio imagens;
	private final JsonMapper json;
	private final Clock relogio;
	private final String urlBase;

	public CompartilhamentoDePlano(PlanoCompartilhadoRepository compartilhados, PlanoService planos,
			ExercicioService exercicioService, ExercicioRepository exercicios, UsuarioAtual usuarioAtual,
			ImagensExercicio imagens, JsonMapper json, Clock relogio, @Value("${trainingapp.web.url-base}") String urlBase) {
		this.compartilhados = compartilhados;
		this.planos = planos;
		this.exercicioService = exercicioService;
		this.exercicios = exercicios;
		this.usuarioAtual = usuarioAtual;
		this.imagens = imagens;
		this.json = json;
		this.relogio = relogio;
		this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
	}

	/** Gera um link novo a cada chamada, com o plano como esta agora. */
	public PlanoCompartilhadoResponse.LinkResponse compartilhar(UUID planoId) {
		PlanoResponse plano = planos.obter(planoId);
		Map<UUID, Exercicio> usados = porId(plano.dias().stream().flatMap(d -> d.exercicios().stream())
				.map(PlanoResponse.ItemResponse::exercicioId).distinct().toList());
		Dados dados = new Dados(plano.nome(), plano.objetivo(), plano.dias().stream()
				.map(d -> new Dados.Dia(d.nome(), d.exercicios().stream()
						.map(i -> new Dados.Item(usados.get(i.exercicioId()).getUsuario() == null ? i.exercicioId() : null,
								i.exercicioNome(), i.grupoMuscular(), i.equipamento(), i.series(), i.repsMin(), i.repsMax(),
								i.descansoSegundos(), i.drops()))
						.toList()))
				.toList());

		PlanoCompartilhado compartilhado = new PlanoCompartilhado();
		compartilhado.setToken(Tokens.novo(12));
		compartilhado.setUsuario(usuarioAtual.obter());
		compartilhado.setDados(json.writeValueAsString(dados));
		compartilhado.setCriadoEm(Instant.now(relogio));
		compartilhados.save(compartilhado);
		return new PlanoCompartilhadoResponse.LinkResponse(urlBase + "/compartilhado/" + compartilhado.getToken());
	}

	@Transactional(readOnly = true)
	public PlanoCompartilhadoResponse previa(String token) {
		PlanoCompartilhado compartilhado = buscar(token);
		Dados dados = ler(compartilhado);
		Map<UUID, Exercicio> catalogo = porId(idsDoCatalogo(dados));
		// Só o primeiro nome: o link pode chegar a qualquer pessoa, e o nome completo nem sai do servidor.
		String nome = compartilhado.getUsuario().getNome();
		String primeiroNome = nome == null || nome.isBlank() ? null : nome.trim().split("\\s+")[0];
		boolean seu = compartilhado.getUsuario().getId().equals(usuarioAtual.obter().getId());
		return new PlanoCompartilhadoResponse(dados.nome(), dados.objetivo(), primeiroNome, seu,
				dados.dias().stream()
						.map(d -> new PlanoCompartilhadoResponse.DiaResponse(d.nome(), d.exercicios().stream()
								.map(i -> new PlanoCompartilhadoResponse.ItemResponse(i.nome(), i.grupoMuscular(), i.equipamento(),
										i.exercicioId() != null && catalogo.containsKey(i.exercicioId())
												? imagens.de(catalogo.get(i.exercicioId())) : List.of(),
										i.series(), i.repsMin(), i.repsMax(), i.descansoSegundos(), i.drops()))
								.toList()))
						.toList());
	}

	/**
	 * Salva a copia como um plano novo de quem abriu o link. Exercicio proprio do dono vira exercicio proprio de quem
	 * salva (reaproveitando um de mesmo nome que ja exista); a carga alvo fica vazia.
	 */
	public PlanoResponse salvar(String token, boolean ativar) {
		Dados dados = ler(buscar(token));
		Map<UUID, Exercicio> catalogo = porId(idsDoCatalogo(dados));
		Map<String, UUID> proprios = new HashMap<>();
		exercicios.findByUsuarioId(usuarioAtual.obter().getId())
				.forEach(e -> proprios.putIfAbsent(chave(e.getNome()), e.getId()));

		List<PlanoRequest.DiaRequest> dias = dados.dias().stream()
				.map(d -> new PlanoRequest.DiaRequest(d.nome(), d.exercicios().stream()
						.map(i -> new PlanoRequest.ItemRequest(
								i.exercicioId() != null && catalogo.containsKey(i.exercicioId()) ? i.exercicioId()
										: proprios.computeIfAbsent(chave(i.nome()), k -> exercicioService
												.criar(new ExercicioRequest(i.nome(), i.grupoMuscular(), i.equipamento())).id()),
								i.series(), i.repsMin(), i.repsMax(), null, i.descansoSegundos(), i.drops()))
						.toList()))
				.toList();
		PlanoResponse plano = planos.criar(new PlanoRequest(dados.nome(), dados.objetivo(), dias));
		return ativar && !plano.ativo() ? planos.ativar(plano.id()) : plano;
	}

	private PlanoCompartilhado buscar(String token) {
		return compartilhados.findByToken(token)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Link de plano invalido ou removido"));
	}

	private Dados ler(PlanoCompartilhado compartilhado) {
		return json.readValue(compartilhado.getDados(), Dados.class);
	}

	private Map<UUID, Exercicio> porId(List<UUID> ids) {
		return exercicios.findAllById(ids).stream().collect(Collectors.toMap(Exercicio::getId, Function.identity()));
	}

	private static List<UUID> idsDoCatalogo(Dados dados) {
		return dados.dias().stream().flatMap(d -> d.exercicios().stream()).map(Dados.Item::exercicioId)
				.filter(Objects::nonNull).distinct().toList();
	}

	private static String chave(String nome) {
		return nome.trim().toLowerCase(Locale.ROOT);
	}
}
