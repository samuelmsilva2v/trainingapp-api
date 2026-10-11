package com.trainingapp.api.service;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.trainingapp.api.config.ErroDeAutenticacaoException;
import com.trainingapp.api.service.VerificadorGoogle.Identidade;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Date;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O token do Google e assinado com a chave privada deles; aqui geramos uma chave RSA local e um token no mesmo formato
 * (RS256, emissor, destinatario, validade e as claims email/email_verified/name) para provar o que a API aceita e rejeita.
 */
class VerificadorGoogleJwtTest {

	static final String CLIENT_ID = "meu-cliente.apps.googleusercontent.com";

	static RSAKey chave;
	static RSAKey outraChave;

	@BeforeAll
	static void gerarChaves() throws Exception {
		chave = new RSAKeyGenerator(2048).keyID("k1").generate();
		outraChave = new RSAKeyGenerator(2048).keyID("k1").generate();
	}

	VerificadorGoogleJwt verificador(String clientId) throws Exception {
		return new VerificadorGoogleJwt(clientId, NimbusJwtDecoder.withPublicKey(chave.toRSAPublicKey()).build());
	}

	JWTClaimsSet.Builder claimsValidas() {
		Instant agora = Instant.now();
		return new JWTClaimsSet.Builder()
				.issuer("https://accounts.google.com")
				.audience(CLIENT_ID)
				.subject("1234567890")
				.claim("email", "maria@teste.com")
				.claim("email_verified", true)
				.claim("name", "Maria Silva")
				.issueTime(Date.from(agora))
				.expirationTime(Date.from(agora.plusSeconds(3600)));
	}

	String token(RSAKey assinatura, UnaryOperator<JWTClaimsSet.Builder> ajuste) throws Exception {
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").type(JOSEObjectType.JWT).build(),
				ajuste.apply(claimsValidas()).build());
		jwt.sign(new RSASSASigner(assinatura));
		return jwt.serialize();
	}

	void exigirRecusa(String credencial, HttpStatus status) throws Exception {
		assertThatThrownBy(() -> verificador(CLIENT_ID).verificar(credencial))
				.isInstanceOfSatisfying(ErroDeAutenticacaoException.class, e -> assertThat(e.status()).isEqualTo(status));
	}

	@Test
	void aceitaTokenValidoEDevolveAIdentidade() throws Exception {
		Identidade g = verificador(CLIENT_ID).verificar(token(chave, c -> c));

		assertThat(g.sub()).isEqualTo("1234567890");
		assertThat(g.email()).isEqualTo("maria@teste.com");
		assertThat(g.emailVerificado()).isTrue();
		assertThat(g.nome()).isEqualTo("Maria Silva");
	}

	@Test
	void aceitaOEmissorSemHttps() throws Exception {
		assertThat(verificador(CLIENT_ID).verificar(token(chave, c -> c.issuer("accounts.google.com"))).sub()).isEqualTo("1234567890");
	}

	@Test
	void emailNaoVerificadoChegaMarcadoParaAApiRecusar() throws Exception {
		assertThat(verificador(CLIENT_ID).verificar(token(chave, c -> c.claim("email_verified", false))).emailVerificado()).isFalse();
		assertThat(verificador(CLIENT_ID).verificar(token(chave, c -> c.claim("email_verified", null))).emailVerificado()).isFalse();
	}

	@Test
	void recusaTokenFeitoParaOutroAplicativo() throws Exception {
		exigirRecusa(token(chave, c -> c.audience("cliente-de-outro-app.apps.googleusercontent.com")), HttpStatus.UNAUTHORIZED);
	}

	@Test
	void recusaEmissorQueNaoEOGoogle() throws Exception {
		exigirRecusa(token(chave, c -> c.issuer("https://accounts.evil.com")), HttpStatus.UNAUTHORIZED);
	}

	@Test
	void recusaTokenVencido() throws Exception {
		exigirRecusa(token(chave, c -> c.issueTime(Date.from(Instant.now().minusSeconds(7200)))
				.expirationTime(Date.from(Instant.now().minusSeconds(3600)))), HttpStatus.UNAUTHORIZED);
	}

	@Test
	void recusaTokenAssinadoComOutraChave() throws Exception {
		exigirRecusa(token(outraChave, c -> c), HttpStatus.UNAUTHORIZED);
	}

	@Test
	void recusaTokenSemEmailOuSemSub() throws Exception {
		exigirRecusa(token(chave, c -> c.claim("email", null)), HttpStatus.UNAUTHORIZED);
		exigirRecusa(token(chave, c -> c.subject(null)), HttpStatus.UNAUTHORIZED);
	}

	@Test
	void recusaLixoQueNaoEUmJwt() throws Exception {
		exigirRecusa("isto-nao-e-um-jwt", HttpStatus.UNAUTHORIZED);
	}

	@Test
	void semClientIdConfiguradoOLoginComGoogleFicaIndisponivel() throws Exception {
		assertThatThrownBy(() -> verificador("").verificar(token(chave, c -> c)))
				.isInstanceOfSatisfying(ErroDeAutenticacaoException.class,
						e -> assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
		assertThatThrownBy(() -> verificador(null).verificar(token(chave, c -> c)))
				.isInstanceOf(ErroDeAutenticacaoException.class);
	}
}
