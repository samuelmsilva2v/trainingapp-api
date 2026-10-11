package com.trainingapp.api.controller;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Roda os testes de sessao num Postgres real: o H2 aceita coisas que o
 * Postgres rejeita (ex.: parametro nulo em JPQL). O schema vem das migracoes do Flyway e o Hibernate so confere
 * (validate), entao tambem prova que o V1 bate com as entidades. Ignorado quando o Docker esta desligado.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = { "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate" })
class SessaoPostgresTest extends SessaoApiTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
}
