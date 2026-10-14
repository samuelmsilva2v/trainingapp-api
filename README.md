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

## API

Tudo em `/api/**` exige `Authorization: Bearer <access token>`, menos `/api/auth/**` (veja "Login"). Erros: `400` validação, `404` não encontrado, `422` regra de negócio, `429` limite de tentativas.

| Rota | O que faz |
| --- | --- |
| `GET/PUT /api/perfil` | Nome do usuário (a resposta traz também `email` e `temSenha`) |
| `GET /api/exercicios?grupo=&busca=`, `POST /api/exercicios` | Catálogo (a busca ignora acento e caixa) e exercício próprio (até 200 por conta; acima disso `422`) |
| `GET/POST /api/planos`, `GET/PUT/DELETE /api/planos/{id}`, `POST /api/planos/{id}/ativar` | Planos: dias de treino (rotação A/B/C) com exercícios, séries, reps e carga. Criar e atualizar enviam o plano inteiro; um ativo por vez; até 50 planos por conta (`422`) |
| `GET /api/hoje`, `POST /api/hoje/pular` | Plano ativo, próximo dia da rotação e treino em andamento; pular o dia |
| `PUT /api/sessoes/{id}` | Salva o treino (upsert idempotente, com os ids gerados no cliente). Treino encerrado não muda mais; só um em andamento; sem registro além de 24 h atrás; concluir avança a rotação e credita o XP |
| `GET /api/sessoes?pagina=&tamanho=`, `GET /api/sessoes/{id}`, `DELETE /api/sessoes/{id}` | Histórico, detalhe e apagar um treino encerrado (estorna o XP e os recordes; a rotação não volta) |
| `GET /api/sessoes/anteriores?exercicioIds=` | Séries do último treino concluído de cada exercício (até 30 ids) |
| `GET /api/gamificacao` | XP total, nível, streak, recordes e conquistas |

**Gamificação** (regras em `service/Regras`, calculadas só no servidor): cada série concluída vale `carga × reps ÷ 40` de XP (mínimo 5, máximo 100; sem carga vale o mínimo), mais 25 por recorde de carga no exercício. Só o primeiro treino do dia conta e precisa de 3 ou mais séries. O nível `n` começa em `100 × n^1,5` de XP. O streak aguenta até 2 dias sem treinar.

**Drop-set:** cada etapa é uma série com `tipo: "DROP"` logo depois da série normal a que pertence (até 3; o plano guarda `drops` da última série e a sessão o snapshot `metaDrops`). O bloco (série + drops) é **uma série só**: tem um check, o da série normal (o servidor grava nos drops o mesmo `concluida`, e drop sem repetição não conta), vale 1 nas 3 séries mínimas e no histórico, e rende XP uma vez, pelo volume somado das etapas (mínimo 5, máximo 100). O recorde olha a carga de cada etapa. Drop sem série normal antes ou mais de 3 seguidos: `422`.

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
trainingapp.auth.google-client-secret=GOCSPX-...
```

Sem esse arquivo ninguém consegue se cadastrar (lista vazia = ninguém entra) e o botão do Google não aparece.

## Login

Cadastro com e-mail e senha (com confirmação por e-mail), login com Google, "esqueci a senha", troca de senha e sair (deste aparelho ou de todos). Só e-mails da lista `trainingapp.auth.emails-permitidos` entram; para abrir o cadastro a qualquer pessoa, defina `trainingapp.auth.cadastro-aberto=true` (`TRAININGAPP_AUTH_CADASTRO_ABERTO=true`); para fechar de novo, volte a `false`.

- **Sessão:** `POST /api/auth/login` (ou `/google`) devolve um access token (JWT, 15 min) no corpo e grava o refresh token num cookie HttpOnly (`SameSite=Strict`, path `/api/auth`). O refresh é opaco, fica no banco só como hash, não rotaciona e vence após 1 ano **sem uso** (cada renovação recomeça a contagem), então no celular o login não cai sozinho. `POST /api/auth/refresh` troca por um access novo. Sair apaga a linha; o access em uso vale até vencer (15 min).
- **Google:** fluxo de código em popup. O botão do app (próprio, no estilo do Stitch) abre o popup do Google Identity Services, que entrega ao front um código de uso único; o front manda o código a `POST /api/auth/google` e a API o troca por tokens no endpoint do Google (com o client id e o **client secret**, `redirect_uri=postmessage`) e valida o ID token recebido: assinatura, emissor, validade e se o `aud` é o nosso client id. Sem o client secret o login com Google fica desligado e o botão nem aparece. Se a troca falhar, o motivo devolvido pelo Google (`invalid_grant`, `redirect_uri_mismatch`, `invalid_client`...) vai para o log da API. O mesmo e-mail com senha e com Google é uma conta só; se a conta de senha nunca confirmou o e-mail, a senha é descartada ao vincular o Google.
- **E-mails:** gravados na tabela `email_pendente` na mesma transação da operação e enviados por um worker agendado, com nova tentativa se o SMTP falhar (1, 2, 4 e 8 min; 5 tentativas). Só liga com `SPRING_MAIL_HOST` definido.
- **Senha forte:** ao definir uma senha (cadastro, redefinir e trocar) a API exige 8 ou mais caracteres, com letra maiúscula, letra minúscula, número e símbolo (qualquer caractere que não seja letra nem número), no máximo 72 bytes (limite do bcrypt). Falhou: `422` com a mensagem da regra (`service/RegrasDeSenha`, espelhada no front em `core/senha.ts`). O login não aplica a regra, então contas criadas antes dela continuam entrando até trocarem a senha.
- **Excluir conta:** `POST /api/auth/excluir-conta` (autenticado) apaga a conta e tudo dela de uma vez, sem volta: planos, treinos e séries, histórico de XP (exceção à regra "XP nunca é apagado"), exercícios próprios, logins, links de e-mail e e-mails na fila. O catálogo global e os dados de outros usuários não são tocados. Exige provar quem pede, para um access token roubado não bastar: o corpo leva `{"senha": ...}` (conta com senha) ou `{"codigo": ...}` (conta só do Google: o código de um novo popup do Google, que tem de ser da mesma conta Google ligada à conta). Senha errada vale como tentativa do limite abaixo. O front mostra o botão "Excluir conta e apagar dados" no Perfil, acima do "Sair".
- **Limite de tentativas:** em memória, para uma instância só. A tentativa é contada **antes** de conferir a senha e zerada se ela estiver certa (assim uma rajada em paralelo não passa toda). Estourou: `429` por 15 min.
  - 5 por 15 min: login (por e-mail e IP), cadastro e "esqueci a senha" (por IP), trocar senha e excluir conta (por usuário).
  - 30 por 15 min por IP, somando todos os e-mails, no login e no login com Google.
- **Tamanho:** e-mail com até 254 caracteres, tokens e código do Google com teto, senha com até 72 bytes. Em produção o Caddy também recusa corpo de requisição acima de 1 MB (a maior real, um treino com todas as séries, tem ~200 KB).

### Criar o client id do Google

1. No [Google Cloud Console](https://console.cloud.google.com/) crie um projeto e abra *Google Auth Platform* (antes chamado "Tela de permissão OAuth"). Em *Identidade visual* (Branding) preencha o nome do app e o e-mail de suporte; em *Público* (Audience) escolha **Externo**; em *Acesso a dados* (Data Access) fique com os escopos padrão `openid`, `email` e `profile`, que dispensam verificação do Google.
2. Em *Clientes* (Clients) > *Criar cliente*, escolha **Aplicativo da Web** e, em *Origens JavaScript autorizadas*, adicione cada endereço do app, só com esquema e host, sem caminho: `http://localhost` **e** `http://localhost:4200` (o Google pede as duas para testes locais), `http://localhost:8080` (perfil `online`) e o endereço do túnel ou do deploy. Não precisa de URI de redirecionamento.
3. Copie o ID do cliente (`...apps.googleusercontent.com`, é um valor público) para `trainingapp.auth.google-client-id` e o **segredo do cliente** (aparece na página do cliente, em *Segredos do cliente*; é secreto, nunca vai para o git nem para o chat) para `trainingapp.auth.google-client-secret`. Reinicie a API. Enquanto o app estiver em modo *Teste* (em *Público*), só as contas listadas como usuários de teste conseguem entrar; adicione a sua ou publique o app.
4. Se o botão mostrar erro de origem, o endereço aberto no navegador não está na lista do passo 2. As mudanças no Google podem levar alguns minutos para valer.

### Variáveis de ambiente (hm e prod)

Sem o perfil `dev`, valem os padrões de produção de `application.properties`: o Flyway cria o schema, o Hibernate valida, o cookie é `Secure` e o proxy reverso é respeitado (`X-Forwarded-*`). Ative o ambiente com `SPRING_PROFILES_ACTIVE=hm` ou `prod` (hoje só dão nome ao ambiente).

| Variável | Para quê |
| --- | --- |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Banco Postgres |
| `TRAININGAPP_AUTH_JWT_SEGREDO` | Chave do access token (32+ caracteres, secreta); a API não sobe sem ela |
| `TRAININGAPP_AUTH_EMAILS_PERMITIDOS` | E-mails que podem entrar, separados por vírgula |
| `TRAININGAPP_AUTH_CADASTRO_ABERTO` | `true` deixa qualquer e-mail se cadastrar e entrar (a lista é ignorada); padrão `false` |
| `TRAININGAPP_AUTH_GOOGLE_CLIENT_ID`, `TRAININGAPP_AUTH_GOOGLE_CLIENT_SECRET` | Client id e segredo do Google (qualquer um vazio = sem botão do Google) |
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

## Deploy (VM com Docker)

Uma VM Linux só, com três contêineres (`compose.prod.yaml`): **Caddy** (HTTPS automático com Let's Encrypt), a **API** (que serve também o front, mesma origem) e o **Postgres** (volume `dados`). Só o Caddy expõe portas (80 e 443).

1. **VM:** qualquer Linux com Docker (`curl -fsSL https://get.docker.com | sh`) e 2 GB de RAM ou mais (o build do Angular + Maven pede memória). Abra as portas 80 e 443 no firewall do provedor (na Oracle Cloud também na *Security List* da VCN e no `iptables` da imagem).
2. **Domínio:** crie um subdomínio gratuito em [duckdns.org](https://www.duckdns.org) (ex.: `treino.duckdns.org`) apontando para o IP público da VM, ou use um domínio seu com um registro A.
3. **Código:** clone os dois repositórios lado a lado (o build da imagem usa os dois):
   ```bash
   git clone https://github.com/samuelmsilva2v/trainingapp-api.git
   git clone https://github.com/samuelmsilva2v/trainingapp-web.git
   ```
4. **Configuração:** `cd trainingapp-api && cp .env.example .env` e preencha o `.env` (domínio, senha do Postgres, chave JWT, e-mails permitidos, Google e SMTP; veja as variáveis acima). Cadastre `https://<domínio>` nas origens JavaScript do client id do Google.
5. **Subir:** `docker compose -f compose.prod.yaml up -d --build`. O Flyway cria o schema na primeira subida e o catálogo é importado em seguida. `docker compose -f compose.prod.yaml logs -f app` mostra o log.
6. **Atualizar:** a esteira abaixo faz isso a cada push na `master`. À mão, numa VM que compila: `git pull` nos dois repositórios e repetir o comando do passo 5.

**VM pequena (ex.: Oracle AMD Micro, 1 GB):** não compile na VM. Crie 2 GB de swap e copie para ela só `compose.prod.yaml`, `Caddyfile` e `.env`; a imagem vem pronta do GHCR (veja a esteira). No `.env` dessa VM, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=50 -XX:+UseSerialGC` reduz o uso de memória da JVM.

### Esteira (GitHub Actions)

`.github/workflows/deploy.yml`, a cada push na `master` (menos mudança só em `.md`):

1. **testes:** os testes da API (inclusive os de Postgres, o Docker do runner está ligado) e os do front, com os dois repositórios lado a lado.
2. **publicar:** monta a imagem (`linux/amd64`) e a envia para `ghcr.io/samuelmsilva2v/trainingapp-app`, com as tags `latest` e o SHA do commit. O pacote tem de estar **público** (Package settings → Change visibility), senão a VM não consegue baixar.
3. **implantar:** entra na VM por SSH, que roda `~/trainingapp/deploy.sh` (`docker compose pull app` e `up -d --no-build app`), e confere `GET /api/auth/config` por até 3 min.

O repositório do front tem o próprio `ci.yml` (testes e build). Um push no front **não** implanta: rode `gh workflow run deploy.yml -R samuelmsilva2v/trainingapp-api`.

**Secrets do repositório da API:** `VM_SSH_KEY` (chave privada só do deploy), `VM_HOST`, `VM_USER` e `VM_KNOWN_HOSTS` (a linha do host em `known_hosts`, para o runner não aceitar qualquer servidor). Na VM, a chave pública entra em `~/.ssh/authorized_keys` presa ao script, então um vazamento do secret não dá shell:

```
restrict,command="/home/ubuntu/trainingapp/deploy.sh" ssh-ed25519 AAAA... github-actions-deploy
```

**Voltar a uma versão:** na VM, edite a tag do `image:` do `app` em `compose.prod.yaml` para o SHA desejado e rode `docker compose -f compose.prod.yaml up -d --no-build app`. Enquanto o `image:` estiver fixo num SHA, os deploys seguintes não mudam nada: volte para `:latest` depois da correção. Mudanças em `compose.prod.yaml` ou no `Caddyfile` não vão sozinhas: copie para a VM à mão.

**Trocar de VM:** só os secrets `VM_HOST`, `VM_KNOWN_HOSTS` (e a chave, se gerar outra) e o `URL` do teste de fumaça no `deploy.yml`. Se a nova for ARM, troque `platforms` no job `publicar`.

O fuso do contêiner é `America/Sao_Paulo` (define o "dia" do treino e do streak). Backup do banco (rode também por cron, ex.: diário):

```bash
docker compose -f compose.prod.yaml exec -T postgres pg_dump -U trainingapp trainingapp | gzip > ~/backup-$(date +%F).sql.gz
```

O backup fica no mesmo disco da VM: não protege contra perder a VM. Copie os arquivos para outro lugar.

## Catálogo de exercícios

O catálogo (876 exercícios) vem do [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense), com nomes traduzidos para português. O arquivo `src/main/resources/catalogo/exercicios.json` é gerado e importado na inicialização de forma idempotente, usando o `slug` (id do free-exercise-db) como chave.

Para regenerar (requer Node 20+), depois de editar as traduções em `tools/catalogo/traducoes-*.txt` (`Nome em inglês | Nome em português`):

```bash
node tools/catalogo/gerar-catalogo.mjs
```

O script falha, sem gravar nada, se faltar tradução, sobrar tradução, houver nome repetido ou um músculo/equipamento sem mapeamento.

**Fotos:** o free-exercise-db tem 2 fotos por exercício (posição inicial e final), em `<slug>/0.jpg` e `<slug>/1.jpg`. Não há coluna no banco: as URLs são montadas a partir do `slug` e de `trainingapp.catalogo.imagens-url` (jsDelivr fixado num commit do dataset) e vêm na lista `imagens` dos exercícios, dos itens do plano e da sessão. Exercício próprio devolve `imagens: []`, e 3 exercícios do dataset não têm foto (a URL dá 404 e o front esconde a imagem). Para hospedar as fotos por conta própria, troque só essa URL base.

## Build e testes

```bash
./mvnw clean verify
```

Os testes rodam no H2 (`MODE=PostgreSQL`). Os `*PostgresTest` repetem os de sessão e gamificação num Postgres 17 via Testcontainers, com Flyway e `ddl-auto=validate` (provam que a migração bate com as entidades); sem Docker ligado eles são ignorados.

## Projeto irmão

Front-end em Angular: [trainingapp-web](https://github.com/samuelmsilva2v/trainingapp-web).
