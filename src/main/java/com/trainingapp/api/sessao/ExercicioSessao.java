package com.trainingapp.api.sessao;

import com.trainingapp.api.exercicio.Exercicio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Exercicio dentro da sessao, com o snapshot da meta do plano e as series realmente feitas. */
@Entity
@Table(name = "exercicio_sessao")
@Getter
@Setter
@NoArgsConstructor
public class ExercicioSessao {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Sessao sessao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Exercicio exercicio;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(nullable = false)
	private int ordem;

	@Column(nullable = false)
	private int metaSeries;

	@Column(nullable = false)
	private int metaRepsMin;

	@Column(nullable = false)
	private int metaRepsMax;

	@Column(precision = 6, scale = 2)
	private BigDecimal metaCarga;

	private Integer metaDescansoSegundos;

	@OneToMany(mappedBy = "exercicioSessao", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<SerieSessao> series = new ArrayList<>();
}
