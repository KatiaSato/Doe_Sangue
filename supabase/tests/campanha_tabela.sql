-- Somente leitura: executar como postgres após aplicar a migração de campanha.
-- Esperado nos dois papéis: RLS=true, SELECT=true e demais privilégios=false.
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
where tabela.oid = 'public.campanha'::regclass
order by papeis.papel;
