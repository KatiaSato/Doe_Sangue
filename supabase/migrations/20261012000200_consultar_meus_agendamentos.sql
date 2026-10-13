begin;

create function public.consultar_meus_agendamentos()
returns table (
    agendamento jsonb,
    nome_unidade text,
    inicio timestamptz,
    fuso_horario text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_doador_id uuid;
begin
    v_doador_id := auth.uid();

    if v_doador_id is null then
        raise exception
            'É necessário entrar na conta para consultar os agendamentos.'
            using errcode = '42501';
    end if;
    return query
    select
        pg_catalog.jsonb_build_object(
            'id', reserva.id,
            'doador_id', reserva.doador_id,
            'horario_id', reserva.horario_id,
            'estado', reserva.estado,
            'chave_idempotencia', reserva.chave_idempotencia,
            'criado_em', reserva.criado_em
        ),
        unidade.nome,
        horario.inicio,
        unidade.fuso_horario
    from public.agendamento as reserva
    join public.horario_disponivel as horario
        on horario.id = reserva.horario_id
    join public.unidade_coleta as unidade
        on unidade.id = horario.unidade_id
    where reserva.doador_id = v_doador_id
    order by horario.inicio, reserva.id;
    return;
end;
$$;

-- Remove as permissões de execução anteriores.
revoke all privileges
on function public.consultar_meus_agendamentos()
from public, anon, authenticated;

-- Permite a chamada somente por contas autenticadas.
grant execute
on function public.consultar_meus_agendamentos()
to authenticated;

commit;