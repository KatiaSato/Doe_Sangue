-- Somente leitura: executar como postgres; não avança a sequência.
-- Esperado: todos false para anon e authenticated.
select
    papeis.papel,
    has_sequence_privilege(papeis.papel, 'public.campanha_id_seq', 'USAGE') as pode_usar,
    has_sequence_privilege(papeis.papel, 'public.campanha_id_seq', 'SELECT') as pode_consultar,
    has_sequence_privilege(papeis.papel, 'public.campanha_id_seq', 'UPDATE') as pode_alterar
from (values ('anon'), ('authenticated')) as papeis(papel)
order by papeis.papel;
