-- Executar INTEIRO como postgres no desenvolvimento acadêmico.
-- Somente dados sintéticos; unidade/campanha temporárias em ROLLBACK.
-- Não altera unidades/campanhas existentes nem a reserva 23.
-- Sequências podem avançar. Em erro, executar ROLLBACK antes de repetir.
begin;

do $$
declare
    v_unidade bigint;
    v_original public.campanha%rowtype;
    v_dados jsonb;
    v_caso record;
    v_coluna text;
    v_codigo text;
    v_constraint text;
    v_bloqueado boolean;
    v_checks integer := 0;
    v_nulos integer := 0;
    v_inicio timestamptz := now() + interval '1 day';
    v_reserva_original jsonb;
    v_campanhas_originais jsonb;
    v_unidades_originais jsonb;
begin
    select to_jsonb(reserva) into strict v_reserva_original
    from public.agendamento as reserva where id = 23;
    select coalesce(jsonb_agg(to_jsonb(c) order by c.id), '[]'::jsonb)
    into v_campanhas_originais from public.campanha as c;
    select coalesce(jsonb_agg(to_jsonb(u) order by u.id), '[]'::jsonb)
    into v_unidades_originais from public.unidade_coleta as u;

    -- Unidade nova: a rejeição de DELETE deve vir da FK da campanha,
    -- não de horários/reservas das unidades originais.
    insert into public.unidade_coleta (nome, cidade, endereco, fuso_horario)
    values ('Unidade Temporária de Integridade de Campanhas',
        'Cidade Sintética', 'Endereço Sintético de Teste', 'America/Sao_Paulo')
    returning id into v_unidade;

    insert into public.campanha (unidade_id, titulo, descricao, inicio, fim, fonte)
    values (v_unidade, 'Campanha Temporária de Integridade',
        'Conteúdo sintético para testar constraints.', v_inicio,
        v_inicio + interval '2 days', 'Roteiro acadêmico sintético')
    returning * into v_original;
    if v_original.id is null or v_original.id <= 0
       or v_original.publicada is distinct from false
       or v_original.criado_em is distinct from now() then
        raise exception 'Defaults: ID/publicação/criação diferentes do esperado.';
    end if;

    if exists (select 1 from public.unidade_coleta where id = 9223372036854775807) then
        raise exception 'Preparação: ID reservado ao teste de FK já existe.';
    end if;

    -- Mesma base válida em cada tentativa, alterando somente o campo do caso.
    for v_caso in
        select * from (values
            ('Título vazio', jsonb_build_object('titulo', ''), '23514', 'campanha_titulo_check', null::text),
            ('Título com espaços', jsonb_build_object('titulo', '   '), '23514', 'campanha_titulo_check', null::text),
            ('Descrição vazia', jsonb_build_object('descricao', ''), '23514', 'campanha_descricao_check', null::text),
            ('Descrição com espaços', jsonb_build_object('descricao', '   '), '23514', 'campanha_descricao_check', null::text),
            ('Fonte vazia', jsonb_build_object('fonte', ''), '23514', 'campanha_fonte_check', null::text),
            ('Fonte com espaços', jsonb_build_object('fonte', '   '), '23514', 'campanha_fonte_check', null::text),
            ('Fim igual ao início', jsonb_build_object('fim', v_inicio), '23514', 'campanha_periodo_check', null::text),
            ('Fim anterior ao início', jsonb_build_object('fim', v_inicio - interval '1 second'), '23514', 'campanha_periodo_check', null::text),
            ('Unidade inexistente', jsonb_build_object('unidade_id', 9223372036854775807::bigint), '23503', 'campanha_unidade_id_fkey', null::text)
        ) as casos(rotulo, alteracoes, codigo, restricao, coluna)
        union all
        select campo || ' NULL', jsonb_build_object(campo, null::text), '23502', null::text, campo
        from unnest(array['unidade_id', 'titulo', 'descricao', 'inicio', 'fim',
            'fonte', 'publicada', 'criado_em']) as campos(campo)
    loop
        v_dados := to_jsonb(v_original) || v_caso.alteracoes;
        v_bloqueado := false;
        begin
            -- Omite id para usar a identidade, sem mascarar o erro do campo testado.
            insert into public.campanha
                (unidade_id, titulo, descricao, inicio, fim, fonte, publicada, criado_em)
            select unidade_id, titulo, descricao, inicio, fim, fonte, publicada, criado_em
            from jsonb_populate_record(null::public.campanha, v_dados);
        exception when others then
            get stacked diagnostics v_codigo = returned_sqlstate,
                v_constraint = constraint_name, v_coluna = column_name;
            if v_codigo is distinct from v_caso.codigo
               or (v_caso.restricao is not null and v_constraint is distinct from v_caso.restricao)
               or (v_caso.coluna is not null and v_coluna is distinct from v_caso.coluna) then
                raise;
            end if;
            v_bloqueado := true;
        end;
        if not v_bloqueado then
            raise exception 'Caso inválido foi aceito: %', v_caso.rotulo;
        end if;
        if v_caso.codigo = '23514' then v_checks := v_checks + 1; end if;
        if v_caso.codigo = '23502' then v_nulos := v_nulos + 1; end if;
    end loop;

    -- Para testar NOT NULL do id, permite explicitamente fornecer a identidade.
    begin
        insert into public.campanha overriding system value
        select * from jsonb_populate_record(null::public.campanha,
            jsonb_set(to_jsonb(v_original), '{id}', 'null'::jsonb));
        raise exception 'ID NULL deveria ser bloqueado.';
    exception when not_null_violation then
        get stacked diagnostics v_coluna = column_name;
        if v_coluna is distinct from 'id' then raise; end if;
        v_nulos := v_nulos + 1;
    end;

    -- Sem OVERRIDING, GENERATED ALWAYS deve impedir ID explícito.
    begin
        insert into public.campanha
        select * from jsonb_populate_record(null::public.campanha, to_jsonb(v_original));
        raise exception 'ID explícito sem OVERRIDING deveria ser bloqueado.';
    exception when sqlstate '428C9' then null;
    end;

    -- Mesmo com OVERRIDING administrativo, PK não aceita ID duplicado.
    begin
        insert into public.campanha overriding system value
        select * from jsonb_populate_record(null::public.campanha, to_jsonb(v_original));
        raise exception 'ID duplicado deveria ser bloqueado.';
    exception when unique_violation then
        get stacked diagnostics v_constraint = constraint_name;
        if v_constraint is distinct from 'campanha_pkey' then raise; end if;
    end;

    begin
        delete from public.unidade_coleta where id = v_unidade;
        raise exception 'Excluir unidade com campanha deveria ser bloqueado.';
    exception when foreign_key_violation then
        get stacked diagnostics v_constraint = constraint_name;
        if v_constraint is distinct from 'campanha_unidade_id_fkey' then raise; end if;
    end;

    if v_checks <> 8 or v_nulos <> 9 then
        raise exception 'Nem todos os casos de CHECK/NOT NULL foram verificados.';
    end if;
    if (select to_jsonb(c) from public.campanha as c where id = v_original.id)
       is distinct from to_jsonb(v_original)
       or (select count(*) from public.campanha where unidade_id = v_unidade) <> 1 then
        raise exception 'Campanha válida foi alterada ou uma tentativa inválida persistiu.';
    end if;
    if (select coalesce(jsonb_agg(to_jsonb(c) order by c.id), '[]'::jsonb)
        from public.campanha as c where id <> v_original.id)
       is distinct from v_campanhas_originais
       or (select coalesce(jsonb_agg(to_jsonb(u) order by u.id), '[]'::jsonb)
           from public.unidade_coleta as u where id <> v_unidade)
          is distinct from v_unidades_originais
       or (select to_jsonb(reserva) from public.agendamento as reserva where id = 23)
          is distinct from v_reserva_original then
        raise exception 'Registros originais foram alterados.';
    end if;
end;
$$;

select 'Defaults de ID, publicação e criação conferidos' as teste, 'Correto' as resultado
union all select 'Título, descrição e fonte vazios ou com espaços', 'Bloqueados'
union all select 'Fim igual ou anterior ao início', 'Bloqueado'
union all select 'NULL nas nove colunas', 'Bloqueado'
union all select 'Unidade inexistente e exclusão de unidade referenciada', 'Bloqueadas'
union all select 'ID explícito sem OVERRIDING e ID duplicado', 'Bloqueados'
union all select 'Campanhas, unidades existentes e reserva 23', 'Preservadas';
rollback;
