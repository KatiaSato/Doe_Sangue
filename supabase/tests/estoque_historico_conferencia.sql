-- Somente leitura administrativa após migração/seed/testes; não exibe e-mails/UUIDs.
select 'Estoque total (inclui unidade oculta)' as item, count(*)::text as resultado from public.estoque_sangue
union all
select 'Histórico fictício total', count(*)::text from public.doacao_historico
union all
select 'Histórico de A', count(*)::text from public.doacao_historico h
join public.perfil_doador p on p.id = h.doador_id where p.nome_exibicao = 'Doador Demo A'
union all
select 'Histórico de C', count(*)::text from public.doacao_historico h
join public.perfil_doador p on p.id = h.doador_id where lower(p.nome_exibicao) = 'doador demo c'
union all
select 'Estado da reserva original 23', estado from public.agendamento where id = 23
union all
select 'Dados da reserva original 23 conferem', (a.estado = 'agendado'
    and a.criado_em = timestamptz '2026-10-12 23:14:35.586383+00'
    and p.nome_exibicao = 'Doador Demo A' and u.nome = 'Unidade Demo A'
    and h.inicio at time zone u.fuso_horario = timestamp '2026-10-19 08:00:00')::text
from public.agendamento a join public.perfil_doador p on p.id = a.doador_id
join public.horario_disponivel h on h.id = a.horario_id
join public.unidade_coleta u on u.id = h.unidade_id where a.id = 23;

select tablename, rowsecurity from pg_tables where schemaname = 'public'
    and tablename in ('estoque_sangue', 'doacao_historico') order by tablename;
select proname, prosecdef as security_definer, provolatile, proconfig
from pg_proc where pronamespace = 'public'::regnamespace
    and proname in ('consultar_estoques', 'consultar_minhas_doacoes') order by proname;
