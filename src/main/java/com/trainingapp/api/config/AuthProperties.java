package com.trainingapp.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

/**
 * Configuracao de login ({@code trainingapp.auth.*}). Todos os valores vem do ambiente em producao.
 *
 * @param jwtSegredo      chave HS256 do access token; pelo menos 32 caracteres
 * @param emailsPermitidos quem pode se cadastrar e entrar; vazia = ninguem (ignorada com {@code cadastroAberto})
 * @param googleClientId  client id web do Google Cloud; vazio desliga o "Entrar com Google"
 * @param googleClientSecret segredo desse client id (a API troca o codigo do popup do Google por tokens); vazio tambem desliga
 * @param cookieSeguro    marca o cookie do refresh como Secure (exige HTTPS; localhost e excecao nos navegadores)
 * @param cadastroAberto  qualquer e-mail pode se cadastrar e entrar; desligado por padrao (so a lista entra)
 */
@ConfigurationProperties("trainingapp.auth")
public record AuthProperties(String jwtSegredo, List<String> emailsPermitidos, String googleClientId,
		String googleClientSecret, boolean cookieSeguro, boolean cadastroAberto) {

	public AuthProperties {
		emailsPermitidos = emailsPermitidos == null ? List.of()
				: emailsPermitidos.stream().map(e -> e.trim().toLowerCase(Locale.ROOT)).filter(e -> !e.isEmpty()).toList();
	}

	/** O login com Google so funciona com o client id e o segredo juntos. */
	public boolean googleConfigurado() {
		return googleClientId != null && !googleClientId.isBlank() && googleClientSecret != null && !googleClientSecret.isBlank();
	}

	public boolean permitido(String email) {
		return cadastroAberto || emailsPermitidos.contains(email.trim().toLowerCase(Locale.ROOT));
	}
}
