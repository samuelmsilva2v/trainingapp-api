package com.trainingapp.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "trainingapp.cors.origens=http://localhost:4200,http://192.168.*.*:4200")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void liberaLocalhostEOIpDaRedeLocalNaPortaDoFront() throws Exception {
		for (String origem : new String[] { "http://localhost:4200", "http://192.168.1.61:4200" }) {
			mvc.perform(options("/api/planos").header("Origin", origem).header("Access-Control-Request-Method", "PUT"))
					.andExpect(status().isOk())
					.andExpect(header().string("Access-Control-Allow-Origin", origem));
		}
	}

	@Test
	void naoLiberaOutrasOrigens() throws Exception {
		mvc.perform(options("/api/planos").header("Origin", "http://evil.example:4200")
				.header("Access-Control-Request-Method", "PUT"))
				.andExpect(status().isForbidden());
		mvc.perform(options("/api/planos").header("Origin", "http://192.168.1.61:9999")
				.header("Access-Control-Request-Method", "PUT"))
				.andExpect(status().isForbidden());
	}
}
