-- Executar INTEIRO, inicialmente como postgres no desenvolvimento acadêmico.
-- Papéis simulados administrativamente, sem login/JWT/API.
-- Somente campanha sintética temporária em ROLLBACK; preserva originais/23.
-- Sequências podem avançar. Em erro, executar ROLLBACK antes de investigar.
begin;

do $$
declare
    v_unidade bigint;
    v_campanha public.campanha%rowtype;
    v_original jsonb;
begin
    select to_jsonb(reserva) into strict v_original
    from public.agendamento as reserva where id = 23;
    perform set_config('doesangue_test.campanha_reserva_original', v_original::text, true);
    perform set_config('doesangue_test.campanhas_originais', (
        select coalesce(jsonb_agg(to_jsonb(c) order by c.id), '[]'::jsonb)::text
        from public.campanha as c
    ), true);

    select id into strict v_unidade from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;
    insert into public.campanha (unidade_id, titulo, descricao, inicio, fim, fonte, publicada)
    values (v_unidade, 'Teste SQL: escrita negada', 'Campanha sintética temporária.',
        now() + interval '1 day', now() + interval '3 days',
        'Roteiro acadêmico sintético', true)
    returning * into v_campanha;
    perform set_config('doesangue_test.campanha_id', v_campanha.id::text, true);
    perform set_config('doesangue_test.campanha_unidade', v_unidade::text, true);
    perform set_config('doesangue_test.campanha_original', to_jsonb(v_campanha)::text, true);

    -- Comandos fixos do roteiro; nenhum SQL vem de entrada do aplicativo/usuário.
    perform set_config('doesangue_test.campanha_comandos', jsonb_build_array(
        $cmd$insert into public.campanha
            (unidade_id, titulo, descricao, inicio, fim, fonte, publicada)
            values (current_setting('doesangue_test.campanha_unidade')::bigint,
                'Tentativa sintética negada', 'Conteúdo sintético.',
                now() + interval '1 day', now() + interval '2 days',
                'Roteiro acadêmico sintético', true)$cmd$,
        $cmd$update public.campanha set titulo = 'Alteração sintética negada'
            where id = current_setting('doesangue_test.campanha_id')::bigint$cmd$,
        $cmd$delete from public.campanha
            where id = current_setting('doesangue_test.campanha_id')::bigint$cmd$,
        $cmd$truncate table public.campanha$cmd$,
        $cmd$select * from public.campanha_id_seq$cmd$,
        $cmd$select nextval('public.campanha_id_seq'::regclass)$cmd$
    )::text, true);
end;
$$;

set local role anon;
do $$
declare
    v_comando text;
    v_bloqueios integer := 0;
begin
    if current_user <> 'anon' then raise exception 'Papel anon não foi aplicado.'; end if;
    if (select count(*) from public.campanha
        where id = current_setting('doesangue_test.campanha_id')::bigint) <> 1 then
        raise exception 'anon: a campanha publicada precisa ser visível antes das tentativas.';
    end if;
    for v_comando in
        select comando from jsonb_array_elements_text(
            current_setting('doesangue_test.campanha_comandos')::jsonb) as comandos(comando)
    loop
        begin
            execute v_comando;
            raise exception 'anon: operação proibida foi aceita: %', v_comando;
        exception when insufficient_privilege then
            v_bloqueios := v_bloqueios + 1;
        end;
    end loop;
    if v_bloqueios <> 6 then raise exception 'anon: nem todas as seis tentativas foram bloqueadas.'; end if;
end;
$$;
reset role;

set local role authenticated;
do $$
declare
    v_comando text;
    v_bloqueios integer := 0;
begin
    if current_user <> 'authenticated' then
        raise exception 'Papel authenticated não foi aplicado.';
    end if;
    if (select count(*) from public.campanha
        where id = current_setting('doesangue_test.campanha_id')::bigint) <> 1 then
        raise exception 'authenticated: a campanha publicada precisa ser visível antes das tentativas.';
    end if;
    for v_comando in
        select comando from jsonb_array_elements_text(
            current_setting('doesangue_test.campanha_comandos')::jsonb) as comandos(comando)
    loop
        begin
            execute v_comando;
            raise exception 'authenticated: operação proibida foi aceita: %', v_comando;
        exception when insufficient_privilege then
            v_bloqueios := v_bloqueios + 1;
        end;
    end loop;
    if v_bloqueios <> 6 then
        raise exception 'authenticated: nem todas as seis tentativas foram bloqueadas.';
    end if;
end;
$$;
reset role;

do $$
begin
    if (select to_jsonb(c) from public.campanha as c
        where id = current_setting('doesangue_test.campanha_id')::bigint)
       is distinct from current_setting('doesangue_test.campanha_original')::jsonb
       or (select coalesce(jsonb_agg(to_jsonb(c) order by c.id), '[]'::jsonb)
           from public.campanha as c where id <> current_setting('doesangue_test.campanha_id')::bigint)
          is distinct from current_setting('doesangue_test.campanhas_originais')::jsonb
       or (select to_jsonb(reserva) from public.agendamento as reserva where id = 23)
          is distinct from current_setting('doesangue_test.campanha_reserva_original')::jsonb then
        raise exception 'Campanhas ou reserva original 23 foram alteradas.';
    end if;
end;
$$;

select 'Escrita como anon' as teste, 'INSERT/UPDATE/DELETE/TRUNCATE bloqueados' as resultado
union all select 'Escrita como authenticated', 'INSERT/UPDATE/DELETE/TRUNCATE bloqueados'
union all select 'Sequência sob os dois papéis', 'SELECT e nextval bloqueados'
union all select 'Campanhas e reserva 23', 'Preservadas';
rollback;
