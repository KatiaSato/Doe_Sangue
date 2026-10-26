begin;

-- Fotografia de disponibilidade; criar_agendamento continua sendo a autoridade da reserva.
create function public.consultar_horarios_disponiveis(
    p_unidade_id bigint,
    p_inicio timestamptz,
    p_fim timestamptz
)
returns table(id bigint, unidade_id bigint, inicio timestamptz, capacidade integer, vagas_disponiveis integer)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if p_unidade_id is null or p_unidade_id <= 0
       or p_inicio is null or p_fim is null
       or not pg_catalog.isfinite(p_inicio) or not pg_catalog.isfinite(p_fim)
       or p_fim <= p_inicio or p_fim - p_inicio > interval '32 days' then
        raise exception using errcode = '22023', message = 'Informe uma unidade e um período válido de até 32 dias.';
    end if;

    return query
    select horario.id, horario.unidade_id, horario.inicio, horario.capacidade,
        (horario.capacidade::bigint - ocupacao.total)::integer
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
    cross join lateral (
        select count(*) as total
        from public.agendamento as reserva
        where reserva.horario_id = horario.id and reserva.estado = 'agendado'
    ) as ocupacao
    where horario.unidade_id = p_unidade_id
      and unidade.publicada and horario.ativo
      and horario.inicio >= p_inicio and horario.inicio < p_fim
      and horario.inicio > pg_catalog.statement_timestamp()
      and ocupacao.total < horario.capacidade
    order by horario.inicio, horario.id;
end;
$$;

create function public.consultar_dias_disponiveis(p_unidade_id bigint, p_mes date)
returns table(dia date)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_fuso text;
    v_inicio timestamptz;
    v_fim timestamptz;
begin
    if p_unidade_id is null or p_unidade_id <= 0 or p_mes is null
       or not pg_catalog.isfinite(p_mes) or extract(day from p_mes) <> 1 then
        raise exception using errcode = '22023', message = 'Informe a unidade e o primeiro dia do mês.';
    end if;

    select unidade.fuso_horario into v_fuso
    from public.unidade_coleta as unidade
    where unidade.id = p_unidade_id and unidade.publicada;
    if not found then return; end if;

    v_inicio := p_mes::timestamp at time zone v_fuso;
    v_fim := (p_mes + interval '1 month')::timestamp at time zone v_fuso;

    return query
    select distinct (horario.inicio at time zone v_fuso)::date
    from public.consultar_horarios_disponiveis(p_unidade_id, v_inicio, v_fim) as horario
    order by 1;
end;
$$;

revoke all on function public.consultar_horarios_disponiveis(bigint,timestamptz,timestamptz) from public, anon, authenticated;
revoke all on function public.consultar_dias_disponiveis(bigint,date) from public, anon, authenticated;
grant execute on function public.consultar_horarios_disponiveis(bigint,timestamptz,timestamptz) to anon, authenticated;
grant execute on function public.consultar_dias_disponiveis(bigint,date) to anon, authenticated;

commit;
