-- Somente depois de ambas as sessões terminarem e resultados conferidos.
-- Executar como postgres. COMMIT conclui a limpeza dos registros do teste.
-- Oferta 23, início 2026-10-21 21:31:31.436406+00; reserva temporária 37.
-- Aborta antes de excluir se parâmetros/configuração/linhas divergirem.
-- Não exclui unidades, perfis, contas nem a reserva 23.
begin;

do $$
declare
    v_horario_id bigint := 23;
    v_reserva_id bigint := 37;
    v_inicio timestamptz := timestamptz '2026-10-21 21:31:31.436406+00';
    v_horario public.horario_disponivel%rowtype;
    v_reserva public.agendamento%rowtype;
    v_evento public.agendamento_cancelamento%rowtype;
    v_a uuid;
begin
    if v_horario_id <= 0 or v_reserva_id <= 0 or v_reserva_id = 23 or v_inicio is null then
        raise exception 'Limpeza não configurada: preencher os dados confirmados da preparação.';
    end if;
    perform set_config('doesangue_test.cancel_horario', v_horario_id::text, true);
    perform set_config('doesangue_test.cancel_reserva', v_reserva_id::text, true);

    select horario.* into v_horario
    from public.horario_disponivel as horario where horario.id = v_horario_id for update;
    if not found then return; end if;

    if v_horario.inicio is distinct from v_inicio
       or v_horario.capacidade <> 1 or not v_horario.ativo
       or not exists (
           select 1 from public.unidade_coleta
           where id = v_horario.unidade_id and nome = 'Unidade Demo A' and publicada
       ) or exists (select 1 from public.agendamento where id = 23 and horario_id = v_horario_id) then
        raise exception 'Limpeza interrompida: oferta diferente da preparação ou vinculada a 23.';
    end if;

    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    if exists (
        select 1 from public.agendamento
        where horario_id = v_horario_id and (id <> v_reserva_id or doador_id <> v_a)
    ) then
        raise exception 'Limpeza interrompida: existem reservas fora do cenário esperado.';
    end if;

    select reserva.* into v_reserva from public.agendamento as reserva
    where reserva.id = v_reserva_id and reserva.horario_id = v_horario_id for update;
    if found then
        if v_reserva.doador_id is distinct from v_a then
            raise exception 'Limpeza interrompida: reserva não pertence à conta A do teste.';
        end if;

        select evento.* into v_evento from public.agendamento_cancelamento as evento
        where evento.agendamento_id = v_reserva_id for update;
        if v_reserva.estado = 'cancelado' then
            if not found or v_evento.cancelado_por is distinct from v_a then
                raise exception 'Limpeza interrompida: cancelamento sem evento próprio consistente.';
            end if;
        elsif v_reserva.estado = 'agendado' then
            if found then raise exception 'Limpeza interrompida: reserva agendada possui evento.'; end if;
        else
            raise exception 'Limpeza interrompida: estado inesperado.';
        end if;

        delete from public.agendamento_cancelamento
        where agendamento_id = v_reserva_id and cancelado_por = v_a;
        delete from public.agendamento
        where id = v_reserva_id and horario_id = v_horario_id and doador_id = v_a;
    end if;
    delete from public.horario_disponivel where id = v_horario_id;
end;
$$;

select
    not exists (select 1 from public.horario_disponivel
        where id = current_setting('doesangue_test.cancel_horario')::bigint) as oferta_removida,
    not exists (select 1 from public.agendamento
        where horario_id = current_setting('doesangue_test.cancel_horario')::bigint) as reservas_removidas,
    not exists (select 1 from public.agendamento_cancelamento
        where agendamento_id = current_setting('doesangue_test.cancel_reserva')::bigint) as eventos_removidos;

commit;
