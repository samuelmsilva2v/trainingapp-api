package com.trainingapp.api.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Um treino executado. O id vem do cliente (upsert idempotente) e os nomes e metas sao um
 * snapshot: editar ou excluir o plano nao altera o historico.
 */
@Entity
@Table(name = "sessao", uniqueConstraints = @UniqueConstraint(columnNames = "andamento_usuario_id"))
@Getter
@Setter
@NoArgsConstructor
public class Sessao {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Usuario usuario;

	// Sem chave estrangeira: o plano pode ser excluido e a sessao continua valida.
	private UUID planoId;

	@Column(nullable = false, length = 100)
	private String planoNome;

	@Column(nullable = false, length = 100)
	private String diaNome;

	@Column(nullable = false)
	private int diaOrdem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoSessao estado;

	// Id do usuario so enquanto EM_ANDAMENTO; a chave unica garante uma sessao em andamento por usuario.
	@Column(name = "andamento_usuario_id")
	private UUID andamentoUsuarioId;

	@Column(nullable = false)
	private Instant iniciadaEm;

	private Instant finalizadaEm;

	// Relogio do cliente: o ultimo a escrever vence.
	@Column(nullable = false)
	private Instant atualizadoEm;

	@OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<ExercicioSessao> exercicios = new ArrayList<>();
}
