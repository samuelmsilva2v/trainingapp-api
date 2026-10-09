# trainingapp-api

Backend do **TrainingApp**, um app de treino para registrar séries, repetições e cargas, com elementos de gamificação (pontos, conquistas, streaks).

## Stack

- Java 21
- Spring Boot 4.1.1
  - Spring Web (MVC)
  - Spring Data JPA
  - Spring Security
  - Bean Validation
  - Flyway (migrações de banco)
- PostgreSQL
- Maven

## Rodando localmente

Pré-requisitos: JDK 21 e Docker (para o banco via `compose.yaml`).

```bash
./mvnw spring-boot:run
```

O `spring-boot-docker-compose` sobe automaticamente os serviços definidos em `compose.yaml` (ex.: PostgreSQL) ao iniciar a aplicação.

## Build

```bash
./mvnw clean verify
```

## Projeto irmão

Front-end em Angular: [trainingapp-web](https://github.com/samuelmsilva2v/trainingapp-web).
