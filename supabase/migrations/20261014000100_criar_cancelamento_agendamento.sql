begin;

create table public.agendamento_cancelamento (
    id bigint generated always as identity primary key,

    agendamento_id bigint not null unique
        references public.agendamento(id)
        on delete restrict,

    cancelado_por uuid not null
        references public.perfil_doador(id)
        on delete restrict,

    cancelado_em timestamptz not null default clock_timestamp()
);

alter table public.agendamento_cancelamento
enable row level security;

revoke all privileges
on table public.agendamento_cancelamento
from public, anon, authenticated;

revoke all privileges
on sequence public.agendamento_cancelamento_id_seq
from public, anon, authenticated;

create function public.cancelar_agendamento (
    p_agendamento_id bigint
)
returns setof public.agendamento
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_doador_id uuid;
    v_agendamento public.agendamento%rowtype;
    v_horario public.horario_disponivel%rowtype;
    v_cancelado_em timestamptz;
begin
-- Identifica a conta autenticada.
v_doador_id := auth.uid();

if v_doador_id is null then
    raise exception 'É necessário entrar na conta para cancelar.'
        using errcode = '42501';
end if;

-- Valida o identificador recebido.
if p_agendamento_id is null or p_agendamento_id <= 0 then
    raise exception 'Informe um agendamento válido.'
        using errcode = '22023';
end if;
-- Exige leituras atualizadas durante a operação.
if current_setting('transaction_isolation') <> 'read committed' then
    raise exception 'O cancelamento exige isolamento READ COMMITTED.'
        using errcode = '25000';
end if;

-- Coordena criação e cancelamento para o mesmo doador.
perform perfil.id
from public.perfil_doador as perfil
where perfil.id = v_doador_id
for update;

if not found then
    raise exception 'Crie seu perfil antes de cancelar.'
        using errcode = '42501';
end if;
-- Busca somente uma reserva da conta autenticada.
select agendamento.*
into v_agendamento
from public.agendamento as agendamento
where agendamento.id = p_agendamento_id
  and agendamento.doador_id = v_doador_id;

if not found then
    raise exception 'Agendamento não encontrado ou indisponível.'
        using errcode = '42501';
end if;
-- Recupera o resultado de um cancelamento já realizado.
if v_agendamento.estado = 'cancelado' then
    return next v_agendamento;
    return;
end if;
-- Coordena cancelamento e criação de reservas neste horário.
select horario.*
into v_horario
from public.horario_disponivel as horario
where horario.id = v_agendamento.horario_id
for update;

if not found then
    raise exception 'Horário do agendamento indisponível.';
end if;

-- Relê e protege a reserva antes de alterar seu estado.
select agendamento.*
into v_agendamento
from public.agendamento as agendamento
where agendamento.id = p_agendamento_id
  and agendamento.doador_id = v_doador_id
  and agendamento.horario_id = v_horario.id
for update;

if not found then
    raise exception 'Agendamento não encontrado ou indisponível.'
        using errcode = '42501';
end if;

if v_agendamento.estado = 'cancelado' then
    return next v_agendamento;
    return;
end if;

-- Confere o prazo depois de obter todos os bloqueios.
v_cancelado_em := clock_timestamp();

if v_horario.inicio <= v_cancelado_em then
    raise exception 'O prazo para cancelar este agendamento terminou.';
end if;

-- Altera somente a reserva própria que ainda está agendada.
update public.agendamento as agendamento
set estado = 'cancelado'
where agendamento.id = p_agendamento_id
  and agendamento.doador_id = v_doador_id
  and agendamento.horario_id = v_horario.id
  and agendamento.estado = 'agendado'
returning agendamento.* into v_agendamento;

if not found then
    raise exception 'Não foi possível cancelar este agendamento.';
end if;

    -- Registra o autor e o instante do cancelamento.
    insert into public.agendamento_cancelamento (
        agendamento_id,
        cancelado_por,
        cancelado_em
    )
    values (
        v_agendamento.id,
        v_doador_id,
        v_cancelado_em
    );

    -- Devolve a reserva com o estado atualizado.
    return next v_agendamento;
    return;
end;
$$;

revoke all privileges
on function public.cancelar_agendamento(bigint)
from public, anon, authenticated;

-- Permite solicitar cancelamento com uma conta autenticada.
grant execute
on function public.cancelar_agendamento(bigint)
to authenticated;

commit;