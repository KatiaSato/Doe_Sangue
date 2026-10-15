# Checkpoint — campanhas no Supabase — 15/10/2026

## Retomada obrigatória

Projeto: `C:\Users\katia\Documents\Apps\DoeSangue`. Ler AGENTS.md, este checkpoint e somente os arquivos do próximo passo. **A usuária escreve produção; o agente orienta e revisa um único passo curto por resposta.** “Feito”, “vamos” e checkpoints não autorizam implementação automática. Testes/documentação e Git autorizado podem ser executados pelo agente. Antes de uma nova etapa, apresentar e salvar o diagrama de sequência. Não criar chat novo automaticamente; a usuária preferiu continuar nesta conversa.

## Confirmado e implementado

- A usuária aprovou campanhas sintéticas, cada uma ligada a exatamente uma unidade. Uma unidade pode ter zero ou várias campanhas. Publicação manual no Supabase, leitura pelo aplicativo; campanhas não criam reservas, confirmam coleta, determinam aptidão ou alteram estoque.
- Diagrama apresentado antes do código em `docs/22-diagrama-sequencia-campanhas.md`; fluxo Android ainda proposto. As telas/rotas de campanhas existentes continuam com conteúdo fixo.
- A usuária escreveu a nona migração, `supabase/migrations/20261015000100_criar_campanha.sql`, em pequenos passos e relatou aplicação integral como postgres. Capturas de catálogo/comportamento conferem o esquema. **Não reaplicar esta migração nem as oito anteriores.** Nenhum SQL remoto foi executado pelo agente; histórico da CLI continua sem conciliação.
- `campanha`: id bigint IDENTITY ALWAYS/PK; unidade_id bigint obrigatório/FK para unidade_coleta com ON DELETE RESTRICT; título/descrição/fonte obrigatórios e não vazios após btrim; início/fim timestamptz obrigatórios com fim > início; publicada boolean DEFAULT false; criado_em timestamptz DEFAULT now(). Nove colunas NOT NULL.
- RLS ativo; anon/authenticated têm somente SELECT. Policy exige campanha publicada e unidade publicada. Sem escrita/esvaziamento da tabela ou acesso à sequência pelos clientes. Publicação é administrativa; nenhum segredo/papel administrativo é levado ao Android.
- Agora existem **seis tabelas e seis FKs entre tabelas do domínio**, além da FK para Supabase Auth. A meta de 200 registros ainda não foi atendida.

## Evidências e limites

Cinco consultas de catálogo executadas pela usuária, com capturas: `campanha_tabela.sql`, `campanha_sequencia.sql`, `campanha_constraints.sql`, `campanha_policy.sql`, `campanha_colunas.sql`. A expressão completa da policy ficou cortada na captura; a filtragem foi exercitada no teste de leitura.

Três roteiros comportamentais preparados pelo agente e executados pela usuária com resultados esperados por captura:

1. `campanha_leitura_publicada.sql`: SELECT direto/JOIN sob anon/authenticated retorna somente a campanha publicada de unidade publicada; oculta rascunho e campanha de unidade oculta. Três fixtures em ROLLBACK.
2. `campanha_integridade.sql`: defaults; oito casos de CHECK; nove NOT NULL; unidade inexistente/RESTRICT; ID explícito sem OVERRIDING e PK duplicada; registros originais/23 preservados. Unidade/campanha temporárias, SQLSTATE/constraint ou coluna conferidos, ROLLBACK.
3. `campanha_escrita_bloqueada.sql`: seis tentativas por papel (INSERT/UPDATE/DELETE/TRUNCATE e SELECT/nextval da sequência), bloqueadas por 42501; originais/23 preservados, ROLLBACK. Não usa setval nem reinicia sequências.

São papéis administrativos simulados no SQL Editor, **sem login/JWT/API do Android**. Sequências podem avançar mesmo com ROLLBACK. Revisão estática e `git diff --check` passaram; não houve nova execução de testes Android porque este recorte mudou SQL/documentação, não Kotlin. Detalhes em `docs/12-validacao.md`.

## Estado remoto final confirmado

Captura de `supabase/tests/campanha_conferencia_final.sql` confirma:

| Tabela/item | Resultado |
| --- | --- |
| unidade_coleta | 2 |
| horario_disponivel | 3 |
| perfil_doador | 1 |
| agendamento | 1 |
| agendamento_cancelamento | 0 |
| campanha | 0 |
| Unidades temporárias de integridade | 0 |
| Total de registros do domínio | 7 |
| Reserva original 23 | agendado |
| Dados conhecidos de 23 conferidos | true |

**Preservar a reserva 23** de A, Unidade Demo A, 19/10/2026 às 08:00 em America/Sao_Paulo, criada em 2026-10-12 23:14:35.586383+00. Contas Auth são separadas da contagem do domínio. Não repetir automaticamente os testes históricos/limpezas; as fixtures já não existem.

## Git e próximo passo

- Base local antes do registro: `3b9846189c596515cf945fc78fb1c6bcddfdb31d`, cancelamento Android, já enviado antes desta etapa. A usuária autorizou commit/push de campanhas/SQL/testes/documentação em 15/10 após a conferência final. Conferir commit resultante e sincronização no Git ao retomar.
- Migração apareceu **AM** (versão parcial no índice); em futuro commit autorizado, incluir a versão completa atual, não o rascunho já preparado. Não versionar local.properties/credenciais.
- Este recorte SQL está concluído e o registro no Git foi autorizado. Próxima etapa funcional: preparar um seed mínimo de campanhas sintéticas em tutoria e validar sua leitura antes de ligar as camadas Android. A distribuição da massa final de 200 registros permanece pendente; nenhum seed foi preparado/aplicado nesta etapa.
- Antes da implementação Android, retomar o diagrama e confirmar filtros temporais/ordenação e detalhe por ID. Reutilizar cliente Supabase e padrões de DTO/mapper, repository, StateFlow, CancellationException e composição manual. Não implementar as camadas automaticamente.
