package com.trainingapp.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.trainingapp.api.UsuarioDeTeste;
import com.trainingapp.api.model.Equipamento;
import com.trainingapp.api.model.Exercicio;
import com.trainingapp.api.model.GrupoMuscular;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.ExercicioRepository;
import com.trainingapp.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
class CompartilhamentoApiTest {

	static final String OUTRO_ID = "00000000-0000-0000-0000-000000000002";

	@Autowired
	MockMvc mvc;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	ExercicioRepository exercicios;

	RequestPostProcessor outro = user(OUTRO_ID);
	String catalogo;
	String proprio;
	String plano;

	@BeforeEach
	void planoDoAtleta() throws Exception {
		Usuario u = new Usuario();
		u.setId(UUID.fromString(OUTRO_ID));
		u.setNome("Outro");
		u.setEmail("outro@teste.com");
		u.setEmailVerificado(true);
		usuarios.save(u);

		Exercicio global = new Exercicio();
		global.setNome("Agachamento livre");
		global.setGrupoMuscular(GrupoMuscular.QUADRICEPS);
		global.setEquipamento(Equipamento.BARRA);
		catalogo = exercicios.save(global).getId().toString();
		proprio = JsonPath.read(mvc.perform(post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Supino do atleta\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
		plano = JsonPath.read(mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(planoCom("ABC")))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
	}

	@Test
	void previaMostraSoOPrimeiroNomeEDizSeQuemAbriuEhODono() throws Exception {
		Usuario dono = usuarios.findById(UUID.fromString(UsuarioDeTeste.ID)).orElseThrow();
		dono.setNome("  Lucas Almeida Souza ");
		usuarios.save(dono);
		String token = compartilhar();

		mvc.perform(get("/api/compartilhados/" + token).with(outro))
				.andExpect(jsonPath("$.compartilhadoPor").value("Lucas"))
				.andExpect(jsonPath("$.seu").value(false));
		mvc.perform(get("/api/compartilhados/" + token))
				.andExpect(jsonPath("$.seu").value(true));
	}

	@Test
	void outraPessoaVeAPreviaESalvaUmaCopiaSemCargaComOExercicioProprioCopiado() throws Exception {
		String token = compartilhar();
		// Editar o original depois de gerar nao muda o link.
		mvc.perform(put("/api/planos/" + plano).contentType(MediaType.APPLICATION_JSON).content(planoCom("Mudou")))
				.andExpect(status().isOk());

		mvc.perform(get("/api/compartilhados/" + token).with(outro))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("ABC"))
				.andExpect(jsonPath("$.compartilhadoPor").value("Atleta"))
				.andExpect(jsonPath("$.dias[0].nome").value("Treino A"))
				.andExpect(jsonPath("$.dias[0].exercicios[0].exercicioNome").value("Agachamento livre"))
				.andExpect(jsonPath("$.dias[0].exercicios[0].drops").value(2))
				.andExpect(jsonPath("$.dias[0].exercicios[1].exercicioNome").value("Supino do atleta"))
				.andExpect(jsonPath("$.dias[0].exercicios[1].descansoSegundos").value(90))
				.andExpect(jsonPath("$.dias[0].exercicios[1].cargaAlvo").doesNotExist());

		String salvo = mvc.perform(post("/api/compartilhados/" + token + "/salvar").with(outro))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("ABC"))
				.andExpect(jsonPath("$.ativo").value(true))
				.andExpect(jsonPath("$.dias[0].exercicios[0].exercicioId").value(catalogo))
				.andExpect(jsonPath("$.dias[0].exercicios[0].cargaAlvo").isEmpty())
				.andExpect(jsonPath("$.dias[0].exercicios[1].exercicioNome").value("Supino do atleta"))
				.andExpect(jsonPath("$.dias[0].exercicios[1].cargaAlvo").isEmpty())
				.andExpect(jsonPath("$.dias[0].exercicios[1].series").value(4))
				.andReturn().getResponse().getContentAsString();
		String copia = JsonPath.read(salvo, "$.dias[0].exercicios[1].exercicioId");
		assertThat(copia).isNotEqualTo(proprio);
		assertThat(exercicios.findById(UUID.fromString(copia)).orElseThrow().getUsuario().getId()).hasToString(OUTRO_ID);

		// Salvar de novo duplica o plano, reaproveitando o exercicio proprio ja copiado; o segundo nao nasce ativo.
		mvc.perform(post("/api/compartilhados/" + token + "/salvar").with(outro))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ativo").value(false))
				.andExpect(jsonPath("$.dias[0].exercicios[1].exercicioId").value(copia));
		assertThat(exercicios.findByUsuarioId(UUID.fromString(OUTRO_ID))).hasSize(1);
		mvc.perform(get("/api/planos").with(outro)).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void salvarEAtivarTrocaOPlanoAtivo() throws Exception {
		mvc.perform(post("/api/compartilhados/" + compartilhar() + "/salvar").param("ativar", "true"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ativo").value(true));
		mvc.perform(get("/api/planos/" + plano)).andExpect(jsonPath("$.ativo").value(false));
	}

	@Test
	void cadaCompartilhamentoGeraUmLinkNovo() throws Exception {
		assertThat(compartilhar()).isNotEqualTo(compartilhar());
	}

	@Test
	void linkInvalidoPlanoDeOutroESemLogin() throws Exception {
		mvc.perform(get("/api/compartilhados/naoexiste")).andExpect(status().isNotFound());
		mvc.perform(post("/api/compartilhados/naoexiste/salvar")).andExpect(status().isNotFound());
		mvc.perform(post("/api/planos/" + plano + "/compartilhar").with(outro)).andExpect(status().isNotFound());
		mvc.perform(get("/api/compartilhados/" + compartilhar()).with(anonymous())).andExpect(status().isUnauthorized());
	}

	private String compartilhar() throws Exception {
		String link = JsonPath.read(mvc.perform(post("/api/planos/" + plano + "/compartilhar"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.link").value(matchesPattern("http://localhost:4200/compartilhado/[A-Za-z0-9_-]{16}")))
				.andReturn().getResponse().getContentAsString(), "$.link");
		return link.substring(link.lastIndexOf('/') + 1);
	}

	private String planoCom(String nome) {
		return """
				{"nome":"%s","objetivo":"HIPERTROFIA","dias":[{"nome":"Treino A","exercicios":[
				 {"exercicioId":"%s","series":3,"repsMin":6,"repsMax":6,"cargaAlvo":100,"drops":2},
				 {"exercicioId":"%s","series":4,"repsMin":8,"repsMax":12,"cargaAlvo":60,"descansoSegundos":90}]}]}
				""".formatted(nome, catalogo, proprio);
	}
}
