-- Catalogo (api#42): exercicios fora de series x reps x kg (cardio, alongamento, pliometria, strongman) ficam
-- indisponiveis, so somem do seletor; e os de peso corporal ganham o fator de XP (XP da serie = reps x fator).
-- O importador preenche as duas colunas pelo slug a cada subida.

alter table exercicio add column disponivel boolean not null default true;
alter table exercicio add column fator_xp numeric(3,1);
