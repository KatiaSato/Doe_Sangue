-- Executar INTEIRO como postgres no desenvolvimento acadêmico sintético.
-- Simula identidades/papéis; não realiza login nem valida JWT pela API.
-- Cria perfil B somente se ausente, sem excluir/modificar perfil existente.
-- Usa reservas/evento temporários, preserva 23 e termina em ROLLBACK.
-- Sequências podem avançar mesmo com ROLLBACK. Não verifica concorrência.
begin;
set transaction isolation level read committed;

do $$
declare
    v_a uuid;
    v_b uuid;
    v_sem_perfil uuid := gen_random_uuid();
    v_unidade bigint;
    v_horario bigint;
    v_reserva public.agendamento%rowtype;
    v_cancelada bigint;
    v_evento public.agendamento_cancelamento%rowtype;
    v_original_23 jsonb;
    v_caso record;
begin
    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';
    select id into strict v_b from auth.users where email = 'doador.b@example.com';
    if v_a = v_b or exists (
        select 1 from public.perfil_doador where id = v_sem_perfil
    ) or exists (select 1 from auth.users where id = v_sem_perfil) then
        raise exception 'Preparação: identidades precisam ser distintas e UUID de teste sem perfil.';
    end if;

    insert into public.perfil_doador (id, nome_exibicao)
    values (v_b, 'Doador Demo B') on conflict (id) do nothing;

    select to_jsonb(reserva) into strict v_original_23
    from public.agendamento as reserva where reserva.id = 23;
    perform set_config('doesangue_test.original_23', v_original_23::text, true);
    perform set_config(
        'doesangue_test.eventos_antes',
        (select count(*)::text from public.agendamento_cancelamento), true
    );

    select id into strict v_unidade from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;
    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade, clock_timestamp() + interval '7 days', 2, true)
    returning id into v_horario;

    for v_caso in select * from (values ('a', v_a), ('b', v_b)) as casos(nome, doador) loop
        insert into public.agendamento (doador_id, horario_id, chave_idempotencia)
        values (v_caso.doador, v_horario, gen_random_uuid())
        returning * into v_reserva;
        perform set_config('doesangue_test.reserva_' || v_caso.nome, v_reserva.id::text, true);
        perform set_config('doesangue_test.original_' || v_caso.nome, to_jsonb(v_reserva)::text, true);
    end loop;

    -- Evento próprio existente torna UPDATE/DELETE tentativas sobre uma linha real.
    insert into public.agendamento (doador_id, horario_id, estado, chave_idempotencia)
    values (v_a, v_horario, 'cancelado', gen_random_uuid())
    returning id into v_cancelada;
    insert into public.agendamento_cancelamento (agendamento_id, cancelado_por)
    values (v_cancelada, v_a) returning * into v_evento;

    perform set_config('doesangue_test.a', v_a::text, true);
    perform set_config('doesangue_test.b', v_b::text, true);
    perform set_config('doesangue_test.sem_perfil', v_sem_perfil::text, true);
    perform set_config('doesangue_test.horario', v_horario::text, true);
    perform set_config('doesangue_test.inexistente', (select (max(id) + 1)::text from public.agendamento), true);
    perform set_config('doesangue_test.evento_id', v_evento.id::text, true);
    perform set_config('doesangue_test.original_evento', to_jsonb(v_evento)::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_a uuid := current_setting('doesangue_test.a')::uuid;
    v_b uuid := current_setting('doesangue_test.b')::uuid;
    v_sem_perfil uuid := current_setting('doesangue_test.sem_perfil')::uuid;
    v_reserva_a bigint := current_setting('doesangue_test.reserva_a')::bigint;
    v_id bigint;
    v_caso record;
begin
    if current_user <> 'authenticated' then
        raise exception 'Preparação: papel precisa ser authenticated.';
    end if;

    perform set_config('request.jwt.claim.sub', '', true);
    perform set_config('request.jwt.claims', '{}', true);
    if auth.uid() is not null then raise exception 'Preparação: identidade deveria estar ausente.'; end if;
    begin
        perform public.cancelar_agendamento(v_reserva_a);
        raise exception 'Teste falhou: pedido sem identidade aceito.';
    exception when sqlstate '42501' then
        if sqlerrm <> 'É necessário entrar na conta para cancelar.' then raise; end if;
    end;

    -- UUID sem conta/perfil somente para testar o guard de perfil na simulação.
    perform set_config('request.jwt.claim.sub', v_sem_perfil::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_sem_perfil, 'role', 'authenticated')::text, true);
    if auth.uid() is distinct from v_sem_perfil then raise exception 'Preparação: UUID sem perfil incorreto.'; end if;
    begin
        perform public.cancelar_agendamento(v_reserva_a);
        raise exception 'Teste falhou: pedido sem perfil aceito.';
    exception when sqlstate '42501' then
        if sqlerrm <> 'Crie seu perfil antes de cancelar.' then raise; end if;
    end;

    perform set_config('request.jwt.claim.sub', v_a::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_a, 'role', 'authenticated')::text, true);
    if auth.uid() is distinct from v_a then raise exception 'Preparação: identidade A incorreta.'; end if;

    foreach v_id in array array[null::bigint, 0::bigint, -1::bigint] loop
        begin
            perform public.cancelar_agendamento(v_id);
            raise exception 'Teste falhou: ID inválido aceito.';
        exception when sqlstate '22023' then
            if sqlerrm <> 'Informe um agendamento válido.' then raise; end if;
        end;
    end loop;

    begin
        perform public.cancelar_agendamento(current_setting('doesangue_test.inexistente')::bigint);
        raise exception 'Teste falhou: ID inexistente aceito.';
    exception when sqlstate '42501' then
        if sqlerrm <> 'Agendamento não encontrado ou indisponível.' then raise; end if;
    end;

    -- Ambos possuem perfil; o bloqueio deve ocorrer pela propriedade da reserva.
    for v_caso in select * from (values
        (v_a, current_setting('doesangue_test.reserva_b')::bigint),
        (v_b, v_reserva_a)
    ) as casos(doador, reserva) loop
        perform set_config('request.jwt.claim.sub', v_caso.doador::text, true);
        perform set_config('request.jwt.claims',
            json_build_object('sub', v_caso.doador, 'role', 'authenticated')::text, true);
        if auth.uid() is distinct from v_caso.doador then raise exception 'Preparação: identidade do acesso cruzado incorreta.'; end if;
        begin
            perform public.cancelar_agendamento(v_caso.reserva);
            raise exception 'Teste falhou: cancelamento de terceiro aceito.';
        exception when sqlstate '42501' then
            if sqlerrm <> 'Agendamento não encontrado ou indisponível.' then raise; end if;
        end;
    end loop;

    perform set_config('request.jwt.claim.sub', v_a::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_a, 'role', 'authenticated')::text, true);
    if auth.uid() is distinct from v_a then raise exception 'Preparação: identidade das operações diretas incorreta.'; end if;

    -- Comandos controlados com parâmetros sintéticos corretamente escapados.
    for v_caso in select * from (values
        ('SELECT de eventos', 'select 1 from public.agendamento_cancelamento'),
        ('INSERT de evento', format(
            'insert into public.agendamento_cancelamento (agendamento_id, cancelado_por) values (%s, %L)', v_reserva_a, v_a)),
        ('UPDATE de evento', format(
            'update public.agendamento_cancelamento set cancelado_em = clock_timestamp() where id = %s',
            current_setting('doesangue_test.evento_id')::bigint)),
        ('DELETE de evento', format(
            'delete from public.agendamento_cancelamento where id = %s', current_setting('doesangue_test.evento_id')::bigint)),
        ('INSERT de reserva', format(
            'insert into public.agendamento (doador_id, horario_id, estado, chave_idempotencia) values (%L, %s, %L, gen_random_uuid())',
            v_a, current_setting('doesangue_test.horario')::bigint, 'cancelado')),
        ('UPDATE de reserva', format(
            'update public.agendamento set estado = %L where id = %s', 'cancelado', v_reserva_a)),
        ('DELETE de reserva', format('delete from public.agendamento where id = %s', v_reserva_a))
    ) as casos(nome, comando) loop
        begin
            execute v_caso.comando;
            raise exception 'Teste falhou: operação direta % permitida.', v_caso.nome;
        exception when sqlstate '42501' then null;
        end;
    end loop;

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
        perform public.cancelar_agendamento(current_setting('doesangue_test.reserva_a')::bigint);
        raise exception 'Teste falhou: visitante executou a função.';
    exception when sqlstate '42501' then null;
    end;
end;
$$;

reset role;

do $$
declare
    v_caso text;
    v_atual jsonb;
begin
    foreach v_caso in array array['a', 'b'] loop
        select to_jsonb(reserva) into strict v_atual
        from public.agendamento as reserva
        where reserva.id = current_setting('doesangue_test.reserva_' || v_caso)::bigint;
        if v_atual is distinct from current_setting('doesangue_test.original_' || v_caso)::jsonb then
            raise exception 'Teste falhou: reserva de % foi alterada.', v_caso;
        end if;
    end loop;

    select to_jsonb(evento) into strict v_atual
    from public.agendamento_cancelamento as evento
    where evento.id = current_setting('doesangue_test.evento_id')::bigint;
    if v_atual is distinct from current_setting('doesangue_test.original_evento')::jsonb
       or (select count(*) from public.agendamento_cancelamento)
          <> current_setting('doesangue_test.eventos_antes')::bigint + 1 then
        raise exception 'Teste falhou: evento original alterado ou eventos indevidos criados.';
    end if;

    if (select count(*) from public.agendamento
        where horario_id = current_setting('doesangue_test.horario')::bigint) <> 3 then
        raise exception 'Teste falhou: reservas removidas ou criadas indevidamente.';
    end if;

    if (select to_jsonb(reserva) from public.agendamento as reserva where reserva.id = 23)
       is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: reserva 23 foi alterada.';
    end if;
end;
$$;

select * from (values
    ('ID nulo/zero/negativo', 'Bloqueado por 22023'),
    ('ID inexistente', 'Bloqueado por 42501'),
    ('Identidade ausente', 'Bloqueado por 42501'),
    ('UUID sintético sem perfil', 'Bloqueado por 42501'),
    ('A tenta cancelar reserva de B', 'Bloqueado por 42501'),
    ('B tenta cancelar reserva de A', 'Bloqueado por 42501'),
    ('Acesso direto aos eventos', 'SELECT/INSERT/UPDATE/DELETE bloqueados'),
    ('Escrita direta nas reservas', 'INSERT/UPDATE/DELETE bloqueados'),
    ('Execução como visitante', 'Bloqueada'),
    ('Reservas, evento e reserva 23', 'Preservados')
) as resultados(teste, resultado);

rollback;
