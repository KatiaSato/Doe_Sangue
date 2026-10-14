-- Executar INTEIRO como postgres ENQUANTO A estiver executando.
-- Oferta 23, início 2026-10-21 21:31:31.436406+00; reserva A temporária 37.
-- B aguarda cancelamento/COMMIT de A e deve reservar a vaga liberada.
-- B termina em ROLLBACK, inclusive perfil B criado somente se ausente.
-- Identidades/papéis simulados; sem login/JWT pela API.
begin;
set transaction isolation level read committed;
set local application_name = 'doesangue_cancel_b_23';

do $$
declare
    v_a uuid;
    v_b uuid;
    v_original_a jsonb;
    v_original_23 jsonb;
begin
    perform pg_stat_clear_snapshot();
    if not exists (
        select 1 from pg_stat_activity
        where application_name = 'doesangue_cancel_a_pronta_23'
          and pid <> pg_backend_pid() and xact_start is not null and state = 'active'
    ) then raise exception 'Preparação: executar A antes de B, em duas abas do navegador.'; end if;

    select perfil.id into strict v_a from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';
    select id into strict v_b from auth.users where email = 'doador.b@example.com';
    if v_a = v_b then raise exception 'Preparação: contas precisam ser distintas.'; end if;

    if not exists (
        select 1 from public.horario_disponivel as horario
        join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
        where horario.id = 23
          and horario.inicio = timestamptz '2026-10-21 21:31:31.436406+00'
          and horario.inicio > clock_timestamp()
          and horario.capacidade = 1 and horario.ativo
          and unidade.nome = 'Unidade Demo A' and unidade.publicada
    ) then raise exception 'Preparação: oferta diferente do cenário confirmado.'; end if;

    -- B ainda vê a versão confirmada anterior: o cancelamento de A está pendente.
    select to_jsonb(reserva) into strict v_original_a from public.agendamento as reserva
    where reserva.id = 37 and reserva.horario_id = 23
      and reserva.doador_id = v_a and reserva.estado = 'agendado';
    if (select count(*) from public.agendamento where horario_id = 23) <> 1 then
        raise exception 'Preparação: oferta contém reservas fora do cenário esperado.';
    end if;
    select to_jsonb(reserva) into strict v_original_23
    from public.agendamento as reserva where reserva.id = 23;
    if exists (select 1 from public.agendamento where id = 23 and horario_id = 23) then
        raise exception 'Preparação: oferta do teste não pode conter a reserva original 23.';
    end if;

    insert into public.perfil_doador (id, nome_exibicao)
    values (v_b, 'Doador Demo B') on conflict (id) do nothing;
    perform set_config('doesangue_test.cancel_a', v_a::text, true);
    perform set_config('doesangue_test.cancel_b', v_b::text, true);
    perform set_config('doesangue_test.cancel_original_a', v_original_a::text, true);
    perform set_config('doesangue_test.original_23', v_original_23::text, true);
    perform set_config('request.jwt.claim.sub', v_b::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_b, 'role', 'authenticated')::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_reserva public.agendamento%rowtype;
    v_chave uuid := gen_random_uuid();
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.cancel_b')::uuid then
        raise exception 'Preparação: papel ou identidade B incorretos.';
    end if;

    -- Aguarda o bloqueio de A; a contagem após a espera deve ver A cancelado.
    select resultado.* into strict v_reserva
    from public.criar_agendamento(23::bigint, v_chave) as resultado;
    if v_reserva.id is null or v_reserva.id in (23, 37)
       or v_reserva.doador_id is distinct from auth.uid()
       or v_reserva.horario_id is distinct from 23::bigint
       or v_reserva.estado is distinct from 'agendado'
       or v_reserva.chave_idempotencia is distinct from v_chave
       or v_reserva.criado_em is null then
        raise exception 'Teste falhou: reserva de B diferente do pedido.';
    end if;
    if exists (select 1 from public.agendamento where id = 37)
       or (select count(*) from public.agendamento where horario_id = 23) <> 1 then
        raise exception 'Teste falhou: B deve ver apenas sua própria reserva neste horário.';
    end if;

    perform set_config('doesangue_test.cancel_reserva_b', v_reserva.id::text, true);
    perform set_config('doesangue_test.cancel_resultado_b', to_jsonb(v_reserva)::text, true);
end;
$$;

reset role;

do $$
declare
    v_total bigint;
    v_ativos_b bigint;
    v_cancelados_a bigint;
    v_atual jsonb;
    v_evento public.agendamento_cancelamento%rowtype;
begin
    select count(*), count(*) filter (
        where doador_id = current_setting('doesangue_test.cancel_b')::uuid and estado = 'agendado'
    ), count(*) filter (
        where id = 37 and doador_id = current_setting('doesangue_test.cancel_a')::uuid and estado = 'cancelado'
    ) into v_total, v_ativos_b, v_cancelados_a
    from public.agendamento where horario_id = 23;
    if v_total <> 2 or v_ativos_b <> 1 or v_cancelados_a <> 1 then
        raise exception 'Teste falhou: esperava A cancelado e somente B ativo para capacidade 1.';
    end if;

    select to_jsonb(reserva) into strict v_atual
    from public.agendamento as reserva where reserva.id = 37;
    if v_atual is distinct from (
        current_setting('doesangue_test.cancel_original_a')::jsonb || jsonb_build_object('estado', 'cancelado')
    ) then raise exception 'Teste falhou: registro original de A não foi preservado.'; end if;

    select to_jsonb(reserva) into strict v_atual from public.agendamento as reserva
    where reserva.id = current_setting('doesangue_test.cancel_reserva_b')::bigint;
    if v_atual is distinct from current_setting('doesangue_test.cancel_resultado_b')::jsonb then
        raise exception 'Teste falhou: reserva persistida de B diferente da resposta.';
    end if;

    select evento.* into strict v_evento from public.agendamento_cancelamento as evento
    where evento.agendamento_id = 37;
    if v_evento.cancelado_por is distinct from current_setting('doesangue_test.cancel_a')::uuid
       or v_evento.cancelado_em is null
       or (select count(*) from public.agendamento_cancelamento as evento
           join public.agendamento as reserva on reserva.id = evento.agendamento_id
           where reserva.horario_id = 23) <> 1 then
        raise exception 'Teste falhou: esperava somente um evento de A no horário do teste.';
    end if;

    if (select to_jsonb(reserva) from public.agendamento as reserva where reserva.id = 23)
       is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: reserva original 23 foi alterada.';
    end if;
end;
$$;

select
    'B reservou após o cancelamento de A' as resultado,
    current_setting('doesangue_test.cancel_reserva_b')::bigint as reserva_b_id,
    'Somente B ativo; A cancelado com um evento' as capacidade,
    'Reserva original 23 preservada' as original;

rollback;
