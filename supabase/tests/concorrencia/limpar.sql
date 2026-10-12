-- Executar INTEIRO como postgres SOMENTE depois de ambas as sessões terminarem.
-- Remove exclusivamente a oferta preparada (ID + início + unidade + capacidade)
-- e sua reserva sintética de A (ID 22 confirmado por captura).
-- Não exclui unidades, perfis ou contas.
-- Aborta se encontrar configuração/pedidos diferentes do cenário esperado.
-- COMMIT conclui a limpeza; repetir após limpeza concluída é inofensivo.
begin;

do $$
declare
    v_horario public.horario_disponivel%rowtype;
    v_doador_a uuid;
begin
    select horario.* into v_horario
    from public.horario_disponivel as horario
    where horario.id = 13
    for update;

    if not found then
        return;
    end if;

    if v_horario.inicio is distinct from timestamptz '2026-10-19 15:44:43.092005+00'
       or v_horario.capacidade <> 1 or not v_horario.ativo
       or not exists (
           select 1 from public.unidade_coleta
           where id = v_horario.unidade_id and nome = 'Unidade Demo A' and publicada
       ) then
        raise exception 'Limpeza interrompida: oferta não corresponde à preparação deste teste.';
    end if;

    select perfil.id into strict v_doador_a
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    if (select count(*) from public.agendamento where horario_id = 13) > 1
       or exists (
           select 1 from public.agendamento
           where horario_id = 13
             and (id <> 22 or doador_id <> v_doador_a or estado <> 'agendado')
       ) then
        raise exception 'Limpeza interrompida: reservas diferem do cenário esperado. Conferir sem excluir.';
    end if;

    delete from public.agendamento
    where id = 22 and horario_id = 13 and doador_id = v_doador_a;
    delete from public.horario_disponivel where id = 13;
end;
$$;

select
    not exists (select 1 from public.horario_disponivel where id = 13) as oferta_removida,
    not exists (select 1 from public.agendamento where horario_id = 13) as reservas_do_teste_removidas;

commit;
