-- Executar inteiro como postgres APÓS o seed. Papéis/identidade simulados, sem JWT/API.
-- Nenhuma alteração permanente; em erro executar ROLLBACK antes de continuar.
begin;
do $$
declare
    v_a uuid;
    v_c uuid;
    v_reserva text;
begin
    select id into strict v_a from public.perfil_doador where nome_exibicao = 'Doador Demo A';
    select id into strict v_c from public.perfil_doador where lower(nome_exibicao) = 'doador demo c';
    perform set_config('test.perfil_a', v_a::text, true);
    perform set_config('test.perfil_c', v_c::text, true);
    select row_to_json(a)::text into strict v_reserva from public.agendamento a where id = 23;
    perform set_config('test.reserva_23', v_reserva, true);
end;
$$;

set local role anon;
do $$
begin
    if (select count(*) from public.consultar_estoques()
        where fonte = 'Demonstração acadêmica Doe Sangue — estoque e histórico v1') <> 8
        or exists (select 1 from public.consultar_estoques() where unidade_nome = 'Unidade Demo B') then
        raise exception 'Estoque público incorreto ou unidade oculta exposta.';
    end if;
    begin
        perform * from public.consultar_minhas_doacoes();
        raise exception 'Visitante consultou histórico.';
    exception when insufficient_privilege then null;
    end;
end;
$$;
reset role;

select set_config('request.jwt.claim.sub', current_setting('test.perfil_a'), true);
set local role authenticated;
do $$
begin
    if (select count(*) from public.consultar_minhas_doacoes()
        where fonte = 'Demonstração acadêmica Doe Sangue — estoque e histórico v1') <> 2
        or not exists (select 1 from public.consultar_minhas_doacoes() where unidade_nome = 'Unidade Demo B') then
        raise exception 'A não recebeu os dois registros próprios, incluindo unidade oculta.';
    end if;
    if (select count(*) from public.consultar_estoques()
        where fonte = 'Demonstração acadêmica Doe Sangue — estoque e histórico v1') <> 8 then
        raise exception 'Estoque autenticado incorreto.';
    end if;
end;
$$;
reset role;

select set_config('request.jwt.claim.sub', current_setting('test.perfil_c'), true);
set local role authenticated;
do $$
begin
    if (select count(*) from public.consultar_minhas_doacoes()
        where fonte = 'Demonstração acadêmica Doe Sangue — estoque e histórico v1') <> 1 then
        raise exception 'C recebeu histórico incorreto ou de A.';
    end if;
end;
$$;
reset role;
select set_config('request.jwt.claim.sub', '', true);
set local role authenticated;
do $$
begin
    begin
        perform * from public.consultar_minhas_doacoes();
        raise exception 'Histórico sem identidade foi aceito.';
    exception when insufficient_privilege then null;
    end;
end;
$$;
reset role;

do $$
declare
    v_papel text;
begin
    foreach v_papel in array array['anon', 'authenticated'] loop
        if has_table_privilege(v_papel, 'public.estoque_sangue', 'INSERT,UPDATE,DELETE,TRUNCATE')
            or has_table_privilege(v_papel, 'public.doacao_historico', 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE')
            or has_sequence_privilege(v_papel, 'public.doacao_historico_id_seq', 'USAGE,SELECT,UPDATE') then
            raise exception 'Privilégio direto inesperado para %.', v_papel;
        end if;
    end loop;
    if (select row_to_json(a)::text from public.agendamento a where id = 23)
        is distinct from current_setting('test.reserva_23') then
        raise exception 'Reserva 23 modificada.';
    end if;
end;
$$;
select 'Somente unidade publicada' as estoque, 'A=2 / C=1; isolados' as historico,
    'Visitante e identidade ausente bloqueados' as acesso, 'Reserva 23 preservada' as original;
rollback;
