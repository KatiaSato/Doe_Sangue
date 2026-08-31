# Banco de dados

## Estado atual

Não há migração, banco configurado ou persistência implementada. O modelo em `05-modelo-de-dominio.md` é proposta para revisão.

## Diretrizes confirmadas

- PostgreSQL/Supabase será o banco principal; credenciais ficam no Supabase Auth.
- Migrações e seed serão versionados e repetíveis; seed conterá apenas dados sintéticos.
- Constraints preservarão integridade e índices atenderão filtros frequentes.
- RLS protegerá perfil, agendamentos e histórico; o doador não elevará papel, alterará estoque nem confirmará coleta.
- Dados públicos não poderão revelar dados pessoais.

## Plano de validação

Testar leitura e escrita próprias, negação sobre outro usuário, negação de escrita operacional e acesso anônimo somente a conteúdo publicado. Aplicação remota exige ambiente e autorização explícitos.

## Pendente

DER definitivo, dicionário completo, status, matriz de permissões, unidade das medidas e projeto Supabase de desenvolvimento.

