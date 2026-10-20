package com.trainingapp.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String nome;

	// Sempre em minusculas: e a chave unica de login e da lista de e-mails permitidos.
	@Column(nullable = false, unique = true)
	private String email;

	// Nulo em conta criada so pelo Google; "esqueci a senha" define uma.
	private String senhaHash;

	// Id estavel da conta Google (claim "sub"); nulo se nunca entrou pelo Google.
	@Column(unique = true)
	private String googleSub;

	// Verdadeiro depois de clicar no link do e-mail, de redefinir a senha ou de entrar pelo Google.
	@Column(nullable = false)
	private boolean emailVerificado;

	// A cada quantos dias lembrar de se pesar (7, 15 ou 30); nulo = desligado.
	private Integer lembretePesoDias;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant criadoEm;
}
