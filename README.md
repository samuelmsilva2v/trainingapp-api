# trainingapp-api

Backend do **TrainingApp**, um app de treino para registrar séries, repetições e cargas, com elementos de gamificação (pontos, conquistas, streaks).

## Stack

- Java 21
- Spring Boot 4.1.1
  - Spring Web (MVC)
  - Spring Data JPA
  - Bean Validation
  - Spring Security (resource server JWT) e Spring Mail
- PostgreSQL com Flyway (migrações em `src/main/resources/db/migration`); H2 nos testes
- Maven

## Rodando localmente

Pré-requisitos: JDK 21 e Docker (para o banco via `compose.yaml`).

```bash
./mvnw spring-boot:run
```

O `spring-boot-docker-compose` sobe automaticamente os serviços do `compose.yaml` ao iniciar a aplicação: o PostgreSQL e o **Mailpit**, uma caixa de entrada falsa em http://localhost:8025 onde caem os e-mails de dev (confirmar cadastro, redefinir senha).

O schema é criado pelo Flyway e o Hibernate só confere (`ddl-auto=validate`). Um banco criado pelo Hibernate numa versão anterior não serve: recrie-o (`docker compose down` nesta pasta apaga o container e os dados de dev).

Quem pode entrar e o Google ficam num arquivo local, fora do git, `dev.local.properties` (nesta pasta):

```properties
trainingapp.auth.emails-permitidos=voce@gmail.com
trainingapp.auth.google-client-id=1234-abc.apps.googleusercontent.com
```

Sem esse arquivo ninguém consegue se cadastrar (lista vazia = ninguém entra) e o botão do Google não aparece.

## Login

Cadastro com e-mail e senha (com confirmação por e-mail), login com Google, "esqueci a senha", troca de senha e sair (deste aparelho ou de todos). Só e-mails da lista `trainingapp.auth.emails-permitidos` entram; para abrir o cadastro a qualquer pessoa, remova a checagem em `AuthService.exigirPermitido`.

- **Sessão:** `POST /api/auth/login` (ou `/google`) devolve um access token (JWT, 15 min) no corpo e grava o refresh token num cookie HttpOnly (`SameSite=Strict`, path `/api/auth`). O refresh é opaco, fica no banco só como hash, não rotaciona e vence após 1 ano **sem uso** (cada renovação recomeça a contagem), então no celular o login não cai sozinho. `POST /api/auth/refresh` troca por um access novo. Sair apaga a linha; o access em uso vale até vencer (15 min).
- **Google:** o front obtém um ID token pelo Google Identity Services e manda a `POST /api/auth/google`; a API valida assinatura, emissor, validade e se o `aud` é o nosso client id. O mesmo e-mail com senha e com Google é uma conta só; se a conta de senha nunca confirmou o e-mail, a senha é descartada ao vincular o Google.
- **E-mails:** gravados na tabela `email_pendente` na mesma transação da operação e enviados por um worker agendado, com nova tentativa se o SMTP falhar (1, 2, 4 e 8 min; 5 tentativas). Só liga com `SPRING_MAIL_HOST` definido.
- **Limite de tentativas:** 5 por 15 min (login por e-mail e IP; cadastro e "esqueci a senha" por IP), em memória, para uma instância só.

### Criar o client id do Google

1. No [Google Cloud Console](https://console.cloud.google.com/) crie um projeto e, em *APIs e serviços > Tela de permissão OAuth*, configure o app (tipo Externo; escopos básicos `openid`, `email` e `profile`, que dispensam verificação do Google).
2. Em *Credenciais > Criar credenciais > ID do cliente OAuth*, escolha **Aplicativo da Web** e, em *Origens JavaScript autorizadas*, adicione cada endereço do app: `http://localhost:4200`, `http://localhost:8080` (perfil `online`) e o endereço do túnel ou do deploy. Não precisa de URI de redirecionamento.
3. Copie o ID do cliente para `trainingapp.auth.google-client-id`. Com a tela de permissão em modo *Teste*, só as contas cadastradas como usuários de teste conseguem entrar; publique o app para liberar as demais.

### Variáveis de ambiente (hm e prod)

Sem o perfil `dev`, valem os padrões de produção de `application.properties`: o Flyway cria o schema, o Hibernate valida, o cookie é `Secure` e o proxy reverso é respeitado (`X-Forwarded-*`). Ative o ambiente com `SPRING_PROFILES_ACTIVE=hm` ou `prod` (hoje só dão nome ao ambiente).

| Variável | Para quê |
| --- | --- |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Banco Postgres |
| `TRAININGAPP_AUTH_JWT_SEGREDO` | Chave do access token (32+ caracteres, secreta); a API não sobe sem ela |
| `TRAININGAPP_AUTH_EMAILS_PERMITIDOS` | E-mails que podem entrar, separados por vírgula |
| `TRAININGAPP_AUTH_GOOGLE_CLIENT_ID` | Client id do Google (vazio = sem botão do Google) |
| `TRAININGAPP_WEB_URL_BASE` | Endereço do app, usado nos links dos e-mails (ex.: `https://app.exemplo.com`) |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | SMTP (Gmail com senha de app, Brevo, Resend...) |
| `TRAININGAPP_EMAIL_REMETENTE` | Remetente, ex.: `TrainingApp <voce@gmail.com>` (obrigatório com SMTP) |

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

- Só entra quem está em `trainingapp.auth.emails-permitidos`. Defina `TRAININGAPP_WEB_URL_BASE` com o endereço do túnel (os links dos e-mails apontam para ele) e cadastre esse endereço nas origens do client id do Google.
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
