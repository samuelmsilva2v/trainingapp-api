-- Bonus de XP por sequencia (streak): um evento STREAK por treino valido a partir do 2o dia da sequencia.
-- A V1 criou o check de xp_event.tipo sem nome; o Postgres o chama xp_event_tipo_check.

alter table xp_event drop constraint xp_event_tipo_check;
alter table xp_event add constraint xp_event_tipo_check check (tipo in ('SERIES', 'RECORDE', 'STREAK'));
