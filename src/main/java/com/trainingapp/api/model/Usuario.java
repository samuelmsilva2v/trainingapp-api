package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(nullable = false, length = 64)
	private String fusoHorario;

	// Nulo enquanto nao ha login; vira obrigatorio na fase de autenticacao.
	@Column(unique = true)
	private String email;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;
}
