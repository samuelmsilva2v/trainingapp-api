package com.trainingapp.api.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = UsuarioDeTeste.ID)
@Transactional
class PerfilApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void usuarioPadraoExisteSemFusoHorario() throws Exception {
		mvc.perform(get("/api/perfil"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Atleta"))
				.andExpect(jsonPath("$.fusoHorario").doesNotExist());
	}

	@Test
	void atualizaNome() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Samuel\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Samuel"));
	}

	@Test
	void clienteAntigoQueAindaEnviaFusoNaoQuebra() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Samuel\",\"fusoHorario\":\"America/Manaus\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fusoHorario").doesNotExist());
	}

	@Test
	void guardaOLembreteDePesagemEOPerfilInteiroSubstitui() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Samuel\",\"lembretePesoDias\":15}"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.lembretePesoDias").value(15));

		// Campo ausente vira nulo: lembrete desligado.
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Samuel\"}"))
				.andExpect(jsonPath("$.lembretePesoDias").isEmpty());
	}

	@Test
	void lembreteForaDe7_15_30Retorna400() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"S\",\"lembretePesoDias\":10}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nomeEmBrancoRetorna400() throws Exception {
		mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\" \"}"))
				.andExpect(status().isBadRequest());
	}

	// --- foto do perfil (api#48) ---

	static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3 };
	static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 9 };
	static final byte[] WEBP = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 9 };

	@Test
	void comecaSemFotoEnviaBuscaERemove() throws Exception {
		mvc.perform(get("/api/perfil")).andExpect(jsonPath("$.fotoTipo").value("PADRAO"));
		mvc.perform(get("/api/perfil/foto")).andExpect(status().isNotFound());

		mvc.perform(put("/api/perfil/foto").contentType(MediaType.IMAGE_JPEG).content(JPEG))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotoTipo").value("PROPRIA"));
		mvc.perform(get("/api/perfil/foto"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(content().bytes(JPEG));

		mvc.perform(delete("/api/perfil/foto"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotoTipo").value("PADRAO"));
		mvc.perform(get("/api/perfil/foto")).andExpect(status().isNotFound());
	}

	@Test
	void reconheceOFormatoPelosBytesENaoPeloContentType() throws Exception {
		mvc.perform(put("/api/perfil/foto").contentType(MediaType.IMAGE_JPEG).content(PNG)).andExpect(status().isOk());
		mvc.perform(get("/api/perfil/foto")).andExpect(content().contentType(MediaType.IMAGE_PNG));
		mvc.perform(put("/api/perfil/foto").contentType(MediaType.APPLICATION_OCTET_STREAM).content(WEBP)).andExpect(status().isOk());
		mvc.perform(get("/api/perfil/foto")).andExpect(content().contentType("image/webp"));

		mvc.perform(put("/api/perfil/foto").contentType(MediaType.IMAGE_JPEG).content("<svg onload=alert(1)>".getBytes()))
				.andExpect(status().isBadRequest());
		mvc.perform(put("/api/perfil/foto").contentType(MediaType.IMAGE_JPEG).content(new byte[0]))
				.andExpect(status().isBadRequest());
	}

	@Test
	void recusaFotoComMaisDe5Mb() throws Exception {
		byte[] grande = new byte[5 * 1024 * 1024 + 1];
		System.arraycopy(JPEG, 0, grande, 0, JPEG.length);
		mvc.perform(put("/api/perfil/foto").contentType(MediaType.IMAGE_JPEG).content(grande)).andExpect(status().isBadRequest());

		byte[] noLimite = new byte[5 * 1024 * 1024];
		System.arraycopy(JPEG, 0, noLimite, 0, JPEG.length);
		mvc.perform(put("/api/perfil/foto").contentType(MediaType.IMAGE_JPEG).content(noLimite)).andExpect(status().isOk());
	}
}
