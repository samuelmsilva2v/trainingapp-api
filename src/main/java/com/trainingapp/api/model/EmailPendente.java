package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de e-mails (outbox): gravada na mesma transacao da operacao que a originou e enviada depois por
 * {@code EmailWorker}, com nova tentativa se o SMTP falhar. O corpo (que carrega o link com o token) e apagado apos o envio.
 */
@Entity
@Table(name = "email_pendente")
@Getter
@Setter
@NoArgsConstructor
public class EmailPendente {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String destinatario;

	@Column(nullable = false, length = 200)
	private String assunto;

	@Column(nullable = false, length = 4000)
	private String corpo;

	@Column(nullable = false)
	private int tentativas;

	@Column(nullable = false)
	private Instant proximaTentativaEm;

	// Nulo enquanto nao foi enviado.
	private Instant enviadoEm;

	@Column(length = 500)
	private String erro;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;
}
