-- Plano compartilhado por link: uma copia do plano no momento em que o link foi gerado (editar ou excluir o original
-- nao muda o link). O plano vai serializado em JSON (sem carga alvo, que e pessoal); exercicio do catalogo vai pelo id,
-- o proprio vai com nome, grupo e equipamento, e vira um exercicio proprio de quem salvar.

create table plano_compartilhado (
    id uuid primary key,
    token varchar(32) not null unique,
    usuario_id uuid not null references usuario (id),
    dados varchar(200000) not null,
    criado_em timestamp(6) with time zone not null
);

create index plano_compartilhado_usuario on plano_compartilhado (usuario_id);
