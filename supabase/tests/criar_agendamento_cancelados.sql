-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente dados sintéticos; identidade A simulada, sem login/JWT pela API.
-- O cancelamento é administrativo apenas para preparar este teste.
-- Não existe operação de cancelamento integrada ao aplicativo nesta etapa.
-- ROLLBACK desfaz todos os dados temporários; sequências podem avançar.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador_id uuid;
    v_unidade_id bigint;
    v_horario_id bigint;
begin
    select perfil.id into strict v_doador_id
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select id into strict v_unidade_id
    from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_id, clock_timestamp() + interval '7 days', 1, true)
    returning id into v_horario_id;

    perform set_config('doesangue_test.doador_id', v_doador_id::text, true);
    perform set_config('doesangue_test.horario_id', v_horario_id::text, true);
    perform set_config('doesangue_test.chave', gen_random_uuid()::text, true);
    perform set_config('request.jwt.claim.sub', v_doador_id::text, true);
    perform set_config(
        'request.jwt.claims',
        json_build_object('sub', v_doador_id, 'role', 'authenticated')::text,
        true
    );
end;
$$;

set local role authenticated;

do $$
declare
    v_reserva public.agendamento%rowtype;
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.doador_id')::uuid then
        raise exception 'Teste falhou: papel ou identidade simulada incorretos.';
    end if;

    select resultado.* into strict v_reserva
    from public.criar_agendamento(
        current_setting('doesangue_test.horario_id')::bigint,
        current_setting('doesangue_test.chave')::uuid
    ) as resultado;

    if v_reserva.estado is distinct from 'agendado' then
        raise exception 'Teste falhou: a primeira reserva não está agendada.';
    end if;
    perform set_config('doesangue_test.reserva_id', v_reserva.id::text, true);
end;
$$;

reset role;

-- Altera somente os registros criados nesta transação.
update public.agendamento
set estado = 'cancelado'
where id = current_setting('doesangue_test.reserva_id')::bigint;

-- A recuperação de pedido antigo deve funcionar mesmo após fechar a oferta.
update public.horario_disponivel
set ativo = false, inicio = clock_timestamp() - interval '1 day'
where id = current_setting('doesangue_test.horario_id')::bigint;

set local role authenticated;

do $$
declare
    v_esperada public.agendamento%rowtype;
    v_repetida public.agendamento%rowtype;
begin
    select agendamento.* into strict v_esperada
    from public.agendamento as agendamento
    where id = current_setting('doesangue_test.reserva_id')::bigint;

    if v_esperada.estado is distinct from 'cancelado' then
        raise exception 'Preparação: a reserva deveria estar cancelada.';
    end if;

    select resultado.* into strict v_repetida
    from public.criar_agendamento(
        current_setting('doesangue_test.horario_id')::bigint,
        current_setting('doesangue_test.chave')::uuid
    ) as resultado;

    if v_repetida is distinct from v_esperada then
        raise exception 'Teste falhou: repetir a chave não preservou o registro cancelado.';
    end if;
end;
$$;

reset role;

-- Reabre somente a oferta temporária para testar um novo pedido.
update public.horario_disponivel
set ativo = true, inicio = clock_timestamp() + interval '7 days'
where id = current_setting('doesangue_test.horario_id')::bigint;

set local role authenticated;

do $$
declare
    v_chave uuid := gen_random_uuid();
    v_antigo_id bigint := current_setting('doesangue_test.reserva_id')::bigint;
    v_nova public.agendamento%rowtype;
    v_antiga public.agendamento%rowtype;
begin
    if v_chave = current_setting('doesangue_test.chave')::uuid then
        raise exception 'Preparação: novo pedido precisa de chave diferente.';
    end if;

    select resultado.* into strict v_nova
    from public.criar_agendamento(
        current_setting('doesangue_test.horario_id')::bigint, v_chave
    ) as resultado;

    if v_nova.id is null or v_nova.id = v_antigo_id
       or v_nova.estado is distinct from 'agendado'
       or v_nova.chave_idempotencia is distinct from v_chave then
        raise exception 'Teste falhou: novo pedido não criou outra reserva ativa.';
    end if;

    -- Mesmo após o novo pedido, a chave antiga continua representando o cancelado.
    select resultado.* into strict v_antiga
    from public.criar_agendamento(
        current_setting('doesangue_test.horario_id')::bigint,
        current_setting('doesangue_test.chave')::uuid
    ) as resultado;

    if v_antiga.id is distinct from v_antigo_id
       or v_antiga.estado is distinct from 'cancelado' then
        raise exception 'Teste falhou: o pedido antigo foi substituído ou reativado.';
    end if;
end;
$$;

reset role;

do $$
declare
    v_total bigint;
    v_ativos bigint;
    v_cancelados bigint;
begin
    select count(*), count(*) filter (where estado = 'agendado'),
        count(*) filter (where estado = 'cancelado')
    into v_total, v_ativos, v_cancelados
    from public.agendamento
    where horario_id = current_setting('doesangue_test.horario_id')::bigint;

    if v_total <> 2 or v_ativos <> 1 or v_cancelados <> 1 then
        raise exception 'Teste falhou: esperava um cancelado e somente uma reserva ativa.';
    end if;
end;
$$;

select * from (values
    ('Chave antiga com oferta fechada/passada', 'Retorna o cancelado'),
    ('Novo pedido com nova chave', 'Cria outra reserva ativa'),
    ('Reserva antiga', 'Continua cancelada'),
    ('Capacidade 1', 'Somente uma reserva ativa')
) as resultados(teste, resultado);

rollback;
