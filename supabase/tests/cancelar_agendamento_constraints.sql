-- Executar como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente leitura das constraints PK, UNIQUE e FK.
select
    conname as nome,
    contype as tipo,
    pg_get_constraintdef(oid) as definicao
from pg_constraint
where conrelid = 'public.agendamento_cancelamento'::regclass
  and contype in ('p', 'u', 'f')
order by conname;
