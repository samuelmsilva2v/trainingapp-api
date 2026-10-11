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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * O token do Google e assinado com a chave privada deles; aqui geramos uma chave RSA local e um token no mesmo formato
 * (RS256, emissor, destinatario, validade e as claims email/email_verified/name) para provar o que a API aceita e rejeita.
 * A troca do codigo do popup por tokens usa um "Google" simulado (endpoint de tokens).
 */
class VerificadorGoogleJwtTest {

	static final String CLIENT_ID = "meu-cliente.apps.googleusercontent.com";
	static final String SEGREDO = "segredo-do-cliente";
	static final String ENDPOINT_DE_TOKENS = "https://oauth2.googleapis.com/token";

	static RSAKey chave;
	static RSAKey outraChave;

	@BeforeAll
	static void gerarChaves() throws Exception {
		chave = new RSAKeyGenerator(2048).keyID("k1").generate();
		outraChave = new RSAKeyGenerator(2048).keyID("k1").generate();
	}

	/** Um verificador que valida com a chave local; a troca de codigo nao e usada nestes testes. */
	VerificadorGoogleJwt verificador(String clientId) throws Exception {
		return new VerificadorGoogleJwt(clientId, SEGREDO, NimbusJwtDecoder.withPublicKey(chave.toRSAPublicKey()).build(),
				RestClient.create());
	}

	/** Um verificador cujo "Google" (endpoint de tokens) e simulado; o servidor simulado vem em {@code servidor[0]}. */
	VerificadorGoogleJwt verificadorComGoogleSimulado(String segredo, MockRestServiceServer[] servidor) throws Exception {
		RestClient.Builder construtor = RestClient.builder();
		servidor[0] = MockRestServiceServer.bindTo(construtor).build();
		return new VerificadorGoogleJwt(CLIENT_ID, segredo, NimbusJwtDecoder.withPublicKey(chave.toRSAPublicKey()).build(),
				construtor.build());
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

	void exigirRecusa(String idToken, HttpStatus status) throws Exception {
		assertThatThrownBy(() -> verificador(CLIENT_ID).verificar(idToken))
				.isInstanceOfSatisfying(ErroDeAutenticacaoException.class, e -> assertThat(e.status()).isEqualTo(status));
	}

	String respostaDeTokensCom(String idToken) {
		return "{\"access_token\":\"x\",\"id_token\":\"" + idToken + "\"}";
	}

	// --- validacao do ID token ---

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

	// --- troca do codigo do popup por tokens ---

	@Test
	void trocaOCodigoPeloIdTokenComOSegredoDoClienteEValidaOToken() throws Exception {
		MockRestServiceServer[] google = new MockRestServiceServer[1];
		VerificadorGoogleJwt v = verificadorComGoogleSimulado(SEGREDO, google);
		google[0].expect(requestTo(ENDPOINT_DE_TOKENS)).andExpect(method(HttpMethod.POST))
				.andExpect(content().formDataContains(Map.of("code", "codigo-do-popup", "client_id", CLIENT_ID,
						"client_secret", SEGREDO, "redirect_uri", "postmessage", "grant_type", "authorization_code")))
				.andRespond(withSuccess(respostaDeTokensCom(token(chave, c -> c)), MediaType.APPLICATION_JSON));

		Identidade g = v.verificarCodigo("codigo-do-popup");

		assertThat(g.email()).isEqualTo("maria@teste.com");
		assertThat(g.sub()).isEqualTo("1234567890");
		google[0].verify();
	}

	@Test
	void recusaCodigoQueOGoogleRejeita() throws Exception {
		MockRestServiceServer[] google = new MockRestServiceServer[1];
		VerificadorGoogleJwt v = verificadorComGoogleSimulado(SEGREDO, google);
		google[0].expect(requestTo(ENDPOINT_DE_TOKENS))
				.andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"invalid_grant\"}"));

		assertThatThrownBy(() -> v.verificarCodigo("codigo-ja-usado")).isInstanceOfSatisfying(ErroDeAutenticacaoException.class,
				e -> assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void recusaRespostaSemIdToken() throws Exception {
		MockRestServiceServer[] google = new MockRestServiceServer[1];
		VerificadorGoogleJwt v = verificadorComGoogleSimulado(SEGREDO, google);
		google[0].expect(requestTo(ENDPOINT_DE_TOKENS)).andRespond(withSuccess("{\"access_token\":\"x\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> v.verificarCodigo("c")).isInstanceOfSatisfying(ErroDeAutenticacaoException.class,
				e -> assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void recusaIdTokenDeOutroAppMesmoVindoDoEndpointDoGoogle() throws Exception {
		MockRestServiceServer[] google = new MockRestServiceServer[1];
		VerificadorGoogleJwt v = verificadorComGoogleSimulado(SEGREDO, google);
		google[0].expect(requestTo(ENDPOINT_DE_TOKENS))
				.andRespond(withSuccess(respostaDeTokensCom(token(chave, c -> c.audience("outro-app"))), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> v.verificarCodigo("c")).isInstanceOfSatisfying(ErroDeAutenticacaoException.class,
				e -> assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void googleForaDoArDa503() throws Exception {
		MockRestServiceServer[] google = new MockRestServiceServer[1];
		VerificadorGoogleJwt v = verificadorComGoogleSimulado(SEGREDO, google);
		google[0].expect(requestTo(ENDPOINT_DE_TOKENS)).andRespond(withException(new IOException("sem rede")));

		assertThatThrownBy(() -> v.verificarCodigo("c")).isInstanceOfSatisfying(ErroDeAutenticacaoException.class,
				e -> assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
	}

	@Test
	void semSegredoDoClienteNemTentaFalarComOGoogle() throws Exception {
		for (String segredo : new String[] { "", null }) {
			MockRestServiceServer[] google = new MockRestServiceServer[1];
			VerificadorGoogleJwt v = verificadorComGoogleSimulado(segredo, google);

			assertThatThrownBy(() -> v.verificarCodigo("c")).isInstanceOfSatisfying(ErroDeAutenticacaoException.class,
					e -> assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
			google[0].verify(); // nenhuma chamada esperada, e o servidor simulado falharia se houvesse uma
		}
	}
}
