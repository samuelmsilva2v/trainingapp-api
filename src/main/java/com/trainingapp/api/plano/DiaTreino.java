package com.trainingapp.api.plano;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dia_treino")
@Getter
@Setter
@NoArgsConstructor
public class DiaTreino {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Plano plano;

	@Column(nullable = false, length = 100)
	private String nome;

	@Column(nullable = false)
	private int ordem;

	@OneToMany(mappedBy = "dia", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<ExercicioPlano> exercicios = new ArrayList<>();
}
