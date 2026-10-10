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

## Usar no celular pela internet (túnel)

Com o perfil `online` a própria API serve o front compilado: o app inteiro fica em `http://localhost:8080` (`/` = app, `/api` = API, sem CORS) e um túnel expõe essa única porta com HTTPS. A API continua ouvindo só em `127.0.0.1`. O PC precisa ficar ligado, com o Docker rodando.

```bash
# 1. compilar o front (a cada mudança no front)
cd ../trainingapp-web && npm run build

# 2. subir a API com o perfil online (PowerShell: mantenha as aspas)
cd ../trainingapp-api && ./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev,online"
```

Túnel (escolha um):

- **Tailscale Funnel**, endereço fixo `https://<pc>.<rede>.ts.net`: `winget install Tailscale.Tailscale`, entre na sua conta, depois `tailscale funnel --bg 8080`. Na primeira vez o comando mostra um link para ativar o HTTPS e o Funnel na sua rede. `tailscale funnel status` mostra o endereço; `tailscale funnel reset` fecha.
- **Cloudflare Tunnel**, sem conta, mas o endereço muda a cada execução: `winget install Cloudflare.cloudflared`, depois `cloudflared tunnel --url http://localhost:8080` (mostra um `https://….trycloudflare.com`).

Cuidados:

- Não há login: **quem tiver o endereço lê e apaga os seus treinos**. Não divulgue e feche o túnel quando não estiver usando.
- Depois de um `npm run build` basta recarregar a página no celular (o `index.html` não fica em cache). Mudanças na API reiniciam sozinhas (devtools).
- O fuso usado para contar os dias de treino é o do PC onde a API roda.

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
