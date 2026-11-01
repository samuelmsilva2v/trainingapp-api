package com.trainingapp.api.service;

import com.trainingapp.api.dto.ConquistaResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Conquistas derivadas dos eventos (nada e gravado): so dependem de totais ja calculados, entao
 * ajustar ou acrescentar uma regra aqui nao exige migracao. Cada familia tem faixas em ordem crescente
 * de meta; os codigos antigos foram mantidos para nao mudar o que o front ja conhece.
 */
final class Conquistas {

	private Conquistas() {
	}

	record Totais(int treinos, int melhorStreak, int melhorSemanasFirmes, int recordes, int recordesVariados,
			int nivel, boolean deVolta, boolean diaInspirado) {
	}

	private record Faixa(String codigo, String nome, String descricao, int meta) {
	}

	/** Nome de cada codigo, para mostrar as conquistas creditadas num treino. */
	static final Map<String, String> NOMES = avaliar(new Totais(0, 0, 0, 0, 0, 1, false, false), HistoricoDeTreinos.Totais.ZERO).stream()
			.collect(Collectors.toMap(ConquistaResponse::codigo, ConquistaResponse::nome));

	/** {@code h}: totais da fatia 2 (api#36), que leem treinos e series, e nao so os eventos de XP. */
	static List<ConquistaResponse> avaliar(Totais t, HistoricoDeTreinos.Totais h) {
		List<ConquistaResponse> lista = new ArrayList<>();
		familia(lista, "Constância", t.treinos(),
				new Faixa("PRIMEIRO_TREINO", "Primeiro treino", "Conclua seu primeiro treino.", 1),
				new Faixa("TREINOS_10", "Constância", "Conclua 10 treinos.", 10),
				new Faixa("TREINOS_50", "Rotina de ferro", "Conclua 50 treinos.", 50),
				new Faixa("TREINOS_100", "Centenário", "Conclua 100 treinos.", 100),
				new Faixa("TREINOS_250", "Veterano", "Conclua 250 treinos.", 250));
		familia(lista, "Sequência", t.melhorStreak(),
				new Faixa("STREAK_7", "Uma semana de fogo", "Alcance uma sequência de 7 dias.", 7),
				new Faixa("STREAK_30", "Hábito formado", "Alcance uma sequência de 30 dias.", 30),
				new Faixa("STREAK_60", "Imparável", "Alcance uma sequência de 60 dias.", 60),
				new Faixa("STREAK_100", "Cem dias", "Alcance uma sequência de 100 dias.", 100));
		familia(lista, "Semanas firmes", t.melhorSemanasFirmes(),
				new Faixa("SEMANAS_4", "Mês firme", "Treine 3 vezes por semana por 4 semanas seguidas.", 4),
				new Faixa("SEMANAS_12", "Trimestre firme", "Treine 3 vezes por semana por 12 semanas seguidas.", 12),
				new Faixa("SEMANAS_26", "Semestre firme", "Treine 3 vezes por semana por 26 semanas seguidas.", 26));
		familia(lista, "Recordes", t.recordes(),
				new Faixa("PRIMEIRO_RECORDE", "Novo recorde", "Bata seu primeiro recorde.", 1),
				new Faixa("RECORDES_10", "Quebrador de marcas", "Bata 10 recordes.", 10),
				new Faixa("RECORDES_25", "Sem teto", "Bata 25 recordes.", 25),
				new Faixa("RECORDES_50", "Lenda das cargas", "Bata 50 recordes.", 50));
		familia(lista, "Recordes variados", t.recordesVariados(),
				new Faixa("RECORDES_VARIADOS_5", "Evolução ampla", "Bata recordes em 5 exercícios diferentes.", 5),
				new Faixa("RECORDES_VARIADOS_10", "Evolução geral", "Bata recordes em 10 exercícios diferentes.", 10),
				new Faixa("RECORDES_VARIADOS_25", "Evolução total", "Bata recordes em 25 exercícios diferentes.", 25));
		familia(lista, "Nível", t.nivel(),
				new Faixa("NIVEL_5", "Nível 5", "Chegue ao nível 5.", 5),
				new Faixa("NIVEL_10", "Nível 10", "Chegue ao nível 10.", 10),
				new Faixa("NIVEL_20", "Nível 20", "Chegue ao nível 20.", 20),
				new Faixa("NIVEL_30", "Nível 30", "Chegue ao nível 30.", 30));
		familia(lista, "De volta", t.deVolta() ? 1 : 0,
				new Faixa("DE_VOLTA", "De volta", "Volte a treinar depois de 14 dias ou mais parado.", 1));
		familia(lista, "Dia inspirado", t.diaInspirado() ? 1 : 0,
				new Faixa("DIA_INSPIRADO", "Dia inspirado", "Bata 3 recordes no mesmo treino.", 1));
		// Fatia 2 (api#36).
		familia(lista, "Volume total", (int) Math.min(Integer.MAX_VALUE, h.toneladas()),
				new Faixa("VOLUME_10T", "10 toneladas", "Levante 10 t somando todos os treinos.", 10),
				new Faixa("VOLUME_100T", "100 toneladas", "Levante 100 t somando todos os treinos.", 100),
				new Faixa("VOLUME_1000T", "1.000 toneladas", "Levante 1.000 t somando todos os treinos.", 1000));
		familia(lista, "Treino pesado", h.treinoPesado() ? 1 : 0,
				new Faixa("TREINO_PESADO", "Treino pesado", "Levante 10 t num único treino.", 1));
		familia(lista, "Variedade", h.variedade(),
				new Faixa("VARIEDADE_20", "Curioso", "Treine 20 exercícios diferentes.", 20),
				new Faixa("VARIEDADE_50", "Explorador", "Treine 50 exercícios diferentes.", 50),
				new Faixa("VARIEDADE_100", "Enciclopédia", "Treine 100 exercícios diferentes.", 100));
		familia(lista, "Corpo completo", h.corpoCompleto() ? 1 : 0,
				new Faixa("CORPO_COMPLETO", "Corpo completo", "Treine peito, costas, ombros, braços, pernas e abdômen na mesma semana.", 1));
		familia(lista, "Rotação completa", h.rotacaoCompleta() ? 1 : 0,
				new Faixa("ROTACAO_COMPLETA", "Rotação completa", "Faça todos os dias de um plano em sequência, na ordem.", 1));
		familia(lista, "Clube dos 100 kg", h.clube100() ? 1 : 0,
				new Faixa("CLUBE_100", "Clube dos 100 kg", "Conclua uma série com 100 kg ou mais na barra.", 1));
		familia(lista, "Drop-set", h.dropSets(),
				new Faixa("DROPSET_1", "Primeiro drop-set", "Conclua um drop-set.", 1),
				new Faixa("DROPSET_25", "Queima total", "Conclua 25 drop-sets.", 25),
				new Faixa("DROPSET_100", "Mestre do drop-set", "Conclua 100 drop-sets.", 100));
		// Peso corporal (api#43).
		familia(lista, "Reps de peso corporal", (int) Math.min(Integer.MAX_VALUE, h.repsPesoCorporal()),
				new Faixa("REPS_PESO_CORPORAL_500", "Peso pena", "Some 500 reps em exercícios de peso corporal.", 500),
				new Faixa("REPS_PESO_CORPORAL_2000", "Calistênico", "Some 2.000 reps em exercícios de peso corporal.", 2000),
				new Faixa("REPS_PESO_CORPORAL_10000", "Mestre do próprio peso", "Some 10.000 reps em exercícios de peso corporal.", 10000));
		familia(lista, "Série longa", h.serieLonga(),
				new Faixa("SERIE_LONGA_20", "Fôlego", "Faça 20 reps numa série de peso corporal.", 20),
				new Faixa("SERIE_LONGA_35", "Resistência", "Faça 35 reps numa série de peso corporal.", 35),
				new Faixa("SERIE_LONGA_50", "Incansável", "Faça 50 reps numa série de peso corporal.", 50));
		oculta(lista, "Madrugador", h.madrugador(), "MADRUGADOR", "Termine um treino antes das 6h.");
		oculta(lista, "Coruja", h.coruja(), "CORUJA", "Termine um treino depois das 22h.");
		oculta(lista, "Ano novo", h.anoNovo(), "ANO_NOVO", "Treine em 1º de janeiro.");
		familia(lista, "Mais forte que eu", h.maisForte() ? 1 : 0,
				new Faixa("MAIS_FORTE", "Mais forte que eu", "Conclua uma série na barra com carga igual ou maior que o seu peso corporal.", 1));
		return lista;
	}

	private static void familia(List<ConquistaResponse> lista, String familia, int progresso, Faixa... faixas) {
		for (int i = 0; i < faixas.length; i++) {
			Faixa f = faixas[i];
			lista.add(new ConquistaResponse(f.codigo(), f.nome(), f.descricao(), progresso >= f.meta(), familia, i + 1,
					Math.min(progresso, f.meta()), f.meta(), false));
		}
	}

	/** Conquista oculta, de faixa unica: o front so a mostra depois de feita. */
	private static void oculta(List<ConquistaResponse> lista, String nome, boolean feita, String codigo, String descricao) {
		lista.add(new ConquistaResponse(codigo, nome, descricao, feita, nome, 1, feita ? 1 : 0, 1, true));
	}
}
