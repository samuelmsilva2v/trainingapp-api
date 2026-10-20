package com.trainingapp.api.controller;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Os testes de peso num Postgres real, com o schema das migracoes: prova que a V4 bate com as entidades. */
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = { "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate" })
class PesoPostgresTest extends PesoApiTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
}
