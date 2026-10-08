begin;

create table public.horario_disponivel (
    id bigint generated always as identity primary key,

    unidade_id bigint not null          -- chave estrangeira - FK
        references public.unidade_coleta(id)
        on delete restrict,

    inicio timestamptz not null,

    capacidade integer not null
        check (capacidade > 0),

    ativo boolean not null default false,

    unique (unidade_id, inicio)
);

-- Habilita o controle de acesso por linha.
alter table public.horario_disponivel
enable row level security;

-- Remove privilégios anteriores antes de conceder somente leitura.
revoke all privileges
on table public.horario_disponivel
from anon, authenticated;

-- Permite consultar a tabela.
grant select
on table public.horario_disponivel
to anon, authenticated;

-- Limita quais horários podem ser consultados.
create policy "Consultar horários ativos de unidades publicadas"
on public.horario_disponivel
for select
to anon, authenticated
using (
    ativo = true
    and exists (
        select 1
        from public.unidade_coleta as unidade
        where unidade.id = horario_disponivel.unidade_id
          and unidade.publicada = true
    )
);

commit;