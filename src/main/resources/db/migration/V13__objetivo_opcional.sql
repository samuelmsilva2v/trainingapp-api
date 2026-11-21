-- O objetivo do plano passa a ser opcional (auditoria de UX, achado 09): o editor deixa de marcar Hipertrofia por padrão,
-- e um plano sem objetivo não mostra etiqueta. O check de valores continua valendo para quem escolhe um.
alter table plano alter column objetivo drop not null;
