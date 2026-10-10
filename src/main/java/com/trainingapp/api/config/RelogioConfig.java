package com.trainingapp.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelogioConfig {

	/** Fonte unica de data e hora: o fuso e o do sistema onde a API roda (nao ha mais fuso no perfil). */
	@Bean
	public Clock relogio() {
		return Clock.systemDefaultZone();
	}
}
