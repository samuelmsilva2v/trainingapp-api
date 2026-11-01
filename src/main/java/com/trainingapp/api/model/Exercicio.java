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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
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

	// Falso = some do seletor (categoria do catalogo fora de series x reps x kg); planos e historico seguem usando.
	@Column(nullable = false)
	private boolean disponivel = true;

	// XP por rep nos exercicios de peso corporal do catalogo; nulo nos demais (exercicio proprio usa 1,0).
	@Column(precision = 3, scale = 1)
	private BigDecimal fatorXp;

	// Nulo = exercicio do catalogo global; preenchido = exercicio criado pelo usuario.
	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario usuario;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;

	/** Peso corporal (api#43): so reps contam; a carga e ignorada no XP, no volume e no recorde. */
	public boolean isPesoCorporal() {
		return equipamento == Equipamento.PESO_CORPORAL;
	}

	/** XP por rep de um exercicio de peso corporal: o fator do catalogo, ou 1,0 (exercicio proprio). */
	public double fatorXpOuPadrao() {
		return fatorXp == null ? 1.0 : fatorXp.doubleValue();
	}
}
