-- Somente leitura: conferir PK, FK com RESTRICT e quatro CHECKs.
-- As nove colunas NOT NULL serão conferidas separadamente em campanha_colunas.sql.
select
    conname as nome,
    contype as tipo,
    pg_get_constraintdef(oid) as definicao
from pg_constraint
where conrelid = 'public.campanha'::regclass
  and contype in ('p', 'f', 'c', 'u')
order by conname;
