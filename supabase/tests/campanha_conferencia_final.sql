-- Somente leitura: executar como postgres depois dos três testes em ROLLBACK.
-- Esperado: 2 unidades, 3 horários, 1 perfil, 1 agendamento, 0 eventos e 0 campanhas.
-- Total do domínio: 7; não conta contas gerenciadas pelo Supabase Auth.
-- Original 23: agendado e dados conhecidos conferidos=true.
with contagens as (
    select 1 as ordem, 'Unidades de coleta' as item, count(*) as quantidade
    from public.unidade_coleta
    union all select 2, 'Horários', count(*) from public.horario_disponivel
    union all select 3, 'Perfis', count(*) from public.perfil_doador
    union all select 4, 'Agendamentos', count(*) from public.agendamento
    union all select 5, 'Eventos de cancelamento', count(*) from public.agendamento_cancelamento
    union all select 6, 'Campanhas', count(*) from public.campanha
), conferencia as (
    select ordem, item, quantidade::text as resultado from contagens
    union all
    select 7, 'Unidades temporárias do teste de integridade', count(*)::text
    from public.unidade_coleta
    where nome = 'Unidade Temporária de Integridade de Campanhas'
    union all
    select 8, 'Total de registros do domínio', sum(quantidade)::text from contagens
    union all
    select 9, 'Estado da reserva original 23', coalesce(
        (select estado from public.agendamento where id = 23), 'AUSENTE')
    union all
    select 10, 'Dados conferidos da reserva original 23', exists (
        select 1 from public.agendamento as reserva
        join public.perfil_doador as perfil on perfil.id = reserva.doador_id
        join auth.users as conta on conta.id = perfil.id
        join public.horario_disponivel as horario on horario.id = reserva.horario_id
        join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
        where reserva.id = 23 and reserva.estado = 'agendado'
          and conta.email = 'doador.a@example.com'
          and unidade.nome = 'Unidade Demo A'
          and unidade.fuso_horario = 'America/Sao_Paulo'
          and horario.inicio at time zone unidade.fuso_horario = timestamp '2026-10-19 08:00:00'
          and reserva.criado_em = timestamptz '2026-10-12 23:14:35.586383+00'
    )::text
)
select item, resultado from conferencia order by ordem;
