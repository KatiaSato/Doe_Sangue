-- Somente leitura. Conferir após a migração incremental de edição do perfil.
select column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public' and table_name = 'perfil_doador'
    and column_name in ('telefone', 'data_nascimento')
order by column_name;

select papel,
    has_column_privilege(papel, 'public.perfil_doador', 'telefone', 'SELECT') as pode_ler_telefone,
    has_column_privilege(papel, 'public.perfil_doador', 'telefone', 'UPDATE') as pode_editar_telefone,
    has_column_privilege(papel, 'public.perfil_doador', 'data_nascimento', 'UPDATE') as pode_editar_nascimento,
    has_column_privilege(papel, 'public.perfil_doador', 'id', 'UPDATE') as pode_mudar_id,
    has_column_privilege(papel, 'public.perfil_doador', 'criado_em', 'UPDATE') as pode_mudar_criacao
from (values ('anon'), ('authenticated')) as papeis(papel);

select tablename, rowsecurity from pg_tables
where schemaname = 'public' and tablename = 'perfil_doador';

select conname, pg_get_constraintdef(oid) as definicao
from pg_constraint where conrelid = 'public.perfil_doador'::regclass
    and conname in ('perfil_doador_telefone_check', 'perfil_doador_nascimento_check')
order by conname;
