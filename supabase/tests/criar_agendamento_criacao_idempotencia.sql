-- Desenvolvimento acadêmico: somente dados e conta sintéticos.
-- Executar INTEIRO no SQL Editor, inicialmente como postgres.
-- Simula identidade/papel; não realiza login nem verifica JWT pela API.
-- ROLLBACK desfaz o horário e a reserva. Perfis existentes são preservados.
-- As sequências podem avançar mesmo com ROLLBACK.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador_id uuid;
    v_unidade_id bigint;
    v_horario_id bigint;
begin
    select perfil.id
    into strict v_doador_id
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select unidade.id
    into v_unidade_id
    from public.unidade_coleta as unidade
    where unidade.nome = 'Unidade Demo A'
      and unidade.publicada
    order by unidade.id
    limit 1;

    if v_unidade_id is null then
        raise exception 'Preparação: precisa existir a Unidade Demo A publicada.';
    end if;

    -- Oferta temporária futura: não depende da data do seed existente.
    -- Capacidade 1 também verifica que repetir o pedido não exige outra vaga.
    insert into public.horario_disponivel (
        unidade_id, inicio, capacidade, ativo
    )
    values (v_unidade_id, clock_timestamp() + interval '7 days', 1, true)
    returning id into v_horario_id;

    perform set_config('doesangue_test.doador_id', v_doador_id::text, true);
    perform set_config('doesangue_test.horario_id', v_horario_id::text, true);
    perform set_config('doesangue_test.chave', gen_random_uuid()::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_doador_id uuid := current_setting('doesangue_test.doador_id')::uuid;
    v_horario_id bigint := current_setting('doesangue_test.horario_id')::bigint;
    v_chave uuid := current_setting('doesangue_test.chave')::uuid;
    v_primeiro public.agendamento%rowtype;
    v_repetido public.agendamento%rowtype;
    v_quantidade bigint;
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

    select resultado.*
    into strict v_primeiro
    from public.criar_agendamento(v_horario_id, v_chave) as resultado;

    if v_primeiro.id is null
       or v_primeiro.doador_id is distinct from v_doador_id
       or v_primeiro.horario_id is distinct from v_horario_id
       or v_primeiro.estado is distinct from 'agendado'
       or v_primeiro.chave_idempotencia is distinct from v_chave
       or v_primeiro.criado_em is null then
        raise exception 'Teste falhou: registro criado não corresponde ao pedido.';
    end if;

    -- Repete exatamente o mesmo pedido quando a única vaga já está ocupada.
    select resultado.*
    into strict v_repetido
    from public.criar_agendamento(v_horario_id, v_chave) as resultado;

    if v_repetido is distinct from v_primeiro then
        raise exception 'Teste falhou: repetição não devolveu o mesmo registro completo.';
    end if;

    select count(*)
    into v_quantidade
    from public.agendamento
    where horario_id = v_horario_id;

    if v_quantidade <> 1 then
        raise exception 'Teste falhou: esperava exatamente uma reserva visível para A.';
    end if;
end;
$$;

reset role;

-- Confere como administrador para não ocultar eventual linha de outro doador.
do $$
begin
    if (
        select count(*)
        from public.agendamento
        where horario_id = current_setting('doesangue_test.horario_id')::bigint
    ) <> 1 then
        raise exception 'Teste falhou: contagem administrativa diferente de uma reserva.';
    end if;
end;
$$;

select
    'Reserva criada corretamente' as criacao,
    'Mesmo pedido retorna o mesmo registro' as repeticao,
    'Somente uma reserva' as quantidade;

rollback;
