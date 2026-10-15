-- Somente leitura: policy SELECT para anon/authenticated, campanha/unidade publicadas.
select
    policyname,
    permissive,
    cmd,
    roles,
    qual as condicao_leitura,
    with_check as condicao_gravacao
from pg_policies
where schemaname = 'public' and tablename = 'campanha'
order by policyname;
