-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente dados sintéticos; identidades A/B simuladas administrativamente.
-- ROLLBACK remove ofertas/reserva e perfil B apenas se criado neste teste.
-- Perfis existentes são preservados; sequências podem avançar.
-- Teste sequencial: não verifica concorrência nem JWT pela API.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador_a uuid;
    v_doador_b uuid;
    v_unidade_a bigint;
    v_unidade_b bigint;
    v_inativo bigint;
    v_oculto bigint;
    v_passado bigint;
    v_cheio bigint;
    v_inicio timestamptz := clock_timestamp() + interval '7 days';
begin
    select perfil.id
    into strict v_doador_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select id into strict v_doador_b
    from auth.users where email = 'doador.b@example.com';

    if v_doador_a = v_doador_b then
        raise exception 'Preparação: as contas sintéticas precisam ser distintas.';
    end if;

    insert into public.perfil_doador (id, nome_exibicao)
    values (v_doador_b, 'Doador Demo B')
    on conflict (id) do nothing;

    select id into strict v_unidade_a
    from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;

    select id into strict v_unidade_b
    from public.unidade_coleta
    where nome = 'Unidade Demo B' and not publicada;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_a, v_inicio, 1, false)
    returning id into v_inativo;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_b, v_inicio, 1, true)
    returning id into v_oculto;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_a, clock_timestamp() - interval '1 day', 1, true)
    returning id into v_passado;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_a, v_inicio + interval '1 hour', 1, true)
    returning id into v_cheio;

    perform set_config('doesangue_test.doador_a', v_doador_a::text, true);
    perform set_config('doesangue_test.doador_b', v_doador_b::text, true);
    perform set_config('doesangue_test.inativo', v_inativo::text, true);
    perform set_config('doesangue_test.oculto', v_oculto::text, true);
    perform set_config('doesangue_test.passado', v_passado::text, true);
    perform set_config('doesangue_test.cheio', v_cheio::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_doador_a uuid := current_setting('doesangue_test.doador_a')::uuid;
    v_doador_b uuid := current_setting('doesangue_test.doador_b')::uuid;
    v_cheio bigint := current_setting('doesangue_test.cheio')::bigint;
    v_reserva_b public.agendamento%rowtype;
    v_caso record;
begin
    if current_user <> 'authenticated' then
        raise exception 'Teste falhou: precisa executar como authenticated.';
    end if;

    -- B ocupa a única vaga pela própria função, sem INSERT administrativo.
    perform set_config('request.jwt.claim.sub', v_doador_b::text, true);
    perform set_config(
        'request.jwt.claims',
        json_build_object('sub', v_doador_b, 'role', 'authenticated')::text,
        true
    );
    if auth.uid() is distinct from v_doador_b then
        raise exception 'Teste falhou: identidade B incorreta.';
    end if;

    select resultado.* into strict v_reserva_b
    from public.criar_agendamento(v_cheio, gen_random_uuid()) as resultado;

    if v_reserva_b.doador_id is distinct from v_doador_b
       or v_reserva_b.horario_id is distinct from v_cheio
       or v_reserva_b.estado is distinct from 'agendado' then
        raise exception 'Teste falhou: a reserva de B não corresponde ao pedido.';
    end if;

    -- A não pode ver a reserva de B, mas o servidor deve contar sua ocupação.
    perform set_config('request.jwt.claim.sub', v_doador_a::text, true);
    perform set_config(
        'request.jwt.claims',
        json_build_object('sub', v_doador_a, 'role', 'authenticated')::text,
        true
    );
    if auth.uid() is distinct from v_doador_a then
        raise exception 'Teste falhou: identidade A incorreta.';
    end if;

    if exists (select 1 from public.agendamento where horario_id = v_cheio) then
        raise exception 'Teste falhou: A consegue consultar a reserva privada de B.';
    end if;

    for v_caso in
        select * from (values
            ('inativo', current_setting('doesangue_test.inativo')::bigint,
                'Horário indisponível para agendamento.'),
            ('unidade não publicada', current_setting('doesangue_test.oculto')::bigint,
                'Horário indisponível para agendamento.'),
            ('passado', current_setting('doesangue_test.passado')::bigint,
                'Este horário já passou.'),
            ('cheio', v_cheio,
                'Este horário está sem vagas.')
        ) as casos(nome, horario_id, mensagem)
    loop
        begin
            perform public.criar_agendamento(v_caso.horario_id, gen_random_uuid());
            raise exception 'Teste falhou: horário % aceito.', v_caso.nome;
        exception
            when raise_exception then
                if sqlerrm <> v_caso.mensagem then
                    raise;
                end if;
        end;
    end loop;
end;
$$;

reset role;

do $$
declare
    v_quantidade bigint;
    v_reservas_b bigint;
begin
    select count(*), count(*) filter (
        where horario_id = current_setting('doesangue_test.cheio')::bigint
          and doador_id = current_setting('doesangue_test.doador_b')::uuid
          and estado = 'agendado'
    )
    into v_quantidade, v_reservas_b
    from public.agendamento
    where horario_id in (
        current_setting('doesangue_test.inativo')::bigint,
        current_setting('doesangue_test.oculto')::bigint,
        current_setting('doesangue_test.passado')::bigint,
        current_setting('doesangue_test.cheio')::bigint
    );

    if v_quantidade <> 1 or v_reservas_b <> 1 then
        raise exception 'Teste falhou: somente a reserva ativa de B deveria existir.';
    end if;
end;
$$;

select * from (values
    ('Horário inativo', 'Bloqueado'),
    ('Unidade não publicada', 'Bloqueado'),
    ('Horário passado', 'Bloqueado'),
    ('Única vaga ocupada por B', 'Bloqueado para A')
) as resultados(teste, resultado);

rollback;
