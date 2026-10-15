-- Executar INTEIRO como postgres após a nova migração. Dados sintéticos em ROLLBACK.
-- Simula papéis, sem login/JWT/API. Não cria contas Auth nem altera unidades/perfis/23.
-- IDs de campanhas podem avançar mesmo com ROLLBACK. Em erro: ROLLBACK e investigar.
begin;
do $$
declare
    v_publicada bigint; v_oculta bigint; v_id bigint; v_ids bigint[] := '{}';
    v_item record;
begin
    perform set_config('doesangue_test.tipo_original',
        (select to_jsonb(a)::text from public.agendamento a where id = 23), true);
    if current_setting('doesangue_test.tipo_original', true) is null then
        raise exception 'Reserva original 23 ausente: interromper roteiro.';
    end if;
    select id into strict v_publicada from public.unidade_coleta where nome = 'Unidade Demo A' and publicada;
    select id into strict v_oculta from public.unidade_coleta where nome = 'Unidade Demo B' and not publicada;
    for v_item in select * from (values
        (v_publicada, 'Geral', true, null::public.tipo_sanguineo),
        (v_publicada, 'O negativo', true, 'O-'::public.tipo_sanguineo),
        (v_publicada, 'A positivo', true, 'A+'::public.tipo_sanguineo),
        (v_publicada, 'Rascunho', false, 'O-'::public.tipo_sanguineo),
        (v_oculta, 'Unidade oculta', true, 'O-'::public.tipo_sanguineo)
    ) as x(unidade, titulo, publicada, tipo)
    loop
        insert into public.campanha(unidade_id, titulo, descricao, inicio, fim, fonte, publicada)
        values(v_item.unidade, 'Teste tipo: ' || v_item.titulo, 'Conteúdo sintético',
            now() + interval '1 day', now() + interval '2 days', 'Roteiro tipo sanguíneo', v_item.publicada)
        returning id into v_id;
        v_ids := array_append(v_ids, v_id);
        if v_item.tipo is not null then
            insert into public.campanha_tipo_sanguineo values(v_id, v_item.tipo);
        end if;
    end loop;
    -- Campanha dirigida a mais de um tipo, sem criar duplicação no retorno.
    insert into public.campanha_tipo_sanguineo values(v_ids[2], 'B-');
    perform set_config('doesangue_test.tipo_ids', v_ids::text, true);
    begin
        insert into public.campanha_tipo_sanguineo values(v_ids[1], 'NAO_SEI');
        raise exception 'NAO_SEI aceito como alvo de campanha.';
    exception when check_violation then null; end;
    begin
        insert into public.campanha_tipo_sanguineo values(v_ids[2], 'O-');
        raise exception 'Vínculo duplicado aceito.';
    exception when unique_violation then null; end;
    begin
        insert into public.campanha_tipo_sanguineo values(-1, 'O-');
        raise exception 'FK inexistente aceita.';
    exception when foreign_key_violation then null; end;
end;
$$;

-- Mesmo bloco de asserções sob os dois papéis, sem função privilegiada de teste.
set local role anon;
do $$
declare
    v_ids bigint[] := current_setting('doesangue_test.tipo_ids')::bigint[];
    v_tipo public.tipo_sanguineo; v_recebidos bigint[]; v_esperados bigint[];
begin
    foreach v_tipo in array array[null, 'O-', 'A+', 'B-', 'AB+']::public.tipo_sanguineo[] loop
        v_esperados := case when v_tipo is null then v_ids[1:3]
            when v_tipo in ('O-', 'B-') then v_ids[1:2]
            when v_tipo = 'A+' then array[v_ids[1], v_ids[3]] else array[v_ids[1]] end;
        select array_agg(id order by id) into v_recebidos
        from public.consultar_campanhas(v_tipo) where id = any(v_ids);
        if v_recebidos is distinct from v_esperados then
            raise exception 'anon: filtro % incorreto: %', v_tipo, v_recebidos;
        end if;
    end loop;
    if exists(select 1 from public.campanha_tipo_sanguineo where campanha_id = any(v_ids[4:5])) then
        raise exception 'anon: tipos de campanhas ocultas expostos.';
    end if;
    if (select tipos_sanguineos from public.consultar_campanhas() where id = v_ids[2])
        is distinct from array['B-', 'O-']::text[] then
        raise exception 'anon: conjunto de tipos incorreto.';
    end if;
    begin perform public.consultar_campanhas('NAO_SEI');
        raise exception 'Filtro desconhecido aceito.';
    exception when invalid_parameter_value then null; end;
    begin insert into public.campanha_tipo_sanguineo values(v_ids[1], 'A-');
        raise exception 'anon: escrita aceita.';
    exception when insufficient_privilege then null; end;
    begin update public.campanha_tipo_sanguineo set tipo_sanguineo = 'A-';
        raise exception 'anon: UPDATE aceito.';
    exception when insufficient_privilege then null; end;
    begin delete from public.campanha_tipo_sanguineo;
        raise exception 'anon: DELETE aceito.';
    exception when insufficient_privilege then null; end;
    begin truncate public.campanha_tipo_sanguineo;
        raise exception 'anon: TRUNCATE aceito.';
    exception when insufficient_privilege then null; end;
end;
$$;
reset role;
set local role authenticated;
do $$
declare
    v_ids bigint[] := current_setting('doesangue_test.tipo_ids')::bigint[];
    v_tipo public.tipo_sanguineo; v_recebidos bigint[]; v_esperados bigint[];
begin
    foreach v_tipo in array array[null, 'O-', 'A+', 'B-', 'AB+']::public.tipo_sanguineo[] loop
        v_esperados := case when v_tipo is null then v_ids[1:3]
            when v_tipo in ('O-', 'B-') then v_ids[1:2]
            when v_tipo = 'A+' then array[v_ids[1], v_ids[3]] else array[v_ids[1]] end;
        select array_agg(id order by id) into v_recebidos
        from public.consultar_campanhas(v_tipo) where id = any(v_ids);
        if v_recebidos is distinct from v_esperados then
            raise exception 'authenticated: filtro % incorreto: %', v_tipo, v_recebidos;
        end if;
    end loop;
    if exists(select 1 from public.campanha_tipo_sanguineo where campanha_id = any(v_ids[4:5])) then
        raise exception 'authenticated: tipos de campanhas ocultas expostos.';
    end if;
    begin insert into public.campanha_tipo_sanguineo values(v_ids[1], 'A-');
        raise exception 'authenticated: escrita aceita.';
    exception when insufficient_privilege then null; end;
    begin update public.campanha_tipo_sanguineo set tipo_sanguineo = 'A-';
        raise exception 'authenticated: UPDATE aceito.';
    exception when insufficient_privilege then null; end;
    begin delete from public.campanha_tipo_sanguineo;
        raise exception 'authenticated: DELETE aceito.';
    exception when insufficient_privilege then null; end;
    begin truncate public.campanha_tipo_sanguineo;
        raise exception 'authenticated: TRUNCATE aceito.';
    exception when insufficient_privilege then null; end;
end;
$$;
reset role;
do $$
begin
    if (select to_jsonb(a) from public.agendamento a where id = 23)
        is distinct from current_setting('doesangue_test.tipo_original')::jsonb then
        raise exception 'Reserva 23 alterada.';
    end if;
end;
$$;
select 'Gerais e tipos exatos conferidos nos dois papéis' as filtro,
    'Campanhas ocultas continuam ocultas' as rls,
    'Escrita direta bloqueada' as permissoes, 'Reserva 23 preservada' as original;
rollback;
