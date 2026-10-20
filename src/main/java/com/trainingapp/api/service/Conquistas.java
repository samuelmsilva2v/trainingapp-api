package com.trainingapp.api.service;

import com.trainingapp.api.dto.ConquistaResponse;
import java.util.ArrayList;
import java.util.List;

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

	static List<ConquistaResponse> avaliar(Totais t) {
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
				new Faixa("PRIMEIRO_RECORDE", "Novo recorde", "Bata seu primeiro recorde de carga.", 1),
				new Faixa("RECORDES_10", "Quebrador de marcas", "Bata 10 recordes de carga.", 10),
				new Faixa("RECORDES_25", "Sem teto", "Bata 25 recordes de carga.", 25),
				new Faixa("RECORDES_50", "Lenda das cargas", "Bata 50 recordes de carga.", 50));
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
		return lista;
	}

	private static void familia(List<ConquistaResponse> lista, String familia, int progresso, Faixa... faixas) {
		for (int i = 0; i < faixas.length; i++) {
			Faixa f = faixas[i];
			lista.add(new ConquistaResponse(f.codigo(), f.nome(), f.descricao(), progresso >= f.meta(), familia, i + 1,
					Math.min(progresso, f.meta()), f.meta()));
		}
	}
}
