-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente dados sintéticos e identidades A/B simuladas; sem login/JWT pela API.
-- Requer A com perfil e B sem perfil, como no cenário atual de demonstração.
-- Não remove nem modifica perfis existentes.
-- ROLLBACK desfaz o horário e o registro cancelado usados no teste.
-- Sequências podem avançar mesmo com ROLLBACK.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador_a uuid;
    v_doador_b uuid;
    v_unidade_id bigint;
    v_horario_id bigint;
    v_inexistente bigint;
    v_reserva_id bigint;
    v_chave uuid := gen_random_uuid();
begin
    select perfil.id into strict v_doador_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select id into strict v_doador_b
    from auth.users where email = 'doador.b@example.com';

    if v_doador_a = v_doador_b then
        raise exception 'Preparação: as contas precisam ser distintas.';
    end if;
    if exists (select 1 from public.perfil_doador where id = v_doador_b) then
        raise exception 'Preparação: B já tem perfil; adaptar o teste sem excluir esse perfil.';
    end if;

    select id into strict v_unidade_id
    from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_id, clock_timestamp() + interval '7 days', 3, true)
    returning id into v_horario_id;

    select max(id) + 1 into v_inexistente from public.horario_disponivel;

    -- Linha própria existente para tornar UPDATE/DELETE tentativas concretas.
    insert into public.agendamento (doador_id, horario_id, estado, chave_idempotencia)
    values (v_doador_a, v_horario_id, 'cancelado', v_chave)
    returning id into v_reserva_id;

    perform set_config('doesangue_test.doador_a', v_doador_a::text, true);
    perform set_config('doesangue_test.doador_b', v_doador_b::text, true);
    perform set_config('doesangue_test.horario_id', v_horario_id::text, true);
    perform set_config('doesangue_test.inexistente', v_inexistente::text, true);
    perform set_config('doesangue_test.reserva_id', v_reserva_id::text, true);
    perform set_config('doesangue_test.chave', v_chave::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_doador_a uuid := current_setting('doesangue_test.doador_a')::uuid;
    v_doador_b uuid := current_setting('doesangue_test.doador_b')::uuid;
    v_horario_id bigint := current_setting('doesangue_test.horario_id')::bigint;
    v_reserva_id bigint := current_setting('doesangue_test.reserva_id')::bigint;
    v_chave uuid := gen_random_uuid();
    v_caso record;
begin
    if current_user <> 'authenticated' then
        raise exception 'Teste falhou: papel precisa ser authenticated.';
    end if;

    perform set_config('request.jwt.claim.sub', '', true);
    perform set_config('request.jwt.claims', '{}', true);
    if auth.uid() is not null then
        raise exception 'Preparação: identidade deveria estar ausente.';
    end if;

    begin
        perform public.criar_agendamento(v_horario_id, v_chave);
        raise exception 'Teste falhou: pedido sem identidade aceito.';
    exception
        when raise_exception then
            if sqlerrm <> 'É necessário entrar na conta para agendar.' then raise; end if;
    end;

    perform set_config('request.jwt.claim.sub', v_doador_b::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador_b, 'role', 'authenticated')::text, true);
    if auth.uid() is distinct from v_doador_b then
        raise exception 'Preparação: identidade B incorreta.';
    end if;

    begin
        perform public.criar_agendamento(v_horario_id, v_chave);
        raise exception 'Teste falhou: pedido sem perfil aceito.';
    exception
        when raise_exception then
            if sqlerrm <> 'Crie seu perfil antes de agendar.' then raise; end if;
    end;

    perform set_config('request.jwt.claim.sub', v_doador_a::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador_a, 'role', 'authenticated')::text, true);
    if auth.uid() is distinct from v_doador_a then
        raise exception 'Preparação: identidade A incorreta.';
    end if;

    for v_caso in
        select * from (values
            (null::bigint, v_chave, 'Informe um horário válido.'),
            (0::bigint, v_chave, 'Informe um horário válido.'),
            (-1::bigint, v_chave, 'Informe um horário válido.'),
            (v_horario_id, null::uuid, 'Informe a chave da solicitação.'),
            (current_setting('doesangue_test.inexistente')::bigint,
                v_chave, 'Horário indisponível para agendamento.')
        ) as casos(horario_id, chave, mensagem)
    loop
        begin
            perform public.criar_agendamento(v_caso.horario_id, v_caso.chave);
            raise exception 'Teste falhou: parâmetros inválidos ou horário inexistente aceitos.';
        exception
            when raise_exception then
                if sqlerrm <> v_caso.mensagem then raise; end if;
        end;
    end loop;

    begin
        insert into public.agendamento (doador_id, horario_id, estado, chave_idempotencia)
        values (v_doador_a, v_horario_id, 'cancelado', gen_random_uuid());
        raise exception 'Teste falhou: INSERT direto permitido.';
    exception when insufficient_privilege then
        null;
    end;

    begin
        update public.agendamento set estado = 'agendado' where id = v_reserva_id;
        raise exception 'Teste falhou: UPDATE direto permitido.';
    exception when insufficient_privilege then
        null;
    end;

    begin
        delete from public.agendamento where id = v_reserva_id;
        raise exception 'Teste falhou: DELETE direto permitido.';
    exception when insufficient_privilege then
        null;
    end;

    perform set_config('request.jwt.claim.sub', '', true);
    perform set_config('request.jwt.claims', '{"role":"anon"}', true);
end;
$$;

set local role anon;

do $$
begin
    if current_user <> 'anon' or auth.uid() is not null then
        raise exception 'Preparação: visitante precisa estar sem identidade.';
    end if;

    begin
        perform public.criar_agendamento(
            current_setting('doesangue_test.horario_id')::bigint, gen_random_uuid()
        );
        raise exception 'Teste falhou: visitante conseguiu executar a função.';
    exception when insufficient_privilege then
        null;
    end;
end;
$$;

reset role;

do $$
declare
    v_total bigint;
    v_preservados bigint;
begin
    select count(*), count(*) filter (
        where id = current_setting('doesangue_test.reserva_id')::bigint
          and doador_id = current_setting('doesangue_test.doador_a')::uuid
          and estado = 'cancelado'
          and chave_idempotencia = current_setting('doesangue_test.chave')::uuid
    ) into v_total, v_preservados
    from public.agendamento
    where horario_id = current_setting('doesangue_test.horario_id')::bigint;

    if v_total <> 1 or v_preservados <> 1 then
        raise exception 'Teste falhou: registro temporário alterado ou novas reservas criadas.';
    end if;
end;
$$;

select * from (values
    ('Horário nulo/zero/negativo e chave nula', 'Bloqueado'),
    ('Horário inexistente', 'Bloqueado'),
    ('Identidade ausente', 'Bloqueado'),
    ('Conta B sem perfil', 'Bloqueado'),
    ('INSERT direto como A', 'Bloqueado'),
    ('UPDATE direto como A', 'Bloqueado'),
    ('DELETE direto como A', 'Bloqueado'),
    ('Execução da função como visitante', 'Bloqueado'),
    ('Dados do teste', 'Preservados')
) as resultados(teste, resultado);

rollback;
