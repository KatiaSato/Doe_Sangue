-- Preparação do teste em duas sessões, somente no desenvolvimento sintético.
-- Executar INTEIRO como postgres, UMA VEZ, antes das sessões A e B.
-- Este arquivo usa COMMIT: a oferta precisa ser visível nas duas sessões.
-- Anotar ID/início retornados para os roteiros de execução e limpeza.
-- Não insere reservas nem cria/modifica perfis.
begin;

do $$
declare
    v_unidade_id bigint;
    v_horario_id bigint;
begin
    select id into strict v_unidade_id
    from public.unidade_coleta
    where nome = 'Unidade Demo A' and publicada;

    if not exists (
        select 1 from public.perfil_doador as perfil
        join auth.users as conta on conta.id = perfil.id
        where conta.email = 'doador.a@example.com'
    ) or not exists (
        select 1 from auth.users where email = 'doador.b@example.com'
    ) then
        raise exception 'Preparação: A com perfil e conta B precisam existir.';
    end if;

    insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
    values (v_unidade_id, clock_timestamp() + interval '7 days', 1, true)
    returning id into v_horario_id;

    perform set_config('doesangue_test.concorrencia_horario_id', v_horario_id::text, true);
end;
$$;

select id as horario_id, inicio, capacidade
from public.horario_disponivel
where id = current_setting('doesangue_test.concorrencia_horario_id')::bigint;

commit;
