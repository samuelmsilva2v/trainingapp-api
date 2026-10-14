-- Indices nas colunas de usuario e nas chaves estrangeiras (o Postgres nao indexa FK sozinho).
-- xp_event(usuario_id) ja e coberto pelo unique (usuario_id, dia_unico).

create index idx_sessao_usuario on sessao (usuario_id);
create index idx_plano_usuario on plano (usuario_id);
create index idx_dia_treino_plano on dia_treino (plano_id);
create index idx_exercicio_plano_dia on exercicio_plano (dia_id);
create index idx_exercicio_plano_exercicio on exercicio_plano (exercicio_id);
create index idx_exercicio_sessao_sessao on exercicio_sessao (sessao_id);
create index idx_exercicio_sessao_exercicio on exercicio_sessao (exercicio_id);
create index idx_serie_sessao_exercicio_sessao on serie_sessao (exercicio_sessao_id);
create index idx_exercicio_usuario on exercicio (usuario_id);
create index idx_refresh_token_usuario on refresh_token (usuario_id);
create index idx_token_email_usuario on token_email (usuario_id);
create index idx_xp_event_sessao on xp_event (sessao_id);
create index idx_email_pendente_fila on email_pendente (enviado_em, proxima_tentativa_em);
