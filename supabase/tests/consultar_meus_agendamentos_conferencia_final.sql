-- Executar como postgres. Somente leitura, após o ROLLBACK dos roteiros.
-- Conta a massa atual e verifica os vestígios identificáveis dos testes.
select 'Unidades de coleta' as item, count(*)::text as resultado
from public.unidade_coleta
union all
select 'Horários', count(*)::text from public.horario_disponivel
union all
select 'Perfis', count(*)::text from public.perfil_doador
union all
select 'Agendamentos', count(*)::text from public.agendamento
union all
select 'Unidades temporárias do roteiro', count(*)::text
from public.unidade_coleta
where nome like 'Unidade Teste Leitura Publicada %'
   or nome like 'Unidade Teste Leitura Oculta %'
union all
select 'Outras reservas no horário de 23', count(*)::text
from public.agendamento
where horario_id = (select horario_id from public.agendamento where id = 23)
  and id <> 23
union all
select 'Estado da reserva 23', coalesce(
    (select estado from public.agendamento where id = 23), 'Ausente'
);
