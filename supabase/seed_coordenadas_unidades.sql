-- Vínculos fictícios com pontos geográficos para demonstração.
begin;

update public.unidade_coleta as unidade
set latitude = demo.latitude,
    longitude = demo.longitude
from (
    values
        ('Unidade Demo A', 'Endereço fictício A', -22.9000, -47.0500),
        ('Unidade Demo B', 'Endereço fictício B', -22.9100, -47.0600)
) as demo(nome, endereco, latitude, longitude)
where unidade.nome = demo.nome
  and unidade.endereco = demo.endereco
  and unidade.cidade = 'Cidade fictícia'
  and unidade.latitude is null
  and unidade.longitude is null
returning unidade.id, unidade.nome,
    unidade.latitude, unidade.longitude, unidade.publicada;

commit;