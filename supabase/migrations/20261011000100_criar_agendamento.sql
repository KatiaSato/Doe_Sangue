begin;

create table public.agendamento (
    id bigint generated always as identity primary key,

    doador_id uuid not null
        references public.perfil_doador(id)         -- chave estrangeira
        on delete restrict,

    horario_id bigint not null
        references public.horario_disponivel(id)    -- chave estrangeira
        on delete restrict,

    estado text not null default 'agendado'
        check (estado in ('agendado', 'cancelado')),

    chave_idempotencia uuid not null,

    criado_em timestamptz not null default now(),

    unique (doador_id, chave_idempotencia)
);

-- Habilita a proteção por linha.
alter table public.agendamento
enable row level security;

-- Os acessos serão definidos nos próximos passos.
revoke all privileges
on table public.agendamento
from public, anon, authenticated;

-- Impede reservas ativas duplicadas do mesmo doador no mesmo horário.
create unique index agendamento_ativo_doador_horario
on public.agendamento (doador_id, horario_id)
where estado = 'agendado';

-- Permite consultar agendamentos com uma sessão autenticada.
grant select        -- grant select permite ao papel authenticated consultar a tabela
on table public.agendamento
to authenticated;

-- Cada doador pode consultar somente os próprios agendamentos.
create policy "Consultar os próprios agendamentos"
on public.agendamento
for select
to authenticated
using (
    doador_id = (select auth.uid())
);

-- A geração dos IDs ficará sob responsabilidade do servidor.
revoke all privileges
on sequence public.agendamento_id_seq
from public, anon, authenticated;

-- Facilita a consulta dos agendamentos que ocupam vagas.
create index agendamento_ativo_por_horario
on public.agendamento (horario_id)
where estado = 'agendado';
commit;