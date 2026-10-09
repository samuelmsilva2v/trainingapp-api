package com.trainingapp.api.plano;

import com.trainingapp.api.exercicio.Exercicio;
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

import java.math.BigDecimal;
import java.util.UUID;

/** Um exercicio dentro de um dia, com a meta uniforme para todas as series. */
@Entity
@Table(name = "exercicio_plano")
@Getter
@Setter
@NoArgsConstructor
public class ExercicioPlano {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private DiaTreino dia;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Exercicio exercicio;

	@Column(nullable = false)
	private int ordem;

	@Column(nullable = false)
	private int series;

	@Column(nullable = false)
	private int repsMin;

	@Column(nullable = false)
	private int repsMax;

	@Column(precision = 6, scale = 2)
	private BigDecimal cargaAlvo;

	private Integer descansoSegundos;
}
