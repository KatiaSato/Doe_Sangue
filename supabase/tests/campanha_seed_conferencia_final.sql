-- Somente leitura, como postgres, após seed de campanhas e percurso no Android.
-- Esperado: 2 unidades, 3 horários, 2 perfis, 1 reserva, 0 eventos, 4 campanhas, 2 vínculos.
-- Total esperado = 14 registros do domínio; contas Auth são contadas separadamente.
-- Perfil C continua NAO_SEI/confirmado; reserva original 23 agendada e preservada.
-- Não mostra endereço pessoal, UUID, senha ou tokens.
with contagens as (
    select 1 as ordem, 'Unidades de coleta' as item, count(*) as quantidade
    from public.unidade_coleta
    union all select 2, 'Horários', count(*) from public.horario_disponivel
    union all select 3, 'Perfis', count(*) from public.perfil_doador
    union all select 4, 'Agendamentos', count(*) from public.agendamento
    union all select 5, 'Eventos de cancelamento', count(*) from public.agendamento_cancelamento
    union all select 6, 'Campanhas', count(*) from public.campanha
    union all select 7, 'Vínculos campanha/tipo', count(*) from public.campanha_tipo_sanguineo
), conferencia as (
    select ordem, item, quantidade::text as resultado from contagens
    union all
    select 8, 'Total de registros do domínio', sum(quantidade)::text from contagens
    union all
    select 9, 'Tipo sanguíneo do perfil C', coalesce((
        select tipo_sanguineo::text from public.perfil_doador
        where lower(btrim(nome_exibicao)) = 'doador demo c'
    ), 'AUSENTE')
    union all
    select 10, 'E-mail de C confirmado', exists (
        select 1 from public.perfil_doador perfil
        join auth.users conta on conta.id = perfil.id
        where lower(btrim(perfil.nome_exibicao)) = 'doador demo c'
          and conta.email_confirmed_at is not null
    )::text
    union all
    select 11, 'Dados conferidos do perfil C', exists (
        select 1 from public.perfil_doador perfil
        join auth.users conta on conta.id = perfil.id
        where lower(btrim(perfil.nome_exibicao)) = 'doador demo c'
          and perfil.tipo_sanguineo = 'NAO_SEI'
          and perfil.criado_em = timestamptz '2026-10-15 16:24:09.945178+00'
          and conta.raw_user_meta_data->>'nome_exibicao' = perfil.nome_exibicao
          and conta.raw_user_meta_data->>'tipo_sanguineo' = 'NAO_SEI'
    )::text
    union all
    select 12, 'Estado da reserva original 23', coalesce(
        (select estado from public.agendamento where id = 23), 'AUSENTE')
    union all
    select 13, 'Dados conferidos da reserva original 23', exists (
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
select item, resultado from conferencia order by ordem;
