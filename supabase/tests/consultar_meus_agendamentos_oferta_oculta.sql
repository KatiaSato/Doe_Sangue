-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Fixtures administrativas representam reservas existentes e ofertas fechadas.
-- Cria somente unidades/horários/reservas sintéticos temporários; termina em ROLLBACK.
-- Não altera unidades/horários existentes nem a reserva 23; sequências podem avançar.
-- Simula identidades A/B; não realiza login nem verifica JWT pela API.
begin;

do $$
declare
    v_a uuid;
    v_b uuid;
    v_unidade_publica bigint;
    v_unidade_oculta bigint;
    v_inativo bigint;
    v_oculto bigint;
    v_passado bigint;
    v_ids bigint[];
    v_esperado jsonb;
    v_original jsonb;
begin
    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select conta.id into strict v_b
    from auth.users as conta where conta.email = 'doador.b@example.com';

    if v_a = v_b then
        raise exception 'Preparação: A/B precisam ser contas distintas.';
    end if;

    select to_jsonb(reserva) into strict v_original
    from public.agendamento as reserva where reserva.id = 23;

    insert into public.unidade_coleta (nome, cidade, endereco, publicada, fuso_horario)
    values (
        'Unidade Teste Leitura Publicada ' || gen_random_uuid()::text,
        'Cidade fictícia', 'Endereço fictício', true, 'America/Sao_Paulo'
    ) returning id into v_unidade_publica;

    insert into public.unidade_coleta (nome, cidade, endereco, publicada, fuso_horario)
    values (
        'Unidade Teste Leitura Oculta ' || gen_random_uuid()::text,
        'Cidade fictícia', 'Endereço fictício', false, 'America/Sao_Paulo'
    ) returning id into v_unidade_oculta;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_publica, clock_timestamp() + interval '7 days', 1, false)
    returning id into v_inativo;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_oculta, clock_timestamp() + interval '8 days', 1, true)
    returning id into v_oculto;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_publica, clock_timestamp() - interval '7 days', 1, true)
    returning id into v_passado;

    -- Preparação administrativa, não novos pedidos aceitos pela função de criação.
    insert into public.agendamento (doador_id, horario_id, estado, chave_idempotencia)
    values
        (v_a, v_inativo, 'agendado', gen_random_uuid()),
        (v_a, v_oculto, 'agendado', gen_random_uuid()),
        (v_a, v_passado, 'cancelado', gen_random_uuid());

    select
        array_agg(reserva.id order by reserva.id),
        jsonb_agg(
            jsonb_build_object(
                'agendamento', to_jsonb(reserva),
                'nome_unidade', unidade.nome,
                'inicio', horario.inicio,
                'fuso_horario', unidade.fuso_horario
            ) order by reserva.id
        )
    into v_ids, v_esperado
    from public.agendamento as reserva
    join public.horario_disponivel as horario on horario.id = reserva.horario_id
    join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
    where reserva.horario_id in (v_inativo, v_oculto, v_passado);

    if cardinality(v_ids) <> 3 then
        raise exception 'Preparação: esperava três reservas temporárias.';
    end if;

    perform set_config('doesangue_test.doador_a', v_a::text, true);
    perform set_config('doesangue_test.doador_b', v_b::text, true);
    perform set_config('doesangue_test.ids', v_ids::text, true);
    perform set_config('doesangue_test.inativo', v_inativo::text, true);
    perform set_config('doesangue_test.oculto', v_oculto::text, true);
    perform set_config('doesangue_test.unidade_publica', v_unidade_publica::text, true);
    perform set_config('doesangue_test.unidade_oculta', v_unidade_oculta::text, true);
    perform set_config('doesangue_test.esperado', v_esperado::text, true);
    perform set_config('doesangue_test.original_23', v_original::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_a uuid := current_setting('doesangue_test.doador_a')::uuid;
    v_b uuid := current_setting('doesangue_test.doador_b')::uuid;
    v_ids bigint[] := current_setting('doesangue_test.ids')::bigint[];
    v_obtido jsonb;
    v_quantidade bigint;
begin
    perform set_config('request.jwt.claim.sub', v_a::text, true);
    perform set_config(
        'request.jwt.claims', json_build_object('sub', v_a, 'role', 'authenticated')::text, true
    );

    if current_user <> 'authenticated' or auth.uid() is distinct from v_a then
        raise exception 'Teste falhou: papel/identidade A incorretos.';
    end if;

    -- As policies públicas continuam ocultando as ofertas fechadas.
    select count(*) into v_quantidade
    from public.horario_disponivel
    where id in (
        current_setting('doesangue_test.inativo')::bigint,
        current_setting('doesangue_test.oculto')::bigint
    );
    if v_quantidade <> 0 then
        raise exception 'Teste falhou: a consulta pública revelou uma oferta oculta.';
    end if;

    select count(*) into v_quantidade from public.unidade_coleta
    where id = current_setting('doesangue_test.unidade_oculta')::bigint;
    if v_quantidade <> 0 then
        raise exception 'Teste falhou: unidade não publicada ficou visível diretamente.';
    end if;

    select count(*) into v_quantidade from public.unidade_coleta
    where id = current_setting('doesangue_test.unidade_publica')::bigint;
    if v_quantidade <> 1 then
        raise exception 'Teste falhou: unidade publicada deveria estar visível.';
    end if;

    -- A função devolve as reservas próprias completas, mesmo com ofertas ocultas.
    select jsonb_agg(
        to_jsonb(resultado) order by (resultado.agendamento ->> 'id')::bigint
    ) into v_obtido
    from public.consultar_meus_agendamentos() as resultado
    where (resultado.agendamento ->> 'id')::bigint = any(v_ids);

    if v_obtido is distinct from current_setting('doesangue_test.esperado')::jsonb then
        raise exception 'Teste falhou: reservas próprias ocultas/passadas ou seus dados não retornaram corretamente.';
    end if;

    perform set_config('request.jwt.claim.sub', v_b::text, true);
    perform set_config(
        'request.jwt.claims', json_build_object('sub', v_b, 'role', 'authenticated')::text, true
    );
    if auth.uid() is distinct from v_b then
        raise exception 'Teste falhou: identidade B incorreta.';
    end if;

    select count(*) into v_quantidade
    from public.consultar_meus_agendamentos() as resultado
    where (resultado.agendamento ->> 'id')::bigint = any(v_ids)
       or (resultado.agendamento ->> 'id')::bigint = 23;
    if v_quantidade <> 0 then
        raise exception 'Teste falhou: B recebeu reservas de A.';
    end if;
end;
$$;

reset role;

do $$
begin
    if (
        select to_jsonb(reserva) from public.agendamento as reserva where reserva.id = 23
    ) is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: a reserva 23 foi alterada.';
    end if;
end;
$$;

select 'Horário inativo' as teste, 'Reserva própria visível' as resultado
union all select 'Unidade não publicada', 'Reserva própria visível'
union all select 'Reserva passada/cancelada', 'Visível, estado preservado'
union all select 'Consulta pública', 'Ofertas ocultas continuam ocultas'
union all select 'Conta B', 'Reservas de A não retornadas'
union all select 'Reserva 23', 'Preservada';

rollback;
