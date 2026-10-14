package com.trainingapp.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O app Android (https://localhost) chama a API por outra origem; nenhuma outra origem ganha CORS nem passa a ser barrada. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsApiTest {

	static final String APP = "https://localhost";

	@Autowired
	MockMvc mvc;

	@Test
	void preflightDoAppLiberaOsMetodosEHeadersDaApiSemCredenciais() throws Exception {
		mvc.perform(options("/api/planos").header(HttpHeaders.ORIGIN, APP)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,x-cliente"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, APP))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

		mvc.perform(get("/api/auth/config").header(HttpHeaders.ORIGIN, APP))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, APP));
	}

	@Test
	void outraOrigemNaoRecebeCorsENaoEhBarradaPeloCors() throws Exception {
		mvc.perform(options("/api/planos").header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

		// Como antes do CORS: um Origin que nao bate com o host (proxy mal configurado) nao vira 403; segue para o login.
		mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, "https://apptraining.duckdns.org")
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ninguem@teste.com\",\"senha\":\"errada\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
