package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "plano_compartilhado")
@Getter
@Setter
@NoArgsConstructor
public class PlanoCompartilhado {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 32)
	private String token;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Usuario usuario;

	/** O plano em JSON ({@code PlanoCompartilhadoDados}). */
	@Column(nullable = false, length = 200000)
	private String dados;

	@Column(nullable = false)
	private Instant criadoEm;
}
