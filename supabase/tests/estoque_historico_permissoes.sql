-- Executar inteiro como postgres após a migração. Sem escrita permanente.
begin;
do $$
declare
    v_papel text;
    v_comando text;
    v_estoque jsonb;
    v_historico jsonb;
begin
    select coalesce(jsonb_agg(to_jsonb(e) order by unidade_id, tipo_sanguineo), '[]') into v_estoque
    from public.estoque_sangue e;
    select coalesce(jsonb_agg(to_jsonb(h) order by id), '[]') into v_historico from public.doacao_historico h;
    foreach v_papel in array array['anon', 'authenticated'] loop
        execute format('set local role %I', v_papel);
        foreach v_comando in array array[
            'insert into public.estoque_sangue (unidade_id,tipo_sanguineo,nivel,fonte) values (-1,''A+'',''estavel'',''Teste'')',
            'update public.estoque_sangue set nivel = ''critico''',
            'delete from public.estoque_sangue',
            'truncate public.estoque_sangue',
            'select * from public.doacao_historico',
            'insert into public.doacao_historico (doador_id,unidade_id,ocorrido_em,fonte,referencia) values (null,-1,now(),''Teste'',''teste-bloqueio'')',
            'update public.doacao_historico set fonte = ''Teste''',
            'delete from public.doacao_historico',
            'truncate public.doacao_historico',
            'select nextval(''public.doacao_historico_id_seq'')',
            'select * from public.doacao_historico_id_seq'
        ] loop
            begin
                execute v_comando;
                raise exception 'Operação permitida para %: %', v_papel, v_comando;
            exception when insufficient_privilege then null;
            end;
        end loop;
        reset role;
    end loop;
    if (select coalesce(jsonb_agg(to_jsonb(e) order by unidade_id, tipo_sanguineo), '[]') from public.estoque_sangue e)
        is distinct from v_estoque
        or (select coalesce(jsonb_agg(to_jsonb(h) order by id), '[]') from public.doacao_historico h)
        is distinct from v_historico then
        raise exception 'Dados modificados pelas tentativas bloqueadas.';
    end if;
end;
$$;
select 'Escrita e sequência bloqueadas para ambos os papéis' as acesso, 'Dados preservados' as dados;
rollback;
