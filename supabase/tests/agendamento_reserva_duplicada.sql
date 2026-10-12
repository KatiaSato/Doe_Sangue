-- Teste administrativo no desenvolvimento, somente com dados sintéticos.
-- Executar o arquivo inteiro no SQL Editor como postgres.
-- Não é uma migração nem a operação de reserva utilizada pelo aplicativo.
-- ROLLBACK remove os registros de teste; a sequência de IDs pode avançar.
begin;

do $$
declare
    doador_teste uuid;
    horario_teste bigint;
    restricao text;
begin
    select perfil.id
    into doador_teste
    from public.perfil_doador as perfil
    join auth.users as conta
        on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    if doador_teste is null then
        raise exception 'Preparação: o perfil do Doador Demo A precisa existir.';
    end if;

    select horario.id
    into horario_teste
    from public.horario_disponivel as horario
    join public.unidade_coleta as unidade
        on unidade.id = horario.unidade_id
    where unidade.nome = 'Unidade Demo A'
      and unidade.publicada
      and horario.ativo
      and not exists (
          select 1
          from public.agendamento as agendamento
          where agendamento.doador_id = doador_teste
            and agendamento.horario_id = horario.id
            and agendamento.estado = 'agendado'
      )
    order by horario.id
    limit 1;

    if horario_teste is null then
        raise exception 'Preparação: precisa existir um horário ativo da Demo A sem reserva ativa desse doador.';
    end if;

    insert into public.agendamento (
        doador_id, horario_id, estado, chave_idempotencia
    )
    values (
        doador_teste, horario_teste, 'agendado', gen_random_uuid()
    );

    begin
        -- Outra chave representa outro pedido para o mesmo doador/horário.
        insert into public.agendamento (
            doador_id, horario_id, estado, chave_idempotencia
        )
        values (
            doador_teste, horario_teste, 'agendado', gen_random_uuid()
        );

        raise exception 'Teste falhou: reserva ativa duplicada foi aceita.';

    exception
        when unique_violation then
            get stacked diagnostics
                restricao = constraint_name;

            if restricao <> 'agendamento_ativo_doador_horario' then
                raise;
            end if;
    end;

    -- Um registro cancelado pode coexistir com a reserva ativa.
    insert into public.agendamento (
        doador_id, horario_id, estado, chave_idempotencia
    )
    values (
        doador_teste, horario_teste, 'cancelado', gen_random_uuid()
    );
end;
$$;

select
    'Reserva ativa duplicada bloqueada' as reserva_ativa,
    'Registro cancelado aceito' as historico;

rollback;
