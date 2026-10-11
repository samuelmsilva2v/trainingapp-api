package com.trainingapp.api.service;

import com.trainingapp.api.config.AuthProperties;
import com.trainingapp.api.config.ErroDeAutenticacaoException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Confere assinatura (chaves publicas do Google), validade, emissor e destinatario (nosso client id) do ID token. */
@Component
public class VerificadorGoogleJwt implements VerificadorGoogle {

	private static final String CHAVES = "https://www.googleapis.com/oauth2/v3/certs";
	private static final Set<String> EMISSORES = Set.of("https://accounts.google.com", "accounts.google.com");

	private final String clientId;
	private final NimbusJwtDecoder decoder;

	public VerificadorGoogleJwt(AuthProperties props) {
		this.clientId = props.googleClientId();
		this.decoder = NimbusJwtDecoder.withJwkSetUri(CHAVES).build(); // so busca as chaves no primeiro uso
		OAuth2TokenValidator<Jwt> emissor = jwt -> EMISSORES.contains(jwt.getClaimAsString("iss"))
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emissor invalido", null));
		OAuth2TokenValidator<Jwt> destinatario = jwt -> clientId != null && jwt.getAudience().contains(clientId)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Destinatario invalido", null));
		this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), emissor, destinatario));
	}

	@Override
	public Identidade verificar(String credencial) {
		if (clientId == null || clientId.isBlank()) {
			throw new ErroDeAutenticacaoException(HttpStatus.SERVICE_UNAVAILABLE, "Login com Google não está configurado");
		}
		Jwt jwt;
		try {
			jwt = decoder.decode(credencial);
		}
		catch (JwtException e) {
			throw new ErroDeAutenticacaoException(HttpStatus.UNAUTHORIZED, "Não foi possível validar o login com o Google");
		}
		String email = jwt.getClaimAsString("email");
		if (jwt.getSubject() == null || email == null) {
			throw new ErroDeAutenticacaoException(HttpStatus.UNAUTHORIZED, "Não foi possível validar o login com o Google");
		}
		return new Identidade(jwt.getSubject(), email, Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
				jwt.getClaimAsString("name"));
	}
}
