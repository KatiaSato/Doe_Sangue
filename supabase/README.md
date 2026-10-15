# Supabase — banco acadêmico e dados sintéticos

## Cancelamento integrado ao Android — conclusão do roteiro de 14/10/2026

- `tests/cancelar_agendamento_android_preparar.sql`: executado manualmente pela usuária, com COMMIT; criou oferta temporária 24/reserva 39 para A, sem alterar 23. Identidade administrativa simulada somente na preparação. Não repetir.
- Após orientação para cancelar somente 39 pelo app, captura da lista mostra 39 Cancelado e 23 Agendado. `tests/cancelar_agendamento_android_conferir.sql`, somente leitura como postgres, confirmou 39 cancelado com um evento, autor_confere=true e instante preenchido; 23 agendado, zero eventos e criado_em original preservado. Não foi inspecionado JWT/tráfego.
- `tests/cancelar_agendamento_android_limpar.sql`: execução manual confirmada por captura, com oferta_removida/reserva_removida/evento_removido=true e original 23 preservada. Remove somente fixture marcada pela chave sintética, seu evento e sua oferta; confere dados completos de 23 antes/depois. Não repetir. A massa anterior de sete registros permanece preservada no escopo do roteiro, sem nova contagem global.
- Integração Android escrita pela usuária; 20 testes focados sem rede aprovados e LintDebug com 31 avisos/zero erros. Nenhuma nova migração nesta integração; não reaplicar DDL. Evidências e limites em [validação](../docs/12-validacao.md) e [checkpoint atualizado](../docs/checkpoints/2026-10-14-cancelamento-supabase.md). Seções abaixo são históricas.

## Cancelamento — preparação local em 14/10/2026

- `migrations/20261014000100_criar_cancelamento_agendamento.sql`: oitava migração, após as sete existentes. Escrita pela usuária em pequenos passos e revisada estaticamente; aplicação manual relatada em 14/10 após orientação de execução completa. Capturas posteriores conferem configuração/permissões e estrutura; resultados dos testes abaixo. Não reaplicar automaticamente esta migração nem as anteriores.
- Cria `agendamento_cancelamento` e `cancelar_agendamento(bigint)`, sem cancelar ou inserir reservas na aplicação do DDL. Identidade pela sessão, acesso próprio, prazo antes do início, repetição sem novo evento e bloqueios perfil → horário → reserva. Estado/evento na mesma transação; escrita direta na nova tabela/sequência revogada e EXECUTE somente authenticated.
- Preservar reserva 23; exercícios de cancelamento deverão usar dados temporários e ROLLBACK. Ver [desenho e limites da etapa](../docs/20-diagrama-sequencia-cancelamento.md). Revisão estática não comprova execução nem concorrência.
- `tests/cancelar_agendamento_configuracao.sql`: captura confirma anon sem EXECUTE, authenticated com EXECUTE, SECURITY DEFINER=true, volatilidade=v e search_path vazio. Somente leitura; não testa o comportamento da função.
- `tests/cancelar_agendamento_tabela.sql`: captura confirma RLS ativo e SELECT/INSERT/UPDATE/DELETE/TRUNCATE negados a anon/authenticated; somente leitura.
- `tests/cancelar_agendamento_sequencia.sql`: captura confirma USAGE/SELECT/UPDATE negados a anon/authenticated; consulta não avança a sequência.
- `tests/cancelar_agendamento_constraints.sql`: captura confirma PK, UNIQUE da reserva e duas FKs com ON DELETE RESTRICT.
- `tests/cancelar_agendamento_criacao_repeticao.sql`: captura confirma cancelamento/repetição, evento único com autor/instante e preservação da reserva 23. Oferta/reserva/evento temporários com ROLLBACK; sequências podem avançar. Papéis/identidade simulados administrativamente, sem login/JWT pela API nem concorrência; limpeza final ainda pendente.
- `tests/cancelar_agendamento_prazo_oferta.sql`: captura confirma cinco resultados esperados: prazo passado bloqueado, cancelamento próprio em oferta inativa/unidade não publicada, repetição após início sem novo evento e reserva 23 preservada. Verifica leitura pública preservada; reservas administrativas temporárias, sem prova de criação aceita nessas condições. Altera somente uma oferta temporária e termina em ROLLBACK.
- `tests/cancelar_agendamento_validacao_acesso.sql`: captura confirma dez resultados esperados de identidade/IDs, propriedade A/B, acessos diretos e anon bloqueados, com reservas/evento/23 preservados. Perfis existentes preservados, B criado apenas se ausente e reservas/evento temporários. Identidades/papéis simulados, incluindo UUID sintético sem conta/perfil; termina em ROLLBACK. Limpeza final ainda pendente.
- `tests/cancelar_agendamento_atomicidade.sql`: captura confirma 23505 especificamente na unicidade do evento, ausência de alteração parcial, evento artificial preservado, nova tentativa após retirar o conflito e reserva 23 intacta. Usa somente reserva/evento temporários e termina em ROLLBACK.
- `tests/cancelar_agendamento_isolamento.sql`: captura confirma REPEATABLE READ/SERIALIZABLE rejeitados por 25000. Roteiro confere mensagem específica, antes da busca da reserva; ID positivo confirmado inexistente, sem novos registros. Duas transações independentes terminando em ROLLBACK.
- `tests/concorrencia_cancelamento/`: capturas confirmam preparação com COMMIT (horário 23/início 2026-10-21 21:31:31.436406+00/capacidade 1/reserva A=37), espera real de B pelo bloqueio de A=true e criação B=38 após o cancelamento, com A cancelado/um evento/somente B ativo. Reserva original 23 preservada. A confirma com COMMIT; B termina em ROLLBACK. Captura da limpeza específica confirma oferta/reservas/eventos removidos=true. Não repetir preparação/sessões nem usar IDs do cenário histórico em `tests/concorrencia/`.
- `tests/cancelar_agendamento_conferencia_final.sql`: captura confirma 2 unidades/3 horários/1 perfil/1 agendamento/0 eventos, registros temporários ausentes, reserva original 23 agendada e dados conferidos=true. Somente leitura. Etapa SQL concluída nesse recorte; Android pendente. Retomada em [checkpoint](../docs/checkpoints/2026-10-14-cancelamento-supabase.md).

## Consulta dos próprios agendamentos — etapa iniciada em 12/10/2026

- `migrations/20261012000200_consultar_meus_agendamentos.sql`: sétima migração, depende das tabelas já criadas. Aplicação manual relatada pela usuária com resultado esperado. Não reaplicar; histórico da CLI ainda não conciliado.
- Função de leitura sem parâmetros, STABLE, SECURITY DEFINER e search_path vazio; exige auth.uid() e restringe a consulta ao próprio doador. Retorna objeto JSON com campos explícitos da reserva e nome/início/fuso por JOIN, incluindo ofertas inativas/unidades não publicadas das reservas próprias. EXECUTE somente a authenticated; nenhuma policy pública foi ampliada.
- Capturas dos quatro roteiros (`configuracao`, `leitura_isolada`, `acesso_vazio`, `oferta_oculta`) confirmam configuração/permissões, leitura própria A/B e formato completo, lista vazia, acesso sem identidade/anon negado, leitura própria de reservas com oferta oculta/passada/cancelada, consulta pública preservada e reserva 23 preservada. Testes simulam identidades administrativamente, sem JWT pela API, com fixtures em ROLLBACK. Captura de `tests/consultar_meus_agendamentos_conferencia_final.sql` confirma 2 unidades, 3 horários, 1 perfil e 1 agendamento; zero unidades temporárias, zero outras reservas no horário de 23 e 23 agendado. A chamada Android está preparada e compilou; ViewModel/tela ainda pendentes. Retomada em [checkpoint de leitura](../docs/checkpoints/2026-10-12-consulta-agendamentos.md), evidências em [validação](../docs/12-validacao.md); seções históricas abaixo devem ser lidas com suas datas.

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
