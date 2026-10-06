-- Cria a tabela de unidades e configura seu acesso.
begin;

create table public.unidade_coleta (
-- Semelhante ao auto_increment
    id bigint generated always as identity primary key,
    nome text not null,
    cidade text not null,
    endereco text not null,
    horario_atendimento text,
    telefone text,
    publicada boolean not null default false
);
-- Consulta somente unidades publicadas
-- Ativa as regras de acesso por linha desta tabela.
alter table public.unidade_coleta
enable row level security;

-- Remove permissões anteriores dos papéis usados pelo aplicativo.
revoke all privileges
on table public.unidade_coleta
from anon, authenticated;

-- Permite apenas a operação de consulta.
grant select
on table public.unidade_coleta
to anon, authenticated;

-- Define quais linhas podem aparecer nas consultas.
create policy "Consultar unidades publicadas"
on public.unidade_coleta
for select
to anon, authenticated
using (publicada = true);

commit;
