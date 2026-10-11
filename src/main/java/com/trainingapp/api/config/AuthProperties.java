package com.trainingapp.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

/**
 * Configuracao de login ({@code trainingapp.auth.*}). Todos os valores vem do ambiente em producao.
 *
 * @param jwtSegredo      chave HS256 do access token; pelo menos 32 caracteres
 * @param emailsPermitidos quem pode se cadastrar e entrar; vazia = ninguem
 * @param googleClientId  client id web do Google Cloud; vazio desliga o "Entrar com Google"
 * @param cookieSeguro    marca o cookie do refresh como Secure (exige HTTPS; localhost e excecao nos navegadores)
 */
@ConfigurationProperties("trainingapp.auth")
public record AuthProperties(String jwtSegredo, List<String> emailsPermitidos, String googleClientId,
		boolean cookieSeguro) {

	public AuthProperties {
		emailsPermitidos = emailsPermitidos == null ? List.of()
				: emailsPermitidos.stream().map(e -> e.trim().toLowerCase(Locale.ROOT)).filter(e -> !e.isEmpty()).toList();
	}

	public boolean permitido(String email) {
		return emailsPermitidos.contains(email.trim().toLowerCase(Locale.ROOT));
	}
}
