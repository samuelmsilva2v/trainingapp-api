package com.trainingapp.api.service;

import com.trainingapp.api.model.EmailPendente;
import com.trainingapp.api.repository.EmailPendenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmailWorkerTest {

	@Autowired
	FilaDeEmail fila;

	@Autowired
	EmailPendenteRepository repository;

	@Autowired
	Clock relogio;

	JavaMailSender smtp = mock(JavaMailSender.class);
	EmailWorker worker;

	@BeforeEach
	void criarWorker() {
		worker = new EmailWorker(repository, smtp, relogio, "TrainingApp <nao-responda@teste.com>");
	}

	@Test
	void enviaEApagaOCorpoComOLink() {
		fila.enfileirar("maria@teste.com", "Assunto", "link secreto");

		worker.enviarPendentes();

		verify(smtp).send(any(SimpleMailMessage.class));
		EmailPendente email = repository.findAll().get(0);
		assertThat(email.getEnviadoEm()).isNotNull();
		assertThat(email.getCorpo()).isEmpty();

		worker.enviarPendentes();
		verify(smtp).send(any(SimpleMailMessage.class)); // nao reenvia
	}

	@Test
	void falhaDeSmtpAgendaNovaTentativaComEsperaCrescenteEDesisteNaQuinta() {
		doThrow(new MailSendException("SMTP fora do ar")).when(smtp).send(any(SimpleMailMessage.class));
		fila.enfileirar("maria@teste.com", "Assunto", "link");

		worker.enviarPendentes();

		EmailPendente email = repository.findAll().get(0);
		assertThat(email.getTentativas()).isEqualTo(1);
		assertThat(email.getEnviadoEm()).isNull();
		assertThat(email.getErro()).contains("SMTP fora do ar");
		assertThat(email.getProximaTentativaEm()).isAfter(Instant.now(relogio).plus(Duration.ofSeconds(30)));

		// Ainda dentro da espera: o worker nem tenta.
		worker.enviarPendentes();
		assertThat(email.getTentativas()).isEqualTo(1);

		// Depois de cinco falhas desiste, mas o e-mail fica guardado com o erro.
		for (int i = 1; i < EmailWorker.MAX_TENTATIVAS; i++) {
			email.setProximaTentativaEm(Instant.now(relogio).minusSeconds(1));
			worker.enviarPendentes();
		}
		assertThat(email.getTentativas()).isEqualTo(EmailWorker.MAX_TENTATIVAS);
		email.setProximaTentativaEm(Instant.now(relogio).minusSeconds(1));
		worker.enviarPendentes();
		assertThat(email.getTentativas()).isEqualTo(EmailWorker.MAX_TENTATIVAS);
		assertThat(email.getCorpo()).isEqualTo("link");
	}

	@Test
	void semNadaPendenteNaoEnvia() {
		worker.enviarPendentes();
		verify(smtp, never()).send(any(SimpleMailMessage.class));
	}
}
