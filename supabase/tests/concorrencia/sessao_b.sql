-- Executar INTEIRO como postgres, ENQUANTO sessão A está executando.
-- Oferta ID 13, início 2026-10-19 15:44:43.092005+00; não mudar só um roteiro.
-- B deve aguardar o bloqueio de A e, após seu COMMIT, receber falta de vagas.
-- Perfil B é criado apenas se ausente e desfeito por ROLLBACK; existentes preservados.
-- Identidades simuladas administrativamente; não verifica JWT pela API.
begin;
set transaction isolation level read committed;
set local application_name = 'doesangue_concorrencia_b_13';

do $$
declare
    v_doador_a uuid;
    v_doador_b uuid;
    v_horario_id bigint;
begin
    perform pg_stat_clear_snapshot();
    if not exists (
        select 1 from pg_stat_activity
        where application_name = 'doesangue_concorrencia_a_pronta_13'
          and pid <> pg_backend_pid()
          and xact_start is not null and state = 'active'
    ) then
        raise exception 'Preparação: A ainda não está aguardando B. Executar A antes de B, em duas abas do navegador.';
    end if;

    select perfil.id into strict v_doador_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select id into strict v_doador_b
    from auth.users where email = 'doador.b@example.com';

    if v_doador_a = v_doador_b then
        raise exception 'Preparação: as contas precisam ser distintas.';
    end if;

    select horario.id into strict v_horario_id
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
    where horario.id = 13
      and horario.inicio = timestamptz '2026-10-19 15:44:43.092005+00'
      and horario.inicio > clock_timestamp()
      and horario.capacidade = 1 and horario.ativo
      and unidade.nome = 'Unidade Demo A' and unidade.publicada;

    insert into public.perfil_doador (id, nome_exibicao)
    values (v_doador_b, 'Doador Demo B')
    on conflict (id) do nothing;

    perform set_config('doesangue_test.doador_a', v_doador_a::text, true);
    perform set_config('doesangue_test.doador_b', v_doador_b::text, true);
    perform set_config('request.jwt.claim.sub', v_doador_b::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador_b, 'role', 'authenticated')::text, true);
end;
$$;

set local role authenticated;

do $$
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.doador_b')::uuid then
        raise exception 'Preparação: papel ou identidade B incorretos.';
    end if;

    begin
        perform public.criar_agendamento(13::bigint, gen_random_uuid());
        raise exception 'Teste falhou: B conseguiu reservar a vaga ocupada por A.';
    exception
        when raise_exception then
            if sqlerrm <> 'Este horário está sem vagas.' then raise; end if;
    end;

    if exists (select 1 from public.agendamento where horario_id = 13) then
        raise exception 'Teste falhou: B vê uma reserva neste horário, que deveria pertencer somente a A.';
    end if;
end;
$$;

reset role;

do $$
declare
    v_total bigint;
    v_reservas_a bigint;
begin
    select count(*), count(*) filter (
        where doador_id = current_setting('doesangue_test.doador_a')::uuid
          and estado = 'agendado'
    ) into v_total, v_reservas_a
    from public.agendamento where horario_id = 13;

    if v_total <> 1 or v_reservas_a <> 1 then
        raise exception 'Teste falhou: esperava somente uma reserva ativa, de A.';
    end if;
end;
$$;

select
    'B bloqueado por falta de vagas' as resultado,
    'Somente uma reserva ativa, de A' as capacidade;

rollback;
