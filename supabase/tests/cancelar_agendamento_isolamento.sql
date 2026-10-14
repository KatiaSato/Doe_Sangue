-- Executar INTEIRO como postgres no desenvolvimento acadêmico sintético.
-- Dois níveis não aceitos, cada um em uma transação independente.
-- Identidade A simulada; não verifica login/JWT pela API nem concorrência.
-- Usa ID positivo confirmado inexistente, sem preparar ou alterar registros.
-- Ambas as transações terminam em ROLLBACK.

begin;
set transaction isolation level repeatable read;

do $$
declare
    v_doador uuid;
begin
    select perfil.id into strict v_doador
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    if exists (select 1 from public.agendamento where id = 9223372036854775807::bigint) then
        raise exception 'Preparação: ID reservado para o teste precisa estar ausente.';
    end if;

    perform set_config('request.jwt.claim.sub', v_doador::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador, 'role', 'authenticated')::text, true);
end;
$$;

set local role authenticated;

do $$
begin
    if current_setting('transaction_isolation') <> 'repeatable read'
       or current_user <> 'authenticated' or auth.uid() is null then
        raise exception 'Preparação: isolamento, papel ou identidade incorretos.';
    end if;

    begin
        -- ID estruturalmente válido: a rejeição deve vir do isolamento,
        -- antes da busca da reserva, não de parâmetro ou identidade inválidos.
        perform public.cancelar_agendamento(9223372036854775807::bigint);
        raise exception 'Teste falhou: REPEATABLE READ foi aceito.';
    exception when sqlstate '25000' then
        if sqlerrm <> 'O cancelamento exige isolamento READ COMMITTED.' then raise; end if;
    end;
end;
$$;

rollback;

begin;
set transaction isolation level serializable;

do $$
declare
    v_doador uuid;
begin
    select perfil.id into strict v_doador
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    if exists (select 1 from public.agendamento where id = 9223372036854775807::bigint) then
        raise exception 'Preparação: ID reservado para o teste precisa estar ausente.';
    end if;

    perform set_config('request.jwt.claim.sub', v_doador::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador, 'role', 'authenticated')::text, true);
end;
$$;

set local role authenticated;

do $$
begin
    if current_setting('transaction_isolation') <> 'serializable'
       or current_user <> 'authenticated' or auth.uid() is null then
        raise exception 'Preparação: isolamento, papel ou identidade incorretos.';
    end if;

    begin
        perform public.cancelar_agendamento(9223372036854775807::bigint);
        raise exception 'Teste falhou: SERIALIZABLE foi aceito.';
    exception when sqlstate '25000' then
        if sqlerrm <> 'O cancelamento exige isolamento READ COMMITTED.' then raise; end if;
    end;
end;
$$;

rollback;

-- Resultado alcançado somente após as duas verificações sem erro.
select * from (values
    ('REPEATABLE READ', 'Bloqueado por 25000'),
    ('SERIALIZABLE', 'Bloqueado por 25000')
) as resultados(isolamento, resultado);
