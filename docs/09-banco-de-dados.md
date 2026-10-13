# Banco de dados

## Estado atual

**Atualizado em 13/10/2026.** Quatro tabelas do domínio estão aplicadas: `unidade_coleta`, `horario_disponivel`, `perfil_doador` e `agendamento`, com existência/RLS confirmados por captura. Há três FKs entre tabelas do domínio (horário → unidade; agendamento → perfil e horário) e uma adicional perfil → `auth.users`. A função `criar_agendamento` foi aplicada manualmente e exercitada nos testes SQL, incluindo disputa A/B por uma vaga, com limpeza confirmada. O Android já a chama: reserva sintética 23 persistida e preservada após segunda intenção sem duplicação. A função `consultar_meus_agendamentos` foi aplicada e teve configuração, leitura própria, acesso/vazio, ofertas ocultas e limpeza conferidos nos roteiros com identidades simuladas. A listagem Android está ligada ao estado/ViewModel e repository; seis testes instrumentados com dados fictícios passaram. Capturas do roteiro com sessões reais A → B → A confirmam leitura de 23 como A, lista vazia como B e retorno de 23 como A, no recorte testado. Evidências e limites em [validação](12-validacao.md) e [checkpoint de leitura](checkpoints/2026-10-12-consulta-agendamentos.md). Login, perfil, logout local e restauração inicial existem; cadastro real, gestão completa da sessão, modelagem e massa acadêmicas ainda não estão concluídos.

A migração do perfil foi aplicada manualmente pela usuária. Capturas e relatos dos testes SQL confirmam RLS, privilégios por coluna, leitura própria com duas identidades simuladas, criação/edição própria como A, bloqueio de criação em nome de B e bloqueio de consulta anônima. Limites e evidências em [validação](12-validacao.md).

Ainda em 13/10, o detalhe por ID foi ligado à navegação e a um ViewModel próprio, reutilizando a consulta da conta e selecionando o ID na resposta. Sete testes do detalhe com dados fictícios passaram, além da regressão de listagem/navegação. Captura confirma os dados do detalhe da reserva 23, e o retorno à lista foi confirmado por relato. Não houve nova tabela, FK, RPC ou migração nesta etapa. Cancelamento persistido e ampliação do modelo/seed acadêmico continuam pendentes. Ver [diagrama da etapa](18-diagrama-sequencia-detalhe.md).

A conferência final dos testes de leitura confirma duas unidades, três horários, um perfil persistido (Doador Demo A) e um agendamento (reserva 23), totalizando sete registros do domínio. Duas contas de teste existem no Supabase Auth, mas não são tabelas/registros do domínio para a meta acadêmica. Perfil A e reserva 23 foram criados pelo aplicativo; os dados dos roteiros de leitura ficaram em ROLLBACK. Ainda não há seed ampliado nem os 200 registros acadêmicos exigidos.

O teste concorrente de 12/10 acrescentou temporariamente oferta 13 e reserva 22, ambas removidas conforme captura da limpeza. A contagem global atual vem da conferência final posterior dos testes de leitura, sem somar dados temporários à meta acadêmica.

### Estrutura inicial de agendamentos — 11/10/2026

- A usuária criou `AppointmentStatus` (SCHEDULED/CANCELLED), `Appointment` e o contrato `AppointmentRepository.createAppointment(timeSlotId, idempotencyKey): Result<Appointment>` em `domain/`. Ainda não há implementação do repository nem ligação da confirmação à gravação.
- A usuária relatou execução manual sem erros de `20261011000100_criar_agendamento.sql` no projeto acadêmico de desenvolvimento. Não reaplicar. A aplicação não insere agendamentos; as contagens de registros confirmados permanecem em seis. Testes de comportamento usam registros temporários em transações com ROLLBACK e não compõem o seed permanente.
- `compileDebugKotlin` passou em 11/10/2026 para os novos modelos e contrato. `git diff --check` não apontou erros de formatação; SQL ainda não foi exercitado pelo agente em um banco.
- O recorte atual referencia obrigatoriamente perfil e horário por FK com ON DELETE RESTRICT. Unidade e instante são obtidos pelo horário, sem duplicação no agendamento; esta definição substitui, para esta etapa, os campos redundantes/opcionais do esboço histórico em `05-modelo-de-dominio.md`.
- Estado no banco: `agendado` ocupa vaga; `cancelado` permanece no histórico sem ocupar vaga. CHECK limita os valores; ID e criado_em são gerados pelo banco. A transição de cancelamento ainda não foi implementada.
- UNIQUE de doador/chave de idempotência impede repetição do mesmo pedido; índice único parcial impede dois agendamentos ativos do mesmo doador/horário. Um segundo índice parcial auxilia a contagem de reservas ativas por horário.
- Capturas de 11/10 confirmam existência da tabela, RLS ativo, SELECT como único privilégio consultado para authenticated, ausência de SELECT/INSERT/UPDATE/DELETE/TRUNCATE para anon e ausência de INSERT/UPDATE/DELETE/TRUNCATE para authenticated. A policy de SELECT próprio está definida e ambos os papéis estão sem USAGE/SELECT/UPDATE na sequência. Também mostram as cinco constraints e os quatro índices esperados, incluindo filtro e unicidade dos índices parciais. Testes administrativos manuais tiveram resultado esperado para defaults, CHECK de estado, idempotência, duplicidade ativa, coexistência de cancelado, FKs inexistentes e isolamento de leitura A/B/anon. A captura de leitura após a correção do teste confirma os três resultados; autenticação/isolamento pela API ainda precisam de validação. Ver [validação](12-validacao.md).
- Pendente: operação no servidor que identifica o doador pela sessão, exige perfil, revalida oferta/unidade/capacidade sob concorrência e trata repetição de pedidos. As constraints e índices da tabela não garantem, sozinhos, o limite de capacidade. DTO/mapper, fonte de dados, implementação do contrato e integração Android virão em pequenos passos. Nenhum SQL remoto foi executado pelo agente nesta preparação.

### Histórico da evolução de unidades e horários

**Atualizado em 06/10/2026.** A primeira migração cria `public.unidade_coleta`: ID `bigint` gerado por identidade, nome/cidade/endereço obrigatórios, horário/telefone opcionais e `publicada` com padrão `false`. RLS e grants permitem somente consulta das linhas publicadas para `anon` e `authenticated`.

O seed contém duas unidades sintéticas: Demo A publicada e Demo B não publicada. A usuária aplicou os arquivos manualmente no Supabase de desenvolvimento e confirmou a leitura de Demo A no aplicativo. O app não grava dados nesta etapa. O restante do modelo em `05-modelo-de-dominio.md` continua proposto; há uma tabela, nenhuma FK e dois registros do seed.

Ver [instruções e limites dos scripts](../supabase/README.md), [fluxo Android](14-integracao-unidades.md) e [validação](12-validacao.md). O histórico da CLI não foi estabelecido pela aplicação manual no painel.

**Evolução de 07/10/2026:** a usuária aplicou a migração de `fuso_horario` e mostrou Demo A e Demo B com `America/Sao_Paulo`, sem alterar a publicação. O script local define campo obrigatório sem default ao final; falta conferir essas propriedades no esquema remoto. O seed atualizado informa o fuso e requer as duas migrações; sua nova versão ainda não foi executada. A integração desse campo no Android está em andamento. A contagem continua em uma tabela, nenhuma FK e duas unidades sintéticas.

## Diretrizes confirmadas

Atualização adicional de 07/10/2026: `horario_disponivel` foi criada manualmente pela usuária; consulta a `pg_tables` confirma RLS ativo. O esquema versionado passa a ter duas tabelas do aplicativo e uma FK (horário → unidade). Foram confirmados cinco registros sintéticos: duas unidades e três horários. Consultas com JOIN sob `anon` e `authenticated` retornaram somente Demo A às 08:00; SELECT permitido e INSERT/UPDATE/DELETE ausentes foram confirmados para ambos. A consulta direta sem JOIN sob ambos os papéis também teve o resultado esperado, conforme relato da usuária. Testes de constraints permanecem pendentes. Esta atualização substitui as contagens históricas acima. Ver [validação](12-validacao.md) e [disponibilidade](15-disponibilidade-agendamento.md).

- PostgreSQL/Supabase será o banco principal, conforme sugestão aceita do professor; credenciais ficam no Supabase Auth.
- Migrações são versionadas e devem ser aplicadas na ordem, uma vez por ambiente. O seed usa apenas dados sintéticos e evita duplicar as mesmas unidades em reexecuções sequenciais.
- Constraints preservarão integridade e índices atenderão filtros frequentes.
- RLS protegerá perfil, agendamentos e histórico; o doador não elevará papel, alterará estoque nem confirmará coleta.
- Dados públicos não poderão revelar dados pessoais.
- Agendamentos devem usar datas/horários disponibilizados por unidade, com revalidação de capacidade no servidor. Sábados, domingos e feriados não terão bloqueio universal. A função de reserva e controle de capacidade está aplicada e teve os resultados esperados nos roteiros manuais, incluindo o cenário concorrente A/B-capacidade 1; ligação da gravação ao Android permanece pendente. Decisão, responsabilidades e limites em [disponibilidade de agendamento](15-disponibilidade-agendamento.md).

## Critérios da entrega acadêmica — ainda não implementados

- Modelo relacional com **no mínimo 6 tabelas relacionadas**, PK em cada uma e **no mínimo 5 relações por FK**, além de `NOT NULL`, `UNIQUE` e `DEFAULT` onde couber. Para evitar depender da interpretação da banca, planejar seis tabelas do domínio do aplicativo sem contar `auth.users`, que é gerenciada pelo Supabase. A escolha final das entidades e cardinalidades depende da aprovação do modelo e das fontes de dados.
- **No mínimo 200 registros sintéticos coerentes**, distribuídos entre as tabelas principais e com FKs válidas; não usar dados pessoais reais. Os números por tabela serão definidos depois do esquema final, sem criar registros sem propósito apenas para atingir a contagem.
- Consultas e alterações feitas pelo aplicativo devem refletir o banco; demonstrar filtros, busca, ordenação e relacionamentos com dados persistidos, não com listas fixas nas telas.
- O Android acessará Auth e dados pela API do Supabase (incluindo PostgREST e operações server-side quando necessárias), com RLS e sem conexão direta ao PostgreSQL ou segredo administrativo no APK.

O [recorte proposto de seis tabelas, PKs e FKs](05-modelo-de-dominio.md#recorte-relacional-de-seis-tabelas--proposta-para-revisão) mostra uma opção com UUID para o perfil vinculado ao Auth e `bigint GENERATED ALWAYS AS IDENTITY` para as demais tabelas. Auto-incremento é uma escolha de modelagem; o enunciado exige chave primária adequada, não auto-incremento em toda tabela. As quatro tabelas implementadas estão listadas no estado atual acima; as demais permanecem propostas.

## Como popular os dados

Os scripts atuais estão em `supabase/migrations/` e `supabase/seed.sql`; o seed roda **após** a migração, com papel administrativo no ambiente acadêmico autorizado. A migração cria a tabela e não pode ser reaplicada sobre a mesma tabela existente. O `NOT EXISTS` do seed compara nome, cidade e endereço; não é uma restrição de unicidade nem garantia contra execuções concorrentes.

As próximas tabelas precisam de aprovação do modelo e novas migrações. Registros dependentes de contas de teste exigem primeiro identidades válidas criadas por fluxo seguro do Supabase Auth; uma linha inserida manualmente em `auth.users` não deve ser tratada como conta apta a fazer login. Antes da apresentação, carregar a massa em um projeto de demonstração autorizado e validar contagens, FKs, consultas e uma alteração feita pelo app. Não executar seed ou reset remoto sem conferir projeto-alvo e obter autorização.

A conta do professor para **entrar no aplicativo** será distinta da massa sintética: e-mail/endereço e forma de acesso ainda serão combinados. Preferir convite ou cadastro com o próprio e-mail, com senha definida pelo titular; login Google também é opção aprovada. Não versionar ou compartilhar senha. O convite ao projeto GitHub exigido pelo enunciado é outro acesso, independente do login no aplicativo. Acesso administrativo ao painel Supabase não está aprovado por esta decisão.

## Plano de validação

Testar leitura e escrita próprias, negação sobre outro usuário, negação de escrita operacional e acesso anônimo somente a conteúdo publicado. Aplicação remota exige ambiente e autorização explícitos.

## Pendente

DER definitivo, dicionário completo, status, matriz de permissões das próximas tabelas, unidade das medidas, distribuição dos 200 registros, estratégia reexecutável para contas sintéticas, e-mail/forma de acesso do professor, entrega de e-mails de convite e conciliação do histórico de migrações antes de usar a CLI no ambiente criado manualmente.
