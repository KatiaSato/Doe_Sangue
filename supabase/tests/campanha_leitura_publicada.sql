-- Desenvolvimento acadêmico: executar INTEIRO, inicialmente como postgres.
-- Cria três campanhas temporárias e testa SELECT direto e JOIN sob os dois papéis.
-- Simulação administrativa de papéis: não realiza login nem verifica JWT/API.
-- Não altera unidades, perfis, horários ou reservas. Preserva a reserva 23.
-- ROLLBACK remove as campanhas de teste; a sequência de IDs pode avançar.
-- Em erro, executar ROLLBACK antes de investigar/repetir.
begin;

do $$
declare
    v_unidade_publicada bigint;
    v_unidade_oculta bigint;
    v_visivel bigint;
    v_rascunho bigint;
    v_unidade_nao_publicada bigint;
    v_original jsonb;
begin
    select to_jsonb(reserva) into strict v_original
    from public.agendamento as reserva where id = 23;
    perform set_config('doesangue_test.campanha_reserva_original', v_original::text, true);
    perform set_config('doesangue_test.campanhas_originais', (
        select coalesce(jsonb_agg(to_jsonb(campanha) order by campanha.id), '[]'::jsonb)::text
        from public.campanha as campanha
    ), true);

    select id into strict v_unidade_publicada from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;
    select id into strict v_unidade_oculta from public.unidade_coleta
    where nome = 'Unidade Demo B' and not publicada;

    insert into public.campanha (unidade_id, titulo, descricao, inicio, fim, fonte, publicada)
    values (v_unidade_publicada, 'Teste SQL: campanha visível',
        'Campanha sintética publicada de unidade publicada.',
        now() + interval '1 day', now() + interval '3 days',
        'Roteiro acadêmico sintético', true)
    returning id into v_visivel;

    insert into public.campanha (unidade_id, titulo, descricao, inicio, fim, fonte, publicada)
    values (v_unidade_publicada, 'Teste SQL: rascunho oculto',
        'Campanha sintética ainda não publicada.',
        now() + interval '1 day', now() + interval '3 days',
        'Roteiro acadêmico sintético', false)
    returning id into v_rascunho;

    insert into public.campanha (unidade_id, titulo, descricao, inicio, fim, fonte, publicada)
    values (v_unidade_oculta, 'Teste SQL: unidade oculta',
        'Campanha sintética publicada de unidade não publicada.',
        now() + interval '1 day', now() + interval '3 days',
        'Roteiro acadêmico sintético', true)
    returning id into v_unidade_nao_publicada;

    perform set_config('doesangue_test.campanha_visivel', v_visivel::text, true);
    perform set_config('doesangue_test.campanha_ids',
        array[v_visivel, v_rascunho, v_unidade_nao_publicada]::text, true);
end;
$$;

set local role anon;
do $$
declare
    v_ids bigint[];
    v_esperado bigint[] := array[current_setting('doesangue_test.campanha_visivel')::bigint];
begin
    if current_user <> 'anon' then raise exception 'Papel anon não foi aplicado.'; end if;
    -- Sem filtro de publicação no cliente: a RLS deve ocultar duas das três linhas.
    select array_agg(id order by id) into v_ids
    from public.campanha
    where id = any(current_setting('doesangue_test.campanha_ids')::bigint[]);
    if v_ids is distinct from v_esperado then
        raise exception 'anon: leitura direta não filtrou as três campanhas corretamente.';
    end if;
    select array_agg(campanha.id order by campanha.id) into v_ids
    from public.campanha as campanha
    join public.unidade_coleta as unidade on unidade.id = campanha.unidade_id
    where campanha.id = any(current_setting('doesangue_test.campanha_ids')::bigint[]);
    if v_ids is distinct from v_esperado then
        raise exception 'anon: JOIN não retornou somente a campanha permitida.';
    end if;
end;
$$;
reset role;

set local role authenticated;
do $$
declare
    v_ids bigint[];
    v_esperado bigint[] := array[current_setting('doesangue_test.campanha_visivel')::bigint];
begin
    if current_user <> 'authenticated' then
        raise exception 'Papel authenticated não foi aplicado.';
    end if;
    select array_agg(id order by id) into v_ids
    from public.campanha
    where id = any(current_setting('doesangue_test.campanha_ids')::bigint[]);
    if v_ids is distinct from v_esperado then
        raise exception 'authenticated: leitura direta não filtrou as três campanhas corretamente.';
    end if;
    select array_agg(campanha.id order by campanha.id) into v_ids
    from public.campanha as campanha
    join public.unidade_coleta as unidade on unidade.id = campanha.unidade_id
    where campanha.id = any(current_setting('doesangue_test.campanha_ids')::bigint[]);
    if v_ids is distinct from v_esperado then
        raise exception 'authenticated: JOIN não retornou somente a campanha permitida.';
    end if;
end;
$$;
reset role;

do $$
declare
    v_originais jsonb;
begin
    select coalesce(jsonb_agg(to_jsonb(campanha) order by campanha.id), '[]'::jsonb)
    into v_originais from public.campanha as campanha
    where not (campanha.id = any(current_setting('doesangue_test.campanha_ids')::bigint[]));
    if v_originais is distinct from current_setting('doesangue_test.campanhas_originais')::jsonb
       or (select count(*) from public.campanha
           where id = any(current_setting('doesangue_test.campanha_ids')::bigint[])) <> 3 then
        raise exception 'Campanhas originais ou preparação temporária foram alteradas.';
    end if;
    if (select to_jsonb(reserva) from public.agendamento as reserva where id = 23)
       is distinct from current_setting('doesangue_test.campanha_reserva_original')::jsonb then
        raise exception 'Reserva original 23 foi alterada.';
    end if;
end;
$$;

select
    'Somente campanha publicada de unidade publicada' as leitura_anon,
    'Somente campanha publicada de unidade publicada' as leitura_authenticated,
    'Mesmo resultado na leitura direta e no JOIN' as formato,
    'Campanhas existentes e reserva 23 preservadas' as original;
rollback;
