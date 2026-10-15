begin;

create table public.campanha (
    id bigint generated always as identity primary key,

        unidade_id bigint not null
            references public.unidade_coleta(id)
            on delete restrict,

        titulo text not null
            check (char_length(btrim(titulo)) > 0),

            descricao text not null
                check (char_length(btrim(descricao)) > 0),

            inicio timestamptz not null,

                fim timestamptz not null,

                fonte text not null
                    check (char_length(btrim(fonte)) > 0),

                publicada boolean not null default false,

                criado_em timestamptz not null default now(),

                constraint campanha_periodo_check
                    check (fim > inicio)
);

-- Habilita o controle de acesso por linha.
alter table public.campanha
enable row level security;

-- Remove permissões gerais de acesso à tabela.
revoke all privileges
on table public.campanha
from public, anon, authenticated;

-- Impede acesso direto à sequência dos IDs.
revoke all privileges
on sequence public.campanha_id_seq
from public, anon, authenticated;

-- Permite somente consulta pelos clientes do aplicativo.
grant select
on table public.campanha
to anon, authenticated;

-- Exibe campanhas publicadas de unidades também publicadas.
create policy "Consultar campanhas publicadas de unidades publicadas"
on public.campanha
for select
to anon, authenticated
using (
    publicada = true
    and exists (
        select 1
        from public.unidade_coleta as unidade
        where unidade.id = campanha.unidade_id
          and unidade.publicada = true
    )
);

commit;