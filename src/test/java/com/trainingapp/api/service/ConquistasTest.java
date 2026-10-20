package com.trainingapp.api.service;

import com.trainingapp.api.dto.ConquistaResponse;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ConquistasTest {

	private static ConquistaResponse codigo(List<ConquistaResponse> lista, String codigo) {
		return lista.stream().filter(c -> c.codigo().equals(codigo)).findFirst().orElseThrow();
	}

	@Test
	void faixaSaiNoLimiteEOProgressoParaNaMeta() {
		var lista = Conquistas.avaliar(new Conquistas.Totais(50, 0, 0, 0, 0, 1, false, false));
		assertThat(codigo(lista, "TREINOS_50").conquistada()).isTrue();
		assertThat(codigo(lista, "TREINOS_10").progresso()).isEqualTo(10);
		var cem = codigo(lista, "TREINOS_100");
		assertThat(cem.conquistada()).isFalse();
		assertThat(cem.progresso()).isEqualTo(50);
		assertThat(cem.meta()).isEqualTo(100);
		assertThat(cem.familia()).isEqualTo("Constância");
		assertThat(cem.faixa()).isEqualTo(4);
		assertThat(codigo(Conquistas.avaliar(new Conquistas.Totais(49, 0, 0, 0, 0, 1, false, false)), "TREINOS_50")
				.conquistada()).isFalse();
	}

	@Test
	void cadaTotalAlimentaSuaFamilia() {
		var lista = Conquistas.avaliar(new Conquistas.Totais(0, 60, 12, 25, 10, 20, true, true));
		for (String c : List.of("STREAK_60", "SEMANAS_12", "RECORDES_25", "RECORDES_VARIADOS_10", "NIVEL_20", "DE_VOLTA",
				"DIA_INSPIRADO")) {
			assertThat(codigo(lista, c).conquistada()).as(c).isTrue();
		}
		for (String c : List.of("STREAK_100", "SEMANAS_26", "RECORDES_50", "RECORDES_VARIADOS_25", "NIVEL_30", "PRIMEIRO_TREINO")) {
			assertThat(codigo(lista, c).conquistada()).as(c).isFalse();
		}
		assertThat(lista).extracting(ConquistaResponse::codigo).doesNotHaveDuplicates();
	}
}
