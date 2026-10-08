begin;

create table public.perfil_doador (
    -- O ID vem da conta criada pelo Supabase Auth.
    id uuid primary key
        references auth.users(id)
        on delete restrict,

    nome_exibicao text not null
        check (length(trim(nome_exibicao)) > 0),

    criado_em timestamptz not null default now()
);

-- Ativa a proteção por linha.
alter table public.perfil_doador
enable row level security;

-- Remove permissões anteriores antes de conceder os acessos necessários.
revoke all privileges
on table public.perfil_doador
from public, anon, authenticated;

-- Permite que usuários autenticados consultem a tabela.
grant select
on table public.perfil_doador
to authenticated;

-- Cada usuário pode consultar somente o próprio perfil.
create policy "Consultar o próprio perfil"
on public.perfil_doador
for select
to authenticated
using (
    id = (select auth.uid())
);

-- Permite informar somente estes campos na criação.
grant insert (id, nome_exibicao)
on table public.perfil_doador
to authenticated;

-- O perfil criado deve pertencer ao usuário autenticado.
create policy "Criar o próprio perfil"
on public.perfil_doador
for insert
to authenticated
with check (
    id = (select auth.uid())
);

-- Permite alterar somente o nome de exibição.
grant update (nome_exibicao)
on table public.perfil_doador
to authenticated;

-- Cada usuário pode editar somente o próprio perfil.
create policy "Editar o próprio perfil"
on public.perfil_doador
for update
to authenticated
using (
    id = (select auth.uid())
)
with check (
    id = (select auth.uid())
);

commit;