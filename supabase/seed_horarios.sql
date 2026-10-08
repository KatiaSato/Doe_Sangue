-- Horários fictícios para testar a consulta e o RLS.
insert into public.horario_disponivel (
    unidade_id,
    inicio,
    capacidade,
    ativo
)
select
    unidade.id,
    demo.inicio_local at time zone unidade.fuso_horario,
    demo.capacidade,
    demo.ativo
from (
    values
        ('Unidade Demo A', timestamp '2026-10-19 08:00:00', 3, true),
        ('Unidade Demo A', timestamp '2026-10-19 09:30:00', 2, false),
        ('Unidade Demo B', timestamp '2026-10-19 08:00:00', 1, true)
) as demo(nome_unidade, inicio_local, capacidade, ativo)
join public.unidade_coleta as unidade
    on unidade.nome = demo.nome_unidade
   and unidade.cidade = 'Cidade fictícia'
on conflict (unidade_id, inicio) do nothing
returning id, unidade_id, inicio, capacidade, ativo;