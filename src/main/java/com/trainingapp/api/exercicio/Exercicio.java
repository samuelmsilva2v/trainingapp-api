package com.trainingapp.api.exercicio;

import com.trainingapp.api.usuario.Usuario;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exercicio")
@Getter
@Setter
@NoArgsConstructor
public class Exercicio {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// Identificador estavel dos exercicios do catalogo; nulo nos exercicios do usuario.
	@Column(unique = true)
	private String slug;

	@Column(nullable = false, length = 150)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GrupoMuscular grupoMuscular;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Equipamento equipamento;

	// Nulo = exercicio do catalogo global; preenchido = exercicio criado pelo usuario.
	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario usuario;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;
}
