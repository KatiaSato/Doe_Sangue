# Banco de dados

## Estado atual

Não há migração, banco configurado ou persistência implementada. O modelo em `05-modelo-de-dominio.md` é proposta para revisão.

## Diretrizes confirmadas

- PostgreSQL/Supabase será o banco principal, conforme sugestão aceita do professor; credenciais ficam no Supabase Auth.
- Migrações e seed serão versionados e repetíveis; seed conterá apenas dados sintéticos.
- Constraints preservarão integridade e índices atenderão filtros frequentes.
- RLS protegerá perfil, agendamentos e histórico; o doador não elevará papel, alterará estoque nem confirmará coleta.
- Dados públicos não poderão revelar dados pessoais.

## Critérios da entrega acadêmica — ainda não implementados

- Modelo relacional com **no mínimo 6 tabelas relacionadas**, PK em cada uma e **no mínimo 5 relações por FK**, além de `NOT NULL`, `UNIQUE` e `DEFAULT` onde couber. Para evitar depender da interpretação da banca, planejar seis tabelas do domínio do aplicativo sem contar `auth.users`, que é gerenciada pelo Supabase. A escolha final das entidades e cardinalidades depende da aprovação do modelo e das fontes de dados.
- **No mínimo 200 registros sintéticos coerentes**, distribuídos entre as tabelas principais e com FKs válidas; não usar dados pessoais reais. Os números por tabela serão definidos depois do esquema final, sem criar registros sem propósito apenas para atingir a contagem.
- Consultas e alterações feitas pelo aplicativo devem refletir o banco; demonstrar filtros, busca, ordenação e relacionamentos com dados persistidos, não com listas fixas nas telas.
- O Android acessará Auth e dados pela API do Supabase (incluindo PostgREST e operações server-side quando necessárias), com RLS e sem conexão direta ao PostgreSQL ou segredo administrativo no APK.

O [recorte proposto de seis tabelas, PKs e FKs](05-modelo-de-dominio.md#recorte-relacional-de-seis-tabelas--proposta-para-revisão) mostra uma opção com UUID para o perfil vinculado ao Auth e `bigint GENERATED ALWAYS AS IDENTITY` para as demais tabelas. Auto-incremento é uma escolha de modelagem; o enunciado exige chave primária adequada, não auto-incremento em toda tabela. Nenhum esquema foi criado ainda.

## Como popular os dados

Depois de aprovar o esquema, criar migrações versionadas em `supabase/migrations/` e um seed reexecutável em `supabase/seed.sql` para registros fictícios do domínio. No ambiente local, o seed roda **após** as migrações. Registros dependentes de contas de teste exigem primeiro identidades válidas criadas por fluxo seguro do Supabase Auth; uma linha inserida manualmente em `auth.users` não deve ser tratada como conta apta a fazer login. Antes da apresentação, carregar a massa em um projeto de demonstração autorizado e validar contagens, FKs, consultas e uma alteração feita pelo app. Não executar seed ou reset remoto sem conferir projeto-alvo e obter autorização.

A conta do professor para **entrar no aplicativo** será distinta da massa sintética: e-mail/endereço e forma de acesso ainda serão combinados. Preferir convite ou cadastro com o próprio e-mail, com senha definida pelo titular; login Google também é opção aprovada. Não versionar ou compartilhar senha. O convite ao projeto GitHub exigido pelo enunciado é outro acesso, independente do login no aplicativo. Acesso administrativo ao painel Supabase não está aprovado por esta decisão.

## Plano de validação

Testar leitura e escrita próprias, negação sobre outro usuário, negação de escrita operacional e acesso anônimo somente a conteúdo publicado. Aplicação remota exige ambiente e autorização explícitos.

## Pendente

DER definitivo, dicionário completo, status, matriz de permissões, unidade das medidas, distribuição dos 200 registros, estratégia reexecutável para contas sintéticas, e-mail/forma de acesso do professor, entrega de e-mails de convite e projeto Supabase de desenvolvimento.
