package com.trainingapp.api.service;

import com.trainingapp.api.UsuarioDeTeste;
import com.trainingapp.api.model.EmailPendente;
import com.trainingapp.api.model.RefreshToken;
import com.trainingapp.api.model.TipoTokenEmail;
import com.trainingapp.api.model.TokenEmail;
import com.trainingapp.api.model.Usuario;
import com.trainingapp.api.repository.EmailPendenteRepository;
import com.trainingapp.api.repository.RefreshTokenRepository;
import com.trainingapp.api.repository.TokenEmailRepository;
import com.trainingapp.api.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LimpezaDiariaTest {

	@Autowired
	LimpezaDiaria limpeza;

	@Autowired
	EmailPendenteRepository emails;

	@Autowired
	TokenEmailRepository tokensEmail;

	@Autowired
	RefreshTokenRepository refreshTokens;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	EntityManager em;

	@Autowired
	Clock relogio;

	@Test
	void apagaSoOQueVenceu() {
		Instant agora = Instant.now(relogio);
		Usuario u = usuarios.findById(UUID.fromString(UsuarioDeTeste.ID)).orElseThrow();

		UUID emailVelho = email(agora.minus(Duration.ofDays(8)));
		UUID emailNovo = email(agora.minus(Duration.ofDays(6)));
		UUID tokenVencido = tokenEmail(u, agora.minusSeconds(60));
		UUID tokenValido = tokenEmail(u, agora.plus(Duration.ofHours(1)));
		UUID loginAbandonado = refresh(u, agora.minus(AuthService.REFRESH_VALIDADE).minusSeconds(60));
		UUID loginEmUso = refresh(u, agora.minus(Duration.ofDays(30)));
		em.flush();
		em.clear();

		limpeza.limpar();
		em.flush();
		em.clear();

		assertThat(emails.existsById(emailVelho)).isFalse();
		assertThat(emails.existsById(emailNovo)).isTrue();
		assertThat(tokensEmail.existsById(tokenVencido)).isFalse();
		assertThat(tokensEmail.existsById(tokenValido)).isTrue();
		assertThat(refreshTokens.existsById(loginAbandonado)).isFalse();
		assertThat(refreshTokens.existsById(loginEmUso)).isTrue();
	}

	// criadoEm e preenchido pelo Hibernate e nao muda pelo save: a data antiga entra por um update direto.
	private UUID email(Instant criadoEm) {
		EmailPendente e = new EmailPendente();
		e.setDestinatario("maria@teste.com");
		e.setAssunto("Assunto");
		e.setCorpo("link");
		e.setProximaTentativaEm(criadoEm);
		UUID id = emails.saveAndFlush(e).getId();
		em.createQuery("update EmailPendente e set e.criadoEm = :criadoEm where e.id = :id")
				.setParameter("criadoEm", criadoEm).setParameter("id", id).executeUpdate();
		return id;
	}

	private UUID tokenEmail(Usuario u, Instant expiraEm) {
		TokenEmail t = new TokenEmail();
		t.setUsuario(u);
		t.setTipo(TipoTokenEmail.CONFIRMACAO);
		t.setTokenHash(UUID.randomUUID().toString());
		t.setExpiraEm(expiraEm);
		return tokensEmail.save(t).getId();
	}

	private UUID refresh(Usuario u, Instant ultimoUso) {
		RefreshToken r = new RefreshToken();
		r.setUsuario(u);
		r.setTokenHash(UUID.randomUUID().toString());
		r.setUltimoUsoEm(ultimoUso);
		return refreshTokens.save(r).getId();
	}
}
