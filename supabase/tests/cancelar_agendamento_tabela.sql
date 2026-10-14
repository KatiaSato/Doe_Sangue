-- Executar como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente leitura; não chama a função nem altera reservas.
select
    papeis.papel,
    tabela.relrowsecurity as rls_ativo,
    has_table_privilege(papeis.papel, tabela.oid, 'SELECT') as pode_consultar,
    has_table_privilege(papeis.papel, tabela.oid, 'INSERT') as pode_inserir,
    has_table_privilege(papeis.papel, tabela.oid, 'UPDATE') as pode_alterar,
    has_table_privilege(papeis.papel, tabela.oid, 'DELETE') as pode_excluir,
    has_table_privilege(papeis.papel, tabela.oid, 'TRUNCATE') as pode_esvaziar
from pg_class as tabela
cross join (values ('anon'), ('authenticated')) as papeis(papel)
where tabela.oid = 'public.agendamento_cancelamento'::regclass
order by papeis.papel;
