-- Executar INTEIRO como postgres, com sessão B já aberta e preparada.
-- Oferta exclusiva deste teste: ID 13, início 2026-10-19 15:44:43.092005+00.
-- Reserva como A; confirma somente após observar B aguardando seu bloqueio.
-- Espera limitada a 35 segundos. Sucesso usa COMMIT; limpar depois.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador_a uuid;
    v_horario_id bigint;
begin
    select perfil.id into strict v_doador_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select horario.id into strict v_horario_id
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
    where horario.id = 13
      and horario.inicio = timestamptz '2026-10-19 15:44:43.092005+00'
      and horario.inicio > clock_timestamp()
      and horario.capacidade = 1 and horario.ativo
      and unidade.nome = 'Unidade Demo A' and unidade.publicada;

    if exists (select 1 from public.agendamento where horario_id = v_horario_id) then
        raise exception 'Preparação: oferta já usada; conferir/limpar antes de repetir o teste.';
    end if;

    perform set_config('doesangue_test.doador_a', v_doador_a::text, true);
    perform set_config('request.jwt.claim.sub', v_doador_a::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador_a, 'role', 'authenticated')::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_reserva public.agendamento%rowtype;
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.doador_a')::uuid then
        raise exception 'Preparação: papel ou identidade A incorretos.';
    end if;

    select resultado.* into strict v_reserva
    from public.criar_agendamento(13::bigint, gen_random_uuid()) as resultado;

    if v_reserva.id is null
       or v_reserva.doador_id is distinct from auth.uid()
       or v_reserva.horario_id is distinct from 13::bigint
       or v_reserva.estado is distinct from 'agendado' then
        raise exception 'Teste falhou: a reserva de A não corresponde ao pedido.';
    end if;
    perform set_config('doesangue_test.reserva_a', v_reserva.id::text, true);
end;
$$;

reset role;

-- Sinaliza prontidão somente depois de criar a reserva e obter seus bloqueios.
set local application_name = 'doesangue_concorrencia_a_pronta_13';

do $$
declare
    v_limite timestamptz := clock_timestamp() + interval '35 seconds';
    v_pid_b integer;
begin
    loop
        -- Atualiza apenas a leitura das estatísticas nesta sessão.
        perform pg_stat_clear_snapshot();

        select sessao.pid into v_pid_b
        from pg_stat_activity as sessao
        where sessao.application_name = 'doesangue_concorrencia_b_13'
          and sessao.pid <> pg_backend_pid()
          and sessao.wait_event_type = 'Lock'
          and pg_backend_pid() = any(pg_blocking_pids(sessao.pid))
        limit 1;

        if v_pid_b is not null then
            perform set_config('doesangue_test.espera_confirmada', 'true', true);
            exit;
        end if;

        if clock_timestamp() >= v_limite then
            raise exception 'Teste sem espera concorrente detectada: A não será confirmado. Iniciar B enquanto A executa.';
        end if;
        perform pg_sleep(0.5);
    end loop;
end;
$$;

select
    'Reserva de A pronta para confirmar' as resultado,
    current_setting('doesangue_test.reserva_a')::bigint as reserva_id,
    current_setting('doesangue_test.espera_confirmada')::boolean as b_aguardou_bloqueio_de_a;

commit;
