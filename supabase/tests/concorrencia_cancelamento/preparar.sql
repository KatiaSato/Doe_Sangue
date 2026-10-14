-- Desenvolvimento acadêmico: executar INTEIRO como postgres, UMA VEZ.
-- COMMIT torna oferta/reserva temporárias visíveis nas duas sessões.
-- Cria reserva de A pela função, com papel/identidade simulados; sem JWT/API.
-- Não cria/modifica perfis. Não cancela nenhuma reserva.
-- Guardar ID/início da oferta e ID da reserva para execução e limpeza.
begin;
set transaction isolation level read committed;

do $$
declare
    v_a uuid;
    v_b uuid;
    v_unidade bigint;
    v_horario bigint;
begin
    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';
    select id into strict v_b from auth.users where email = 'doador.b@example.com';
    if v_a = v_b then raise exception 'Preparação: contas sintéticas precisam ser distintas.'; end if;

    select id into strict v_unidade from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;
    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade, clock_timestamp() + interval '7 days', 1, true)
    returning id into v_horario;

    perform set_config('doesangue_test.cancel_horario', v_horario::text, true);
    perform set_config('doesangue_test.cancel_a', v_a::text, true);
    perform set_config('request.jwt.claim.sub', v_a::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_a, 'role', 'authenticated')::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_reserva public.agendamento%rowtype;
begin
    if current_user <> 'authenticated'
       or auth.uid() is distinct from current_setting('doesangue_test.cancel_a')::uuid then
        raise exception 'Preparação: papel ou identidade A incorretos.';
    end if;
    select resultado.* into strict v_reserva
    from public.criar_agendamento(
        current_setting('doesangue_test.cancel_horario')::bigint, gen_random_uuid()
    ) as resultado;

    if v_reserva.id is null or v_reserva.id = 23
       or v_reserva.doador_id is distinct from auth.uid()
       or v_reserva.horario_id is distinct from current_setting('doesangue_test.cancel_horario')::bigint
       or v_reserva.estado is distinct from 'agendado' then
        raise exception 'Preparação: reserva temporária diferente do pedido.';
    end if;
    perform set_config('doesangue_test.cancel_reserva', v_reserva.id::text, true);
end;
$$;

reset role;

select
    horario.id as horario_id,
    horario.inicio,
    horario.capacidade,
    reserva.id as reserva_id,
    reserva.estado
from public.horario_disponivel as horario
join public.agendamento as reserva on reserva.horario_id = horario.id
where horario.id = current_setting('doesangue_test.cancel_horario')::bigint
  and reserva.id = current_setting('doesangue_test.cancel_reserva')::bigint;

commit;
