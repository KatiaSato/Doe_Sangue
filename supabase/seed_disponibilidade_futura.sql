-- Somente ofertas fictícias para demonstração de T03. Não altera reservas/perfis.
begin;
insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
select unidade.id, demo.inicio_local at time zone unidade.fuso_horario, demo.capacidade, true
from public.unidade_coleta as unidade
cross join (values
    (timestamp '2026-10-28 08:00:00',2),
    (timestamp '2026-10-28 10:00:00',3),
    (timestamp '2026-10-29 08:00:00',1)
) as demo(inicio_local,capacidade)
where unidade.nome='Unidade Demo A' and unidade.cidade='Cidade fictícia'
  and unidade.endereco='Endereço fictício A' and unidade.publicada
  and demo.inicio_local at time zone unidade.fuso_horario > statement_timestamp()
on conflict (unidade_id,inicio) do nothing
returning id,unidade_id,inicio,capacidade,ativo;
commit;
