-- Depois da migração. Executar inteiro como postgres; dados temporários em ROLLBACK.
-- IDs de sequência podem avançar. Em erro: ROLLBACK antes de repetir.
begin;
do $$
declare
    v_unidade bigint;
    v_perfil uuid;
    v_historico public.doacao_historico%rowtype;
    v_original jsonb;
begin
    select to_jsonb(a) into strict v_original from public.agendamento a where id = 23;
    select id into strict v_perfil from public.perfil_doador where nome_exibicao = 'Doador Demo A';
    insert into public.unidade_coleta (nome, cidade, endereco, fuso_horario)
    values ('Unidade temporária estoque/histórico', 'Cidade fictícia', 'Endereço de teste', 'America/Sao_Paulo')
    returning id into v_unidade;
    insert into public.estoque_sangue (unidade_id, tipo_sanguineo, nivel, fonte)
    values (v_unidade, 'A+', 'estavel', 'Teste sintético');
    begin
        insert into public.estoque_sangue (unidade_id, tipo_sanguineo, nivel, fonte)
        values (v_unidade, 'A+', 'alerta', 'Teste');
        raise exception 'PK duplicada foi aceita.';
    exception when unique_violation then null;
    end;
    begin
        insert into public.estoque_sangue (unidade_id, tipo_sanguineo, nivel, fonte)
        values (v_unidade, 'NAO_SEI', 'estavel', 'Teste');
        raise exception 'Não sei foi aceito como estoque.';
    exception when check_violation then null;
    end;
    begin
        update public.estoque_sangue set nivel = 'invalido' where unidade_id = v_unidade;
        raise exception 'Nível desconhecido aceito.';
    exception when check_violation then null;
    end;
    begin
        update public.estoque_sangue set sintetico = false where unidade_id = v_unidade;
        raise exception 'Estoque real aceito.';
    exception when check_violation then null;
    end;
    begin
        update public.estoque_sangue set fonte = '  ' where unidade_id = v_unidade;
        raise exception 'Fonte vazia aceita.';
    exception when check_violation then null;
    end;
    begin
        update public.estoque_sangue set nivel = null where unidade_id = v_unidade;
        raise exception 'NULL obrigatório aceito.';
    exception when not_null_violation then null;
    end;
    begin
        insert into public.estoque_sangue (unidade_id, tipo_sanguineo, nivel, fonte)
        values (-1, 'A+', 'estavel', 'Teste');
        raise exception 'Estoque com unidade inexistente aceito.';
    exception when foreign_key_violation then null;
    end;

    insert into public.doacao_historico (doador_id, unidade_id, ocorrido_em, fonte, referencia)
    values (v_perfil, v_unidade, timestamptz '2026-01-01 12:00:00+00', 'Teste sintético', 'teste-integridade-historico')
    returning * into v_historico;
    if v_historico.id <= 0 or not v_historico.sintetico or v_historico.criado_em <> now() then
        raise exception 'Defaults de histórico incorretos.';
    end if;
    begin
        insert into public.doacao_historico (doador_id, unidade_id, ocorrido_em, fonte, referencia)
        values (v_perfil, v_unidade, now(), 'Teste', 'teste-integridade-historico');
        raise exception 'Referência duplicada aceita.';
    exception when unique_violation then null;
    end;
    begin
        update public.doacao_historico set sintetico = false where id = v_historico.id;
        raise exception 'Histórico real aceito.';
    exception when check_violation then null;
    end;
    begin
        update public.doacao_historico set fonte = '' where id = v_historico.id;
        raise exception 'Fonte vazia aceita.';
    exception when check_violation then null;
    end;
    begin
        update public.doacao_historico set referencia = '  ' where id = v_historico.id;
        raise exception 'Referência vazia aceita.';
    exception when check_violation then null;
    end;
    begin
        update public.doacao_historico set doador_id = null where id = v_historico.id;
        raise exception 'Doador NULL aceito.';
    exception when not_null_violation then null;
    end;
    begin
        update public.doacao_historico set doador_id = '00000000-0000-0000-0000-000000000000' where id = v_historico.id;
        raise exception 'Perfil inexistente aceito.';
    exception when foreign_key_violation then null;
    end;
    begin
        update public.doacao_historico set unidade_id = -1 where id = v_historico.id;
        raise exception 'Unidade inexistente aceita.';
    exception when foreign_key_violation then null;
    end;
    begin
        delete from public.unidade_coleta where id = v_unidade;
        raise exception 'Unidade referenciada excluída.';
    exception when foreign_key_violation or sqlstate '23001' then null;
    end;
    if (select to_jsonb(a) from public.agendamento a where id = 23) is distinct from v_original then
        raise exception 'Reserva 23 alterada.';
    end if;
end;
$$;
select 'PK/FKs, defaults, CHECKs e NULL conferidos' as integridade, 'Reserva 23 preservada' as original;
rollback;
