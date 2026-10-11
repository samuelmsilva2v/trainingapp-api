-- Schema inicial: o que o Hibernate gerava em dev (fases 1 a 4) mais o login (usuario com senha/Google,
-- refresh_token, token_email) e a fila de e-mails (email_pendente). Banco novo; os dados de teste anteriores foram descartados.

create table usuario (
    id uuid primary key,
    nome varchar(100) not null,
    email varchar(255) not null unique,
    senha_hash varchar(255),
    google_sub varchar(255) unique,
    email_verificado boolean not null,
    criado_em timestamp(6) with time zone not null
);

create table refresh_token (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    token_hash varchar(64) not null unique,
    ultimo_uso_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null
);

create table token_email (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    tipo varchar(20) not null check (tipo in ('CONFIRMACAO', 'REDEFINICAO')),
    token_hash varchar(64) not null unique,
    expira_em timestamp(6) with time zone not null
);

create table email_pendente (
    id uuid primary key,
    destinatario varchar(255) not null,
    assunto varchar(200) not null,
    corpo varchar(4000) not null,
    tentativas integer not null,
    proxima_tentativa_em timestamp(6) with time zone not null,
    enviado_em timestamp(6) with time zone,
    erro varchar(500),
    criado_em timestamp(6) with time zone not null
);

create table exercicio (
    id uuid primary key,
    usuario_id uuid references usuario (id),
    slug varchar(255) unique,
    nome varchar(150) not null,
    grupo_muscular varchar(30) not null check (grupo_muscular in ('PEITO', 'COSTAS', 'LOMBAR', 'TRAPEZIO', 'OMBROS', 'BICEPS',
        'TRICEPS', 'ANTEBRACO', 'ABDOMEN', 'QUADRICEPS', 'POSTERIORES', 'GLUTEOS', 'ADUTORES', 'ABDUTORES', 'PANTURRILHA',
        'PESCOCO', 'CORPO_INTEIRO', 'CARDIO')),
    equipamento varchar(30) not null check (equipamento in ('BARRA', 'HALTERES', 'MAQUINA', 'CABO', 'PESO_CORPORAL',
        'KETTLEBELL', 'ELASTICO', 'OUTRO')),
    criado_em timestamp(6) with time zone not null
);

create table plano (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    nome varchar(100) not null,
    objetivo varchar(30) not null check (objetivo in ('HIPERTROFIA', 'FORCA', 'RESISTENCIA', 'EMAGRECIMENTO', 'SAUDE_GERAL')),
    ativo boolean not null,
    proximo_dia integer not null default 0,
    criado_em timestamp(6) with time zone not null
);

create table dia_treino (
    id uuid primary key,
    plano_id uuid not null references plano (id),
    nome varchar(100) not null,
    ordem integer not null
);

create table exercicio_plano (
    id uuid primary key,
    dia_id uuid not null references dia_treino (id),
    exercicio_id uuid not null references exercicio (id),
    ordem integer not null,
    series integer not null,
    reps_min integer not null,
    reps_max integer not null,
    carga_alvo numeric(6, 2),
    descanso_segundos integer
);

create table sessao (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    plano_id uuid,
    plano_nome varchar(100) not null,
    dia_nome varchar(100) not null,
    dia_ordem integer not null,
    estado varchar(20) not null check (estado in ('EM_ANDAMENTO', 'CONCLUIDA', 'ABANDONADA')),
    andamento_usuario_id uuid unique,
    iniciada_em timestamp(6) with time zone not null,
    finalizada_em timestamp(6) with time zone,
    atualizado_em timestamp(6) with time zone not null
);

create table exercicio_sessao (
    id uuid primary key,
    sessao_id uuid not null references sessao (id),
    exercicio_id uuid not null references exercicio (id),
    nome varchar(150) not null,
    ordem integer not null,
    meta_series integer not null,
    meta_reps_min integer not null,
    meta_reps_max integer not null,
    meta_carga numeric(6, 2),
    meta_descanso_segundos integer
);

create table serie_sessao (
    id uuid primary key,
    exercicio_sessao_id uuid not null references exercicio_sessao (id),
    ordem integer not null,
    reps integer,
    carga numeric(6, 2),
    concluida boolean not null
);

create table xp_event (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    sessao_id uuid not null,
    tipo varchar(20) not null check (tipo in ('SERIES', 'RECORDE')),
    pontos integer not null,
    exercicio_id uuid,
    dia date not null,
    dia_unico date,
    criado_em timestamp(6) with time zone not null,
    unique (usuario_id, dia_unico)
);
