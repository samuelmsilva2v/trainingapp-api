package com.trainingapp.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.trainingapp.api.UsuarioDeTeste;
import com.trainingapp.api.model.Usuario;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Com o cadastro aberto, o filtro por usuario e a unica barreira entre contas: o outro usuario nao le nem altera nada. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
class IsolamentoEntreUsuariosApiTest {

	static final String OUTRO_ID = "00000000-0000-0000-0000-000000000002";

	@Autowired
	MockMvc mvc;

	@Autowired
	UsuarioRepository usuarios;

	RequestPostProcessor outro = user(OUTRO_ID);
	String exercicio;
	String plano;
	UUID sessao = UUID.randomUUID();
	UUID exSessao = UUID.randomUUID();
	UUID serie = UUID.randomUUID();

	@BeforeEach
	void dadosDoAtleta() throws Exception {
		Usuario u = new Usuario();
		u.setId(UUID.fromString(OUTRO_ID));
		u.setNome("Outro");
		u.setEmail("outro@teste.com");
		u.setEmailVerificado(true);
		usuarios.save(u);

		exercicio = criarExercicio(null);
		plano = JsonPath.read(mvc.perform(post("/api/planos").contentType(MediaType.APPLICATION_JSON).content(planoCom(exercicio)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
		mvc.perform(put("/api/sessoes/" + sessao).contentType(MediaType.APPLICATION_JSON)
				.content(sessaoCom(exSessao, exercicio, serie))).andExpect(status().isOk());
	}

	@Test
	void outroUsuarioNaoVeNemAlteraPlanosETreinos() throws Exception {
		mvc.perform(get("/api/planos").with(outro)).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/planos/" + plano).with(outro)).andExpect(status().isNotFound());
		mvc.perform(put("/api/planos/" + plano).with(outro).contentType(MediaType.APPLICATION_JSON).content(planoCom(criarExercicio(outro))))
				.andExpect(status().isNotFound());
		mvc.perform(post("/api/planos/" + plano + "/ativar").with(outro)).andExpect(status().isNotFound());
		mvc.perform(delete("/api/planos/" + plano).with(outro)).andExpect(status().isNotFound());

		mvc.perform(get("/api/hoje").with(outro)).andExpect(jsonPath("$.sessaoEmAndamento").isEmpty());
		mvc.perform(get("/api/sessoes/" + sessao).with(outro)).andExpect(status().isNotFound());
		mvc.perform(delete("/api/sessoes/" + sessao).with(outro)).andExpect(status().isNotFound());

		// Nem usar o exercicio proprio do atleta num plano dele.
		mvc.perform(post("/api/planos").with(outro).contentType(MediaType.APPLICATION_JSON).content(planoCom(exercicio)))
				.andExpect(status().isUnprocessableContent());

		mvc.perform(get("/api/planos/" + plano)).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("ABC"));
	}

	@Test
	void outroUsuarioNaoReaproveitaIdsDoTreinoDoAtleta() throws Exception {
		String proprio = criarExercicio(outro);
		// O id da sessao, de um exercicio ou de uma serie do atleta: o merge levaria a linha para o outro usuario.
		mvc.perform(put("/api/sessoes/" + sessao).with(outro).contentType(MediaType.APPLICATION_JSON)
				.content(sessaoCom(UUID.randomUUID(), proprio, UUID.randomUUID()))).andExpect(status().isUnprocessableContent());
		UUID nova = UUID.randomUUID();
		mvc.perform(put("/api/sessoes/" + nova).with(outro).contentType(MediaType.APPLICATION_JSON)
				.content(sessaoCom(exSessao, proprio, UUID.randomUUID()))).andExpect(status().isUnprocessableContent());
		mvc.perform(put("/api/sessoes/" + nova).with(outro).contentType(MediaType.APPLICATION_JSON)
				.content(sessaoCom(UUID.randomUUID(), proprio, serie))).andExpect(status().isUnprocessableContent());

		mvc.perform(get("/api/sessoes/" + sessao)).andExpect(status().isOk())
				.andExpect(jsonPath("$.exercicios[0].id").value(exSessao.toString()))
				.andExpect(jsonPath("$.exercicios[0].series[0].id").value(serie.toString()))
				.andExpect(jsonPath("$.exercicios[0].series[0].reps").value(10));
	}

	@Test
	void outroUsuarioNaoVeNemApagaAsPesagensDoAtleta() throws Exception {
		String hoje = java.time.LocalDate.now().toString();
		mvc.perform(put("/api/peso/" + hoje).contentType(MediaType.APPLICATION_JSON).content("{\"kg\":80}")).andExpect(status().isOk());

		mvc.perform(get("/api/peso").with(outro)).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(delete("/api/peso/" + hoje).with(outro)).andExpect(status().isNoContent());
		mvc.perform(put("/api/peso/" + hoje).with(outro).contentType(MediaType.APPLICATION_JSON).content("{\"kg\":90}")).andExpect(status().isOk());

		mvc.perform(get("/api/peso")).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].kg").value(80));
	}

	private String criarExercicio(RequestPostProcessor quem) throws Exception {
		var requisicao = post("/api/exercicios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Supino\",\"grupoMuscular\":\"PEITO\",\"equipamento\":\"BARRA\"}");
		return JsonPath.read(mvc.perform(quem == null ? requisicao : requisicao.with(quem))
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private static String planoCom(String exercicioId) {
		return """
				{"nome":"ABC","objetivo":"HIPERTROFIA","dias":[{"nome":"Treino A","exercicios":[
				  {"exercicioId":"%s","series":3,"repsMin":8,"repsMax":12}]}]}
				""".formatted(exercicioId);
	}

	private static String sessaoCom(UUID exSessao, String exercicioId, UUID serie) {
		Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		return """
				{"planoNome":"ABC","diaNome":"Treino A","diaOrdem":0,"estado":"EM_ANDAMENTO",
				 "iniciadaEm":"%s","finalizadaEm":null,"atualizadoEm":"%s",
				 "exercicios":[{"id":"%s","exercicioId":"%s","metaSeries":3,"metaRepsMin":8,"metaRepsMax":12,
				   "series":[{"id":"%s","reps":10,"carga":60,"concluida":true}]}]}
				""".formatted(agora, agora, exSessao, exercicioId, serie);
	}
}
