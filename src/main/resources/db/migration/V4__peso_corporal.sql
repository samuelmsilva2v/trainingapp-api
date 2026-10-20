-- Peso corporal: uma pesagem por usuario por dia (registrar de novo no mesmo dia substitui).
-- No perfil, a frequencia do lembrete de pesagem; nula = desligado.

create table peso_registro (
    id uuid primary key,
    usuario_id uuid not null references usuario (id),
    data date not null,
    kg numeric(5,2) not null,
    unique (usuario_id, data)
);

alter table usuario add column lembrete_peso_dias integer;
