package com.trainingapp.api.service;

import com.trainingapp.api.config.AuthProperties;
import com.trainingapp.api.config.ErroDeAutenticacaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Component
public class VerificadorGoogleJwt implements VerificadorGoogle {

	private static final Logger log = LoggerFactory.getLogger(VerificadorGoogleJwt.class);

	private static final String CHAVES = "https://www.googleapis.com/oauth2/v3/certs";
	private static final String TOKENS = "https://oauth2.googleapis.com/token";
	// Se a troca falhar com redirect_uri_mismatch, trocar pela origem real do app (o log traz o motivo).
	private static final String REDIRECT_DO_POPUP = "postmessage";
	private static final Set<String> EMISSORES = Set.of("https://accounts.google.com", "accounts.google.com");

	private final String clientId;
	private final String clientSecret;
	private final NimbusJwtDecoder decoder;
	private final RestClient rest;

	@Autowired
	public VerificadorGoogleJwt(AuthProperties props) {
		this(props.googleClientId(), props.googleClientSecret(), NimbusJwtDecoder.withJwkSetUri(CHAVES).build(), restComTimeout());
	}

	VerificadorGoogleJwt(String clientId, String clientSecret, NimbusJwtDecoder decoder, RestClient rest) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.decoder = decoder;
		this.rest = rest;
		OAuth2TokenValidator<Jwt> emissor = jwt -> EMISSORES.contains(jwt.getClaimAsString("iss"))
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emissor invalido", null));
		OAuth2TokenValidator<Jwt> destinatario = jwt -> clientId != null && jwt.getAudience().contains(clientId)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Destinatario invalido", null));
		this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), emissor, destinatario));
	}

	private static RestClient restComTimeout() {
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
		fabrica.setReadTimeout(Duration.ofSeconds(10));
		return RestClient.builder().requestFactory(fabrica).build();
	}

	@Override
	public Identidade verificarCodigo(String codigo) {
		if (clientSecret == null || clientSecret.isBlank()) {
			throw naoConfigurado();
		}
		return verificar(trocarCodigo(codigo));
	}

	private String trocarCodigo(String codigo) {
		if (clientId == null || clientId.isBlank()) {
			throw naoConfigurado();
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", codigo);
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("redirect_uri", REDIRECT_DO_POPUP);
		form.add("grant_type", "authorization_code");
		try {
			Map<String, Object> resposta = rest.post().uri(TOKENS).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
					.retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {
					});
			if (resposta != null && resposta.get("id_token") instanceof String idToken && !idToken.isBlank()) {
				return idToken;
			}
			throw recusado();
		}
		catch (RestClientResponseException e) {
			log.warn("O Google recusou a troca do codigo: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw recusado();
		}
		catch (RestClientException e) {
			throw new ErroDeAutenticacaoException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível falar com o Google. Tente de novo.");
		}
	}

	@Override
	public Identidade verificarIdToken(String idToken) {
		return verificar(idToken);
	}

	Identidade verificar(String idToken) {
		if (clientId == null || clientId.isBlank()) {
			throw naoConfigurado();
		}
		Jwt jwt;
		try {
			jwt = decoder.decode(idToken);
		}
		catch (JwtException e) {
			throw recusado();
		}
		String email = jwt.getClaimAsString("email");
		if (jwt.getSubject() == null || email == null) {
			throw recusado();
		}
		return new Identidade(jwt.getSubject(), email, Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
				jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
	}

	private static ErroDeAutenticacaoException recusado() {
		return new ErroDeAutenticacaoException(HttpStatus.UNAUTHORIZED, "Não foi possível validar o login com o Google");
	}

	private static ErroDeAutenticacaoException naoConfigurado() {
		return new ErroDeAutenticacaoException(HttpStatus.SERVICE_UNAVAILABLE, "Login com Google não está configurado");
	}
}
