-- F14: preferência de calendário; nenhuma reserva ou notificação automática.
begin;
create sequence public.rotina_lembrete_versao_seq;
create table public.rotina_lembrete (
    id bigint generated always as identity primary key,
    doador_id uuid not null unique references public.perfil_doador(id) on delete restrict,
    inicio date not null check (inicio between date '1900-01-01' and date '9999-12-31'),
    intervalo_dias integer not null check (intervalo_dias between 1 and 3650),
    fuso_horario text not null check (length(btrim(fuso_horario))>0),
    ativa boolean not null default true,
    versao bigint not null default nextval('public.rotina_lembrete_versao_seq'::regclass) check (versao>0),
    versao_anterior bigint not null default 0,
    agendamento_origem_id bigint references public.agendamento(id) on delete restrict,
    criado_em timestamptz not null default clock_timestamp(),
    atualizado_em timestamptz not null default clock_timestamp()
);
create table public.agendamento_rotina_pedido (
    id bigint generated always as identity primary key,
    doador_id uuid not null references public.perfil_doador(id) on delete restrict,
    chave_idempotencia uuid not null,
    agendamento_id bigint not null unique references public.agendamento(id) on delete restrict,
    horario_id bigint not null references public.horario_disponivel(id) on delete restrict,
    inicio date not null,
    intervalo_dias integer not null,
    fuso_horario text not null,
    versao_esperada bigint not null,
    rotina_id bigint references public.rotina_lembrete(id) on delete set null,
    unique(doador_id,chave_idempotencia)
);
alter table public.rotina_lembrete enable row level security;
alter table public.agendamento_rotina_pedido enable row level security;
revoke all on public.rotina_lembrete,public.agendamento_rotina_pedido from public,anon,authenticated;
revoke all on sequence public.rotina_lembrete_id_seq,public.agendamento_rotina_pedido_id_seq,public.rotina_lembrete_versao_seq from public,anon,authenticated;

-- Somente funções privilegiadas chamam estes auxiliares internos.
create function public._doador_rotina() returns uuid
language plpgsql volatile security invoker set search_path=''
as $$
declare v_doador uuid:=auth.uid();
begin
    if v_doador is null then
        raise exception 'Entre na conta para acessar a rotina.' using errcode='42501',detail='doesangue.session_required.v1';
    end if;
    if current_setting('transaction_isolation')<>'read committed' then
        raise exception 'A rotina exige READ COMMITTED.' using errcode='25000',detail='doesangue.isolation_unsupported.v1';
    end if;
    perform id from public.perfil_doador where id=v_doador for update;
    if not found then
        raise exception 'Perfil indisponível.' using errcode='42501',detail='doesangue.profile_required.v1';
    end if;
    return v_doador;
end;
$$;
revoke all on function public._doador_rotina() from public,anon,authenticated;

create function public._salvar_rotina(p_doador uuid,p_inicio date,p_intervalo integer,p_fuso text,p_versao bigint,p_origem bigint)
returns public.rotina_lembrete
language plpgsql volatile security invoker set search_path=''
as $$
declare v_rotina public.rotina_lembrete%rowtype;
begin
    if p_inicio is null or p_inicio<date '1900-01-01' or p_inicio>date '9999-12-31'
       or p_intervalo is null or p_intervalo not between 1 and 3650
       or p_fuso is null or not exists(select 1 from pg_catalog.pg_timezone_names where name=p_fuso)
       or p_versao is null or p_versao<0 then
        raise exception 'Confira data, intervalo e fuso da rotina.' using errcode='22023',detail='doesangue.invalid_request.v1';
    end if;
    select * into v_rotina from public.rotina_lembrete where doador_id=p_doador for update;
    if found then
        if v_rotina.inicio=p_inicio and v_rotina.intervalo_dias=p_intervalo and v_rotina.fuso_horario=p_fuso
           and p_versao in(v_rotina.versao,v_rotina.versao_anterior) then
            return v_rotina;
        end if;
        if p_versao<>v_rotina.versao then
            raise exception 'A rotina mudou. Recarregue antes de editar.' using detail='doesangue.routine_changed.v1';
        end if;
        if p_inicio<>(v_rotina.inicio) and p_inicio<(clock_timestamp() at time zone p_fuso)::date then
            raise exception 'Escolha uma data inicial a partir de hoje.' using detail='doesangue.routine_date_past.v1';
        end if;
        update public.rotina_lembrete set inicio=p_inicio,intervalo_dias=p_intervalo,fuso_horario=p_fuso,
            versao_anterior=versao,versao=nextval('public.rotina_lembrete_versao_seq'::regclass),atualizado_em=clock_timestamp()
        where id=v_rotina.id returning * into v_rotina;
    else
        if p_versao<>0 then
            raise exception 'A rotina mudou. Recarregue antes de editar.' using detail='doesangue.routine_changed.v1';
        end if;
        if p_inicio<(clock_timestamp() at time zone p_fuso)::date then
            raise exception 'Escolha uma data inicial a partir de hoje.' using detail='doesangue.routine_date_past.v1';
        end if;
        insert into public.rotina_lembrete(doador_id,inicio,intervalo_dias,fuso_horario,agendamento_origem_id)
        values(p_doador,p_inicio,p_intervalo,p_fuso,p_origem) returning * into v_rotina;
    end if;
    return v_rotina;
end;
$$;
revoke all on function public._salvar_rotina(uuid,date,integer,text,bigint,bigint) from public,anon,authenticated;

create function public.consultar_minha_rotina() returns setof public.rotina_lembrete
language plpgsql stable security definer set search_path=''
as $$
declare v_doador uuid:=auth.uid();
begin
    if v_doador is null then
        raise exception 'Entre na conta para consultar a rotina.' using errcode='42501',detail='doesangue.session_required.v1';
    end if;
    return query select * from public.rotina_lembrete where doador_id=v_doador;
end;
$$;
create function public.salvar_minha_rotina(p_inicio date,p_intervalo integer,p_fuso text,p_versao bigint)
returns setof public.rotina_lembrete
language plpgsql volatile security definer set search_path=''
as $$
declare v_doador uuid;
begin
    v_doador:=public._doador_rotina();
    return next public._salvar_rotina(v_doador,p_inicio,p_intervalo,p_fuso,p_versao,null);
end;
$$;
create function public.definir_minha_rotina_ativa(p_ativa boolean,p_versao bigint)
returns setof public.rotina_lembrete
language plpgsql volatile security definer set search_path=''
as $$
declare v_doador uuid; v_rotina public.rotina_lembrete%rowtype;
begin
    v_doador:=public._doador_rotina();
    if p_ativa is null or p_versao is null or p_versao<=0 then
        raise exception 'Informe uma rotina válida.' using errcode='22023',detail='doesangue.invalid_request.v1';
    end if;
    select * into v_rotina from public.rotina_lembrete where doador_id=v_doador for update;
    if not found or (v_rotina.versao<>p_versao and not(v_rotina.ativa=p_ativa and v_rotina.versao_anterior=p_versao)) then
        raise exception 'A rotina mudou. Recarregue antes de editar.' using detail='doesangue.routine_changed.v1';
    end if;
    if v_rotina.ativa<>p_ativa then
        update public.rotina_lembrete set ativa=p_ativa,versao_anterior=versao,versao=nextval('public.rotina_lembrete_versao_seq'::regclass),atualizado_em=clock_timestamp()
        where id=v_rotina.id returning * into v_rotina;
    end if;
    return next v_rotina;
end;
$$;
create function public.excluir_minha_rotina(p_versao bigint) returns boolean
language plpgsql volatile security definer set search_path=''
as $$
declare v_doador uuid; v_rotina public.rotina_lembrete%rowtype;
begin
    v_doador:=public._doador_rotina();
    if p_versao is null or p_versao<=0 then
        raise exception 'Informe uma rotina válida.' using errcode='22023',detail='doesangue.invalid_request.v1';
    end if;
    select * into v_rotina from public.rotina_lembrete where doador_id=v_doador for update;
    if not found then return true; end if;
    if v_rotina.versao<>p_versao then
        raise exception 'A rotina mudou. Recarregue antes de excluir.' using detail='doesangue.routine_changed.v1';
    end if;
    delete from public.rotina_lembrete where id=v_rotina.id;
    return true;
end;
$$;

create function public.criar_agendamento_com_rotina(p_horario_id bigint,p_chave_idempotencia uuid,p_inicio date,p_intervalo integer,p_fuso text,p_versao bigint)
returns setof public.agendamento
language plpgsql volatile security definer set search_path=''
as $$
declare v_doador uuid; v_pedido public.agendamento_rotina_pedido%rowtype;
    v_reserva public.agendamento%rowtype; v_rotina public.rotina_lembrete%rowtype;
begin
    v_doador:=public._doador_rotina();
    if p_horario_id is null or p_horario_id<=0 or p_chave_idempotencia is null or p_inicio is null or p_intervalo is null or p_fuso is null or p_versao is null then
        raise exception 'Confira o pedido e a rotina.' using errcode='22023',detail='doesangue.invalid_request.v1';
    end if;
    select * into v_pedido from public.agendamento_rotina_pedido where doador_id=v_doador and chave_idempotencia=p_chave_idempotencia;
    if found then
        if v_pedido.horario_id<>p_horario_id or v_pedido.inicio<>p_inicio or v_pedido.intervalo_dias<>p_intervalo or v_pedido.fuso_horario<>p_fuso or v_pedido.versao_esperada<>p_versao then
            raise exception 'Pedido reutilizado com outros dados.' using detail='doesangue.request_conflict.v1';
        end if;
        return query select * from public.agendamento where id=v_pedido.agendamento_id and doador_id=v_doador;
        return;
    end if;
    if exists(select 1 from public.agendamento where doador_id=v_doador and chave_idempotencia=p_chave_idempotencia) then
        raise exception 'Pedido já utilizado.' using detail='doesangue.request_conflict.v1';
    end if;
    select * into strict v_reserva from public.criar_agendamento(p_horario_id,p_chave_idempotencia);
    v_rotina:=public._salvar_rotina(v_doador,p_inicio,p_intervalo,p_fuso,p_versao,v_reserva.id);
    insert into public.agendamento_rotina_pedido(doador_id,chave_idempotencia,agendamento_id,horario_id,inicio,intervalo_dias,fuso_horario,versao_esperada,rotina_id)
    values(v_doador,p_chave_idempotencia,v_reserva.id,p_horario_id,p_inicio,p_intervalo,p_fuso,p_versao,v_rotina.id);
    return next v_reserva;
end;
$$;
revoke all on function public.consultar_minha_rotina(),public.salvar_minha_rotina(date,integer,text,bigint),public.definir_minha_rotina_ativa(boolean,bigint),public.excluir_minha_rotina(bigint),public.criar_agendamento_com_rotina(bigint,uuid,date,integer,text,bigint) from public,anon,authenticated;
grant execute on function public.consultar_minha_rotina(),public.salvar_minha_rotina(date,integer,text,bigint),public.definir_minha_rotina_ativa(boolean,bigint),public.excluir_minha_rotina(bigint),public.criar_agendamento_com_rotina(bigint,uuid,date,integer,text,bigint) to authenticated;
commit;
