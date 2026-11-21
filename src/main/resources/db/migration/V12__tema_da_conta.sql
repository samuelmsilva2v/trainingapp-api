-- Tema escolhido pela conta, para acompanhar a pessoa em outro aparelho. Nulo = nunca escolheu (o app usa o do aparelho).
alter table usuario add column tema varchar(30);
