-- Desenvolvimento acadêmico: somente contas/dados sintéticos.
-- Executar o arquivo INTEIRO no SQL Editor, inicialmente como postgres.
-- Simulação administrativa de identidade; não realiza login nem verifica JWT.
-- Referência: https://supabase.com/docs/guides/database/postgres/row-level-security
-- ROLLBACK remove os registros de teste e o perfil B, se criado neste teste.
-- Perfis existentes são preservados; a sequência de IDs pode avançar.
begin;

-- Prepara os registros como administrador, antes de trocar o papel.
do $$
declare
    doador_a uuid;
    doador_b uuid;
    horario_teste bigint;
    agendamento_a bigint;
    agendamento_b bigint;
begin
    select perfil.id
    into doador_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select id
    into doador_b
    from auth.users
    where email = 'doador.b@example.com';

    if doador_a is null or doador_b is null or doador_a = doador_b then
        raise exception 'Preparação: perfil A e duas contas sintéticas distintas precisam existir.';
    end if;

    -- Cria B apenas nesta transação, se não houver perfil; não altera o existente.
    insert into public.perfil_doador (id, nome_exibicao)
    values (doador_b, 'Doador Demo B')
    on conflict (id) do nothing;

    select horario.id
    into horario_teste
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
    where unidade.nome = 'Unidade Demo A'
      and unidade.publicada
      and horario.ativo
    order by horario.id
    limit 1;

    if horario_teste is null then
        raise exception 'Preparação: precisa existir um horário ativo/publicado da Demo A.';
    end if;

    -- Cancelados isolam a leitura da regra de duplicidade ativa/capacidade.
    insert into public.agendamento (
        doador_id, horario_id, estado, chave_idempotencia
    )
    values (doador_a, horario_teste, 'cancelado', gen_random_uuid())
    returning id into agendamento_a;

    insert into public.agendamento (
        doador_id, horario_id, estado, chave_idempotencia
    )
    values (doador_b, horario_teste, 'cancelado', gen_random_uuid())
    returning id into agendamento_b;

    -- Transporta somente IDs sintéticos entre os blocos desta transação.
    perform set_config('doesangue_test.doador_a', doador_a::text, true);
    perform set_config('doesangue_test.doador_b', doador_b::text, true);
    perform set_config('doesangue_test.agendamento_a', agendamento_a::text, true);
    perform set_config('doesangue_test.agendamento_b', agendamento_b::text, true);
end;
$$;

-- A leitura precisa ocorrer como authenticated, sujeito às policies.
set local role authenticated;

do $$
declare
    doador_a uuid := current_setting('doesangue_test.doador_a')::uuid;
    doador_b uuid := current_setting('doesangue_test.doador_b')::uuid;
    agendamento_a bigint := current_setting('doesangue_test.agendamento_a')::bigint;
    agendamento_b bigint := current_setting('doesangue_test.agendamento_b')::bigint;
    identidade uuid;
    visiveis bigint;
    proprios bigint;
begin
    if current_user <> 'authenticated' then
        raise exception 'Teste falhou: consulta precisa usar o papel authenticated.';
    end if;

    -- Executa a mesma verificação para A e B, trocando a identidade simulada.
    for identidade in select unnest(array[doador_a, doador_b]) loop
        perform set_config('request.jwt.claim.sub', identidade::text, true);
        perform set_config(
            'request.jwt.claims',
            json_build_object('sub', identidade, 'role', 'authenticated')::text,
            true
        );

        if auth.uid() is distinct from identidade then
            raise exception 'Teste falhou: auth.uid() não corresponde à identidade simulada.';
        end if;

        -- O filtro usa os dois IDs, sem restringir por doador no cliente.
        -- Portanto, é a RLS que deve ocultar o registro da outra identidade.
        select
            count(*),
            count(*) filter (where doador_id = identidade)
        into visiveis, proprios
        from public.agendamento
        where id in (agendamento_a, agendamento_b);

        if visiveis <> 1 or proprios <> 1 then
            raise exception 'Teste falhou: cada identidade deve ver somente seu próprio registro.';
        end if;
    end loop;

    -- Prepara o cenário sem usuário autenticado para o próximo bloco.
    perform set_config('request.jwt.claim.sub', '', true);
    perform set_config('request.jwt.claims', '{"role":"anon"}', true);
end;
$$;

set local role anon;

do $$
begin
    if current_user <> 'anon' or auth.uid() is not null then
        raise exception 'Teste falhou: cenário anon precisa estar sem identidade.';
    end if;

    begin
        perform 1 from public.agendamento limit 1;
        raise exception 'Teste falhou: anon conseguiu consultar agendamentos.';
    exception
        when insufficient_privilege then
            null; -- Resultado esperado: anon não tem SELECT na tabela.
    end;
end;
$$;

reset role;

select
    'A vê somente A' as doador_a,
    'B vê somente B' as doador_b,
    'Consulta bloqueada' as visitante;

rollback;
