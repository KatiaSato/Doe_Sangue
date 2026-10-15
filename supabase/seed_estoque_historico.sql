-- Publicação manual no desenvolvimento acadêmico APÓS a migração 20261015000300.
-- Somente INSERT de dados fictícios. Não altera perfis, reservas ou campanhas.
begin;
lock table public.estoque_sangue, public.doacao_historico in share row exclusive mode;
do $$
declare
    v_a bigint;
    v_b bigint;
    v_perfil_a uuid;
    v_perfil_c uuid;
    v_fonte constant text := 'Demonstração acadêmica Doe Sangue — estoque e histórico v1';
begin
    select id into strict v_a from public.unidade_coleta
    where nome = 'Unidade Demo A' and cidade = 'Cidade fictícia'
      and endereco = 'Endereço fictício A' and fuso_horario = 'America/Sao_Paulo' and publicada
    for share;
    select id into strict v_b from public.unidade_coleta
    where nome = 'Unidade Demo B' and cidade = 'Cidade fictícia'
      and endereco = 'Endereço fictício B' and fuso_horario = 'America/Sao_Paulo' and not publicada
    for share;
    select id into strict v_perfil_a from public.perfil_doador
    where nome_exibicao = 'Doador Demo A' for share;
    select id into strict v_perfil_c from public.perfil_doador
    where lower(nome_exibicao) = 'doador demo c' for share;

    insert into public.estoque_sangue (unidade_id, tipo_sanguineo, nivel, fonte, atualizado_em)
    select unidade.id, demo.tipo::public.tipo_sanguineo, demo.nivel, v_fonte,
        timestamptz '2026-10-15 12:00:00+00'
    from (values (v_a), (v_b)) as unidade(id)
    cross join (values ('A+', 'alerta'), ('A-', 'critico'), ('B+', 'estavel'),
        ('B-', 'alerta'), ('AB+', 'estavel'), ('AB-', 'alerta'), ('O+', 'estavel'), ('O-', 'critico'))
        as demo(tipo, nivel)
    on conflict (unidade_id, tipo_sanguineo) do nothing;

    -- Datas apenas fictícias: não ensinam intervalos de doação nem atestam coleta.
    insert into public.doacao_historico (doador_id, unidade_id, ocorrido_em, fonte, referencia)
    values
        (v_perfil_a, v_a, timestamptz '2026-05-10 13:00:00+00', v_fonte, 'demo-historico-v1-a-1'),
        (v_perfil_a, v_b, timestamptz '2025-10-10 13:00:00+00', v_fonte, 'demo-historico-v1-a-2'),
        (v_perfil_c, v_a, timestamptz '2026-07-10 13:00:00+00', v_fonte, 'demo-historico-v1-c-1')
    on conflict (referencia) do nothing;

    if (select count(*) from public.estoque_sangue where fonte = v_fonte) <> 16
        or (select count(*) from public.doacao_historico where fonte = v_fonte) <> 3 then
        raise exception 'Massa existente difere do seed esperado; revisar sem sobrescrever.';
    end if;
    if exists (select 1 from public.doacao_historico where fonte = v_fonte and
        ((referencia = 'demo-historico-v1-a-1' and (doador_id <> v_perfil_a or unidade_id <> v_a
            or ocorrido_em <> timestamptz '2026-05-10 13:00:00+00'))
         or (referencia = 'demo-historico-v1-a-2' and (doador_id <> v_perfil_a or unidade_id <> v_b
            or ocorrido_em <> timestamptz '2025-10-10 13:00:00+00'))
         or (referencia = 'demo-historico-v1-c-1' and (doador_id <> v_perfil_c or unidade_id <> v_a
            or ocorrido_em <> timestamptz '2026-07-10 13:00:00+00')))) then
        raise exception 'Referência de histórico reutilizada com dados diferentes.';
    end if;
end;
$$;
commit;
