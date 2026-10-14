-- Executar INTEIRO como postgres, com consulta B já aberta/preparada.
-- Oferta 23, início 2026-10-21 21:31:31.436406+00; reserva temporária 37.
-- Cancela 37 como A. A reserva original 23 NÃO é o alvo.
-- COMMIT somente após observar B aguardando o bloqueio; limite 35 segundos.
-- Identidade/papel simulados, sem login/JWT pela API. Limpar depois.
begin;
set transaction isolation level read committed;

do $$
declare
    v_a uuid;
    v_original public.agendamento%rowtype;
    v_original_23 jsonb;
begin
    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    if not exists (
        select 1 from public.horario_disponivel as horario
        join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
        where horario.id = 23
          and horario.inicio = timestamptz '2026-10-21 21:31:31.436406+00'
          and horario.inicio > clock_timestamp()
          and horario.capacidade = 1 and horario.ativo
          and unidade.nome = 'Unidade Demo A' and unidade.publicada
    ) then raise exception 'Preparação: oferta diferente do cenário confirmado.'; end if;

    select reserva.* into strict v_original from public.agendamento as reserva
    where reserva.id = 37 and reserva.horario_id = 23
      and reserva.doador_id = v_a and reserva.estado = 'agendado';
    if (select count(*) from public.agendamento where horario_id = 23) <> 1
       or exists (select 1 from public.agendamento_cancelamento where agendamento_id = 37) then
        raise exception 'Preparação: cenário já usado ou diferente; conferir antes de repetir.';
    end if;
    select to_jsonb(reserva) into strict v_original_23
    from public.agendamento as reserva where reserva.id = 23;
    if exists (select 1 from public.agendamento where id = 23 and horario_id = 23) then
        raise exception 'Preparação: oferta do teste não pode conter a reserva original 23.';
    end if;

    perform set_config('doesangue_test.cancel_a', v_a::text, true);
    perform set_config('doesangue_test.cancel_original', to_jsonb(v_original)::text, true);
    perform set_config('doesangue_test.original_23', v_original_23::text, true);
    perform set_config('request.jwt.claim.sub', v_a::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_a, 'role', 'authenticated')::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_cancelado jsonb;
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.cancel_a')::uuid then
        raise exception 'Preparação: papel ou identidade A incorretos.';
    end if;
    select to_jsonb(resultado) into strict v_cancelado
    from public.cancelar_agendamento(37::bigint) as resultado;
    if v_cancelado is distinct from (
        current_setting('doesangue_test.cancel_original')::jsonb || jsonb_build_object('estado', 'cancelado')
    ) then raise exception 'Teste falhou: cancelamento de 37 alterou outros campos.'; end if;
end;
$$;

reset role;

do $$
declare
    v_evento public.agendamento_cancelamento%rowtype;
begin
    select evento.* into strict v_evento
    from public.agendamento_cancelamento as evento where evento.agendamento_id = 37;
    if v_evento.cancelado_por is distinct from current_setting('doesangue_test.cancel_a')::uuid
       or v_evento.cancelado_em is null then
        raise exception 'Teste falhou: evento de A incorreto.';
    end if;
    if (select to_jsonb(reserva) from public.agendamento as reserva where reserva.id = 23)
       is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: reserva original 23 foi alterada.';
    end if;
end;
$$;

-- Sinaliza prontidão só depois do cancelamento e obtenção dos bloqueios.
set local application_name = 'doesangue_cancel_a_pronta_23';

do $$
declare
    v_limite timestamptz := clock_timestamp() + interval '35 seconds';
    v_pid_b integer;
begin
    loop
        perform pg_stat_clear_snapshot();
        select sessao.pid into v_pid_b from pg_stat_activity as sessao
        where sessao.application_name = 'doesangue_cancel_b_23'
          and sessao.pid <> pg_backend_pid()
          and sessao.wait_event_type = 'Lock'
          and pg_backend_pid() = any(pg_blocking_pids(sessao.pid))
        limit 1;

        if v_pid_b is not null then
            perform set_config('doesangue_test.cancel_espera', 'true', true);
            exit;
        end if;
        if clock_timestamp() >= v_limite then
            raise exception 'Teste sem espera concorrente detectada: cancelamento não será confirmado. Iniciar B enquanto A executa.';
        end if;
        perform pg_sleep(0.5);
    end loop;
end;
$$;

select
    'Cancelamento de A pronto para confirmar' as resultado,
    37::bigint as reserva_id,
    current_setting('doesangue_test.cancel_espera')::boolean as b_aguardou_bloqueio_de_a;

commit;
