-- Unidades fictícias para a demonstração acadêmica.
insert into public.unidade_coleta (
    nome,
    cidade,
    endereco,
    publicada
)
select
    demo.nome,
    demo.cidade,
    demo.endereco,
    demo.publicada
from (
    values
        ('Unidade Demo A', 'Cidade fictícia', 'Endereço fictício A', true),
        ('Unidade Demo B', 'Cidade fictícia', 'Endereço fictício B', false)
) as demo(nome, cidade, endereco, publicada)
where not exists (
    select 1
    from public.unidade_coleta as existente
    where existente.nome = demo.nome
      and existente.cidade = demo.cidade
      and existente.endereco = demo.endereco
)
returning id, nome, publicada;
