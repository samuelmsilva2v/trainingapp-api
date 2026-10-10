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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Registro append-only de XP: nunca e alterado nem apagado. XP total, nivel e streak sao
 * calculados a partir dele (o evento SESSAO existe uma vez por dia de treino valido).
 */
@Entity
@Table(name = "xp_event", uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "dia_unico" }))
@Getter
@Setter
@NoArgsConstructor
public class XpEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Usuario usuario;

	// Sem chave estrangeira, como o planoId da sessao: o evento sobrevive a qualquer limpeza.
	@Column(nullable = false)
	private UUID sessaoId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoXp tipo;

	@Column(nullable = false)
	private int pontos;

	// So nos eventos RECORDE.
	private UUID exercicioId;

	// Dia do treino no fuso do perfil na hora da conclusao.
	@Column(nullable = false)
	private LocalDate dia;

	// Preenchido so no evento SESSAO: a chave unica garante um treino valido por usuario por dia.
	@Column(name = "dia_unico")
	private LocalDate diaUnico;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;
}
