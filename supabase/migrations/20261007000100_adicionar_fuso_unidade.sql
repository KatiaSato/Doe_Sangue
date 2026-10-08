begin;

-- Adiciona o fuso e preenche as unidades de demonstração existentes.
alter table public.unidade_coleta
add column fuso_horario text not null
default 'America/Sao_Paulo';

-- Novas unidades deverão informar seu fuso explicitamente.
alter table public.unidade_coleta
alter column fuso_horario drop default;

commit;