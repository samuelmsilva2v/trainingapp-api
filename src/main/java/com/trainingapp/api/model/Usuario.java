package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String nome;

	@Column(nullable = false, unique = true)
	private String email;

	private String senhaHash;

	@Column(unique = true)
	private String googleSub;

	@Column(nullable = false)
	private boolean emailVerificado;

	private Integer lembretePesoDias;

	/** Nulo = a conta nunca escolheu; o app fica com o tema do aparelho. */
	@Column(length = 30)
	private String tema;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private FotoTipo fotoTipo = FotoTipo.PADRAO;

	@Column(length = 2048)
	private String fotoGoogleUrl;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;
}
