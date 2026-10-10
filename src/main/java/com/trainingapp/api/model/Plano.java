package com.trainingapp.api.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plano")
@Getter
@Setter
@NoArgsConstructor
public class Plano {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Usuario usuario;

	@Column(nullable = false, length = 100)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Objetivo objetivo;

	@Column(nullable = false)
	private boolean ativo;

	// Indice do proximo dia da rotacao; avanca ao concluir ou pular um treino. Lido com modulo, pois o plano pode encolher.
	@Column(nullable = false, columnDefinition = "integer default 0 not null")
	private int proximoDia;

	// A ordem da lista e a sequencia de rotacao (A, B, C...).
	@OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<DiaTreino> dias = new ArrayList<>();

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;

	/** Dia que o usuario treina a seguir (0 quando o plano nao tem dias). */
	public int indiceProximoDia() {
		return dias.isEmpty() ? 0 : proximoDia % dias.size();
	}

	/** Avanca a rotacao para o dia seguinte ao informado. */
	public void avancarApos(int diaOrdem) {
		proximoDia = dias.isEmpty() ? 0 : (diaOrdem + 1) % dias.size();
	}
}
