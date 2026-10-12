# Supabase — banco acadêmico e dados sintéticos

## Função de reserva — 12/10/2026

- A usuária relatou aplicação manual de `migrations/20261012000100_criar_funcao_agendamento.sql` com “Success. No rows returned”. É a sexta migração e depende das cinco anteriores. Não reaplicar no desenvolvimento existente; o histórico da CLI continua sem conciliação.
- Define `criar_agendamento(bigint, uuid)` com SECURITY DEFINER, search_path vazio, VOLATILE e exigência de READ COMMITTED. Coordena solicitações por perfil/horário, recupera pedidos pela chave e valida oferta, início futuro, duplicidade e capacidade antes de gravar. EXECUTE é concedido somente a authenticated; escrita direta na tabela continua revogada.
- Captura da primeira consulta de `tests/criar_agendamento_configuracao.sql` confirma SECURITY DEFINER=true, volatilidade=v e search_path vazio. A usuária relatou privilégios esperados na segunda: anon sem EXECUTE/INSERT direto; authenticated com EXECUTE e sem INSERT direto. Captura confirma criação/repetição sem duplicação em `tests/criar_agendamento_criacao_idempotencia.sql`, com oferta temporária capacidade 1 e ROLLBACK. Captura confirma os bloqueios e preservação do original em `tests/criar_agendamento_pedidos_duplicados.sql`. Captura confirma os quatro bloqueios em `tests/criar_agendamento_disponibilidade.sql`, inclusive vaga ocupada por B e indisponível para A. Captura confirma os quatro resultados em `tests/criar_agendamento_cancelados.sql`; cancelamento administrativo somente de dados temporários para testar recuperação/nova chave. Captura confirma oito bloqueios e dados preservados em `tests/criar_agendamento_validacao_acesso.sql`, incluindo tentativas diretas de escrita e EXECUTE como anon. Captura confirma REPEATABLE READ/SERIALIZABLE bloqueados em `tests/criar_agendamento_isolamento.sql`. Captura da preparação em `tests/concorrencia/` confirma oferta temporária ID 13, início 2026-10-19 15:44:43.092005+00, capacidade 1, criada com COMMIT. Capturas A/B confirmam reserva A ID 22, espera de B pelo bloqueio=true e falta de vagas para B, com somente uma reserva ativa de A. Captura da limpeza confirma reserva 22/oferta 13 removidas; não repetir automaticamente o roteiro histórico. [Checkpoint](../docs/checkpoints/2026-10-12-reserva-supabase.md) concluído; estes testes simulam identidade administrativamente, sem JWT pela API. O Android ainda não chama a função; Confirmar continua visual. Ver [disponibilidade](../docs/15-disponibilidade-agendamento.md) e [validação](../docs/12-validacao.md).

## Agendamentos — 11/10/2026

- A usuária relatou execução manual sem erros de `migrations/20261011000100_criar_agendamento.sql` no projeto acadêmico de desenvolvimento. Não reaplicar no ambiente existente; em ambiente novo, ela é a quinta migração, após perfil e suas dependências.
- O script cria a estrutura com duas FKs, estado, idempotência, índices e RLS de leitura própria, sem inserir registros. Capturas confirmam existência/RLS, privilégios da tabela, definição da policy, sequência sem acesso direto para anon/authenticated, cinco constraints e quatro índices (incluindo filtros/unicidade dos parciais). Testes manuais tiveram resultado esperado para defaults, estado inválido, idempotência, duplicidade ativa/cancelado coexistente e FKs inexistentes; captura posterior confirma leitura isolada A/B e bloqueio de anon. São simulações administrativas, sem comprovar autenticação ou JWT pela API; ver [validação](../docs/12-validacao.md).
- Scripts em `tests/` são executados inteiros no SQL Editor como postgres, no desenvolvimento sintético, e encerram com ROLLBACK. Não são migrações nem alteram a massa permanente; não criar contas diretamente em auth.users. A sequência de IDs pode avançar mesmo com ROLLBACK.
- Função de reserva com capacidade/concorrência e integração Android ainda estão pendentes. O sucesso da migração não significa que o aplicativo já grava agendamentos. O histórico da CLI continua sem conciliação; o agente não executou SQL remoto. Ver [estado do banco](../docs/09-banco-de-dados.md).

## Perfil do doador — 08/10/2026

- `migrations/20261008000100_criar_perfil_doador.sql` foi executada manualmente pela usuária após as três migrações anteriores. Não reaplicar no ambiente existente.
- Perfil mínimo com ID UUID vinculado a `auth.users`, nome de exibição e data de criação gerada pelo banco. Não armazena senha nem cria automaticamente conta ou perfil.
- Capturas confirmam RLS ativo, três policies de acesso próprio e privilégios por coluna: authenticated pode consultar, inserir ID/nome e editar somente nome; anon não tem esses acessos, e ambos não podem excluir. Testes manuais com duas identidades simuladas confirmaram leitura própria; inserção própria como A, edição própria preservando B, inserção em nome de B bloqueada e consulta anon bloqueada também foram verificados por captura ou relato. Evidências e limites em [validação](../docs/12-validacao.md).
- Duas contas sintéticas foram criadas pelo painel Auth; perfis dos testes SQL são temporários, com ROLLBACK. Não inserir contas diretamente em auth.users nem guardar senhas nos scripts. Os testes de policies não comprovam login pela API.
- O histórico remoto da CLI continua sem conciliação. Em ambiente novo, esta é a quarta migração, após as três descritas abaixo. Ainda não há seed de perfis ou login Android implementado.

## Implementado em 06/10/2026

- `migrations/20261002000100_criar_unidade_coleta.sql`: cria a tabela, ativa RLS, revoga privilégios anteriores de `anon`/`authenticated` e permite somente SELECT das linhas com `publicada = true`.
- `seed.sql`: insere Demo A publicada e Demo B não publicada, sem dados pessoais ou credenciais.
- O aplicativo consulta a API com chave publicável. A inserção do seed é administrativa; não deve ser feita com a chave do Android.

## Evolução do fuso — 07/10/2026

- `migrations/20261007000100_adicionar_fuso_unidade.sql`: adiciona `fuso_horario` obrigatório, preenche as unidades existentes com `America/Sao_Paulo` e remove o default para exigir o fuso em novas inserções.
- A usuária executou a migração manualmente e mostrou as duas unidades sintéticas com esse fuso e publicação preservada. A captura confirma os valores; NOT NULL e ausência de default ainda não foram consultados remotamente.
- O seed atualizado informa o fuso e depende das duas migrações. Esta versão do seed ainda não foi executada. O Android já recebe o campo no DTO e o leva ao domínio pelo mapper; aplicativo e APK de testes compilaram. A usuária confirmou o carregamento da unidade sem erro na consulta real com o DTO atualizado em 07/10/2026.

## Ordem e aplicação

Migração de horários — 07/10/2026: `migrations/20261007000200_criar_horario_disponivel.sql` foi executada manualmente pela usuária. Captura de `pg_tables` confirma a tabela com RLS ativo. `seed_horarios.sql` foi aplicado e a consulta administrativa confirmou três ofertas. Consultas com JOIN sob `anon` e `authenticated` mostraram somente Demo A às 08:00. SELECT permitido e INSERT/UPDATE/DELETE ausentes foram confirmados para ambos. A consulta direta sem JOIN sob ambos os papéis também teve o resultado esperado, conforme relato da usuária. Testes de constraints permanecem pendentes; ver [evidências e limites](../docs/12-validacao.md).

1. Identificar o ambiente acadêmico de desenvolvimento e obter autorização antes de qualquer aplicação remota.
2. Em um ambiente novo, aplicar as três migrações na ordem dos nomes, uma vez cada. Não são idempotentes: tabela/política/coluna existentes causam erro. Não apagar objetos para reaplicá-las. Antes de adicionar o fuso em uma base já populada, conferir que suas unidades correspondem ao cenário sintético previsto.
3. Executar `seed.sql` após as migrações de unidade e fuso. Reexecuções sequenciais não duplicam unidades com o mesmo nome, cidade e endereço; não há garantia de unicidade concorrente nem atualização de linhas já existentes.
4. Executar `seed_horarios.sql` após as três migrações e a criação das unidades sintéticas. Usa o fuso da unidade; ON CONFLICT evita duplicação por unidade/início, sem atualizar registros existentes.

A usuária já aplicou as três migrações e a versão anterior do seed de unidades pelo SQL Editor do painel. Essa execução manual não configura o histórico de migrações da CLI. Não executar `db push`, reset ou reaplicar migrações no banco existente sem antes reconciliar o histórico e obter autorização. O agente não executou SQL remoto nesta etapa.

## Verificação manual já relatada

- Papel administrativo: duas unidades.
- Papéis `anon` e `authenticated`: somente Demo A.
- SELECT permitido; INSERT, UPDATE, DELETE e TRUNCATE ausentes para esses papéis.
- Consulta no Android: Demo A exibida, conforme confirmação da usuária.

O relato de privilégios das unidades não substitui testes de RLS dos horários. A tabela de horários e sua consulta pelo Android estão implementadas. O intervalo usa gte/lt agrupados em and para preservar os dois limites no Supabase Kotlin 3.6.0. Auth e tabelas de agendamentos continuam pendentes. Ver [banco](../docs/09-banco-de-dados.md), [integração](../docs/14-integracao-unidades.md), [disponibilidade](../docs/15-disponibilidade-agendamento.md) e [resultados dos testes](../docs/12-validacao.md).
