-- Desenvolvimento acadêmico: executar INTEIRO como postgres, UMA VEZ.
-- COMMIT cria uma oferta e uma reserva temporárias para o teste no Android.
-- Não cancela nem modifica a reserva original 23. Guardar os IDs retornados.
-- Identidade simulada somente na preparação; o cancelamento será pelo app.
-- Se ocorrer erro, executar ROLLBACK; não repetir automaticamente.
begin;
set transaction isolation level read committed;

do $$
declare
    v_a uuid;
    v_unidade bigint;
    v_horario bigint;
    v_original jsonb;
begin
    select to_jsonb(reserva) into strict v_original
    from public.agendamento as reserva where id = 23;
    if v_original->>'estado' <> 'agendado' then
        raise exception 'Preparação: reserva original 23 deve permanecer agendada.';
    end if;
    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';
    if exists (
        select 1 from public.agendamento
        where doador_id = v_a
          and chave_idempotencia = '21c4f132-c3d8-4717-b47f-d82c3bb3ec99'::uuid
    ) then
        raise exception 'Preparação já existe: conferir a reserva temporária antes de prosseguir.';
    end if;
    select id into strict v_unidade from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;
    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade, clock_timestamp() + interval '7 days', 1, true)
    returning id into v_horario;

    perform set_config('doesangue_test.android_original', v_original::text, true);
    perform set_config('doesangue_test.android_horario', v_horario::text, true);
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
    select resultado.* into strict v_reserva
    from public.criar_agendamento(
        current_setting('doesangue_test.android_horario')::bigint,
        '21c4f132-c3d8-4717-b47f-d82c3bb3ec99'::uuid
    ) as resultado;
    if v_reserva.id = 23 or v_reserva.doador_id is distinct from auth.uid()
       or v_reserva.estado is distinct from 'agendado'
       or v_reserva.horario_id is distinct from current_setting('doesangue_test.android_horario')::bigint then
        raise exception 'Preparação: resposta diferente da reserva temporária solicitada.';
    end if;
    perform set_config('doesangue_test.android_reserva', v_reserva.id::text, true);
end;
$$;
reset role;

do $$
begin
    if (select to_jsonb(reserva) from public.agendamento as reserva where id = 23)
       is distinct from current_setting('doesangue_test.android_original')::jsonb then
        raise exception 'Preparação: reserva original 23 alterada.';
    end if;
end;
$$;

select reserva.id as reserva_temporaria_id, horario.id as horario_temporario_id,
    horario.inicio at time zone unidade.fuso_horario as inicio_local,
    reserva.estado, 'Reserva original 23 preservada' as original
from public.agendamento as reserva
join public.horario_disponivel as horario on horario.id = reserva.horario_id
join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
where reserva.id = current_setting('doesangue_test.android_reserva')::bigint;
commit;
