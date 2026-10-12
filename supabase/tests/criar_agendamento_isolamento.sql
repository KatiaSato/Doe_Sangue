-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Testa dois níveis não aceitos, cada um em uma transação independente.
-- Identidade A simulada; não verifica JWT pela API nem concorrência.
-- Não prepara novos registros; ambas as transações terminam em ROLLBACK.

begin;
set transaction isolation level repeatable read;

do $$
declare
    v_doador_id uuid;
begin
    select perfil.id into strict v_doador_id
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    perform set_config('request.jwt.claim.sub', v_doador_id::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador_id, 'role', 'authenticated')::text, true);
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
        -- Parâmetros válidos; a rejeição precisa vir do nível de isolamento.
        perform public.criar_agendamento(1::bigint, gen_random_uuid());
        raise exception 'Teste falhou: REPEATABLE READ foi aceito.';
    exception
        when raise_exception then
            if sqlerrm <> 'A reserva exige isolamento READ COMMITTED.' then
                raise;
            end if;
    end;
end;
$$;

rollback;

begin;
set transaction isolation level serializable;

do $$
declare
    v_doador_id uuid;
begin
    select perfil.id into strict v_doador_id
    from public.perfil_doador as perfil
    join auth.users as conta on conta.id = perfil.id
    where conta.email = 'doador.a@example.com';

    perform set_config('request.jwt.claim.sub', v_doador_id::text, true);
    perform set_config('request.jwt.claims',
        json_build_object('sub', v_doador_id, 'role', 'authenticated')::text, true);
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
        perform public.criar_agendamento(1::bigint, gen_random_uuid());
        raise exception 'Teste falhou: SERIALIZABLE foi aceito.';
    exception
        when raise_exception then
            if sqlerrm <> 'A reserva exige isolamento READ COMMITTED.' then
                raise;
            end if;
    end;
end;
$$;

rollback;

-- Este resultado só é alcançado após as duas verificações sem erro.
select * from (values
    ('REPEATABLE READ', 'Bloqueado'),
    ('SERIALIZABLE', 'Bloqueado')
) as resultados(isolamento, resultado);
