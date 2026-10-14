-- Drop-set: cada etapa e uma linha de serie com tipo DROP, logo depois da serie normal a que pertence.
-- O plano guarda quantos drops entram na ultima serie, e a sessao o snapshot dessa meta.

alter table serie_sessao add column tipo varchar(20) not null default 'NORMAL' check (tipo in ('NORMAL', 'DROP'));
alter table exercicio_plano add column drops integer not null default 0;
alter table exercicio_sessao add column meta_drops integer not null default 0;
