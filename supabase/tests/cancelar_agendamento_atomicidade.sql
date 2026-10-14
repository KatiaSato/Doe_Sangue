-- Executar INTEIRO como postgres no desenvolvimento acadêmico sintético.
-- Identidade/papel simulados; sem login/JWT pela API nem concorrência.
-- Insere evento artificial SOMENTE para uma reserva temporária ainda agendada,
-- forçando o INSERT da função a violar a unicidade do evento (23505).
-- Esse cenário inconsistente é injeção administrativa de falha para o teste.
-- Depois remove SOMENTE esse evento artificial e tenta cancelar novamente.
-- Preserva 23 e termina em ROLLBACK; sequências podem avançar.
begin;
set transaction isolation level read committed;

do $$
declare
    v_doador uuid;
    v_unidade bigint;
    v_horario bigint;
    v_reserva public.agendamento%rowtype;
    v_evento public.agendamento_cancelamento%rowtype;
    v_original_23 jsonb;
begin
    select perfil.id into strict v_doador
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';
    select id into strict v_unidade from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;

    select to_jsonb(reserva) into strict v_original_23
    from public.agendamento as reserva where reserva.id = 23;
    perform set_config('doesangue_test.original_23', v_original_23::text, true);
    perform set_config(
        'doesangue_test.eventos_antes',
        (select count(*)::text from public.agendamento_cancelamento), true
    );

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade, clock_timestamp() + interval '7 days', 1, true)
    returning id into v_horario;
    insert into public.agendamento (doador_id, horario_id, chave_idempotencia)
    values (v_doador, v_horario, gen_random_uuid()) returning * into v_reserva;

    insert into public.agendamento_cancelamento (agendamento_id, cancelado_por)
    values (v_reserva.id, v_doador) returning * into v_evento;

    perform set_config('doesangue_test.doador', v_doador::text, true);
    perform set_config('doesangue_test.reserva', v_reserva.id::text, true);
    perform set_config('doesangue_test.original', to_jsonb(v_reserva)::text, true);
    perform set_config('doesangue_test.evento_id', v_evento.id::text, true);
    perform set_config('doesangue_test.evento_artificial', to_jsonb(v_evento)::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_doador uuid := current_setting('doesangue_test.doador')::uuid;
    v_constraint text;
    v_atual jsonb;
begin
    perform set_config('request.jwt.claim.sub', v_doador::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador, 'role', 'authenticated')::text, true);
    if current_user <> 'authenticated' or auth.uid() is distinct from v_doador then
        raise exception 'Preparação: papel ou identidade incorretos.';
    end if;

    begin
        perform public.cancelar_agendamento(current_setting('doesangue_test.reserva')::bigint);
        raise exception 'Teste falhou: conflito do evento deveria impedir o cancelamento.';
    exception when sqlstate '23505' then
        get stacked diagnostics v_constraint = constraint_name;
        if v_constraint is distinct from 'agendamento_cancelamento_agendamento_id_key' then
            raise;
        end if;
    end;

    select to_jsonb(reserva) into strict v_atual
    from public.agendamento as reserva
    where reserva.id = current_setting('doesangue_test.reserva')::bigint;
    if v_atual is distinct from current_setting('doesangue_test.original')::jsonb then
        raise exception 'Teste falhou: falha do evento deixou alteração parcial na reserva.';
    end if;
end;
$$;

reset role;

do $$
declare
    v_atual jsonb;
begin
    select to_jsonb(evento) into strict v_atual
    from public.agendamento_cancelamento as evento
    where evento.id = current_setting('doesangue_test.evento_id')::bigint;
    if v_atual is distinct from current_setting('doesangue_test.evento_artificial')::jsonb
       or (select count(*) from public.agendamento_cancelamento)
          <> current_setting('doesangue_test.eventos_antes')::bigint + 1 then
        raise exception 'Teste falhou: falha alterou o evento artificial ou criou outro.';
    end if;

    -- Remove apenas a causa artificial da falha, nunca um evento do domínio.
    delete from public.agendamento_cancelamento
    where id = current_setting('doesangue_test.evento_id')::bigint
      and agendamento_id = current_setting('doesangue_test.reserva')::bigint;
    if not found then raise exception 'Preparação: evento artificial não foi removido.'; end if;
end;
$$;

set local role authenticated;

do $$
declare
    v_cancelado jsonb;
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.doador')::uuid then
        raise exception 'Preparação: identidade da nova tentativa incorreta.';
    end if;

    select to_jsonb(resultado) into strict v_cancelado
    from public.cancelar_agendamento(current_setting('doesangue_test.reserva')::bigint) as resultado;
    if v_cancelado is distinct from (
        current_setting('doesangue_test.original')::jsonb || jsonb_build_object('estado', 'cancelado')
    ) then
        raise exception 'Teste falhou: nova tentativa não devolveu reserva cancelada preservada.';
    end if;
    perform set_config('doesangue_test.cancelado', v_cancelado::text, true);
end;
$$;

reset role;

do $$
declare
    v_evento public.agendamento_cancelamento%rowtype;
    v_atual jsonb;
begin
    select to_jsonb(reserva) into strict v_atual
    from public.agendamento as reserva
    where reserva.id = current_setting('doesangue_test.reserva')::bigint;
    if v_atual is distinct from current_setting('doesangue_test.cancelado')::jsonb then
        raise exception 'Teste falhou: nova tentativa não persistiu o cancelamento.';
    end if;

    select evento.* into strict v_evento
    from public.agendamento_cancelamento as evento
    where evento.agendamento_id = current_setting('doesangue_test.reserva')::bigint;
    if v_evento.id = current_setting('doesangue_test.evento_id')::bigint
       or v_evento.cancelado_por is distinct from current_setting('doesangue_test.doador')::uuid
       or v_evento.cancelado_em is null
       or (select count(*) from public.agendamento_cancelamento)
          <> current_setting('doesangue_test.eventos_antes')::bigint + 1 then
        raise exception 'Teste falhou: nova tentativa não criou exatamente um evento válido.';
    end if;

    if (select to_jsonb(reserva) from public.agendamento as reserva where reserva.id = 23)
       is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: reserva 23 foi alterada.';
    end if;
end;
$$;

select * from (values
    ('Falha no INSERT do evento', '23505 provocado e conferido'),
    ('Reserva após falha', 'Continua agendada, campos preservados'),
    ('Evento artificial após falha', 'Preservado'),
    ('Nova tentativa sem conflito', 'Reserva cancelada e evento criado'),
    ('Reserva 23', 'Preservada')
) as resultados(teste, resultado);

rollback;
