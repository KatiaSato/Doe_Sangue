-- Teste administrativo no desenvolvimento, somente com dados sintéticos.
-- Executar o arquivo inteiro no SQL Editor como postgres.
-- ROLLBACK encerra o teste; a sequência de IDs pode avançar.
begin;

do $$
declare
    doador_teste uuid;
    horario_teste bigint;
    doador_inexistente uuid := gen_random_uuid();
    horario_inexistente bigint;
    restricao text;
begin
    select perfil.id
    into doador_teste
    from public.perfil_doador as perfil
    join auth.users as conta
        on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    select horario.id
    into horario_teste
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade
        on unidade.id = horario.unidade_id
    where unidade.nome = 'Unidade Demo A'
      and unidade.publicada
      and horario.ativo
    order by horario.id
    limit 1;

    if doador_teste is null or horario_teste is null then
        raise exception 'Preparação: perfil A e horário ativo/publicado da Demo A precisam existir.';
    end if;

    if exists (
        select 1 from public.perfil_doador where id = doador_inexistente
    ) then
        raise exception 'Preparação: o UUID de teste precisa ser inexistente.';
    end if;

    -- Escolhe um ID acima de todos os horários existentes no desenvolvimento.
    select coalesce(max(id), 0) + 1
    into horario_inexistente
    from public.horario_disponivel;

    begin
        -- Somente o doador é inexistente; o horário é válido.
        insert into public.agendamento (
            doador_id, horario_id, estado, chave_idempotencia
        )
        values (
            doador_inexistente, horario_teste, 'cancelado', gen_random_uuid()
        );

        raise exception 'Teste falhou: doador inexistente foi aceito.';

    exception
        when foreign_key_violation then
            get stacked diagnostics restricao = constraint_name;
            if restricao <> 'agendamento_doador_id_fkey' then
                raise;
            end if;
    end;

    begin
        -- Somente o horário é inexistente; o doador é válido.
        insert into public.agendamento (
            doador_id, horario_id, estado, chave_idempotencia
        )
        values (
            doador_teste, horario_inexistente, 'cancelado', gen_random_uuid()
        );

        raise exception 'Teste falhou: horário inexistente foi aceito.';

    exception
        when foreign_key_violation then
            get stacked diagnostics restricao = constraint_name;
            if restricao <> 'agendamento_horario_id_fkey' then
                raise;
            end if;
    end;
end;
$$;

select
    'Doador inexistente bloqueado' as fk_doador,
    'Horário inexistente bloqueado' as fk_horario;

rollback;
