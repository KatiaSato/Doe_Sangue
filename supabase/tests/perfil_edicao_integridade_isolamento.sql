-- Executar inteiro como administrador no desenvolvimento: tudo termina em ROLLBACK.
-- Usa dois perfis existentes; não cria contas Auth nem altera dados persistentes.
begin;

create temporary table perfil_edicao_fixture on commit drop as
select id, row_number() over (order by id) as pos, to_jsonb(perfil) as original
from public.perfil_doador perfil order by id limit 2;
grant select on perfil_edicao_fixture to authenticated;

do $$ begin
    if (select count(*) from perfil_edicao_fixture) <> 2 then
        raise exception 'Este roteiro exige dois perfis sintéticos existentes.';
    end if;
    assert (select rowsecurity from pg_tables where schemaname = 'public' and tablename = 'perfil_doador');
    assert has_column_privilege('authenticated', 'public.perfil_doador', 'telefone', 'UPDATE');
    assert has_column_privilege('authenticated', 'public.perfil_doador', 'data_nascimento', 'UPDATE');
    assert not has_column_privilege('authenticated', 'public.perfil_doador', 'id', 'UPDATE');
    assert not has_column_privilege('authenticated', 'public.perfil_doador', 'criado_em', 'UPDATE');
end $$;

create temporary table perfil_edicao_reserva on commit drop as
select to_jsonb(a) as original from public.agendamento a where id = 23;

select set_config('request.jwt.claim.sub', (select id::text from perfil_edicao_fixture where pos = 1), true);
set local role authenticated;

update public.perfil_doador set nome_exibicao = 'Doador sintético em edição',
    tipo_sanguineo = 'NAO_SEI', telefone = '00900000000', data_nascimento = date '2000-02-29'
where id = (select id from perfil_edicao_fixture where pos = 1);

do $$ declare affected integer; begin
    assert (select count(*) = 1 from public.perfil_doador);
    assert (select telefone = '00900000000' and data_nascimento = date '2000-02-29'
        from public.perfil_doador where id = auth.uid());
    update public.perfil_doador set telefone = '0000000000'
        where id = (select id from perfil_edicao_fixture where pos = 2);
    get diagnostics affected = row_count;
    assert affected = 0, 'Não pode atualizar perfil de terceiros';
    begin
        update public.perfil_doador set telefone = 'abc' where id = auth.uid();
        raise exception 'Telefone inválido foi aceito';
    exception when check_violation then null; end;
    begin
        update public.perfil_doador set telefone = '' where id = auth.uid();
        raise exception 'Telefone vazio deve ser enviado como NULL';
    exception when check_violation then null; end;
    begin
        update public.perfil_doador set data_nascimento = current_date + 1 where id = auth.uid();
        raise exception 'Data futura foi aceita';
    exception when check_violation then null; end;
    begin
        update public.perfil_doador set id = gen_random_uuid() where id = auth.uid();
        raise exception 'Alteração de ID foi aceita';
    exception when insufficient_privilege then null; end;
end $$;

update public.perfil_doador set telefone = null, data_nascimento = null where id = auth.uid();
do $$ begin
    assert (select telefone is null and data_nascimento is null from public.perfil_doador where id = auth.uid());
end $$;

reset role;
do $$ begin
    assert (select to_jsonb(p) = f.original from public.perfil_doador p
        join perfil_edicao_fixture f on f.id = p.id where f.pos = 2), 'Perfil de terceiros preservado';
    assert not exists (select 1 from perfil_edicao_reserva f where f.original is distinct from
        (select to_jsonb(a) from public.agendamento a where id = 23)), 'Reserva 23 preservada';
end $$;

set local role anon;
do $$ begin
    begin
        perform 1 from public.perfil_doador;
        raise exception 'Leitura anônima foi aceita';
    exception when insufficient_privilege then null; end;
    begin
        update public.perfil_doador set telefone = '0000000000';
        raise exception 'Escrita anônima foi aceita';
    exception when insufficient_privilege then null; end;
end $$;
reset role;

select 'Nome/tipo/telefone/nascimento próprios conferidos' as edicao,
    'CHECKs, NULL e privilégios por coluna conferidos' as integridade,
    'Terceiros/anon bloqueados; reserva 23 preservada' as isolamento;
rollback;
