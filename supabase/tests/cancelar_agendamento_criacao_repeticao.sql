-- Desenvolvimento acadêmico: somente conta e dados sintéticos.
-- Executar INTEIRO no SQL Editor, inicialmente como postgres.
-- Simula identidade/papel; não realiza login nem verifica JWT pela API.
-- Cria uma oferta/reserva temporária e termina em ROLLBACK.
-- Não cancela a reserva 23. Sequências podem avançar mesmo com ROLLBACK.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador_id uuid;
    v_unidade_id bigint;
    v_horario_id bigint;
    v_original_23 jsonb;
begin
    select perfil.id
    into strict v_doador_id
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select unidade.id
    into v_unidade_id
    from public.unidade_coleta as unidade
    where unidade.nome = 'Unidade Demo A'
      and unidade.publicada
    order by unidade.id
    limit 1;

    if v_unidade_id is null then
        raise exception 'Preparação: precisa existir a Unidade Demo A publicada.';
    end if;

    select to_jsonb(reserva)
    into strict v_original_23
    from public.agendamento as reserva
    where reserva.id = 23;

    perform set_config('doesangue_test.original_23', v_original_23::text, true);
    perform set_config(
        'doesangue_test.eventos_antes',
        (select count(*)::text from public.agendamento_cancelamento),
        true
    );

    insert into public.horario_disponivel (
        unidade_id, inicio, capacidade, ativo
    )
    values (v_unidade_id, clock_timestamp() + interval '7 days', 1, true)
    returning id into v_horario_id;

    perform set_config('doesangue_test.doador_id', v_doador_id::text, true);
    perform set_config('doesangue_test.horario_id', v_horario_id::text, true);
    perform set_config('doesangue_test.chave', gen_random_uuid()::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_doador_id uuid := current_setting('doesangue_test.doador_id')::uuid;
    v_horario_id bigint := current_setting('doesangue_test.horario_id')::bigint;
    v_chave uuid := current_setting('doesangue_test.chave')::uuid;
    v_criado public.agendamento%rowtype;
    v_cancelado public.agendamento%rowtype;
    v_antes timestamptz;
    v_depois timestamptz;
begin
    perform set_config('request.jwt.claim.sub', v_doador_id::text, true);
    perform set_config(
        'request.jwt.claims',
        json_build_object('sub', v_doador_id, 'role', 'authenticated')::text,
        true
    );

    if current_user <> 'authenticated' or auth.uid() is distinct from v_doador_id then
        raise exception 'Teste falhou: papel ou identidade simulada incorretos.';
    end if;

    select resultado.*
    into strict v_criado
    from public.criar_agendamento(v_horario_id, v_chave) as resultado;

    if v_criado.id is null
       or v_criado.doador_id is distinct from v_doador_id
       or v_criado.horario_id is distinct from v_horario_id
       or v_criado.estado is distinct from 'agendado'
       or v_criado.chave_idempotencia is distinct from v_chave
       or v_criado.criado_em is null then
        raise exception 'Teste falhou: reserva criada diferente do pedido.';
    end if;

    v_antes := clock_timestamp();
    select resultado.*
    into strict v_cancelado
    from public.cancelar_agendamento(v_criado.id) as resultado;
    v_depois := clock_timestamp();

    if to_jsonb(v_cancelado) is distinct from (
        to_jsonb(v_criado) || jsonb_build_object('estado', 'cancelado')
    ) then
        raise exception 'Teste falhou: cancelamento alterou campos além do estado.';
    end if;

    if (
        select to_jsonb(reserva)
        from public.agendamento as reserva
        where reserva.id = v_criado.id
    ) is distinct from to_jsonb(v_cancelado) then
        raise exception 'Teste falhou: resposta diferente da reserva persistida.';
    end if;

    perform set_config('doesangue_test.reserva_id', v_criado.id::text, true);
    perform set_config('doesangue_test.cancelado', to_jsonb(v_cancelado)::text, true);
    perform set_config('doesangue_test.antes', v_antes::text, true);
    perform set_config('doesangue_test.depois', v_depois::text, true);
end;
$$;

reset role;

-- A tabela de eventos não é legível por authenticated; conferir como postgres.
do $$
declare
    v_evento public.agendamento_cancelamento%rowtype;
begin
    select evento.*
    into strict v_evento
    from public.agendamento_cancelamento as evento
    where evento.agendamento_id = current_setting('doesangue_test.reserva_id')::bigint;

    if v_evento.id is null
       or v_evento.cancelado_por is distinct from current_setting('doesangue_test.doador_id')::uuid
       or v_evento.cancelado_em is null
       or v_evento.cancelado_em < current_setting('doesangue_test.antes')::timestamptz
       or v_evento.cancelado_em > current_setting('doesangue_test.depois')::timestamptz then
        raise exception 'Teste falhou: autor ou instante do evento incorretos.';
    end if;

    perform set_config('doesangue_test.evento', to_jsonb(v_evento)::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_repetido jsonb;
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.doador_id')::uuid then
        raise exception 'Teste falhou: papel ou identidade da repetição incorretos.';
    end if;

    select to_jsonb(resultado)
    into strict v_repetido
    from public.cancelar_agendamento(
        current_setting('doesangue_test.reserva_id')::bigint
    ) as resultado;

    if v_repetido is distinct from current_setting('doesangue_test.cancelado')::jsonb then
        raise exception 'Teste falhou: repetição não devolveu a mesma reserva completa.';
    end if;
end;
$$;

reset role;

do $$
begin
    if (
        select to_jsonb(evento)
        from public.agendamento_cancelamento as evento
        where evento.agendamento_id = current_setting('doesangue_test.reserva_id')::bigint
    ) is distinct from current_setting('doesangue_test.evento')::jsonb then
        raise exception 'Teste falhou: repetição alterou o evento original.';
    end if;

    if (select count(*) from public.agendamento_cancelamento)
       <> current_setting('doesangue_test.eventos_antes')::bigint + 1 then
        raise exception 'Teste falhou: esperava somente um novo evento.';
    end if;

    if (
        select count(*) from public.agendamento
        where horario_id = current_setting('doesangue_test.horario_id')::bigint
    ) <> 1 or exists (
        select 1 from public.agendamento
        where horario_id = current_setting('doesangue_test.horario_id')::bigint
          and estado = 'agendado'
    ) then
        raise exception 'Teste falhou: reserva não foi preservada ou ainda ocupa vaga.';
    end if;

    if (
        select to_jsonb(reserva) from public.agendamento as reserva
        where reserva.id = 23
    ) is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: reserva 23 foi alterada.';
    end if;
end;
$$;

select
    'Reserva temporária cancelada' as cancelamento,
    'Mesmo registro, sem outro evento' as repeticao,
    'Autor e instante conferidos' as evento,
    'Reserva 23 preservada' as original;

rollback;
