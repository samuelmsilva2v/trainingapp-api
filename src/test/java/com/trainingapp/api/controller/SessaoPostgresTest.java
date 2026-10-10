package com.trainingapp.api.controller;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Roda os testes de sessao num Postgres real: o H2 aceita coisas que o
 * Postgres rejeita (ex.: parametro nulo em JPQL). Ignorado quando o Docker esta desligado.
 */
@Testcontainers(disabledWithoutDocker = true)
class SessaoPostgresTest extends SessaoApiTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
}
