package com.trainingapp.api.service;

import com.trainingapp.api.model.EmailPendente;
import com.trainingapp.api.repository.EmailPendenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Esvazia a fila de e-mails: envia por SMTP e, se falhar, tenta de novo com espera crescente (1, 2, 4 e 8 min) ate
 * {@value #MAX_TENTATIVAS} vezes. So liga com {@code spring.mail.host} definido. O SMTP nunca roda dentro de uma
 * transacao aberta: cada {@code save} abre a sua.
 * shortcut: assume uma unica instancia da API; com varias, dois workers poderiam enviar o mesmo e-mail (usar SKIP LOCKED).
 */
@Component
@ConditionalOnProperty("spring.mail.host")
public class EmailWorker {

	static final int MAX_TENTATIVAS = 5;

	private static final Logger log = LoggerFactory.getLogger(EmailWorker.class);

	private final EmailPendenteRepository repository;
	private final JavaMailSender mailSender;
	private final Clock relogio;
	private final String remetente;

	public EmailWorker(EmailPendenteRepository repository, JavaMailSender mailSender, Clock relogio,
			@Value("${trainingapp.email.remetente}") String remetente) {
		this.repository = repository;
		this.mailSender = mailSender;
		this.relogio = relogio;
		this.remetente = remetente;
	}

	@Scheduled(fixedDelayString = "${trainingapp.email.intervalo-ms:5000}")
	public void enviarPendentes() {
		List<EmailPendente> pendentes = repository
				.findTop20ByEnviadoEmIsNullAndTentativasLessThanAndProximaTentativaEmLessThanEqualOrderByCriadoEmAsc(
						MAX_TENTATIVAS, Instant.now(relogio));
		for (EmailPendente email : pendentes) {
			try {
				SimpleMailMessage mensagem = new SimpleMailMessage();
				mensagem.setFrom(remetente);
				mensagem.setTo(email.getDestinatario());
				mensagem.setSubject(email.getAssunto());
				mensagem.setText(email.getCorpo());
				mailSender.send(mensagem);
				email.setEnviadoEm(Instant.now(relogio));
				email.setCorpo(""); // o corpo carrega o link com o token: nao fica no banco depois de enviado
				email.setErro(null);
			}
			catch (RuntimeException e) {
				email.setTentativas(email.getTentativas() + 1);
				email.setProximaTentativaEm(Instant.now(relogio).plus(Duration.ofMinutes(1L << (email.getTentativas() - 1))));
				String erro = String.valueOf(e.getMessage());
				email.setErro(erro.substring(0, Math.min(erro.length(), 500)));
				log.warn("Falha ao enviar e-mail {} (tentativa {}): {}", email.getId(), email.getTentativas(), e.toString());
			}
			repository.save(email);
		}
	}
}
