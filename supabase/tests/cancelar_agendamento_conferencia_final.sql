-- Executar como postgres após os ROLLBACKs e a limpeza concorrente.
-- Somente leitura; não chama funções de gravação nem avança sequências.
-- Quantidades esperadas correspondem ao seed confirmado desta etapa.
select 'Unidades de coleta' as item, count(*)::text as resultado
from public.unidade_coleta
union all
select 'Horários', count(*)::text from public.horario_disponivel
union all
select 'Perfis', count(*)::text from public.perfil_doador
union all
select 'Agendamentos', count(*)::text from public.agendamento
union all
select 'Eventos de cancelamento', count(*)::text from public.agendamento_cancelamento
union all
select 'Horário temporário 23', count(*)::text
from public.horario_disponivel where id = 23
union all
select 'Reservas temporárias 37/38', count(*)::text
from public.agendamento where id in (37, 38)
union all
select 'Eventos das reservas 37/38', count(*)::text
from public.agendamento_cancelamento where agendamento_id in (37, 38)
union all
select 'Estado da reserva original 23', coalesce(
    (select estado from public.agendamento where id = 23), 'Ausente'
)
union all
select 'Dados conferidos da reserva original 23', exists (
    select 1
    from public.agendamento as reserva
    join public.perfil_doador as perfil on perfil.id = reserva.doador_id
    join auth.users as conta on conta.id = perfil.id
    join public.horario_disponivel as horario on horario.id = reserva.horario_id
    join public.unidade_coleta as unidade on unidade.id = horario.unidade_id
    where reserva.id = 23
      and conta.email = 'doador.a@example.com'
      and unidade.nome = 'Unidade Demo A'
      and unidade.fuso_horario = 'America/Sao_Paulo'
      and horario.inicio at time zone unidade.fuso_horario = timestamp '2026-10-19 08:00:00'
      and reserva.estado = 'agendado'
      and reserva.criado_em = timestamptz '2026-10-12 23:14:35.586383+00'
)::text;
