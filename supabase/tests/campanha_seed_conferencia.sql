-- Executar como postgres após o seed de campanhas. Única consulta, somente leitura.
-- Esperado: quatro campanhas desta fonte, dois vínculos e reserva original 23 preservada.
-- As campanhas ocultas aparecem aqui porque esta é uma conferência administrativa.
with campanhas_seed as (
    select campanha.*, unidade.nome as unidade_nome,
        unidade.publicada as unidade_publicada,
        coalesce((
            select string_agg(vinculo.tipo_sanguineo::text, ', '
                order by vinculo.tipo_sanguineo)
            from public.campanha_tipo_sanguineo as vinculo
            where vinculo.campanha_id = campanha.id
        ), 'Geral') as tipos
    from public.campanha as campanha
    join public.unidade_coleta as unidade on unidade.id = campanha.unidade_id
    where campanha.fonte = 'Demonstração acadêmica Doe Sangue — campanhas v1'
), conferencia as (
    select 1 as ordem, titulo as item,
        format('ID %s | %s | campanha publicada: %s | unidade publicada: %s | %s',
            id, unidade_nome, publicada, unidade_publicada, tipos) as resultado
    from campanhas_seed
    union all
    select 2, 'Campanhas desta fonte', count(*)::text from campanhas_seed
    union all
    select 3, 'Vínculos desta fonte', count(*)::text
    from public.campanha_tipo_sanguineo
    where campanha_id in (select id from campanhas_seed)
    union all
    select 4, 'Estado da reserva original 23', coalesce(
        (select estado from public.agendamento where id = 23), 'AUSENTE')
    union all
    select 5, 'Dados conferidos da reserva original 23', exists (
        select 1 from public.agendamento reserva
        join public.perfil_doador perfil on perfil.id = reserva.doador_id
        join auth.users conta on conta.id = perfil.id
        join public.horario_disponivel horario on horario.id = reserva.horario_id
        join public.unidade_coleta unidade on unidade.id = horario.unidade_id
        where reserva.id = 23 and reserva.estado = 'agendado'
          and conta.email = 'doador.a@example.com'
          and unidade.nome = 'Unidade Demo A'
          and unidade.fuso_horario = 'America/Sao_Paulo'
          and horario.inicio at time zone unidade.fuso_horario = timestamp '2026-10-19 08:00:00'
          and reserva.criado_em = timestamptz '2026-10-12 23:14:35.586383+00'
    )::text
)
select item, resultado from conferencia order by ordem, item;
