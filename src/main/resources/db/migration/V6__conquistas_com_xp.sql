-- Conquistas com XP: desbloquear uma conquista grava um evento CONQUISTA com o codigo dela, uma vez por usuario.
-- O Postgres aceita varios nulos na chave unica, entao os eventos sem codigo (SERIES, RECORDE, STREAK) nao sao afetados.

alter table xp_event drop constraint xp_event_tipo_check;
alter table xp_event add constraint xp_event_tipo_check check (tipo in ('SERIES', 'RECORDE', 'STREAK', 'CONQUISTA'));
alter table xp_event add column codigo varchar(60);
alter table xp_event add constraint xp_event_usuario_id_codigo_key unique (usuario_id, codigo);
