-- Executar como postgres no SQL Editor do desenvolvimento acadêmico.
-- Somente leitura de privilégios; não avança a sequência.
select
    papeis.papel,
    has_sequence_privilege(
        papeis.papel,
        'public.agendamento_cancelamento_id_seq',
        'USAGE'
    ) as pode_usar,
    has_sequence_privilege(
        papeis.papel,
        'public.agendamento_cancelamento_id_seq',
        'SELECT'
    ) as pode_consultar,
    has_sequence_privilege(
        papeis.papel,
        'public.agendamento_cancelamento_id_seq',
        'UPDATE'
    ) as pode_alterar
from (values ('anon'), ('authenticated')) as papeis(papel)
order by papeis.papel;
