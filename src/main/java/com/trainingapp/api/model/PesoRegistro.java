package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Uma pesagem por usuario por dia: registrar de novo no mesmo dia substitui o peso. */
@Entity
@Table(name = "peso_registro", uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "data" }))
@Getter
@Setter
@NoArgsConstructor
public class PesoRegistro {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Usuario usuario;

	@Column(nullable = false)
	private LocalDate data;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal kg;
}
