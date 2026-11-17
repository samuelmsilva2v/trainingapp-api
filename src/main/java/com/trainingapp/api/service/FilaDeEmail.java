package com.trainingapp.api.service;

import com.trainingapp.api.model.EmailPendente;
import com.trainingapp.api.repository.EmailPendenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class FilaDeEmail {

	private final EmailPendenteRepository repository;
	private final Clock relogio;

	public FilaDeEmail(EmailPendenteRepository repository, Clock relogio) {
		this.repository = repository;
		this.relogio = relogio;
	}

	@Transactional
	public void enfileirar(String destinatario, String assunto, String corpo) {
		enfileirar(destinatario, assunto, corpo, null);
	}

	@Transactional
	public void enfileirar(String destinatario, String assunto, String corpo, String responderPara) {
		EmailPendente email = new EmailPendente();
		email.setDestinatario(destinatario);
		email.setResponderPara(responderPara);
		email.setAssunto(assunto);
		email.setCorpo(corpo);
		email.setProximaTentativaEm(Instant.now(relogio));
		repository.save(email);
	}
}
