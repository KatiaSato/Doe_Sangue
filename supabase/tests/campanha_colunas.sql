-- Somente leitura: nove colunas obrigatórias, id IDENTITY ALWAYS,
-- publicada DEFAULT false e criado_em DEFAULT now().
select
    column_name as coluna,
    data_type as tipo,
    is_nullable as aceita_null,
    column_default as valor_padrao,
    is_identity as identidade,
    identity_generation as geracao
from information_schema.columns
where table_schema = 'public' and table_name = 'campanha'
order by ordinal_position;
