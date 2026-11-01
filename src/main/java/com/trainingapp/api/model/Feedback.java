package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Relato de bug ou sugestao. O contexto (plataforma, versao, tela, user agent) vem do app, sem a pessoa digitar. */
@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Usuario usuario;

	@Column(nullable = false, length = 100)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CategoriaFeedback categoria;

	@Column(nullable = false, length = 2000)
	private String mensagem;

	@Column(length = 10)
	private String plataforma;

	@Column(length = 50)
	private String versaoApp;

	@Column(length = 200)
	private String tela;

	@Column(length = 500)
	private String userAgent;

	// Pelo Clock (e nao @CreationTimestamp): o limite diario conta pelo dia desse mesmo relogio.
	@Column(nullable = false)
	private Instant criadoEm;
}
