-- Desafios semanais: um evento DESAFIO por desafio cumprido na semana, com codigo <desafio>:<ano>-W<semana>.
-- A chave unica (usuario_id, codigo) da V6 garante um credito por semana.

alter table xp_event drop constraint xp_event_tipo_check;
alter table xp_event add constraint xp_event_tipo_check check (tipo in ('SERIES', 'RECORDE', 'STREAK', 'CONQUISTA', 'DESAFIO'));
