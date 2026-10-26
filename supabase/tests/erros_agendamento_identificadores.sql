-- Executar inteiro como administrador no projeto acadêmico, após autorização específica.
-- Somente fixtures do domínio; não cria/altera contas Auth ou perfis. Tudo termina em ROLLBACK.
begin;
set transaction isolation level read committed;

create function pg_temp.expect_app_error(p_sql text, p_state text, p_identifier text)
returns void language plpgsql as $$
declare v_state text; v_detail text; v_failed boolean := false;
begin
    begin execute p_sql;
    exception when others then
        v_failed := true;
        get stacked diagnostics v_state = returned_sqlstate, v_detail = pg_exception_detail;
        if v_state <> p_state or v_detail is distinct from 'doesangue.' || p_identifier || '.v1' then
            raise exception 'Identificador ou SQLSTATE divergente no teste.';
        end if;
    end;
    if not v_failed then raise exception 'O teste esperava uma rejeição.'; end if;
end;
$$;

do $$
declare
    v_a uuid; v_b uuid; v_missing uuid := gen_random_uuid();
    v_unit bigint; v_free bigint; v_full bigint; v_past bigint; v_inactive bigint;
    v_foreign bigint; v_old bigint; v_key uuid := gen_random_uuid();
    v_created public.agendamento%rowtype; v_cancelled public.agendamento%rowtype;
    v_original jsonb; v_units jsonb; v_slots jsonb; v_profiles jsonb; v_bookings jsonb; v_events jsonb;
begin
    select to_jsonb(a), a.doador_id into strict v_original, v_a from public.agendamento a where id=23;
    select id into strict v_b from public.perfil_doador where id<>v_a order by id limit 1;
    if exists(select 1 from auth.users where id=v_missing) then raise exception 'UUID temporário já existe.'; end if;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_units from public.unidade_coleta t;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_slots from public.horario_disponivel t;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_profiles from public.perfil_doador t;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_bookings from public.agendamento t;
    select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') into v_events from public.agendamento_cancelamento t;

    insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada)
    values('Unidade Sintética F19','Cidade fictícia','Endereço fictício','America/Sao_Paulo',true) returning id into v_unit;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(v_unit,statement_timestamp()+interval '3 days',3,true) returning id into v_free;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(v_unit,statement_timestamp()+interval '4 days',1,true) returning id into v_full;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(v_unit,statement_timestamp()-interval '1 day',2,true) returning id into v_past;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(v_unit,statement_timestamp()+interval '5 days',2,false) returning id into v_inactive;
    insert into public.agendamento(doador_id,horario_id,chave_idempotencia)
    values(v_b,v_full,gen_random_uuid()) returning id into v_foreign;
    insert into public.agendamento(doador_id,horario_id,chave_idempotencia)
    values(v_a,v_past,gen_random_uuid()) returning id into v_old;

    set local role authenticated;
    perform set_config('request.jwt.claim.sub','',true);
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,%L::uuid)',v_free,v_key),'P0001','session_required');
    perform pg_temp.expect_app_error(format('select public.cancelar_agendamento(%s)',v_old),'42501','session_required');
    perform set_config('request.jwt.claim.sub',v_missing::text,true);
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,%L::uuid)',v_free,v_key),'P0001','profile_required');
    perform pg_temp.expect_app_error(format('select public.cancelar_agendamento(%s)',v_old),'42501','profile_required');
    perform set_config('request.jwt.claim.sub',v_a::text,true);
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(null,%L::uuid)',v_key),'P0001','invalid_request');
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,null)',v_free),'P0001','invalid_request');
    perform pg_temp.expect_app_error('select public.cancelar_agendamento(null)','22023','invalid_request');
    perform pg_temp.expect_app_error('select public.cancelar_agendamento(0)','22023','invalid_request');
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,%L::uuid)',v_inactive,v_key),'P0001','slot_unavailable');
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(9223372036854775807,%L::uuid)',v_key),'P0001','slot_unavailable');
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,%L::uuid)',v_past,v_key),'P0001','slot_started');
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,%L::uuid)',v_full,v_key),'P0001','slot_full');
    select * into strict v_created from public.criar_agendamento(v_free,v_key);
    if (select id from public.criar_agendamento(v_free,v_key))<>v_created.id then raise exception 'Idempotência mudou.'; end if;
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,%L::uuid)',v_full,v_key),'P0001','request_conflict');
    perform pg_temp.expect_app_error(format('select public.criar_agendamento(%s,%L::uuid)',v_free,gen_random_uuid()),'P0001','already_booked');
    perform pg_temp.expect_app_error(format('select public.cancelar_agendamento(%s)',v_old),'P0001','cancellation_closed');
    -- Mesmo identificador para inexistente e reserva alheia: não revela existência/dono.
    perform pg_temp.expect_app_error(format('select public.cancelar_agendamento(%s)',v_foreign),'42501','appointment_unavailable');
    perform pg_temp.expect_app_error('select public.cancelar_agendamento(9223372036854775807)','42501','appointment_unavailable');
    select * into strict v_cancelled from public.cancelar_agendamento(v_created.id);
    if v_cancelled.estado<>'cancelado' or (select id from public.cancelar_agendamento(v_created.id))<>v_created.id then
        raise exception 'Cancelamento/repetição mudou.';
    end if;
    reset role;
    if (select count(*) from public.agendamento_cancelamento where agendamento_id=v_created.id)<>1 then
        raise exception 'Evento duplicado/ausente.';
    end if;
    delete from public.agendamento_cancelamento where agendamento_id=v_created.id;
    delete from public.agendamento where horario_id in (v_free,v_full,v_past,v_inactive);
    delete from public.horario_disponivel where unidade_id=v_unit;
    delete from public.unidade_coleta where id=v_unit;
    if v_units is distinct from (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.unidade_coleta t)
       or v_slots is distinct from (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.horario_disponivel t)
       or v_profiles is distinct from (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.perfil_doador t)
       or v_bookings is distinct from (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.agendamento t)
       or v_events is distinct from (select coalesce(jsonb_agg(to_jsonb(t) order by id),'[]') from public.agendamento_cancelamento t)
       or v_original is distinct from (select to_jsonb(a) from public.agendamento a where id=23) then
        raise exception 'Dados anteriores divergentes.';
    end if;
end;
$$;
select 'Causas identificadas; idempotência, privacidade e reserva23 preservadas' as resultado;
rollback;

begin;
set transaction isolation level repeatable read;
do $$
declare v_detail text;
begin
    perform set_config('request.jwt.claim.sub',(select doador_id::text from public.agendamento where id=23),true);
    begin perform public.criar_agendamento(1,gen_random_uuid()); raise exception 'Esperava rejeição';
    exception when raise_exception then
        get stacked diagnostics v_detail=pg_exception_detail;
        if v_detail is distinct from 'doesangue.isolation_unsupported.v1' then raise exception 'Identificador divergente'; end if;
    end;
    begin perform public.cancelar_agendamento(23); raise exception 'Esperava rejeição';
    exception when sqlstate '25000' then
        get stacked diagnostics v_detail=pg_exception_detail;
        if v_detail is distinct from 'doesangue.isolation_unsupported.v1' then raise exception 'Identificador divergente'; end if;
    end;
end;
$$;
rollback;

begin;
set transaction isolation level serializable;
do $$
declare v_detail text;
begin
    perform set_config('request.jwt.claim.sub',(select doador_id::text from public.agendamento where id=23),true);
    begin perform public.criar_agendamento(1,gen_random_uuid()); raise exception 'Esperava rejeição';
    exception when raise_exception then
        get stacked diagnostics v_detail=pg_exception_detail;
        if v_detail is distinct from 'doesangue.isolation_unsupported.v1' then raise exception 'Identificador divergente'; end if;
    end;
    begin perform public.cancelar_agendamento(23); raise exception 'Esperava rejeição';
    exception when sqlstate '25000' then
        get stacked diagnostics v_detail=pg_exception_detail;
        if v_detail is distinct from 'doesangue.isolation_unsupported.v1' then raise exception 'Identificador divergente'; end if;
    end;
end;
$$;
select 'READ COMMITTED preservado; identificadores conferidos nos dois isolamentos rejeitados' as resultado;
rollback;
