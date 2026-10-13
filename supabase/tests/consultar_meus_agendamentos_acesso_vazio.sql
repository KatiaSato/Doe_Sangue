-- Executar INTEIRO como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente leitura e configurações locais; não insere/altera/exclui reservas.
-- Simula papel/identidade; não realiza login nem verifica JWT pela API.
-- Requer a conta sintética B sem reservas e a reserva 23 existente.
begin;

do $$
declare
    v_b uuid;
    v_original jsonb;
begin
    select conta.id into strict v_b
    from auth.users as conta
    where conta.email = 'doador.b@example.com';

    if exists (select 1 from public.agendamento where doador_id = v_b) then
        raise exception 'Preparação: este cenário exige B sem reservas.';
    end if;

    select to_jsonb(reserva) into strict v_original
    from public.agendamento as reserva where reserva.id = 23;

    perform set_config('doesangue_test.doador_b', v_b::text, true);
    perform set_config('doesangue_test.original_23', v_original::text, true);
end;
$$;

set local role authenticated;

do $$
declare
    v_b uuid := current_setting('doesangue_test.doador_b')::uuid;
    v_quantidade bigint;
    v_negado boolean := false;
    v_mensagem text;
begin
    perform set_config('request.jwt.claim.sub', v_b::text, true);
    perform set_config(
        'request.jwt.claims',
        json_build_object('sub', v_b, 'role', 'authenticated')::text,
        true
    );

    if current_user <> 'authenticated' or auth.uid() is distinct from v_b then
        raise exception 'Teste falhou: papel/identidade B incorretos.';
    end if;

    select count(*) into v_quantidade from public.consultar_meus_agendamentos();
    if v_quantidade <> 0 then
        raise exception 'Teste falhou: B sem reservas precisa receber lista vazia.';
    end if;

    perform set_config('request.jwt.claim.sub', '', true);
    perform set_config('request.jwt.claims', '{"role":"authenticated"}', true);
    if auth.uid() is not null then
        raise exception 'Teste falhou: cenário sem identidade mal preparado.';
    end if;

    begin
        perform 1 from public.consultar_meus_agendamentos();
    exception when insufficient_privilege then
        get stacked diagnostics v_mensagem = message_text;
        if v_mensagem is distinct from
            'É necessário entrar na conta para consultar os agendamentos.' then
            raise exception 'Teste falhou: recebeu outro erro 42501, não a validação de identidade.';
        end if;
        v_negado := true;
    end;
    if not v_negado then
        raise exception 'Teste falhou: authenticated sem identidade conseguiu executar.';
    end if;

    perform set_config('request.jwt.claims', '{"role":"anon"}', true);
end;
$$;

set local role anon;

do $$
declare
    v_negado boolean := false;
begin
    if current_user <> 'anon' or auth.uid() is not null then
        raise exception 'Teste falhou: cenário visitante incorreto.';
    end if;

    begin
        perform 1 from public.consultar_meus_agendamentos();
    exception when insufficient_privilege then
        v_negado := true;
    end;
    if not v_negado then
        raise exception 'Teste falhou: anon conseguiu executar a função.';
    end if;
end;
$$;

reset role;

do $$
begin
    if (
        select to_jsonb(reserva) from public.agendamento as reserva where reserva.id = 23
    ) is distinct from current_setting('doesangue_test.original_23')::jsonb then
        raise exception 'Teste falhou: a reserva 23 foi alterada.';
    end if;
end;
$$;

select 'Conta B sem reservas' as teste, 'Lista vazia' as resultado
union all select 'Authenticated sem identidade', 'Bloqueado por 42501'
union all select 'Execução como anon', 'Bloqueada'
union all select 'Reserva 23', 'Preservada';

rollback;
