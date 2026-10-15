-- Somente após conferir o teste no Android. Executar INTEIRO como postgres.
-- COMMIT remove exclusivamente a reserva marcada por este roteiro, evento e oferta.
-- Preserva a reserva original 23, unidades, perfis e contas.
-- Se ocorrer erro, executar ROLLBACK e conferir os dados antes de repetir.
begin;
do $$
declare
    v_a uuid;
    v_reserva public.agendamento%rowtype;
    v_horario public.horario_disponivel%rowtype;
    v_original jsonb;
    v_eventos bigint;
begin
    select to_jsonb(reserva) into strict v_original
    from public.agendamento as reserva where id = 23;
    if v_original->>'estado' <> 'agendado' then
        raise exception 'Limpeza: reserva original 23 deve permanecer agendada.';
    end if;
    select perfil.id into strict v_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com'
    for update of perfil;
    select * into strict v_reserva from public.agendamento
    where doador_id = v_a
      and chave_idempotencia = '21c4f132-c3d8-4717-b47f-d82c3bb3ec99'::uuid;
    select * into strict v_horario from public.horario_disponivel
    where id = v_reserva.horario_id for update;
    if v_reserva.id = 23 or v_horario.id = (v_original->>'horario_id')::bigint
       or v_horario.capacidade <> 1 or not v_horario.ativo
       or (select count(*) from public.agendamento where horario_id = v_horario.id) <> 1
       or not exists (select 1 from public.unidade_coleta
           where id = v_horario.unidade_id and nome = 'Unidade Demo A' and publicada) then
        raise exception 'Limpeza: oferta/reserva não correspondem ao roteiro temporário.';
    end if;
    select count(*) into v_eventos from public.agendamento_cancelamento
    where agendamento_id = v_reserva.id;
    if (v_reserva.estado = 'agendado' and v_eventos <> 0)
       or (v_reserva.estado = 'cancelado' and v_eventos <> 1)
       or exists (select 1 from public.agendamento_cancelamento
           where agendamento_id = v_reserva.id and cancelado_por <> v_a) then
        raise exception 'Limpeza: conferir o estado e o evento antes de remover o teste.';
    end if;

    delete from public.agendamento_cancelamento where agendamento_id = v_reserva.id;
    delete from public.agendamento where id = v_reserva.id;
    delete from public.horario_disponivel where id = v_horario.id;
    if (select to_jsonb(reserva) from public.agendamento as reserva where id = 23)
       is distinct from v_original then
        raise exception 'Limpeza: reserva original 23 alterada.';
    end if;
    perform set_config('doesangue_test.android_horario', v_horario.id::text, true);
    perform set_config('doesangue_test.android_reserva', v_reserva.id::text, true);
end;
$$;
select
    not exists (select 1 from public.horario_disponivel
        where id = current_setting('doesangue_test.android_horario')::bigint) as oferta_removida,
    not exists (select 1 from public.agendamento
        where id = current_setting('doesangue_test.android_reserva')::bigint) as reserva_removida,
    not exists (select 1 from public.agendamento_cancelamento
        where agendamento_id = current_setting('doesangue_test.android_reserva')::bigint) as evento_removido,
    'Reserva original 23 preservada' as original;
commit;
