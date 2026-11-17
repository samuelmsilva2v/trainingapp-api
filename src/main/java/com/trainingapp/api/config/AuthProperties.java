package com.trainingapp.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

@ConfigurationProperties("trainingapp.auth")
public record AuthProperties(String jwtSegredo, List<String> emailsPermitidos, String googleClientId,
		String googleClientSecret, boolean cookieSeguro, boolean cadastroAberto) {

	public AuthProperties {
		emailsPermitidos = emailsPermitidos == null ? List.of()
				: emailsPermitidos.stream().map(e -> e.trim().toLowerCase(Locale.ROOT)).filter(e -> !e.isEmpty()).toList();
	}

	public boolean googleConfigurado() {
		return googleClientId != null && !googleClientId.isBlank() && googleClientSecret != null && !googleClientSecret.isBlank();
	}

	public boolean permitido(String email) {
		return cadastroAberto || emailsPermitidos.contains(email.trim().toLowerCase(Locale.ROOT));
	}
}
