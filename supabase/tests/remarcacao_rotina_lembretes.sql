-- Executar inteiro somente após autorização. Perfis existentes são apenas consultados.
-- Fixtures descartáveis e ROLLBACK; não demonstra JWT nem duas sessões concorrentes.
begin;
set transaction isolation level read committed;
create function pg_temp.expect_f13_f14(p_sql text,p_state text,p_detail text default null)
returns void language plpgsql as $$
declare v_state text; v_detail text; v_failed boolean:=false;
begin
    begin execute p_sql;
    exception when others then
        v_failed:=true;
        get stacked diagnostics v_state=returned_sqlstate,v_detail=pg_exception_detail;
        if v_state<>p_state or (p_detail is not null and v_detail is distinct from 'doesangue.'||p_detail||'.v1') then
            raise exception 'Rejeição divergente: % / %',v_state,v_detail;
        end if;
    end;
    if not v_failed then raise exception 'Esperava rejeição.'; end if;
end;
$$;
do $$
declare
    a uuid; b uuid; u bigint; old_slot bigint; target bigint; full_slot bigint; past_slot bigint; inactive bigint;
    original public.agendamento%rowtype; changed public.agendamento%rowtype; foreign_booking bigint; past_booking bigint;
    second_original bigint; event_original bigint; k uuid:=gen_random_uuid(); k2 uuid:=gen_random_uuid();
    r public.rotina_lembrete%rowtype; r2 public.rotina_lembrete%rowtype; old_version bigint;
    existing_routine public.rotina_lembrete%rowtype; day0 date:=(clock_timestamp() at time zone 'America/Sao_Paulo')::date;
    linked public.agendamento%rowtype; linked_key uuid:=gen_random_uuid(); before_count bigint; saved23 jsonb;
begin
    select doador_id,to_jsonb(t) into strict a,saved23 from public.agendamento t where id=23;
    select id into strict b from public.perfil_doador where id<>a order by id limit 1;
    -- Não altera uma rotina real preexistente; o roteiro requer contas sem rotina para estas fixtures.
    if exists(select 1 from public.rotina_lembrete where doador_id in(a,b)) then
        raise exception 'Há rotina existente: adaptar fixtures antes de executar, sem removê-la.';
    end if;
    insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada)
    values('Unidade Sintética F13 F14','Cidade fictícia','Endereço fictício','America/Sao_Paulo',true) returning id into u;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(u,clock_timestamp()+interval '60 days',5,true) returning id into old_slot;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(u,clock_timestamp()+interval '61 days',5,true) returning id into target;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(u,clock_timestamp()+interval '62 days',1,true) returning id into full_slot;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(u,clock_timestamp()-interval '1 day',5,true) returning id into past_slot;
    insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
    values(u,clock_timestamp()+interval '63 days',5,false) returning id into inactive;
    insert into public.agendamento(doador_id,horario_id,chave_idempotencia) values(b,full_slot,gen_random_uuid()) returning id into foreign_booking;
    insert into public.agendamento(doador_id,horario_id,chave_idempotencia) values(a,past_slot,gen_random_uuid()) returning id into past_booking;
    set local role authenticated;
    perform set_config('request.jwt.claim.sub','',true);
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(23,%s,%L)',target,k),'42501','session_required');
    perform pg_temp.expect_f13_f14('select public.consultar_minha_rotina()','42501','session_required');
    perform set_config('request.jwt.claim.sub',a::text,true);
    select * into strict original from public.criar_agendamento(old_slot,k2);
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(null,%s,%L)',target,k),'22023','invalid_request');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',foreign_booking,target,k),'42501','appointment_unavailable');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(9223372036854775807,%s,%L)',target,k),'42501','appointment_unavailable');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',past_booking,target,k),'P0001','rescheduling_closed');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',original.id,old_slot,k),'P0001','slot_same');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',original.id,full_slot,k),'P0001','slot_full');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',original.id,inactive,k),'P0001','slot_unavailable');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',original.id,target,k2),'P0001','request_conflict');
    select * into strict changed from public.remarcar_agendamento(original.id,target,k);
    if changed.estado<>'agendado' or changed.horario_id<>target or changed.id=original.id then raise exception 'Substituta incorreta.'; end if;
    if (select id from public.remarcar_agendamento(original.id,target,k))<>changed.id then raise exception 'Repetição F13 divergente.'; end if;
    if (select id from public.criar_agendamento(old_slot,k2))<>original.id then raise exception 'Identidade de criação antiga mudou.'; end if;
    if (select remarcado_para_id from public.consultar_meus_agendamentos_v2() where (agendamento->>'id')::bigint=original.id)<>changed.id then raise exception 'Vínculo de saída ausente.'; end if;
    if (select remarcado_de_id from public.consultar_meus_agendamentos_v2() where (agendamento->>'id')::bigint=changed.id)<>original.id then raise exception 'Vínculo de entrada ausente.'; end if;
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',original.id,full_slot,k),'P0001','request_conflict');
    perform * from public.cancelar_agendamento(changed.id);
    if (select estado from public.remarcar_agendamento(original.id,target,k))<>'cancelado' then raise exception 'Replay reativou reserva.'; end if;
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',original.id,full_slot,gen_random_uuid()),'P0001','rescheduling_closed');
    reset role;
    if (select estado from public.agendamento where id=original.id)<>'cancelado'
       or (select horario_id from public.agendamento where id=original.id)<>old_slot
       or (select count(*) from public.agendamento_cancelamento where agendamento_id=original.id)<>1
       or (select count(*) from public.agendamento_remarcacao where agendamento_original_id=original.id)<>1 then raise exception 'Histórico F13 inválido.'; end if;
    -- Falha na gravação do evento após criar a substituta: desfaz a transação inteira.
    insert into public.agendamento(doador_id,horario_id,chave_idempotencia) values(a,old_slot,gen_random_uuid()) returning id into event_original;
    insert into public.agendamento_cancelamento(agendamento_id,cancelado_por,cancelado_em) values(event_original,a,clock_timestamp());
    select count(*) into before_count from public.agendamento;
    set local role authenticated;
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',event_original,target,gen_random_uuid()),'23505');
    reset role;
    if (select count(*) from public.agendamento)<>before_count or (select estado from public.agendamento where id=event_original)<>'agendado' then raise exception 'Falha F13 deixou alteração parcial.'; end if;
    delete from public.agendamento_cancelamento where agendamento_id=event_original;
    delete from public.agendamento where id=event_original;
    -- Preferência pessoal, versão, repetição, pausa e exclusão sem alterar reservas.
    set local role authenticated;
    perform pg_temp.expect_f13_f14(format('select public.salvar_minha_rotina(%L,0,%L,0)',day0,'America/Sao_Paulo'),'22023','invalid_request');
    perform pg_temp.expect_f13_f14(format('select public.salvar_minha_rotina(%L,7,%L,0)',day0,'Fuso/Ficticio'),'22023','invalid_request');
    perform pg_temp.expect_f13_f14(format('select public.salvar_minha_rotina(%L,7,%L,0)',day0-1,'America/Sao_Paulo'),'P0001','routine_date_past');
    select * into strict r from public.salvar_minha_rotina(day0,7,'America/Sao_Paulo',0);
    if (select id from public.salvar_minha_rotina(day0,7,'America/Sao_Paulo',0))<>r.id then raise exception 'Repetição F14 divergente.'; end if;
    old_version:=r.versao;
    select * into strict r from public.definir_minha_rotina_ativa(false,r.versao);
    if r.ativa or (select versao from public.definir_minha_rotina_ativa(false,old_version))<>r.versao then raise exception 'Pausa não idempotente.'; end if;
    perform pg_temp.expect_f13_f14(format('select public.salvar_minha_rotina(%L,8,%L,%s)',day0,'America/Sao_Paulo',old_version),'P0001','routine_changed');
    select * into strict r from public.salvar_minha_rotina(day0+1,8,'America/Sao_Paulo',r.versao);
    if r.ativa then raise exception 'Editar retomou rotina pausada.'; end if;
    perform set_config('request.jwt.claim.sub',b::text,true);
    if exists(select 1 from public.consultar_minha_rotina()) then raise exception 'Rotina de A exposta a B.'; end if;
    select * into strict r2 from public.salvar_minha_rotina(day0+1,9,'UTC',0);
    perform set_config('request.jwt.claim.sub',a::text,true);
    if (select id from public.consultar_minha_rotina())<>r.id then raise exception 'Rotina própria ausente.'; end if;
    perform public.excluir_minha_rotina(r.versao);
    perform public.excluir_minha_rotina(r.versao);
    select * into strict r2 from public.salvar_minha_rotina(day0,10,'America/Sao_Paulo',0);
    perform pg_temp.expect_f13_f14(format('select public.excluir_minha_rotina(%s)',r.versao),'P0001','routine_changed');
    perform public.excluir_minha_rotina(r2.versao);
    select * into strict linked from public.criar_agendamento_com_rotina(old_slot,linked_key,day0,11,'America/Sao_Paulo',0);
    select * into strict r from public.consultar_minha_rotina();
    if r.agendamento_origem_id<>linked.id then raise exception 'Origem da rotina ausente.'; end if;
    perform public.excluir_minha_rotina(r.versao);
    if (select id from public.criar_agendamento_com_rotina(old_slot,linked_key,day0,11,'America/Sao_Paulo',0))<>linked.id
       or exists(select 1 from public.consultar_minha_rotina()) then raise exception 'Replay recriou rotina excluída.'; end if;
    perform pg_temp.expect_f13_f14(format('select public.criar_agendamento_com_rotina(%s,%L,%L,12,%L,0)',old_slot,linked_key,day0,'America/Sao_Paulo'),'P0001','request_conflict');
    perform * from public.cancelar_agendamento(linked.id);
    select * into strict r from public.salvar_minha_rotina(day0,15,'America/Sao_Paulo',0);
    reset role;
    select count(*) into before_count from public.agendamento;
    set local role authenticated;
    perform pg_temp.expect_f13_f14(format('select public.criar_agendamento_com_rotina(%s,%L,%L,20,%L,0)',old_slot,gen_random_uuid(),day0,'America/Sao_Paulo'),'P0001','routine_changed');
    reset role;
    if (select count(*) from public.agendamento)<>before_count then raise exception 'Falha F14 deixou reserva parcial.'; end if;
    -- Cliente não acessa tabelas internas ou auxiliares.
    set local role authenticated;
    perform pg_temp.expect_f13_f14('select * from public.rotina_lembrete','42501');
    perform pg_temp.expect_f13_f14('select * from public.agendamento_remarcacao','42501');
    perform pg_temp.expect_f13_f14('select * from public.agendamento_rotina_pedido','42501');
    perform pg_temp.expect_f13_f14('select public._doador_rotina()','42501');
    perform set_config('request.jwt.claim.sub',b::text,true);
    if exists(select 1 from public.consultar_meus_agendamentos_v2() where (agendamento->>'id')::bigint=linked.id) then raise exception 'Reserva alheia exposta.'; end if;
    reset role;
    set local role anon;
    perform pg_temp.expect_f13_f14('select public.consultar_minha_rotina()','42501');
    perform pg_temp.expect_f13_f14('select public.consultar_meus_agendamentos_v2()','42501');
    perform pg_temp.expect_f13_f14(format('select public.remarcar_agendamento(%s,%s,%L)',original.id,target,k),'42501');
    reset role;
    if saved23 is distinct from (select to_jsonb(t) from public.agendamento t where id=23) then raise exception 'Reserva23 mudou.'; end if;
end;
$$;
select 'Troca atômica e replay; rotina pessoal, versão, isolamento e reserva23 conferidos' as resultado;
rollback;
