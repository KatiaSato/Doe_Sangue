-- Somente conferência em ambiente acadêmico autorizado após a migração.
-- Unidades sintéticas temporárias, sem posições de hemocentros reais.
-- Executar inteiro como administrador; termina em ROLLBACK.
-- Sequências de identidade podem avançar mesmo após ROLLBACK.
begin;

do $$
declare
    v_publica bigint;
    v_oculta bigint;
    v_caso record;
    v_constraint text;
    v_papel text;
    v_comando text;
    v_visiveis integer;
    v_unidades jsonb;
    v_reservas jsonb;
begin
    select coalesce(jsonb_agg(to_jsonb(u) order by id), '[]'::jsonb)
        into v_unidades from public.unidade_coleta u;
    select coalesce(jsonb_agg(to_jsonb(a) order by id), '[]'::jsonb)
        into v_reservas from public.agendamento a;

    insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada)
        values ('Unidade Sintética Coordenadas Pública', 'Cidade Sintética',
            'Endereço Sintético', 'America/Sao_Paulo', true) returning id into v_publica;
    insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada,latitude,longitude)
        values ('Unidade Sintética Coordenadas Oculta', 'Cidade Sintética',
            'Endereço Sintético', 'America/Sao_Paulo', false, -10.123, 20.456)
        returning id into v_oculta;
    if exists (select 1 from public.unidade_coleta where id=v_publica
        and (latitude is not null or longitude is not null)) then
        raise exception 'Campos novos não iniciaram nulos.';
    end if;

    -- Valores válidos incluindo zero e todos os limites inclusivos.
    for v_caso in select * from (values
        (-90.0,-180.0),(-90.0,180.0),(90.0,-180.0),(90.0,180.0),(0.0,0.0)
    ) as pares(lat,lon) loop
        update public.unidade_coleta set latitude=v_caso.lat,longitude=v_caso.lon where id=v_publica;
        if not exists (select 1 from public.unidade_coleta where id=v_publica
            and latitude=v_caso.lat and longitude=v_caso.lon) then
            raise exception 'Par válido não preservado.';
        end if;
    end loop;

    -- Confere a constraint específica, não apenas qualquer erro.
    for v_caso in select * from (values
        (null::double precision,1::double precision,'unidade_coleta_coordenadas_par_check'),
        (1::double precision,null::double precision,'unidade_coleta_coordenadas_par_check'),
        (-90.000001,0,'unidade_coleta_latitude_check'),
        (90.000001,0,'unidade_coleta_latitude_check'),
        (0,-180.000001,'unidade_coleta_longitude_check'),
        (0,180.000001,'unidade_coleta_longitude_check'),
        ('NaN'::double precision,0,'unidade_coleta_latitude_check'),
        (0,'NaN'::double precision,'unidade_coleta_longitude_check'),
        ('Infinity'::double precision,0,'unidade_coleta_latitude_check'),
        ('-Infinity'::double precision,0,'unidade_coleta_latitude_check'),
        (0,'Infinity'::double precision,'unidade_coleta_longitude_check'),
        (0,'-Infinity'::double precision,'unidade_coleta_longitude_check')
    ) as casos(lat,lon,restricao) loop
        begin
            update public.unidade_coleta set latitude=v_caso.lat,longitude=v_caso.lon where id=v_publica;
            raise exception 'Par inválido aceito.';
        exception when check_violation then
            get stacked diagnostics v_constraint = constraint_name;
            if v_constraint is distinct from v_caso.restricao then raise; end if;
        end;
    end loop;
    update public.unidade_coleta set latitude=null,longitude=null where id=v_publica;
    update public.unidade_coleta set latitude=12.345,longitude=-67.89 where id=v_publica;

    if not (select relrowsecurity from pg_class where oid='public.unidade_coleta'::regclass) then
        raise exception 'RLS não está ativo.';
    end if;
    foreach v_papel in array array['anon','authenticated'] loop
        execute format('set local role %I',v_papel);
        select count(*) into v_visiveis from public.unidade_coleta where id in (v_publica,v_oculta);
        if v_visiveis <> 1 or not exists (select 1 from public.unidade_coleta
            where id=v_publica and latitude=12.345 and longitude=-67.89) then
            raise exception 'Leitura/publicação/coordenadas incorretas para %.',v_papel;
        end if;
        foreach v_comando in array array[
            'insert into public.unidade_coleta(nome,cidade,endereco) values (''Teste'',''Teste'',''Teste'')',
            'update public.unidade_coleta set latitude=0,longitude=0',
            'delete from public.unidade_coleta',
            'truncate public.unidade_coleta'
        ] loop
            begin
                execute v_comando;
                raise exception 'Escrita permitida para %.',v_papel;
            exception when insufficient_privilege then null;
            end;
        end loop;
        reset role;
    end loop;

    delete from public.unidade_coleta where id in (v_publica,v_oculta);
    if (select coalesce(jsonb_agg(to_jsonb(u) order by id),'[]'::jsonb) from public.unidade_coleta u)
        is distinct from v_unidades
        or (select coalesce(jsonb_agg(to_jsonb(a) order by id),'[]'::jsonb) from public.agendamento a)
        is distinct from v_reservas then
        raise exception 'Registros originais alterados.';
    end if;
end;
$$;

select 'Par completo/nulo, limites e inválidos conferidos' as integridade,
    'Leitura pública filtrada; escrita bloqueada nos dois papéis' as acesso,
    'Unidades e reservas preservadas' as original;
rollback;
