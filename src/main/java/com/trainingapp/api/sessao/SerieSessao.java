package com.trainingapp.api.sessao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

/** Uma serie registrada: carga e repeticoes reais. */
@Entity
@Table(name = "serie_sessao")
@Getter
@Setter
@NoArgsConstructor
public class SerieSessao {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private ExercicioSessao exercicioSessao;

	@Column(nullable = false)
	private int ordem;

	private Integer reps;

	@Column(precision = 6, scale = 2)
	private BigDecimal carga;

	@Column(nullable = false)
	private boolean concluida;
}
