# trainingapp-api

Backend do **TrainingApp**, um app de treino para registrar séries, repetições e cargas, com elementos de gamificação (pontos, conquistas, streaks).

## Stack

- Java 21
- Spring Boot 4.1.1
  - Spring Web (MVC)
  - Spring Data JPA
  - Bean Validation
- PostgreSQL (H2 nos testes)
- Maven

Planejado para a fase de autenticação e deploy: Spring Security (JWT) e Flyway (migrações). Hoje não há login e o schema é gerenciado pelo Hibernate.

## Rodando localmente

Pré-requisitos: JDK 21 e Docker (para o banco via `compose.yaml`).

```bash
./mvnw spring-boot:run
```

O `spring-boot-docker-compose` sobe automaticamente os serviços definidos em `compose.yaml` (ex.: PostgreSQL) ao iniciar a aplicação.

## Catálogo de exercícios

O catálogo (876 exercícios) vem do [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense), com nomes traduzidos para português. O arquivo `src/main/resources/catalogo/exercicios.json` é gerado e importado na inicialização de forma idempotente, usando o `slug` (id do free-exercise-db) como chave.

Para regenerar (requer Node 20+), depois de editar as traduções em `tools/catalogo/traducoes-*.txt` (`Nome em inglês | Nome em português`):

```bash
node tools/catalogo/gerar-catalogo.mjs
```

O script falha, sem gravar nada, se faltar tradução, sobrar tradução, houver nome repetido ou um músculo/equipamento sem mapeamento.

## Build

```bash
./mvnw clean verify
```

## Projeto irmão

Front-end em Angular: [trainingapp-web](https://github.com/samuelmsilva2v/trainingapp-web).
