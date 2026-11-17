package com.trainingapp.api.service;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.TipoSerie;
import com.trainingapp.api.repository.ExercicioDoTreino;
import com.trainingapp.api.repository.TreinoDaConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricoDeTreinosTest {

	private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
	private static final UUID PLANO = UUID.randomUUID();
	private static final UUID OUTRO_PLANO = UUID.randomUUID();
	private final List<TreinoDaConta> treinos = new ArrayList<>();
	private final List<ExercicioDoTreino> exercicios = new ArrayList<>();
	private final TreeMap<LocalDate, BigDecimal> pesos = new TreeMap<>();

	private UUID treino(String fimLocal, UUID plano, int diaOrdem) {
		UUID id = UUID.randomUUID();
		treinos.add(new TreinoDaConta(id, java.time.LocalDateTime.parse(fimLocal).atZone(SP).toInstant(), plano, diaOrdem));
		return id;
	}

	private void exercicio(UUID treino, GrupoMuscular grupo, Equipamento equip, String volume, String maior) {
		exercicios.add(new ExercicioDoTreino(treino, UUID.randomUUID(), grupo, equip, new BigDecimal(volume), new BigDecimal(maior),
				0L, 0));
	}

	private void pesoCorporal(UUID treino, String volume, long reps, int maisReps) {
		exercicios.add(new ExercicioDoTreino(treino, UUID.randomUUID(), GrupoMuscular.COSTAS, Equipamento.PESO_CORPORAL,
				new BigDecimal(volume), new BigDecimal("80"), reps, maisReps));
	}

	@Test
	void pesoCorporalFicaForaDoVolumeESomaRepsSoDeTreinosValidos() {
		UUID a = treino("2026-10-26T18:00", null, 0);
		UUID b = treino("2026-10-27T18:00", null, 0);
		UUID invalido = treino("2026-10-27T19:00", null, 0);
		exercicio(a, GrupoMuscular.PEITO, Equipamento.BARRA, "2000", "60");
		pesoCorporal(a, "9000", 30, 12);
		pesoCorporal(b, "9000", 45, 25);
		pesoCorporal(invalido, "9000", 500, 60);
		var t = totais(Set.of(a, b));
		assertThat(t.toneladas()).isEqualTo(2);
		assertThat(t.treinoPesado()).isFalse();
		assertThat(t.repsPesoCorporal()).isEqualTo(75);
		assertThat(t.serieLonga()).isEqualTo(25);
	}

	private HistoricoDeTreinos.Totais totais(Set<UUID> validos) {
		return HistoricoDeTreinos.de(treinos, validos, exercicios, List.of(), pesos, Map.of(PLANO, 3, OUTRO_PLANO, 1), SP);
	}

	@Test
	void volumeEmToneladasETreinoPesadoContamSoTreinosValidos() {
		UUID a = treino("2026-10-26T18:00", null, 0);
		UUID b = treino("2026-10-27T18:00", null, 0);
		UUID invalido = treino("2026-10-27T19:00", null, 0);
		exercicio(a, GrupoMuscular.PEITO, Equipamento.BARRA, "6000", "60");
		exercicio(a, GrupoMuscular.COSTAS, Equipamento.CABO, "4000", "40");
		exercicio(b, GrupoMuscular.PEITO, Equipamento.BARRA, "9999", "70");
		exercicio(invalido, GrupoMuscular.PEITO, Equipamento.BARRA, "50000", "80");
		var t = totais(Set.of(a, b));
		assertThat(t.toneladas()).isEqualTo(19);
		assertThat(t.treinoPesado()).isTrue();
		assertThat(t.variedade()).isEqualTo(3);

		var abaixo = HistoricoDeTreinos.de(treinos, Set.of(b), exercicios, List.of(), pesos, Map.of(), SP);
		assertThat(abaixo.treinoPesado()).isFalse();
	}

	@Test
	void corpoCompletoPedeOsSeisMacrogruposNaMesmaSemana() {
		UUID seg = treino("2026-10-26T18:00", null, 0);
		UUID ter = treino("2026-10-27T18:00", null, 0);
		exercicio(seg, GrupoMuscular.PEITO, Equipamento.BARRA, "1", "1");
		exercicio(seg, GrupoMuscular.COSTAS, Equipamento.BARRA, "1", "1");
		exercicio(seg, GrupoMuscular.OMBROS, Equipamento.HALTERES, "1", "1");
		exercicio(ter, GrupoMuscular.TRICEPS, Equipamento.CABO, "1", "1");
		exercicio(ter, GrupoMuscular.GLUTEOS, Equipamento.MAQUINA, "1", "1");
		assertThat(totais(Set.of(seg, ter)).corpoCompleto()).isFalse();
		UUID domingoSeguinte = treino("2026-11-02T18:00", null, 0);
		exercicio(domingoSeguinte, GrupoMuscular.ABDOMEN, Equipamento.PESO_CORPORAL, "1", "1");
		assertThat(totais(Set.of(seg, ter, domingoSeguinte)).corpoCompleto()).isFalse();
		UUID qua = treino("2026-10-28T18:00", null, 0);
		exercicio(qua, GrupoMuscular.ABDOMEN, Equipamento.PESO_CORPORAL, "1", "1");
		assertThat(totais(Set.of(seg, ter, qua)).corpoCompleto()).isTrue();
	}

	@Test
	void clubeDos100EMaisForteSoContamNaBarra() {
		UUID maquina = treino("2026-10-26T18:00", null, 0);
		exercicio(maquina, GrupoMuscular.QUADRICEPS, Equipamento.MAQUINA, "2000", "200");
		pesos.put(LocalDate.of(2026, 10, 1), new BigDecimal("80"));
		var t = totais(Set.of(maquina));
		assertThat(t.clube100()).isFalse();
		assertThat(t.maisForte()).isFalse();

		UUID barra = treino("2026-10-27T18:00", null, 0);
		exercicio(barra, GrupoMuscular.QUADRICEPS, Equipamento.BARRA, "800", "100");
		t = totais(Set.of(maquina, barra));
		assertThat(t.clube100()).isTrue();
		assertThat(t.maisForte()).isTrue();
	}

	@Test
	void maisForteUsaAPesagemMaisRecenteAteODiaDoTreino() {
		UUID barra = treino("2026-10-27T18:00", null, 0);
		exercicio(barra, GrupoMuscular.PEITO, Equipamento.BARRA, "800", "80");
		assertThat(totais(Set.of(barra)).maisForte()).isFalse();
		pesos.put(LocalDate.of(2026, 10, 28), new BigDecimal("70"));
		assertThat(totais(Set.of(barra)).maisForte()).isFalse();
		pesos.put(LocalDate.of(2026, 10, 20), new BigDecimal("85"));
		assertThat(totais(Set.of(barra)).maisForte()).isFalse();
		pesos.put(LocalDate.of(2026, 10, 25), new BigDecimal("79.5"));
		assertThat(totais(Set.of(barra)).maisForte()).isTrue();
	}

	@Test
	void ocultasOlhamQualquerTreinoConcluidoPeloHorarioLocal() {
		UUID cedo = treino("2026-10-26T05:59", null, 0);
		UUID tarde = treino("2026-10-27T22:01", null, 0);
		UUID anoNovo = treino("2027-01-01T10:00", null, 0);
		var t = totais(Set.of());
		assertThat(t.madrugador()).isTrue();
		assertThat(t.coruja()).isTrue();
		assertThat(t.anoNovo()).isTrue();

		treinos.clear();
		treino("2026-10-26T06:00", null, 0);
		treino("2026-10-27T22:00", null, 0);
		var limite = totais(Set.of());
		assertThat(limite.madrugador()).isFalse();
		assertThat(limite.coruja()).isFalse();
		assertThat(cedo).isNotEqualTo(tarde).isNotEqualTo(anoNovo);
	}

	@Test
	void rotacaoCompletaNoMesmoPlanoEmOrdem() {
		UUID a = treino("2026-10-26T18:00", PLANO, 0);
		UUID b = treino("2026-10-27T18:00", PLANO, 1);
		UUID c = treino("2026-10-28T18:00", PLANO, 2);
		assertThat(totais(Set.of(a, b, c)).rotacaoCompleta()).isTrue();
		assertThat(totais(Set.of(a, c)).rotacaoCompleta()).isFalse();
	}

	@Test
	void rotacaoQuebraComTreinoDeOutroPlanoNoMeioEPlanoDeUmDiaNaoVale() {
		UUID a = treino("2026-10-26T18:00", PLANO, 0);
		UUID outro = treino("2026-10-27T10:00", OUTRO_PLANO, 0);
		UUID b = treino("2026-10-27T18:00", PLANO, 1);
		UUID c = treino("2026-10-28T18:00", PLANO, 2);
		assertThat(totais(Set.of(a, outro, b, c)).rotacaoCompleta()).isFalse();

		treinos.clear();
		UUID unico = treino("2026-10-26T18:00", OUTRO_PLANO, 0);
		assertThat(totais(Set.of(unico)).rotacaoCompleta()).isFalse();
	}

	@Test
	void blocosComDropContamUmaVezPorBloco() {
		UUID sessao = UUID.randomUUID();
		UUID ex = UUID.randomUUID();
		List<Object[]> linhas = List.of(
				new Object[] { sessao, ex, 0, TipoSerie.NORMAL, true },
				new Object[] { sessao, ex, 1, TipoSerie.DROP, true },
				new Object[] { sessao, ex, 2, TipoSerie.DROP, true },
				new Object[] { sessao, ex, 3, TipoSerie.NORMAL, false },
				new Object[] { sessao, ex, 4, TipoSerie.DROP, false },
				new Object[] { sessao, ex, 5, TipoSerie.NORMAL, true },
				new Object[] { sessao, ex, 6, TipoSerie.DROP, true });
		assertThat(HistoricoDeTreinos.blocosComDrop(linhas)).hasSize(2);
		UUID valido = UUID.randomUUID();
		var t = HistoricoDeTreinos.de(List.of(), Set.of(valido), List.of(), List.of(valido, valido, UUID.randomUUID()), new TreeMap<>(),
				Map.of(), SP);
		assertThat(t.dropSets()).isEqualTo(2);
	}

	@Test
	void validosSaoAsSessoesComSaldoPositivoDeSeries() {
		UUID ok = UUID.randomUUID();
		UUID estornada = UUID.randomUUID();
		assertThat(HistoricoDeTreinos.validos(Map.of(ok, 36, estornada, 0))).containsExactly(ok);
	}
}
