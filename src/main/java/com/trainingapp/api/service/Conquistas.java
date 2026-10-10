package com.trainingapp.api.service;

import com.trainingapp.api.dto.ConquistaResponse;
import java.util.List;

/**
 * Conquistas derivadas dos eventos (nada e gravado): so dependem de totais ja calculados, entao
 * ajustar ou acrescentar uma regra aqui nao exige migracao.
 */
final class Conquistas {

	private Conquistas() {
	}

	static List<ConquistaResponse> avaliar(int treinos, int recordes, int melhorStreak, int nivel) {
		return List.of(
				new ConquistaResponse("PRIMEIRO_TREINO", "Primeiro treino", "Conclua seu primeiro treino.", treinos >= 1),
				new ConquistaResponse("TREINOS_10", "Constância", "Conclua 10 treinos.", treinos >= 10),
				new ConquistaResponse("TREINOS_50", "Rotina de ferro", "Conclua 50 treinos.", treinos >= 50),
				new ConquistaResponse("STREAK_7", "Uma semana de fogo", "Alcance uma sequência de 7 dias.", melhorStreak >= 7),
				new ConquistaResponse("STREAK_30", "Hábito formado", "Alcance uma sequência de 30 dias.", melhorStreak >= 30),
				new ConquistaResponse("PRIMEIRO_RECORDE", "Novo recorde", "Bata seu primeiro recorde de carga.", recordes >= 1),
				new ConquistaResponse("RECORDES_10", "Quebrador de marcas", "Bata 10 recordes de carga.", recordes >= 10),
				new ConquistaResponse("NIVEL_5", "Nível 5", "Chegue ao nível 5.", nivel >= 5),
				new ConquistaResponse("NIVEL_10", "Nível 10", "Chegue ao nível 10.", nivel >= 10));
	}
}
