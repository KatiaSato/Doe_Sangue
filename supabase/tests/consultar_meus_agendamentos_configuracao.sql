-- Executar como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente leitura; não chama a função nem altera reservas.
select
    papeis.papel,
    has_function_privilege(
        papeis.papel,
        'public.consultar_meus_agendamentos()',
        'EXECUTE'
    ) as pode_executar,
    funcao.prosecdef as security_definer,
    funcao.provolatile as volatilidade,
    funcao.proconfig as configuracao
from pg_proc as funcao
cross join (values ('anon'), ('authenticated')) as papeis(papel)
where funcao.oid = 'public.consultar_meus_agendamentos()'::regprocedure
order by papeis.papel;
