begin;

create function public.criar_agendamento (
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
        raise exception 'É necessário entrar na conta para agendar.';
    end if;

    -- Valida os parâmetros recebidos.
    if p_horario_id is null or p_horario_id <= 0 then
        raise exception 'Informe um horário válido.';
    end if;

    if p_chave_idempotencia is null then
        raise exception 'Informe a chave da solicitação.';
    end if;

    -- Este controle de capacidade exige leituras atualizadas entre consultas.
    if current_setting('transaction_isolation') <> 'read committed' then
        raise exception 'A reserva exige isolamento READ COMMITTED.';
    end if;

    -- Verifica o perfil e coordena solicitações do mesmo doador.
    perform perfil.id
    from public.perfil_doador as perfil
    where perfil.id = v_doador_id
    for update;

    if not found then
        raise exception 'Crie seu perfil antes de agendar.';
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
            raise exception 'Esta chave já foi usada para outro horário.';
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
        raise exception 'Horário indisponível para agendamento.';
    end if;

    -- Aceita novas reservas somente para horários futuros.
    if v_horario.inicio <= clock_timestamp() then
        raise exception 'Este horário já passou.';
    end if;

    -- Identifica outra reserva ativa do próprio doador.
    if exists (
        select 1
        from public.agendamento as agendamento
        where agendamento.doador_id = v_doador_id
          and agendamento.horario_id = v_horario.id
          and agendamento.estado = 'agendado'
    ) then
        raise exception 'Você já possui um agendamento ativo para este horário.';
    end if;

    -- Conta as reservas que ocupam vagas neste horário.
    select count(*)
    into v_agendamentos_ativos
    from public.agendamento as agendamento
    where agendamento.horario_id = v_horario.id
      and agendamento.estado = 'agendado';

    if v_agendamentos_ativos >= v_horario.capacidade then
        raise exception 'Este horário está sem vagas.';
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

-- Remove a permissão geral antes de liberar somente contas autenticadas.
revoke all privileges
on function public.criar_agendamento(bigint, uuid)
from public, anon, authenticated;

grant execute
on function public.criar_agendamento(bigint, uuid)
to authenticated;

commit;
