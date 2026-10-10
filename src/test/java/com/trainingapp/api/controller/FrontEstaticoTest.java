package com.trainingapp.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontEstaticoTest {

	static final Path PASTA = criarPastaDoFront();

	static Path criarPastaDoFront() {
		try {
			Path pasta = Files.createTempDirectory("front");
			Files.writeString(pasta.resolve("index.html"), "<app-root></app-root>");
			Files.writeString(pasta.resolve("main-ABC.js"), "console.log('app')");
			return pasta;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@DynamicPropertySource
	static void pastaDoFront(DynamicPropertyRegistry registro) {
		registro.add("trainingapp.web.pasta", PASTA::toString);
	}

	@Autowired
	MockMvc mvc;

	@Test
	void raizEncaminhaParaOIndexEArquivosSaoServidosSemCache() throws Exception {
		// MockMvc nao executa o forward; so registra o destino.
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/index.html"));
		mvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string("<app-root></app-root>"))
				.andExpect(header().string("Cache-Control", "no-cache"));
		mvc.perform(get("/main-ABC.js"))
				.andExpect(status().isOk())
				.andExpect(content().string("console.log('app')"));
	}

	@Test
	void rotaDoAngularDevolveOIndex() throws Exception {
		mvc.perform(get("/planos/00000000-0000-0000-0000-000000000001"))
				.andExpect(status().isOk())
				.andExpect(content().string("<app-root></app-root>"));
	}

	@Test
	void apiContinuaFuncionandoEApiInexistenteDa404() throws Exception {
		mvc.perform(get("/api/perfil"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Atleta"));
		mvc.perform(get("/api/nao-existe"))
				.andExpect(status().isNotFound());
	}
}
