-- Relatos de bug e sugestoes enviados pelo app: cada um vira um e-mail para TRAININGAPP_FEEDBACK_EMAIL, com o e-mail
-- do usuario no Reply-To (responder_para). O limite diario de relatos e contado nesta tabela.

create table feedback (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    nome varchar(100) not null,
    categoria varchar(20) not null check (categoria in ('BUG', 'MELHORIA', 'FUNCIONALIDADE', 'OUTRO')),
    mensagem varchar(2000) not null,
    plataforma varchar(10),
    versao_app varchar(50),
    tela varchar(200),
    user_agent varchar(500),
    criado_em timestamp(6) with time zone not null
);

create index feedback_usuario_criado_em on feedback (usuario_id, criado_em);

alter table email_pendente add column responder_para varchar(255);
