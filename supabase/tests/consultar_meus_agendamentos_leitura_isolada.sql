-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Simulação administrativa de A/B; não realiza login nem verifica JWT pela API.
-- Cria duas reservas CANCELADAS temporárias, sem ocupar vagas.
-- Perfil B é criado somente se ausente; ROLLBACK desfaz os dados deste teste.
-- Não altera a reserva 23. Sequências de IDs podem avançar com ROLLBACK.
begin;

do $$
declare
    v_a uuid;
    v_b uuid;
    v_original public.agendamento%rowtype;
    v_a_teste public.agendamento%rowtype;
    v_b_teste public.agendamento%rowtype;
    v_nome text;
    v_inicio timestamptz;
    v_fuso text;
begin
    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select conta.id into strict v_b
    from auth.users as conta
    where conta.email = 'doador.b@example.com';

    if v_a = v_b then
        raise exception 'Preparação: as contas sintéticas precisam ser distintas.';
    end if;

    select reserva.* into strict v_original
    from public.agendamento as reserva
    where reserva.id = 23;

    if v_original.doador_id is distinct from v_a
       or v_original.estado is distinct from 'agendado' then
        raise exception 'Preparação: a reserva 23 precisa estar agendada para A.';
    end if;

    select unidade.nome, horario.inicio, unidade.fuso_horario
    into strict v_nome, v_inicio, v_fuso
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
    where horario.id = v_original.horario_id;

    insert into public.perfil_doador (id, nome_exibicao)
    values (v_b, 'Doador Demo B')
    on conflict (id) do nothing;

    -- Fixtures administrativas: isolam a consulta da criação/capacidade.
    insert into public.agendamento (doador_id, horario_id, estado, chave_idempotencia)
    values (v_a, v_original.horario_id, 'cancelado', gen_random_uuid())
    returning * into v_a_teste;

    insert into public.agendamento (doador_id, horario_id, estado, chave_idempotencia)
    values (v_b, v_original.horario_id, 'cancelado', gen_random_uuid())
    returning * into v_b_teste;

    perform set_config('doesangue_test.doador_a', v_a::text, true);
    perform set_config('doesangue_test.doador_b', v_b::text, true);
    perform set_config('doesangue_test.agendamento_a', v_a_teste.id::text, true);
    perform set_config('doesangue_test.agendamento_b', v_b_teste.id::text, true);
    perform set_config('doesangue_test.json_a', to_jsonb(v_a_teste)::text, true);
    perform set_config('doesangue_test.json_b', to_jsonb(v_b_teste)::text, true);
    perform set_config('doesangue_test.original_23', to_jsonb(v_original)::text, true);
    perform set_config('doesangue_test.nome', v_nome, true);
    perform set_config('doesangue_test.inicio', v_inicio::text, true);
    perform set_config('doesangue_test.fuso', v_fuso, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_a uuid := current_setting('doesangue_test.doador_a')::uuid;
    v_b uuid := current_setting('doesangue_test.doador_b')::uuid;
    v_id_a bigint := current_setting('doesangue_test.agendamento_a')::bigint;
    v_id_b bigint := current_setting('doesangue_test.agendamento_b')::bigint;
    v_identidade uuid;
    v_json_esperado jsonb;
    v_resultado record;
    v_quantidade bigint;
begin
    if current_user <> 'authenticated' then
        raise exception 'Teste falhou: a chamada precisa usar authenticated.';
    end if;

    for v_identidade in select unnest(array[v_a, v_b]) loop
        perform set_config('request.jwt.claim.sub', v_identidade::text, true);
        perform set_config(
            'request.jwt.claims',
            json_build_object('sub', v_identidade, 'role', 'authenticated')::text,
            true
        );

        if auth.uid() is distinct from v_identidade then
            raise exception 'Teste falhou: identidade simulada incorreta.';
        end if;

        -- Verifica TODA a resposta, sem filtrar o doador no cliente.
        if exists (
            select 1 from public.consultar_meus_agendamentos() as resultado
            where (resultado.agendamento ->> 'doador_id')::uuid
                is distinct from v_identidade
        ) then
            raise exception 'Teste falhou: retornou uma reserva de outra conta.';
        end if;

        select count(*) into v_quantidade
        from public.consultar_meus_agendamentos() as resultado
        where (resultado.agendamento ->> 'id')::bigint in (v_id_a, v_id_b);

        if v_quantidade <> 1 then
            raise exception 'Teste falhou: esperava somente a fixture da própria conta.';
        end if;

        select resultado.* into strict v_resultado
        from public.consultar_meus_agendamentos() as resultado
        where (resultado.agendamento ->> 'id')::bigint in (v_id_a, v_id_b);

        v_json_esperado := case when v_identidade = v_a
            then current_setting('doesangue_test.json_a')::jsonb
            else current_setting('doesangue_test.json_b')::jsonb end;

        if v_resultado.agendamento is distinct from v_json_esperado
           or v_resultado.nome_unidade is distinct from current_setting('doesangue_test.nome')
           or v_resultado.inicio is distinct from current_setting('doesangue_test.inicio')::timestamptz
           or v_resultado.fuso_horario is distinct from current_setting('doesangue_test.fuso') then
            raise exception 'Teste falhou: campos da reserva/unidade/horário não correspondem.';
        end if;

        -- A vê também sua reserva persistida; B não a recebe.
        select count(*) into v_quantidade
        from public.consultar_meus_agendamentos() as resultado
        where (resultado.agendamento ->> 'id')::bigint = 23;

        if v_quantidade <> (case when v_identidade = v_a then 1 else 0 end) then
            raise exception 'Teste falhou: visibilidade da reserva 23 incorreta.';
        end if;
    end loop;
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

select
    'A vê somente A' as doador_a,
    'B vê somente B' as doador_b,
    'Reserva, unidade, início e fuso conferidos' as formato,
    'Reserva 23 preservada' as original;

rollback;
