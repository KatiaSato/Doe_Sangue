-- Executar como administrador. Somente fixtures sintéticas; finaliza com ROLLBACK.
begin;
do $$
declare
    v_publica bigint; v_oculta bigint; v_outra bigint;
    v_a uuid := gen_random_uuid(); v_b uuid := gen_random_uuid();
    v_mes date := (date_trunc('month', statement_timestamp()) + interval '2 months')::date;
    v_inicio timestamptz; v_fim timestamptz;
    v_livre bigint; v_parcial bigint; v_outro bigint; v_lotado bigint; v_borda bigint;
    v_papel text; v_ids bigint[]; v_dias date[]; v_sql text; v_total integer;
    v_original jsonb; v_unidades jsonb; v_perfis jsonb; v_horarios jsonb; v_reservas jsonb;
begin
    select to_jsonb(a) into strict v_original from public.agendamento a where id=23;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_unidades from public.unidade_coleta t;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_perfis from public.perfil_doador t;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_horarios from public.horario_disponivel t;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_reservas from public.agendamento t;

    insert into auth.users(id,raw_user_meta_data) values
        (v_a,'{"nome_exibicao":"Doador Sintético Disponibilidade A","tipo_sanguineo":"NAO_SEI"}'),
        (v_b,'{"nome_exibicao":"Doador Sintético Disponibilidade B","tipo_sanguineo":"NAO_SEI"}');
    insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada)
        values('Unidade Sintética Disponibilidade','Cidade fictícia','Endereço fictício','America/Sao_Paulo',true)
        returning id into v_publica;
    insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada)
        values('Unidade Sintética Oculta','Cidade fictícia','Endereço fictício','UTC',false)
        returning id into v_oculta;
    insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada)
        values('Outra Unidade Sintética','Cidade fictícia','Endereço fictício','UTC',true)
        returning id into v_outra;
    v_inicio := v_mes::timestamp at time zone 'America/Sao_Paulo';
    v_fim := (v_mes + interval '1 month')::timestamp at time zone 'America/Sao_Paulo';

    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        values(v_publica,v_inicio,1,true) returning id into v_livre;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        values(v_publica,v_inicio+interval '1 day 8 hours',3,true) returning id into v_parcial;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        values(v_publica,v_inicio+interval '1 day 9 hours',2,true) returning id into v_outro;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        values(v_publica,v_inicio+interval '2 days 8 hours',1,true) returning id into v_lotado;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        values(v_publica,v_fim-interval '30 minutes',1,true) returning id into v_borda;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo) values
        (v_publica,v_inicio+interval '3 days',1,false),
        (v_publica,v_fim,1,true),
        -- UTC já no mês consultado, mas ainda no dia anterior na unidade.
        (v_publica,(v_mes::timestamp at time zone 'UTC')+interval '30 minutes',1,true),
        (v_publica,statement_timestamp()-interval '1 minute',1,true),
        (v_publica,statement_timestamp(),1,true),
        (v_oculta,v_inicio,1,true),(v_outra,v_inicio,1,true);
    insert into public.agendamento(doador_id,horario_id,chave_idempotencia,estado) values
        (v_a,v_parcial,gen_random_uuid(),'agendado'),
        (v_b,v_parcial,gen_random_uuid(),'agendado'),
        (v_b,v_parcial,gen_random_uuid(),'cancelado'),
        (v_b,v_lotado,gen_random_uuid(),'agendado');

    perform set_config('request.jwt.claim.sub',v_a::text,true);
    foreach v_papel in array array['anon','authenticated'] loop
        execute format('set local role %I',v_papel);
        select array_agg(h.id order by h.inicio,h.id) into v_ids
            from public.consultar_horarios_disponiveis(v_publica,v_inicio,v_fim) h;
        if v_ids is distinct from array[v_livre,v_parcial,v_outro,v_borda] then
            raise exception 'Filtro/ordenação incorretos em %.',v_papel;
        end if;
        select vagas_disponiveis into v_total from public.consultar_horarios_disponiveis(v_publica,v_inicio,v_fim)
            where id=v_parcial;
        if v_total <> 1 then raise exception 'Não contou B ou descontou cancelado.'; end if;
        if exists(select 1 from public.consultar_horarios_disponiveis(v_oculta,v_inicio,v_fim)) then
            raise exception 'Unidade oculta retornada.';
        end if;
        if exists(select 1 from public.consultar_horarios_disponiveis(9223372036854775807,v_inicio,v_fim)) then
            raise exception 'Unidade inexistente retornada.';
        end if;
        if exists(select 1 from public.consultar_horarios_disponiveis(v_publica,
            statement_timestamp()-interval '1 day',statement_timestamp()+interval '1 day')) then
            raise exception 'Horário passado ou no instante atual retornado.';
        end if;
        select array_agg(d.dia order by d.dia) into v_dias from public.consultar_dias_disponiveis(v_publica,v_mes) d;
        if v_dias is distinct from array[v_mes,v_mes+1,(v_fim at time zone 'America/Sao_Paulo')::date-1] then
            raise exception 'Dias/fuso/bordas incorretos em %: %.',v_papel,v_dias;
        end if;
        if exists(select 1 from public.consultar_dias_disponiveis(v_oculta,v_mes)) then
            raise exception 'Calendário de unidade oculta retornado.';
        end if;
        if v_papel='authenticated' then
            select count(*) into v_total from public.agendamento where horario_id=v_parcial;
            if v_total<>1 then raise exception 'Leitura própria deixou de estar isolada.'; end if;
        else
            begin
                perform 1 from public.agendamento limit 1;
                raise exception 'Visitante leu reservas privadas.';
            exception when insufficient_privilege then null; end;
        end if;
        reset role;
    end loop;

    foreach v_sql in array array[
        format('select * from public.consultar_horarios_disponiveis(null,%L,%L)',v_inicio,v_fim),
        format('select * from public.consultar_horarios_disponiveis(0,%L,%L)',v_inicio,v_fim),
        format('select * from public.consultar_horarios_disponiveis(-1,%L,%L)',v_inicio,v_fim),
        format('select * from public.consultar_horarios_disponiveis(%s,null,%L)',v_publica,v_fim),
        format('select * from public.consultar_horarios_disponiveis(%s,%L,null)',v_publica,v_inicio),
        format('select * from public.consultar_horarios_disponiveis(%s,%L,%L)',v_publica,v_inicio,v_inicio),
        format('select * from public.consultar_horarios_disponiveis(%s,%L,%L)',v_publica,v_fim,v_inicio),
        format('select * from public.consultar_horarios_disponiveis(%s,%L,%L)',v_publica,v_inicio,v_inicio+interval '33 days'),
        format('select * from public.consultar_horarios_disponiveis(%s,''-infinity'',''infinity'')',v_publica),
        format('select * from public.consultar_dias_disponiveis(%s,null)',v_publica),
        format('select * from public.consultar_dias_disponiveis(0,%L)',v_mes),
        format('select * from public.consultar_dias_disponiveis(%s,%L)',v_publica,v_mes+1),
        format('select * from public.consultar_dias_disponiveis(%s,''infinity'')',v_publica)
    ] loop
        begin execute v_sql; raise exception 'Parâmetro inválido aceito: %',v_sql;
        exception when invalid_parameter_value then null; end;
    end loop;

    -- Cancelamento libera vaga na próxima fotografia; não muda regra da reserva.
    update public.agendamento set estado='cancelado' where doador_id=v_b and horario_id=v_parcial and estado='agendado';
    select vagas_disponiveis into v_total from public.consultar_horarios_disponiveis(v_publica,v_inicio,v_fim)
        where id=v_parcial;
    if v_total<>2 then raise exception 'Vaga não liberada após cancelamento.'; end if;

    if exists(select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
        where n.nspname='public' and p.proname in('consultar_horarios_disponiveis','consultar_dias_disponiveis')
        and (not p.prosecdef or p.provolatile<>'s' or p.proconfig is distinct from array['search_path=""'])) then
        raise exception 'Configuração das funções incorreta.';
    end if;

    delete from public.agendamento where doador_id in(v_a,v_b);
    delete from public.horario_disponivel where unidade_id in(v_publica,v_oculta,v_outra);
    delete from public.unidade_coleta where id in(v_publica,v_oculta,v_outra);
    delete from public.perfil_doador where id in(v_a,v_b);
    delete from auth.users where id in(v_a,v_b);
    if (select to_jsonb(a) from public.agendamento a where id=23) is distinct from v_original
       or (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.unidade_coleta t) is distinct from v_unidades
       or (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.horario_disponivel t) is distinct from v_horarios
       or (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.perfil_doador t) is distinct from v_perfis
       or (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.agendamento t) is distinct from v_reservas then
        raise exception 'Dados originais alterados.';
    end if;
end;
$$;
select 'Futuros/ativos/publicados/vagas/ordenação conferidos nos dois papéis' as horarios,
    'Dias locais e bordas do mês conferidos' as calendario,
    'Sem dados de terceiros; cancelamento libera vaga' as acesso,
    'Originais e reserva 23 preservados' as original;
rollback;
