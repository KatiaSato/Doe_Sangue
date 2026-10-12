-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Usa somente dados sintéticos e identidade administrativa simulada.
-- ROLLBACK desfaz os dois horários temporários e a reserva.
-- Não verifica login/JWT pela API nem concorrência; sequências podem avançar.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador_id uuid;
    v_unidade_id bigint;
    v_primeiro_horario bigint;
    v_outro_horario bigint;
    v_inicio timestamptz := clock_timestamp() + interval '7 days';
begin
    select perfil.id
    into strict v_doador_id
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select unidade.id
    into v_unidade_id
    from public.unidade_coleta as unidade
    where unidade.nome = 'Unidade Demo A' and unidade.publicada
    order by unidade.id
    limit 1;

    if v_unidade_id is null then
        raise exception 'Preparação: precisa existir a Unidade Demo A publicada.';
    end if;

    -- Capacidade 3 isola duplicidade da regra de horário cheio.
    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_id, v_inicio, 3, true)
    returning id into v_primeiro_horario;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_id, v_inicio + interval '1 hour', 3, true)
    returning id into v_outro_horario;

    perform set_config('doesangue_test.doador_id', v_doador_id::text, true);
    perform set_config('doesangue_test.primeiro_horario', v_primeiro_horario::text, true);
    perform set_config('doesangue_test.outro_horario', v_outro_horario::text, true);
    perform set_config('doesangue_test.chave', gen_random_uuid()::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_doador_id uuid := current_setting('doesangue_test.doador_id')::uuid;
    v_primeiro_horario bigint := current_setting('doesangue_test.primeiro_horario')::bigint;
    v_outro_horario bigint := current_setting('doesangue_test.outro_horario')::bigint;
    v_chave uuid := current_setting('doesangue_test.chave')::uuid;
    v_nova_chave uuid := gen_random_uuid();
    v_reserva public.agendamento%rowtype;
    v_preservada public.agendamento%rowtype;
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

    if v_nova_chave = v_chave then
        raise exception 'Preparação: as chaves precisam ser diferentes.';
    end if;

    select resultado.*
    into strict v_reserva
    from public.criar_agendamento(v_primeiro_horario, v_chave) as resultado;

    begin
        perform public.criar_agendamento(v_outro_horario, v_chave);
        raise exception 'Teste falhou: a mesma chave foi aceita para outro horário.';
    exception
        when raise_exception then
            if sqlerrm <> 'Esta chave já foi usada para outro horário.' then
                raise;
            end if;
    end;

    begin
        perform public.criar_agendamento(v_primeiro_horario, v_nova_chave);
        raise exception 'Teste falhou: outra reserva ativa foi aceita para o mesmo horário.';
    exception
        when raise_exception then
            if sqlerrm <> 'Você já possui um agendamento ativo para este horário.' then
                raise;
            end if;
    end;

    select agendamento.*
    into strict v_preservada
    from public.agendamento as agendamento
    where agendamento.id = v_reserva.id;

    if v_preservada is distinct from v_reserva then
        raise exception 'Teste falhou: a reserva original foi alterada.';
    end if;
end;
$$;

reset role;

-- Contagem administrativa inclui todas as identidades, sem filtro da RLS.
do $$
begin
    if (
        select count(*)
        from public.agendamento
        where horario_id in (
            current_setting('doesangue_test.primeiro_horario')::bigint,
            current_setting('doesangue_test.outro_horario')::bigint
        )
    ) <> 1 then
        raise exception 'Teste falhou: esperava somente a primeira reserva.';
    end if;
end;
$$;

select
    'Chave reutilizada para outro horário bloqueada' as chave,
    'Reserva ativa duplicada bloqueada' as duplicidade,
    'Primeira reserva preservada' as original;

rollback;
