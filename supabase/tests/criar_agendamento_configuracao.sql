-- Executar como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente leitura: confere a configuração e os privilégios da função.
select
    funcao.prosecdef as security_definer,
    funcao.provolatile as volatilidade,
    funcao.proconfig as configuracao
from pg_proc as funcao
where funcao.oid = 'public.criar_agendamento(bigint, uuid)'::regprocedure;

select
    papel,
    has_function_privilege(
        papel,
        'public.criar_agendamento(bigint, uuid)',
        'EXECUTE'
    ) as pode_executar,
    has_table_privilege(papel, 'public.agendamento', 'INSERT') as pode_inserir_diretamente
from (values ('anon'), ('authenticated')) as papeis(papel);
