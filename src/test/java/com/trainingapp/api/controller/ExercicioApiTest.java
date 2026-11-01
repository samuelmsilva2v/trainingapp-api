package com.trainingapp.api.controller;

import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.repository.ExercicioRepository;
import com.trainingapp.api.repository.UsuarioRepository;
import com.trainingapp.api.service.CatalogoImportador;
import com.trainingapp.api.service.ExercicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import com.trainingapp.api.UsuarioDeTeste;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
class ExercicioApiTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	CatalogoImportador importador;

	@Autowired
	ExercicioRepository exercicios;

	@Autowired
	UsuarioRepository usuarios;

	@BeforeEach
	void carregarCatalogo() {
		importador.importar();
	}

	@Test
	void buscaIgnoraAcentoECaixa() throws Exception {
		mvc.perform(get("/api/exercicios").param("busca", "ELEVACAO de pernas SUSPENSO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nome").value("Elevação de pernas suspenso"))
				.andExpect(jsonPath("$[0].personalizado").value(false))
				.andExpect(jsonPath("$[0].imagens.length()").value(2))
				.andExpect(jsonPath("$[0].imagens[0]").value(
						"https://cdn.jsdelivr.net/gh/yuhonas/free-exercise-db@f00c92c7dcf1216a928a52c3706c7ce8e2f71ed5/exercises/Hanging_Leg_Raise/0.jpg"));
	}

	@Test
	void filtraPorGrupoMuscular() throws Exception {
		mvc.perform(get("/api/exercicios").param("grupo", "TRAPEZIO"))
				.andExpect(jsonPath("$[*].grupoMuscular").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("TRAPEZIO"))));
	}

	@Test
	void naoListaCardioAlongamentoPliometriaNemStrongman() throws Exception {
		mvc.perform(get("/api/exercicios").param("grupo", "CARDIO")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/exercicios")).andExpect(jsonPath("$.length()").value(876 - 14 - 123 - 61 - 21));
		// Peso corporal fica, com o fator de XP do catalogo.
		mvc.perform(get("/api/exercicios").param("busca", "barra fixa pronada")).andExpect(jsonPath("$.length()").value(1));
		org.assertj.core.api.Assertions.assertThat(exercicios.findBySlug("Pullups").orElseThrow().getFatorXp())
				.isEqualByComparingTo("2.0");
		org.assertj.core.api.Assertions.assertThat(exercicios.findBySlug("Barbell_Squat").orElseThrow().getFatorXp()).isNull();
	}

	@Test
	void exercicioPersonalizadoApareceNaListagem() throws Exception {
		mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Sled push\",\"grupoMuscular\":\"CORPO_INTEIRO\",\"equipamento\":\"OUTRO\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.personalizado").value(true))
				.andExpect(jsonPath("$.imagens.length()").value(0));

		mvc.perform(get("/api/exercicios").param("busca", "sled push"))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void exercicioSemNomeRetorna400() throws Exception {
		mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\" \",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void exercicioAlemDoLimiteRetorna422() throws Exception {
		var usuario = usuarios.findById(UUID.fromString(UsuarioDeTeste.ID)).orElseThrow();
		exercicios.saveAll(IntStream.range(0, ExercicioService.LIMITE_POR_USUARIO).mapToObj(i -> {
			Exercicio e = new Exercicio();
			e.setNome("Proprio " + i);
			e.setGrupoMuscular(GrupoMuscular.PEITO);
			e.setEquipamento(Equipamento.OUTRO);
			e.setUsuario(usuario);
			return e;
		}).toList());

		mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Mais um\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"OUTRO\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(startsWith("Limite de ")));
	}
}
