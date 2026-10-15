-- Somente leitura, como postgres, após o cadastro de Doador Demo C pelo app.
-- Não exibe e-mail, UUID, senha, tokens ou o JSON completo dos metadados do Auth.
-- Filtra apenas o nome fictício do teste, tolerando a diferença C/c da captura.
select
    perfil.nome_exibicao,
    perfil.tipo_sanguineo,
    perfil.criado_em,
    conta.email_confirmed_at is not null as email_confirmado,
    coalesce(
        conta.raw_user_meta_data->>'nome_exibicao' = perfil.nome_exibicao
        and conta.raw_user_meta_data->>'tipo_sanguineo' = perfil.tipo_sanguineo::text,
        false
    ) as metadados_conferem
from public.perfil_doador as perfil
join auth.users as conta on conta.id = perfil.id
where lower(btrim(perfil.nome_exibicao)) = 'doador demo c'
order by perfil.criado_em desc;
