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

@Entity
@Table(name = "foto_perfil")
@Getter
@Setter
@NoArgsConstructor
public class FotoPerfil {

	@Id
	private UUID usuarioId;

	@Column(nullable = false, columnDefinition = "bytea")
	private byte[] bytes;

	@Column(nullable = false, length = 20)
	private String contentType;

	@Column(nullable = false)
	private Instant atualizadaEm;
}
