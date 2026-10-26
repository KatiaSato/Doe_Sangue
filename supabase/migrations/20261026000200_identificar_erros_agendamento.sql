-- F19: somente identificadores estáveis nos erros, sem mudança de regras ou permissões.
begin;

create or replace function public.criar_agendamento (
    p_horario_id bigint,
    p_chave_idempotencia uuid
)
returns setof public.agendamento
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    v_doador_id uuid;
    v_agendamento_existente public.agendamento%rowtype;
    v_horario public.horario_disponivel%rowtype;
    v_agendamentos_ativos bigint;
begin
    -- Identifica a conta que está fazendo a solicitação.
    v_doador_id := auth.uid();

    if v_doador_id is null then
        raise exception 'É necessário entrar na conta para agendar.' using detail = 'doesangue.session_required.v1';
    end if;

    -- Valida os parâmetros recebidos.
    if p_horario_id is null or p_horario_id <= 0 then
        raise exception 'Informe um horário válido.' using detail = 'doesangue.invalid_request.v1';
    end if;

    if p_chave_idempotencia is null then
        raise exception 'Informe a chave da solicitação.' using detail = 'doesangue.invalid_request.v1';
    end if;

    -- Este controle de capacidade exige leituras atualizadas entre consultas.
    if current_setting('transaction_isolation') <> 'read committed' then
        raise exception 'A reserva exige isolamento READ COMMITTED.' using detail = 'doesangue.isolation_unsupported.v1';
    end if;

    -- Verifica o perfil e coordena solicitações do mesmo doador.
    perform perfil.id
    from public.perfil_doador as perfil
    where perfil.id = v_doador_id
    for update;

    if not found then
        raise exception 'Crie seu perfil antes de agendar.' using detail = 'doesangue.profile_required.v1';
    end if;

    -- Recupera um pedido já gravado para este doador.
    select agendamento.*
    into v_agendamento_existente
    from public.agendamento as agendamento
    where agendamento.doador_id = v_doador_id
      and agendamento.chave_idempotencia = p_chave_idempotencia;

    if found then
        -- Uma chave não pode representar pedidos para horários diferentes.
        if v_agendamento_existente.horario_id <> p_horario_id then
            raise exception 'Esta chave já foi usada para outro horário.' using detail = 'doesangue.request_conflict.v1';
        end if;

        return next v_agendamento_existente;
        return;
    end if;

    -- Busca uma oferta ativa de uma unidade publicada.
    select horario.*
    into v_horario
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade
        on unidade.id = horario.unidade_id
    where horario.id = p_horario_id
      and horario.ativo
      and unidade.publicada
    for update of horario
    for share of unidade;

    if not found then
        raise exception 'Horário indisponível para agendamento.' using detail = 'doesangue.slot_unavailable.v1';
    end if;

    -- Aceita novas reservas somente para horários futuros.
    if v_horario.inicio <= clock_timestamp() then
        raise exception 'Este horário já passou.' using detail = 'doesangue.slot_started.v1';
    end if;

    -- Identifica outra reserva ativa do próprio doador.
    if exists (
        select 1
        from public.agendamento as agendamento
        where agendamento.doador_id = v_doador_id
          and agendamento.horario_id = v_horario.id
          and agendamento.estado = 'agendado'
    ) then
        raise exception 'Você já possui um agendamento ativo para este horário.' using detail = 'doesangue.already_booked.v1';
    end if;

    -- Conta as reservas que ocupam vagas neste horário.
    select count(*)
    into v_agendamentos_ativos
    from public.agendamento as agendamento
    where agendamento.horario_id = v_horario.id
      and agendamento.estado = 'agendado';

    if v_agendamentos_ativos >= v_horario.capacidade then
        raise exception 'Este horário está sem vagas.' using detail = 'doesangue.slot_full.v1';
    end if;

    -- Grava a reserva e devolve o registro criado.
    return query
    insert into public.agendamento as novo (
        doador_id,
        horario_id,
        estado,
        chave_idempotencia
    )
    values (
        v_doador_id,
        v_horario.id,
        'agendado',
        p_chave_idempotencia
    )
    returning novo.*;
end;
$$;

create or replace function public.cancelar_agendamento (
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
        using errcode = '42501', detail = 'doesangue.session_required.v1';
end if;

-- Valida o identificador recebido.
if p_agendamento_id is null or p_agendamento_id <= 0 then
    raise exception 'Informe um agendamento válido.'
        using errcode = '22023', detail = 'doesangue.invalid_request.v1';
end if;
-- Exige leituras atualizadas durante a operação.
if current_setting('transaction_isolation') <> 'read committed' then
    raise exception 'O cancelamento exige isolamento READ COMMITTED.'
        using errcode = '25000', detail = 'doesangue.isolation_unsupported.v1';
end if;

-- Coordena criação e cancelamento para o mesmo doador.
perform perfil.id
from public.perfil_doador as perfil
where perfil.id = v_doador_id
for update;

if not found then
    raise exception 'Crie seu perfil antes de cancelar.'
        using errcode = '42501', detail = 'doesangue.profile_required.v1';
end if;
-- Busca somente uma reserva da conta autenticada.
select agendamento.*
into v_agendamento
from public.agendamento as agendamento
where agendamento.id = p_agendamento_id
  and agendamento.doador_id = v_doador_id;

if not found then
    raise exception 'Agendamento não encontrado ou indisponível.'
        using errcode = '42501', detail = 'doesangue.appointment_unavailable.v1';
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
    raise exception 'Horário do agendamento indisponível.' using detail = 'doesangue.appointment_unavailable.v1';
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
        using errcode = '42501', detail = 'doesangue.appointment_unavailable.v1';
end if;

if v_agendamento.estado = 'cancelado' then
    return next v_agendamento;
    return;
end if;

-- Confere o prazo depois de obter todos os bloqueios.
v_cancelado_em := clock_timestamp();

if v_horario.inicio <= v_cancelado_em then
    raise exception 'O prazo para cancelar este agendamento terminou.' using detail = 'doesangue.cancellation_closed.v1';
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
    raise exception 'Não foi possível cancelar este agendamento.' using detail = 'doesangue.appointment_unavailable.v1';
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

commit;
