-- Executar INTEIRO como postgres no desenvolvimento acadêmico sintético.
-- Identidade/papel simulados; não verifica login/JWT pela API nem concorrência.
-- Reservas administrativas temporárias representam situações já existentes.
-- Não testa que criar_agendamento aceite ofertas inativas/ocultas/passadas.
-- Altera somente uma oferta temporária para simular passagem do início.
-- Preserva reserva 23 e termina em ROLLBACK. Sequências podem avançar.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador uuid;
    v_unidade_a bigint;
    v_unidade_b bigint;
    v_inicio timestamptz := clock_timestamp();
    v_horario bigint;
    v_reserva public.agendamento%rowtype;
    v_original_23 jsonb;
    v_caso record;
begin
    select perfil.id into strict v_doador
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select id into strict v_unidade_a from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;
    select id into strict v_unidade_b from public.unidade_coleta
    where nome = 'Unidade Demo B' and not publicada;

    select to_jsonb(reserva) into strict v_original_23
    from public.agendamento as reserva where reserva.id = 23;

    perform set_config('doesangue_test.original_23', v_original_23::text, true);
    perform set_config('doesangue_test.doador', v_doador::text, true);
    perform set_config(
        'doesangue_test.eventos_antes',
        (select count(*)::text from public.agendamento_cancelamento), true
    );

    for v_caso in
        select * from (values
            ('passado', v_unidade_a, true, v_inicio - interval '1 day'),
            ('inativo', v_unidade_a, false, v_inicio + interval '7 days'),
            ('oculto', v_unidade_b, true, v_inicio + interval '7 days')
        ) as casos(nome, unidade, ativo, inicio)
    loop
        insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
        values (v_caso.unidade, v_caso.inicio, 1, v_caso.ativo)
        returning id into v_horario;

        insert into public.agendamento (doador_id, horario_id, chave_idempotencia)
        values (v_doador, v_horario, gen_random_uuid())
        returning * into v_reserva;

        perform set_config('doesangue_test.horario_' || v_caso.nome, v_horario::text, true);
        perform set_config('doesangue_test.reserva_' || v_caso.nome, v_reserva.id::text, true);
        perform set_config('doesangue_test.original_' || v_caso.nome, to_jsonb(v_reserva)::text, true);
    end loop;
end;
$$;

set local role authenticated;

do $$
declare
    v_doador uuid := current_setting('doesangue_test.doador')::uuid;
    v_caso text;
    v_resultado jsonb;
begin
    perform set_config('request.jwt.claim.sub', v_doador::text, true);
    perform set_config(
        'request.jwt.claims',
        json_build_object('sub', v_doador, 'role', 'authenticated')::text, true
    );

    if current_user <> 'authenticated' or auth.uid() is distinct from v_doador then
        raise exception 'Teste falhou: papel ou identidade incorretos.';
    end if;

    begin
        perform public.cancelar_agendamento(
            current_setting('doesangue_test.reserva_passado')::bigint
        );
        raise exception 'Teste falhou: primeiro cancelamento após início foi aceito.';
    exception when sqlstate 'P0001' then
        if sqlerrm <> 'O prazo para cancelar este agendamento terminou.' then
            raise;
        end if;
    end;

    foreach v_caso in array array['inativo', 'oculto'] loop
        select to_jsonb(resultado) into strict v_resultado
        from public.cancelar_agendamento(
            current_setting('doesangue_test.reserva_' || v_caso)::bigint
        ) as resultado;

        if v_resultado is distinct from (
            current_setting('doesangue_test.original_' || v_caso)::jsonb
            || jsonb_build_object('estado', 'cancelado')
        ) then
            raise exception 'Teste falhou: cancelamento de oferta % incorreto.', v_caso;
        end if;
        perform set_config('doesangue_test.cancelado_' || v_caso, v_resultado::text, true);
    end loop;

    -- O cancelamento próprio não amplia a leitura pública das ofertas.
    if exists (
        select 1 from public.horario_disponivel
        where id in (
            current_setting('doesangue_test.horario_inativo')::bigint,
            current_setting('doesangue_test.horario_oculto')::bigint
        )
    ) then
        raise exception 'Teste falhou: ofertas inativa/oculta foram expostas.';
    end if;
end;
$$;

reset role;

do $$
declare
    v_evento public.agendamento_cancelamento%rowtype;
begin
    select evento.* into strict v_evento
    from public.agendamento_cancelamento as evento
    where evento.agendamento_id = current_setting('doesangue_test.reserva_inativo')::bigint;
    perform set_config('doesangue_test.evento_inativo', to_jsonb(v_evento)::text, true);

    -- Simula o início ter passado, alterando somente a oferta do teste.
    update public.horario_disponivel
    set inicio = clock_timestamp() - interval '1 day'
    where id = current_setting('doesangue_test.horario_inativo')::bigint;
end;
$$;

set local role authenticated;

do $$
declare
    v_repetido jsonb;
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.doador')::uuid then
        raise exception 'Teste falhou: identidade da repetição incorreta.';
    end if;

    select to_jsonb(resultado) into strict v_repetido
    from public.cancelar_agendamento(
        current_setting('doesangue_test.reserva_inativo')::bigint
    ) as resultado;

    if v_repetido is distinct from current_setting('doesangue_test.cancelado_inativo')::jsonb then
        raise exception 'Teste falhou: repetição após início alterou a reserva cancelada.';
    end if;
end;
$$;

reset role;

do $$
declare
    v_caso text;
    v_reserva_id bigint;
    v_atual jsonb;
    v_evento public.agendamento_cancelamento%rowtype;
begin
    foreach v_caso in array array['inativo', 'oculto'] loop
        v_reserva_id := current_setting('doesangue_test.reserva_' || v_caso)::bigint;
        select to_jsonb(reserva) into strict v_atual
        from public.agendamento as reserva where reserva.id = v_reserva_id;

        if v_atual is distinct from current_setting('doesangue_test.cancelado_' || v_caso)::jsonb then
            raise exception 'Teste falhou: reserva % não está cancelada no banco.', v_caso;
        end if;

        select evento.* into strict v_evento
        from public.agendamento_cancelamento as evento where evento.agendamento_id = v_reserva_id;
        if v_evento.cancelado_por is distinct from current_setting('doesangue_test.doador')::uuid
           or v_evento.cancelado_em is null then
            raise exception 'Teste falhou: evento de % inválido.', v_caso;
        end if;
    end loop;

    if (
        select to_jsonb(evento) from public.agendamento_cancelamento as evento
        where evento.agendamento_id = current_setting('doesangue_test.reserva_inativo')::bigint
    ) is distinct from current_setting('doesangue_test.evento_inativo')::jsonb then
        raise exception 'Teste falhou: repetição mudou o evento original.';
    end if;

    if (
        select to_jsonb(reserva) from public.agendamento as reserva
        where reserva.id = current_setting('doesangue_test.reserva_passado')::bigint
    ) is distinct from current_setting('doesangue_test.original_passado')::jsonb
       or exists (
           select 1 from public.agendamento_cancelamento
           where agendamento_id = current_setting('doesangue_test.reserva_passado')::bigint
       ) then
        raise exception 'Teste falhou: pedido fora do prazo alterou reserva ou criou evento.';
    end if;

    if (select count(*) from public.agendamento_cancelamento)
       <> current_setting('doesangue_test.eventos_antes')::bigint + 2 then
        raise exception 'Teste falhou: esperava exatamente dois novos eventos.';
    end if;

    if (
        select to_jsonb(reserva) from public.agendamento as reserva where reserva.id = 23
    ) is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: reserva 23 foi alterada.';
    end if;
end;
$$;

select * from (values
    ('Horário passado', 'Cancelamento bloqueado'),
    ('Horário inativo', 'Reserva própria cancelada'),
    ('Unidade não publicada', 'Reserva própria cancelada'),
    ('Repetição após início', 'Mesmo cancelamento, sem novo evento'),
    ('Reserva 23', 'Preservada')
) as resultados(teste, resultado);

rollback;
