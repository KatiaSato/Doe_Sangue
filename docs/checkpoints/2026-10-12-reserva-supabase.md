# Checkpoint — reserva no Supabase — 12/10/2026

## Retomada

**Atualização posterior na mesma conversa:** a usuária integrou repository, ViewModel, revisão e sucesso à chamada RPC. O estado atual está em [integração da confirmação](../15-disponibilidade-agendamento.md#integração-da-confirmação-no-android--12102026) e os resultados em [validação](../12-validacao.md). Dez testes de navegação e seis de confirmação passaram. A usuária informou ID 23 no aplicativo e mostrou consulta administrativa confirmando a reserva persistida de Demo A em 19/10/2026 às 08:00, do perfil sintético A. Segunda intenção exibiu erro na revisão; consulta posterior mostrou somente a reserva 23, preservada. Preservar esse registro; isolamento A/B pela API e recuperação real com a mesma chave ainda precisam de validação. Próxima integração funcional: consulta dos agendamentos persistidos, em pequenos passos. Este checkpoint registra a conclusão anterior do banco; seus próximos passos Android são históricos. As regras de tutoria continuam obrigatórias.

Projeto: `C:\Users\katia\Documents\Apps\DoeSangue`. A usuária implementa acompanhando pequenos passos e explicações em português. Ler `AGENTS.md`, este checkpoint e apenas os arquivos relacionados ao próximo passo; não percorrer todo o repositório.

**MODO DE TUTORIA: a usuária escreve o código de produção. O agente mostra um trecho curto, explica e aguarda a implementação. Não criar nem editar automaticamente arquivos Kotlin, mesmo um DTO ou mapper isolado. “Continuar” significa apresentar o próximo passo didático.**

**Etapa da função de reserva no banco concluída, com testes SQL manuais e limpeza confirmados. O Android ainda não grava agendamentos. A modelagem acadêmica inteira não está concluída.**

Próximo passo didático planejado: orientar a criação do DTO, um trecho por vez; mapper e DataSource serão etapas posteriores, cada uma após confirmação da usuária. Manter o contrato existente. A usuária autorizou commit e push da etapa do banco antes de retomar em nova conversa.

## Correção da retomada — 12/10/2026

A conversa “Criar DTO e mapper do agendamento” interpretou “pequenos passos” como autorização para implementar e gerou estes três arquivos, ainda sem commit:

- `app/src/main/java/br/edu/fatec/doesangue/data/supabase/dto/AppointmentDto.kt`
- `app/src/main/java/br/edu/fatec/doesangue/data/supabase/mapper/AppointmentMapper.kt`
- `app/src/main/java/br/edu/fatec/doesangue/data/supabase/SupabaseAppointmentDataSource.kt`

Essa produção automática não corresponde ao modo de aprendizado desejado. Os arquivos foram preservados; não apagar, alterar, integrar ou commitar esses arquivos como parte da correção do checkpoint. Antes de continuar, combinar com a usuária como retomar o aprendizado desses trechos. O código gerado não significa que ela já realizou essas etapas. A conversa está idle na conferência; nenhum aviso foi enviado a ela por ferramenta de mensagens.

Commit do banco concluído e enviado: `1903172b366fe8d7184198bf5189c7b94df87b1c`. A correção de tutoria é documental e separada dos três arquivos Kotlin novos. Conferir o Git ao retomar; a árvore passou a conter esses arquivos não rastreados após a conversa nova.

## Git e arquivos desta etapa

- Branch `main`; base anterior a esta etapa: `154e432880fe0c184cd184c09de4185736a8fe81`, `feat: adiciona estrutura e validações de agendamentos`.
- Este checkpoint acompanha a versão da função de reserva e seus testes. O hash do commit que o contém pode ser consultado no Git; não gravar aqui o próprio hash, que muda ao editar o documento.
- Conjunto desta etapa: nova migração da função; sete scripts `supabase/tests/criar_agendamento_*.sql`; cinco arquivos em `supabase/tests/concorrencia/`; documentação atualizada e este checkpoint.
- Commit/push autorizados em 12/10/2026. Ao retomar, conferir último commit, árvore de trabalho e sincronização com o remoto; a confirmação do envio fica na resposta final desta conversa. Incluir as versões finais, não a criação vazia que estava inicialmente no índice.
- Documentação da etapa do banco: `AGENTS.md`, `docs/09-banco-de-dados.md`, `docs/12-validacao.md`, `docs/15-disponibilidade-agendamento.md`, `supabase/README.md` e instruções do teste concorrente. Nenhum código Android foi alterado nessa etapa; os três arquivos posteriores estão registrados na correção da retomada acima.

## Aplicado no desenvolvimento — não reaplicar

Aplicações manuais pela usuária; histórico da CLI não conciliado. Não executar `db push`, reset ou reaplicações automáticas.

| Migração em `supabase/migrations/` | Conteúdo |
| --- | --- |
| `20261002000100_criar_unidade_coleta.sql` | Unidades e leitura pública restrita |
| `20261007000100_adicionar_fuso_unidade.sql` | Fuso obrigatório da unidade |
| `20261007000200_criar_horario_disponivel.sql` | Ofertas por unidade/data/instante e capacidade |
| `20261008000100_criar_perfil_doador.sql` | Perfil mínimo vinculado ao Auth |
| `20261011000100_criar_agendamento.sql` | Reservas, FKs, idempotência, índices e RLS |
| `20261012000100_criar_funcao_agendamento.sql` | Operação de reserva protegida no servidor |

Somente dados sintéticos. Contas A/B: `doador.a@example.com` e `doador.b@example.com`; não incluir senhas, tokens ou segredos no checkpoint, código ou logs. Credenciais são gerenciadas pelo Supabase Auth. `local.properties` é privado e ignorado; não ler/exibir seus valores nem incluir segredos administrativos no Android.

## Contrato e decisões da função

`public.criar_agendamento(p_horario_id bigint, p_chave_idempotencia uuid) RETURNS SETOF public.agendamento`.

- SECURITY DEFINER, search_path vazio, VOLATILE e exigência de READ COMMITTED. EXECUTE somente a authenticated entre os papéis do aplicativo; escrita direta permanece negada.
- Identidade vem de `auth.uid()`. Valida parâmetros e exige perfil; bloqueia o perfil próprio antes do horário para coordenar pedidos do mesmo doador.
- Chave é por doador: mesmo pedido retorna o registro existente; mesma chave para outro horário falha. O estado atual é preservado, inclusive cancelado; nova reserva após cancelamento requer nova chave.
- Nova reserva exige oferta ativa, unidade publicada e início futuro (`clock_timestamp()`). Não há bloqueio universal de finais de semana nem critérios médicos de aptidão na função.
- FOR UPDATE no horário e FOR SHARE na unidade protegem a verificação. Conta todos os agendamentos ativos, inclusive de outros doadores; cancelados não ocupam vaga. Recusa duplicidade ativa do próprio doador e ocupação maior ou igual à capacidade.
- INSERT define doador/horário/estado/chave; ID e criado_em vêm dos defaults. Retorna a linha completa. Unidade/data/hora vêm da FK para o horário, sem duplicação no agendamento.
- Agendamento não comprova comparecimento, coleta, aptidão ou alteração de estoque. Cancelamento no aplicativo ainda não existe; alterações administrativas de estado ocorreram somente nos testes temporários.

## Evidências e limites

Detalhes em [validação](../12-validacao.md). Execuções remotas feitas pela usuária, com capturas/relatos; o agente preparou/revisou arquivos, sem executar SQL remoto.

| Roteiro em `supabase/tests/` | Resultado confirmado |
| --- | --- |
| `criar_agendamento_configuracao.sql` | Definer=true, volatilidade=v, search_path vazio; privilégios esperados (parte por relato) |
| `criar_agendamento_criacao_idempotencia.sql` | Criação e repetição com mesmo registro; somente uma reserva para capacidade 1 |
| `criar_agendamento_pedidos_duplicados.sql` | Mesma chave/outro horário e nova chave/mesmo horário ativo rejeitados; original preservado |
| `criar_agendamento_disponibilidade.sql` | Inativo, unidade oculta, passado e vaga ocupada por B bloqueados; A não vê reserva privada de B |
| `criar_agendamento_cancelados.sql` | Antigo cancelado retornado sem reativação; nova chave aceita; somente um ativo para capacidade 1 |
| `criar_agendamento_validacao_acesso.sql` | Parâmetros/identidade/perfil inválidos bloqueados; INSERT/UPDATE/DELETE diretos e EXECUTE anon negados |
| `criar_agendamento_isolamento.sql` | REPEATABLE READ e SERIALIZABLE rejeitados em transações separadas |
| `concorrencia/sessao_a.sql` e `sessao_b.sql` | B aguardou bloqueio de A; reserva A=22 confirmada; B sem vagas; somente um ativo |
| `concorrencia/limpar.sql` | oferta_removida=true e reservas_do_teste_removidas=true |

Os testes sequenciais terminam com ROLLBACK. O concorrente compartilhou oferta 13, início `2026-10-19 15:44:43.092005+00`, capacidade 1, e confirmou reserva 22. Captura final confirma remoção dessas duas linhas; **não repetir a preparação ou os scripts históricos automaticamente**. Perfil B temporário foi desfeito pelo ROLLBACK da sessão B; existentes preservados. Sequências podem avançar com ROLLBACK, sem indicar registros permanentes.

São simulações administrativas de papéis/identidades, não login nem verificação de JWT pela API. Concorrência comprovada no cenário controlado de duas sessões/uma vaga; não prova de todas as combinações. A contagem global da massa não foi repetida após a limpeza. Não foram repetidos testes Android/Lint, pois não houve mudanças Android; `git diff --check` passou. Histórico: compileDebugKotlin passou em 11/10; suíte instrumentada de 10/10 tinha 32 aprovados e um remoto ignorado, conforme docs/12.

## Android e próximo passo didático

- Kotlin/Compose; MVVM, Repository, StateFlow, corrotinas e DI manual. Manter domain/data/presentation/ui separados e NavController somente na navegação.
- Unidades e horários vêm do Supabase; unidade/data/horário/ID ficam no rascunho. Perfil, login, logout local e restauração inicial de sessão já implementados. Cadastro, recuperação de senha, provedores sociais e recorrência ainda visuais; falta observador global de sessão durante o uso.
- `domain/model/Appointment.kt` já contém id, donorId, timeSlotId, status e createdAt; status SCHEDULED/CANCELLED. `domain/repository/AppointmentRepository.kt`: `suspend createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment>`.
- Arquivos Kotlin estão sob `app/src/main/java/br/edu/fatec/doesangue/`. No commit do banco, Confirmar em `ui/navigation/SchedulingNavGraph.kt` ainda era navegação visual, sem implementação de AppointmentRepository, chamada RPC ou estados de gravação. A conversa nova acrescentou um DataSource ainda não integrado e sem commit, conforme correção acima; não tratar isso como aprendizado concluído ou reserva já ligada ao botão.
- Começar por DTO/mapper em data/supabase, depois DataSource/RPC, repository e composição em di/AppContainer. Preservar CancellationException; tratar retorno de uma linha e estado retornado explicitamente.
- Depois ligar SchedulingViewModel e revisão com carregamento/erro/nova tentativa/sucesso. Uma chave identifica um pedido lógico: reutilizar nas tentativas após falha/perda de resposta, sem gerar outra a cada clique. Novas escolhas/pedido exigem nova chave. Não navegar para sucesso de reserva ativa se o servidor devolver cancelado.
- Preservar agrupamento `and` dos filtros gte/lt em inicio (Supabase Kotlin 3.6.0 descarta um limite quando há chaves duplicadas fora do grupo). Não alterar esta consulta fora do escopo.

## Pendências acadêmicas e funcionais

Entrega em **25/11/2026**, apresentação focada no banco. Requisitos: pelo menos seis tabelas do domínio relacionadas, cinco FKs e 200 registros sintéticos distribuídos. Atualmente quatro tabelas, três FKs entre elas e uma adicional perfil→auth.users; massa anteriormente confirmada: duas unidades, três horários e um perfil (seis linhas), com as duas linhas extras do concorrente removidas. Contas Auth são separadas da meta de massa do domínio.

Ainda faltam integração/gravação pelo Android, validação autenticada A/B pela API, modelagem final das demais tabelas/cardinalidades/permissões, seed ampliado, DER/dicionário, consultas da apresentação e conta do professor. Cadastro real, cancelamento, recorrência e ajustes administrativos de capacidade precisam de decisões/operações próprias. Não assumir coleta ou estoque a partir da reserva. Ver [banco](../09-banco-de-dados.md) e [disponibilidade](../15-disponibilidade-agendamento.md).
