-- Somente leitura: executar como postgres após cancelar a reserva 39 no Android.
-- Esperado: 23 agendado, zero eventos; 39 cancelado, um evento, autor_confere=true.
-- Datas/autor do evento de 23 ficam NULL porque ela não foi cancelada.
select
    reserva.id as reserva_id,
    reserva.estado,
    perfil.nome_exibicao as doador,
    reserva.criado_em,
    evento.cancelado_em,
    evento.cancelado_por = reserva.doador_id as autor_confere,
    (select count(*) from public.agendamento_cancelamento as registro
     where registro.agendamento_id = reserva.id) as total_eventos
from public.agendamento as reserva
join public.perfil_doador as perfil on perfil.id = reserva.doador_id
left join public.agendamento_cancelamento as evento
    on evento.agendamento_id = reserva.id
where reserva.id in (23, 39)
order by reserva.id;
