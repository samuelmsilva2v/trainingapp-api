package com.trainingapp.api.service;

import com.trainingapp.api.repository.EmailPendenteRepository;
import com.trainingapp.api.repository.RefreshTokenRepository;
import com.trainingapp.api.repository.TokenEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Apaga, uma vez por dia, o que nao serve mais: e-mails da fila com mais de {@link #RETENCAO_EMAIL} (enviados ou que
 * esgotaram as tentativas, ainda com o link no corpo), links de e-mail vencidos e logins sem uso alem da validade.
 */
@Component
public class LimpezaDiaria {

	static final Duration RETENCAO_EMAIL = Duration.ofDays(7);

	private static final Logger log = LoggerFactory.getLogger(LimpezaDiaria.class);

	private final EmailPendenteRepository emails;
	private final TokenEmailRepository tokensEmail;
	private final RefreshTokenRepository refreshTokens;
	private final Clock relogio;

	public LimpezaDiaria(EmailPendenteRepository emails, TokenEmailRepository tokensEmail,
			RefreshTokenRepository refreshTokens, Clock relogio) {
		this.emails = emails;
		this.tokensEmail = tokensEmail;
		this.refreshTokens = refreshTokens;
		this.relogio = relogio;
	}

	@Scheduled(cron = "${trainingapp.limpeza.cron:0 0 4 * * *}")
	@Transactional
	public void limpar() {
		Instant agora = Instant.now(relogio);
		long e = emails.deleteByCriadoEmBefore(agora.minus(RETENCAO_EMAIL));
		long t = tokensEmail.deleteByExpiraEmBefore(agora);
		long r = refreshTokens.deleteByUltimoUsoEmBefore(agora.minus(AuthService.REFRESH_VALIDADE));
		log.info("Limpeza: {} e-mails, {} links de e-mail e {} logins apagados", e, t, r);
	}
}
