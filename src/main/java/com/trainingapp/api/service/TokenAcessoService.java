package com.trainingapp.api.service;

import com.trainingapp.api.dto.AcessoResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Access token curto (JWT HS256) com o id do usuario em "sub"; a sessao longa fica no refresh token. */
@Service
public class TokenAcessoService {

	static final Duration VALIDADE = Duration.ofMinutes(15);

	private final JwtEncoder encoder;
	private final Clock relogio;

	public TokenAcessoService(JwtEncoder encoder, Clock relogio) {
		this.encoder = encoder;
		this.relogio = relogio;
	}

	public AcessoResponse gerar(UUID usuarioId) {
		Instant agora = Instant.now(relogio);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("trainingapp")
				.subject(usuarioId.toString())
				.issuedAt(agora)
				.expiresAt(agora.plus(VALIDADE))
				.build();
		String jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
		return new AcessoResponse(jwt, VALIDADE.toSeconds());
	}
}
