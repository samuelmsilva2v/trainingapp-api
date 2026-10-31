package com.trainingapp.api.service;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.TipoSerie;
import com.trainingapp.api.repository.ExercicioDoTreino;
import com.trainingapp.api.repository.TreinoDaConta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Totais das conquistas da fatia 2 (api#36), calculados em memoria a partir do que o banco ja somou por exercicio de cada
 * treino. Puro (sem banco), entao cada regra e testada direto. So contam treinos validos, menos nos horarios (ocultas),
 * que olham qualquer treino concluido.
 */
final class HistoricoDeTreinos {

	private HistoricoDeTreinos() {
	}

	record Totais(long toneladas, boolean treinoPesado, int variedade, boolean corpoCompleto, boolean rotacaoCompleta,
			boolean clube100, int dropSets, boolean madrugador, boolean coruja, boolean anoNovo, boolean maisForte) {

		static final Totais ZERO = new Totais(0, false, 0, false, false, false, 0, false, false, false, false);
	}

	static final BigDecimal TREINO_PESADO_KG = new BigDecimal("10000");
	static final BigDecimal CLUBE_KG = new BigDecimal("100");
	private static final int MACROGRUPOS = 6;

	/** Macrogrupos do "Corpo completo": os 18 grupos do catalogo nao cabem numa semana. */
	static String macrogrupo(GrupoMuscular grupo) {
		return switch (grupo) {
			case PEITO -> "peito";
			case COSTAS -> "costas";
			case OMBROS -> "ombros";
			case BICEPS, TRICEPS -> "bracos";
			case QUADRICEPS, POSTERIORES, GLUTEOS -> "pernas";
			case ABDOMEN -> "abdomen";
			default -> null;
		};
	}

	/**
	 * @param treinos treinos concluidos (e o atual, na previa, com a hora de termino ja resolvida)
	 * @param validos ids dos treinos validos (1o do dia, 3+ series)
	 * @param exercicios agregados por exercicio de cada treino
	 * @param blocosComDrop a sessao de cada bloco concluido com drop (um item por bloco)
	 * @param pesoAte pesagens por data, para achar a mais recente ate o dia do treino
	 * @param diasPorPlano dias de cada plano que ainda existe
	 */
	static Totais de(List<TreinoDaConta> treinos, Set<UUID> validos, List<ExercicioDoTreino> exercicios, List<UUID> blocosComDrop,
			NavigableMap<LocalDate, BigDecimal> pesoAte, Map<UUID, Integer> diasPorPlano, ZoneId fuso) {
		Map<UUID, LocalDate> diaDoTreino = new HashMap<>();
		for (TreinoDaConta t : treinos) {
			if (t.finalizadaEm() != null) {
				diaDoTreino.put(t.id(), t.finalizadaEm().atZone(fuso).toLocalDate());
			}
		}
		List<ExercicioDoTreino> validosEx = exercicios.stream().filter(e -> validos.contains(e.sessaoId())).toList();

		BigDecimal volume = BigDecimal.ZERO;
		Map<UUID, BigDecimal> volumePorTreino = new HashMap<>();
		Set<UUID> distintos = new HashSet<>();
		Map<LocalDate, Set<String>> macrogruposPorSemana = new HashMap<>();
		boolean clube100 = false;
		boolean maisForte = false;
		for (ExercicioDoTreino e : validosEx) {
			BigDecimal v = e.volume() == null ? BigDecimal.ZERO : e.volume();
			volume = volume.add(v);
			volumePorTreino.merge(e.sessaoId(), v, BigDecimal::add);
			distintos.add(e.exercicioId());
			LocalDate dia = diaDoTreino.get(e.sessaoId());
			String macro = macrogrupo(e.grupo());
			if (dia != null && macro != null) {
				macrogruposPorSemana.computeIfAbsent(Desafios.inicioDaSemana(dia), s -> new HashSet<>()).add(macro);
			}
			if (e.equipamento() == Equipamento.BARRA && e.maiorCarga() != null) {
				clube100 |= e.maiorCarga().compareTo(CLUBE_KG) >= 0;
				// O peso que vale e a pesagem mais recente ate o dia do treino; sem pesagem ate ali, nao conta.
				Map.Entry<LocalDate, BigDecimal> peso = dia == null ? null : pesoAte.floorEntry(dia);
				maisForte |= peso != null && e.maiorCarga().compareTo(peso.getValue()) >= 0;
			}
		}

		boolean madrugador = false;
		boolean coruja = false;
		boolean anoNovo = false;
		for (TreinoDaConta t : treinos) {
			if (t.finalizadaEm() == null) {
				continue;
			}
			ZonedDateTime fim = t.finalizadaEm().atZone(fuso);
			madrugador |= fim.toLocalTime().isBefore(LocalTime.of(6, 0));
			coruja |= fim.toLocalTime().isAfter(LocalTime.of(22, 0));
			anoNovo |= fim.getMonthValue() == 1 && fim.getDayOfMonth() == 1;
		}

		return new Totais(volume.divideToIntegralValue(new BigDecimal(1000)).longValue(),
				volumePorTreino.values().stream().anyMatch(v -> v.compareTo(TREINO_PESADO_KG) >= 0),
				distintos.size(),
				macrogruposPorSemana.values().stream().anyMatch(s -> s.size() >= MACROGRUPOS),
				rotacaoCompleta(treinos.stream().filter(t -> validos.contains(t.id()) && t.finalizadaEm() != null).toList(), diasPorPlano),
				clube100,
				(int) blocosComDrop.stream().filter(validos::contains).count(),
				madrugador, coruja, anoNovo, maisForte);
	}

	/**
	 * Treinos do mesmo plano em sequencia (pela hora de termino) cobrindo os dias 0..n-1 na ordem. Treino de outro plano (ou
	 * fora da vez) no meio quebra; plano com menos de 2 dias nao vale. Abandonados nem chegam aqui (nao sao validos).
	 */
	static boolean rotacaoCompleta(List<TreinoDaConta> validos, Map<UUID, Integer> diasPorPlano) {
		UUID plano = null;
		int esperado = 0;
		for (TreinoDaConta t : validos.stream().sorted(Comparator.comparing(TreinoDaConta::finalizadaEm)).toList()) {
			Integer dias = t.planoId() == null ? null : diasPorPlano.get(t.planoId());
			if (dias == null || dias < 2) {
				plano = null;
				continue;
			}
			if (t.diaOrdem() == 0) {
				plano = t.planoId();
				esperado = 1;
			}
			else if (Objects.equals(t.planoId(), plano) && t.diaOrdem() == esperado) {
				esperado++;
			}
			else {
				plano = null;
				continue;
			}
			if (esperado == dias) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Uma entrada (a sessao) por bloco concluido com pelo menos um drop: linhas (sessao, exercicio da sessao, ordem, tipo,
	 * concluida) em ordem de exercicio e de serie. O drop so fica concluido com o bloco concluido e com repeticao.
	 */
	static List<UUID> blocosComDrop(List<Object[]> linhas) {
		List<UUID> blocos = new ArrayList<>();
		Object exercicio = null;
		boolean contado = true; // so conta depois de achar a serie normal do bloco
		for (Object[] l : linhas) {
			if (!Objects.equals(l[1], exercicio)) {
				exercicio = l[1];
				contado = true;
			}
			if (l[3] != TipoSerie.DROP) {
				contado = false;
			}
			else if (!contado && Boolean.TRUE.equals(l[4])) {
				blocos.add((UUID) l[0]);
				contado = true;
			}
		}
		return blocos;
	}

	/** Sessoes com treino valido: saldo positivo de SERIES (estorno desconta). */
	static Set<UUID> validos(Map<UUID, Integer> saldoDeSeriesPorSessao) {
		return saldoDeSeriesPorSessao.entrySet().stream().filter(s -> s.getValue() > 0).map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}
}
