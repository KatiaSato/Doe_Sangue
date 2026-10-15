-- Somente leitura, como postgres, APÓS a migração 20261015000200.
-- Esperado: perfil NOT NULL/default NAO_SEI; RLS dos vínculos ativo;
-- RPC invoker/STABLE, trigger definer/search_path vazio, execução pública da RPC.
select column_name, udt_name, is_nullable, column_default
from information_schema.columns
where table_schema = 'public' and table_name = 'perfil_doador'
  and column_name = 'tipo_sanguineo';

select tablename, rowsecurity from pg_tables
where schemaname = 'public' and tablename in ('perfil_doador', 'campanha_tipo_sanguineo');

select p.proname, p.prosecdef as security_definer, p.provolatile as volatilidade,
    p.proconfig as configuracao, r.papel,
    has_function_privilege(r.papel, p.oid, 'EXECUTE') as pode_executar
from pg_proc p join pg_namespace n on n.oid = p.pronamespace
cross join (values ('anon'), ('authenticated')) as r(papel)
where n.nspname = 'public' and p.proname in ('criar_perfil_no_cadastro', 'consultar_campanhas')
order by p.proname, r.papel;

select tgname, tgenabled, pg_get_triggerdef(oid) as definicao
from pg_trigger where tgrelid = 'auth.users'::regclass
and tgname = 'criar_perfil_apos_cadastro';

select r.papel,
    has_column_privilege(r.papel, 'public.perfil_doador', 'tipo_sanguineo', 'SELECT') as ler_tipo,
    has_column_privilege(r.papel, 'public.perfil_doador', 'tipo_sanguineo', 'UPDATE') as editar_tipo,
    has_column_privilege(r.papel, 'public.perfil_doador', 'id', 'UPDATE') as mudar_dono,
    has_table_privilege(r.papel, 'public.campanha_tipo_sanguineo', 'SELECT') as ler_vinculos,
    has_table_privilege(r.papel, 'public.campanha_tipo_sanguineo', 'INSERT,UPDATE,DELETE,TRUNCATE') as escrever_vinculos
from (values ('anon'), ('authenticated')) as r(papel);

select conname, pg_get_constraintdef(oid) as definicao
from pg_constraint where conrelid = 'public.campanha_tipo_sanguineo'::regclass;

select tablename, policyname, cmd, roles, qual, with_check
from pg_policies where schemaname = 'public'
and tablename in ('perfil_doador', 'campanha_tipo_sanguineo')
order by tablename, policyname;
