-- Executar INTEIRO como postgres após a nova migração, somente desenvolvimento.
-- Usa as contas sintéticas A/B já existentes. Não insere contas em auth.users.
-- Perfis temporários e alterações de tipo ficam em ROLLBACK; não é login/JWT/API.
begin;
do $$
declare v_a uuid; v_b uuid;
begin
    select id into strict v_a from auth.users where email = 'doador.a@example.com';
    select id into strict v_b from auth.users where email = 'doador.b@example.com';
    if v_a = v_b then raise exception 'Contas A/B precisam ser diferentes.'; end if;
    perform set_config('doesangue_test.tipo_a', v_a::text, true);
    perform set_config('doesangue_test.tipo_b', v_b::text, true);
    insert into public.perfil_doador(id, nome_exibicao)
    values(v_a, 'Doador temporário A'), (v_b, 'Doador temporário B') on conflict(id) do nothing;
    perform set_config('doesangue_test.tipo_perfil_b',
        (select to_jsonb(p)::text from public.perfil_doador p where id = v_b), true);
    perform set_config('doesangue_test.tipo_reserva',
        (select to_jsonb(a)::text from public.agendamento a where id = 23), true);
    if current_setting('doesangue_test.tipo_reserva', true) is null then
        raise exception 'Reserva original 23 ausente.';
    end if;
end;
$$;
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('doesangue_test.tipo_a'), true);
do $$
declare v_linhas integer;
begin
    update public.perfil_doador set tipo_sanguineo = 'O-'
    where id = current_setting('doesangue_test.tipo_a')::uuid;
    get diagnostics v_linhas = row_count;
    if v_linhas <> 1 or (select tipo_sanguineo from public.perfil_doador
        where id = current_setting('doesangue_test.tipo_a')::uuid) <> 'O-' then
        raise exception 'A não conseguiu atualizar/ler o próprio tipo.';
    end if;
    if exists(select 1 from public.perfil_doador where id = current_setting('doesangue_test.tipo_b')::uuid) then
        raise exception 'A consultou perfil de B.';
    end if;
    update public.perfil_doador set tipo_sanguineo = 'A+'
    where id = current_setting('doesangue_test.tipo_b')::uuid;
    get diagnostics v_linhas = row_count;
    if v_linhas <> 0 then raise exception 'A alterou perfil de B.'; end if;
    update public.perfil_doador set tipo_sanguineo = 'NAO_SEI'
    where id = current_setting('doesangue_test.tipo_a')::uuid;
    if (select tipo_sanguineo from public.perfil_doador
        where id = current_setting('doesangue_test.tipo_a')::uuid) <> 'NAO_SEI' then
        raise exception 'Não sei não foi persistido.';
    end if;
    begin
        update public.perfil_doador set id = id where id = current_setting('doesangue_test.tipo_a')::uuid;
        raise exception 'UPDATE de ID foi permitido.';
    exception when insufficient_privilege then null; end;
    begin
        update public.perfil_doador set tipo_sanguineo = null
        where id = current_setting('doesangue_test.tipo_a')::uuid;
        raise exception 'NULL foi permitido.';
    exception when not_null_violation then null; end;
end;
$$;
reset role;
set local role anon;
do $$
begin
    begin perform tipo_sanguineo from public.perfil_doador;
        raise exception 'anon consultou tipos privados.';
    exception when insufficient_privilege then null; end;
    begin update public.perfil_doador set tipo_sanguineo = 'O-';
        raise exception 'anon editou perfil.';
    exception when insufficient_privilege then null; end;
end;
$$;
reset role;
do $$
begin
    if (select to_jsonb(p) from public.perfil_doador p where id = current_setting('doesangue_test.tipo_b')::uuid)
        is distinct from current_setting('doesangue_test.tipo_perfil_b')::jsonb then
        raise exception 'Perfil B alterado.';
    end if;
    if (select to_jsonb(a) from public.agendamento a where id = 23)
        is distinct from current_setting('doesangue_test.tipo_reserva')::jsonb then
        raise exception 'Reserva 23 alterada.';
    end if;
end;
$$;
select 'Tipo próprio atualizado e Não sei aceito' as perfil,
    'Leitura/escrita de B bloqueadas para A; anon bloqueado' as isolamento,
    'B e reserva 23 preservados; alterações desfeitas no ROLLBACK' as original;
rollback;
