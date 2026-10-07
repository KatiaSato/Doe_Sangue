# Banco de dados

## Estado atual

**Atualizado em 06/10/2026.** A primeira migração cria `public.unidade_coleta`: ID `bigint` gerado por identidade, nome/cidade/endereço obrigatórios, horário/telefone opcionais e `publicada` com padrão `false`. RLS e grants permitem somente consulta das linhas publicadas para `anon` e `authenticated`.

O seed contém duas unidades sintéticas: Demo A publicada e Demo B não publicada. A usuária aplicou os arquivos manualmente no Supabase de desenvolvimento e confirmou a leitura de Demo A no aplicativo. O app não grava dados nesta etapa. O restante do modelo em `05-modelo-de-dominio.md` continua proposto; há uma tabela, nenhuma FK e dois registros do seed.

Ver [instruções e limites dos scripts](../supabase/README.md), [fluxo Android](14-integracao-unidades.md) e [validação](12-validacao.md). O histórico da CLI não foi estabelecido pela aplicação manual no painel.

## Diretrizes confirmadas

- PostgreSQL/Supabase será o banco principal, conforme sugestão aceita do professor; credenciais ficam no Supabase Auth.
- Migrações são versionadas e devem ser aplicadas na ordem, uma vez por ambiente. O seed usa apenas dados sintéticos e evita duplicar as mesmas unidades em reexecuções sequenciais.
- Constraints preservarão integridade e índices atenderão filtros frequentes.
- RLS protegerá perfil, agendamentos e histórico; o doador não elevará papel, alterará estoque nem confirmará coleta.
- Dados públicos não poderão revelar dados pessoais.
- Agendamentos devem usar datas/horários disponibilizados por unidade, com revalidação de capacidade no servidor. Sábados, domingos e feriados não terão bloqueio universal. Decisão, responsabilidades e pendências em [disponibilidade de agendamento](15-disponibilidade-agendamento.md); o esquema dessa agenda ainda não foi implementado.

## Critérios da entrega acadêmica — ainda não implementados

- Modelo relacional com **no mínimo 6 tabelas relacionadas**, PK em cada uma e **no mínimo 5 relações por FK**, além de `NOT NULL`, `UNIQUE` e `DEFAULT` onde couber. Para evitar depender da interpretação da banca, planejar seis tabelas do domínio do aplicativo sem contar `auth.users`, que é gerenciada pelo Supabase. A escolha final das entidades e cardinalidades depende da aprovação do modelo e das fontes de dados.
- **No mínimo 200 registros sintéticos coerentes**, distribuídos entre as tabelas principais e com FKs válidas; não usar dados pessoais reais. Os números por tabela serão definidos depois do esquema final, sem criar registros sem propósito apenas para atingir a contagem.
- Consultas e alterações feitas pelo aplicativo devem refletir o banco; demonstrar filtros, busca, ordenação e relacionamentos com dados persistidos, não com listas fixas nas telas.
- O Android acessará Auth e dados pela API do Supabase (incluindo PostgREST e operações server-side quando necessárias), com RLS e sem conexão direta ao PostgreSQL ou segredo administrativo no APK.

O [recorte proposto de seis tabelas, PKs e FKs](05-modelo-de-dominio.md#recorte-relacional-de-seis-tabelas--proposta-para-revisão) mostra uma opção com UUID para o perfil vinculado ao Auth e `bigint GENERATED ALWAYS AS IDENTITY` para as demais tabelas. Auto-incremento é uma escolha de modelagem; o enunciado exige chave primária adequada, não auto-incremento em toda tabela. Até esta etapa, somente `unidade_coleta` foi criada.

## Como popular os dados

Os scripts atuais estão em `supabase/migrations/` e `supabase/seed.sql`; o seed roda **após** a migração, com papel administrativo no ambiente acadêmico autorizado. A migração cria a tabela e não pode ser reaplicada sobre a mesma tabela existente. O `NOT EXISTS` do seed compara nome, cidade e endereço; não é uma restrição de unicidade nem garantia contra execuções concorrentes.

As próximas tabelas precisam de aprovação do modelo e novas migrações. Registros dependentes de contas de teste exigem primeiro identidades válidas criadas por fluxo seguro do Supabase Auth; uma linha inserida manualmente em `auth.users` não deve ser tratada como conta apta a fazer login. Antes da apresentação, carregar a massa em um projeto de demonstração autorizado e validar contagens, FKs, consultas e uma alteração feita pelo app. Não executar seed ou reset remoto sem conferir projeto-alvo e obter autorização.

A conta do professor para **entrar no aplicativo** será distinta da massa sintética: e-mail/endereço e forma de acesso ainda serão combinados. Preferir convite ou cadastro com o próprio e-mail, com senha definida pelo titular; login Google também é opção aprovada. Não versionar ou compartilhar senha. O convite ao projeto GitHub exigido pelo enunciado é outro acesso, independente do login no aplicativo. Acesso administrativo ao painel Supabase não está aprovado por esta decisão.

## Plano de validação

Testar leitura e escrita próprias, negação sobre outro usuário, negação de escrita operacional e acesso anônimo somente a conteúdo publicado. Aplicação remota exige ambiente e autorização explícitos.

## Pendente

DER definitivo, dicionário completo, status, matriz de permissões das próximas tabelas, unidade das medidas, distribuição dos 200 registros, estratégia reexecutável para contas sintéticas, e-mail/forma de acesso do professor, entrega de e-mails de convite e conciliação do histórico de migrações antes de usar a CLI no ambiente criado manualmente.
