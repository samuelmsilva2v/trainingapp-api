-- Foto do perfil (api#48): quem entrou pelo Google comeca com a foto da conta Google; quem se cadastrou com e-mail,
-- sem foto (o app mostra as iniciais). A foto propria fica numa tabela a parte, para nao pesar nas leituras do usuario.

alter table usuario add column foto_tipo varchar(10) not null default 'PADRAO'
    check (foto_tipo in ('PADRAO', 'GOOGLE', 'PROPRIA'));
alter table usuario add column foto_google_url varchar(2048);
-- Contas ja ligadas ao Google: a URL chega no proximo login com Google; ate la o app mostra as iniciais.
update usuario set foto_tipo = 'GOOGLE' where google_sub is not null;

create table foto_perfil (
    usuario_id uuid primary key references usuario (id),
    bytes bytea not null,
    content_type varchar(20) not null,
    atualizada_em timestamp(6) with time zone not null
);
