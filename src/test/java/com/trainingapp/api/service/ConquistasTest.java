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
		var lista = Conquistas.avaliar(new Conquistas.Totais(50, 0, 0, 0, 0, 1, false, false), HistoricoDeTreinos.Totais.ZERO);
		assertThat(codigo(lista, "TREINOS_50").conquistada()).isTrue();
		assertThat(codigo(lista, "TREINOS_10").progresso()).isEqualTo(10);
		var cem = codigo(lista, "TREINOS_100");
		assertThat(cem.conquistada()).isFalse();
		assertThat(cem.progresso()).isEqualTo(50);
		assertThat(cem.meta()).isEqualTo(100);
		assertThat(cem.familia()).isEqualTo("Constância");
		assertThat(cem.faixa()).isEqualTo(4);
		assertThat(codigo(Conquistas.avaliar(new Conquistas.Totais(49, 0, 0, 0, 0, 1, false, false), HistoricoDeTreinos.Totais.ZERO), "TREINOS_50")
				.conquistada()).isFalse();
	}

	@Test
	void cadaTotalAlimentaSuaFamilia() {
		var lista = Conquistas.avaliar(new Conquistas.Totais(0, 60, 12, 25, 10, 20, true, true), HistoricoDeTreinos.Totais.ZERO);
		for (String c : List.of("STREAK_60", "SEMANAS_12", "RECORDES_25", "RECORDES_VARIADOS_10", "NIVEL_20", "DE_VOLTA",
				"DIA_INSPIRADO")) {
			assertThat(codigo(lista, c).conquistada()).as(c).isTrue();
		}
		for (String c : List.of("STREAK_100", "SEMANAS_26", "RECORDES_50", "RECORDES_VARIADOS_25", "NIVEL_30", "PRIMEIRO_TREINO")) {
			assertThat(codigo(lista, c).conquistada()).as(c).isFalse();
		}
		assertThat(lista).extracting(ConquistaResponse::codigo).doesNotHaveDuplicates();
	}

	@Test
	void fatia2AlimentaAsFamiliasNovasEAsOcultasSaoMarcadas() {
		var h = new HistoricoDeTreinos.Totais(100, true, 50, true, true, true, 25, true, false, true, true);
		var lista = Conquistas.avaliar(new Conquistas.Totais(0, 0, 0, 0, 0, 1, false, false), h);
		for (String c : List.of("VOLUME_100T", "TREINO_PESADO", "VARIEDADE_50", "CORPO_COMPLETO", "ROTACAO_COMPLETA", "CLUBE_100",
				"DROPSET_25", "MADRUGADOR", "ANO_NOVO", "MAIS_FORTE")) {
			assertThat(codigo(lista, c).conquistada()).as(c).isTrue();
		}
		for (String c : List.of("VOLUME_1000T", "VARIEDADE_100", "DROPSET_100", "CORUJA")) {
			assertThat(codigo(lista, c).conquistada()).as(c).isFalse();
		}
		assertThat(lista).filteredOn(ConquistaResponse::oculta).extracting(ConquistaResponse::codigo)
				.containsExactlyInAnyOrder("MADRUGADOR", "CORUJA", "ANO_NOVO");
		assertThat(lista).hasSize(25 + 17) // fatia 1 (25 faixas) + fatia 2 (17)
				.extracting(ConquistaResponse::codigo).doesNotHaveDuplicates();
	}
}
