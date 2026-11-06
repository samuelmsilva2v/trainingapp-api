package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A foto enviada pelo usuario (tipo PROPRIA), uma por conta. O front ja manda 256x256 em JPEG; o teto e 5 MB. */
@Entity
@Table(name = "foto_perfil")
@Getter
@Setter
@NoArgsConstructor
public class FotoPerfil {

	@Id
	private UUID usuarioId;

	// bytea como na V10 (o H2 aceita o nome); sem isso, o Hibernate escolheria BLOB pelo tamanho, que o H2 em modo Postgres recusa.
	@Column(nullable = false, columnDefinition = "bytea")
	private byte[] bytes;

	@Column(nullable = false, length = 20)
	private String contentType;

	@Column(nullable = false)
	private Instant atualizadaEm;
}
