-- F13: substituição atômica, preservando as identidades dos pedidos anteriores.
begin;

create table public.agendamento_remarcacao (
    id bigint generated always as identity primary key,
    agendamento_original_id bigint not null unique references public.agendamento(id) on delete restrict,
    novo_agendamento_id bigint not null unique references public.agendamento(id) on delete restrict,
    doador_id uuid not null references public.perfil_doador(id) on delete restrict,
    horario_destino_id bigint not null references public.horario_disponivel(id) on delete restrict,
    chave_idempotencia uuid not null,
    remarcado_em timestamptz not null default clock_timestamp(),
    unique (doador_id, chave_idempotencia),
    check (agendamento_original_id <> novo_agendamento_id)
);
alter table public.agendamento_remarcacao enable row level security;
revoke all on table public.agendamento_remarcacao from public, anon, authenticated;
revoke all on sequence public.agendamento_remarcacao_id_seq from public, anon, authenticated;

create function public.remarcar_agendamento(p_agendamento_id bigint, p_horario_id bigint, p_chave_idempotencia uuid)
returns setof public.agendamento
language plpgsql volatile security definer set search_path = ''
as $$
declare
    v_doador uuid := auth.uid();
    v_original public.agendamento%rowtype;
    v_novo public.agendamento%rowtype;
    v_pedido public.agendamento_remarcacao%rowtype;
    v_inicio timestamptz;
    v_detail text;
begin
    if v_doador is null then
        raise exception 'Entre na conta para remarcar.' using errcode='42501', detail='doesangue.session_required.v1';
    end if;
    if p_agendamento_id is null or p_agendamento_id <= 0 or p_horario_id is null or p_horario_id <= 0 or p_chave_idempotencia is null then
        raise exception 'Informe uma remarcação válida.' using errcode='22023', detail='doesangue.invalid_request.v1';
    end if;
    if current_setting('transaction_isolation') <> 'read committed' then
        raise exception 'A remarcação exige READ COMMITTED.' using errcode='25000', detail='doesangue.isolation_unsupported.v1';
    end if;
    perform id from public.perfil_doador where id=v_doador for update;
    if not found then
        raise exception 'Perfil indisponível.' using errcode='42501', detail='doesangue.profile_required.v1';
    end if;
    select * into v_pedido from public.agendamento_remarcacao
    where doador_id=v_doador and chave_idempotencia=p_chave_idempotencia;
    if found then
        if v_pedido.agendamento_original_id<>p_agendamento_id or v_pedido.horario_destino_id<>p_horario_id then
            raise exception 'Pedido reutilizado com outros dados.' using detail='doesangue.request_conflict.v1';
        end if;
        return query select * from public.agendamento where id=v_pedido.novo_agendamento_id and doador_id=v_doador;
        return;
    end if;
    -- Não vincula a remarcação a uma criação anterior não relacionada.
    if exists(select 1 from public.agendamento where doador_id=v_doador and chave_idempotencia=p_chave_idempotencia) then
        raise exception 'Pedido já utilizado.' using detail='doesangue.request_conflict.v1';
    end if;
    select * into v_original from public.agendamento where id=p_agendamento_id and doador_id=v_doador;
    if not found then
        raise exception 'Agendamento indisponível.' using errcode='42501', detail='doesangue.appointment_unavailable.v1';
    end if;
    if v_original.estado<>'agendado' then
        raise exception 'Este agendamento já foi encerrado.' using detail='doesangue.rescheduling_closed.v1';
    end if;
    if v_original.horario_id=p_horario_id then
        raise exception 'Escolha outro horário.' using detail='doesangue.slot_same.v1';
    end if;
    -- Todas as trocas adquirem os dois horários na mesma ordem.
    perform id from public.horario_disponivel where id in(v_original.horario_id,p_horario_id) order by id for update;
    select inicio into v_inicio from public.horario_disponivel where id=v_original.horario_id;
    if v_inicio is null or v_inicio<=clock_timestamp() then
        raise exception 'O prazo para remarcar terminou.' using detail='doesangue.rescheduling_closed.v1';
    end if;
    select * into strict v_novo from public.criar_agendamento(p_horario_id,p_chave_idempotencia);
    begin
        perform * from public.cancelar_agendamento(p_agendamento_id);
    exception when raise_exception then
        get stacked diagnostics v_detail=pg_exception_detail;
        if v_detail='doesangue.cancellation_closed.v1' then
            raise exception 'O prazo para remarcar terminou.' using detail='doesangue.rescheduling_closed.v1';
        end if;
        raise;
    end;
    insert into public.agendamento_remarcacao(agendamento_original_id,novo_agendamento_id,doador_id,horario_destino_id,chave_idempotencia)
    values(p_agendamento_id,v_novo.id,v_doador,p_horario_id,p_chave_idempotencia);
    return next v_novo;
end;
$$;
revoke all on function public.remarcar_agendamento(bigint,bigint,uuid) from public,anon,authenticated;
grant execute on function public.remarcar_agendamento(bigint,bigint,uuid) to authenticated;

-- A consulta anterior continua disponível para versões já instaladas.
create function public.consultar_meus_agendamentos_v2()
returns table(agendamento jsonb,nome_unidade text,inicio timestamptz,fuso_horario text,unidade_id bigint,remarcado_para_id bigint,remarcado_de_id bigint)
language plpgsql stable security definer set search_path = ''
as $$
declare v_doador uuid:=auth.uid();
begin
    if v_doador is null then
        raise exception 'Entre na conta para consultar.' using errcode='42501', detail='doesangue.session_required.v1';
    end if;
    return query
    select to_jsonb(a),u.nome,h.inicio,u.fuso_horario,u.id,saida.novo_agendamento_id,entrada.agendamento_original_id
    from public.agendamento a
    join public.horario_disponivel h on h.id=a.horario_id
    join public.unidade_coleta u on u.id=h.unidade_id
    left join public.agendamento_remarcacao saida on saida.agendamento_original_id=a.id and saida.doador_id=v_doador
    left join public.agendamento_remarcacao entrada on entrada.novo_agendamento_id=a.id and entrada.doador_id=v_doador
    where a.doador_id=v_doador order by h.inicio,a.id;
end;
$$;
revoke all on function public.consultar_meus_agendamentos_v2() from public,anon,authenticated;
grant execute on function public.consultar_meus_agendamentos_v2() to authenticated;
commit;
