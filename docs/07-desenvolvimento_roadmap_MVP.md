# Doe Sangue — desenvolvimento e roadmap do MVP

**Sprint 0 — proposta para revisão (16/09/2026).** Nenhuma implementação, migração, operação remota ou commit foi feito nesta etapa. A trilha funcional do MVP é **Unidade → Data/Horário → preferência de recorrência se aprovada → Revisão → registro no Supabase → Minhas Doações/Agendamentos**. Auth, fonte de horários e contrato de persistência são preparados antes do registro. O antigo `07-roadmap.md` permanece no repositório como documento legado; este arquivo é a proposta nova para aprovação, não uma alteração silenciosa daquele plano.

## Critério de MVP

Uma pessoa autenticada consulta unidade e horários fornecidos por fonte autorizada, escolhe uma vaga, registra uma preferência de rotina somente se aprovada **sem reserva automática futura**, envia uma solicitação que recebe ID e estado persistidos e o encontra na própria lista. O fluxo deve lidar com ausência de vaga, conflito, sessão expirada e falha sem exibir falso sucesso. Nenhuma etapa confirma coleta, calcula aptidão ou altera estoque. Este recorte cobre RF001–RF008 e CU001–CU004; ver [visão](01-visao-do-produto.md), [requisitos](02-requisitos.md) e [modelo](05-modelo-de-dominio.md).

## MVP — etapas pequenas e verificáveis

| Etapa | Objetivo e entregáveis | Dependências | Aceite e testes | Documentação a atualizar |
| --- | --- | --- | --- | --- |
| **S0 — revisão desta proposta** | Aprovar intenção, escopo, regras/pendências, modelo conceitual e papéis. Sem SQL ou código. | Inspeção do repositório concluída. | Sete documentos coerentes, IDs cruzados, DP01/DP02 resolvidas quanto à viabilidade e natureza da operação (ou MVP redefinido), responsável/ambiente de DP10 definido e demais decisões atribuídas. | Estes sete documentos; registrar decisões aprovadas sem reescrever fatos como implementação. |
| **S1 — contratos e ambiente seguro** | Detalhar contrato da fonte de unidade/vagas e a natureza aprovada em S0; fechar campos mínimos, status, política de rotina e matriz de permissões; preparar projeto Supabase **de desenvolvimento**, modelo com pelo menos seis tabelas do app/cinco FKs para a entrega e estratégia de seed sintético. | Aprovação S0, viabilidade de DP01/DP02 e responsável/ambiente de DP10 definidos. | Contratos com exemplos sintéticos, política RLS proposta/revisada, sem credencial administrativa no APK; nenhum acesso a produção. Testes de contrato planejados. | 02, 03, 05, 06, 09 e decisões/ADR se aprovadas. |
| **S2 — identidade e perfil mínimo** | Integrar Auth por e-mail/senha e Google, sessão e perfil mínimo; proteger rotas privadas; persistência/RLS de perfil após aprovação explícita da migração. Configurar no Auth e apresentar no cadastro o mínimo de 8 caracteres com maiúscula, minúscula, número e símbolo para e-mail/senha. | S1; ambiente de desenvolvimento autorizado; configuração Google/deep link e entrega de e-mails definidas. | Login real não entra com credenciais inválidas; cadastro rejeita senha fraca também no servidor; Google não pede senha do app; sessão expirada bloqueia dados; dois usuários não leem perfis alheios. Testes Auth/RLS e UI de estados. | 02, 04, 05, 06, 09 e 12-validação legado, se mantido. |
| **S3 — unidades e disponibilidade** | Substituir listas e calendário fixos por fonte aprovada; exibir unidade, data, horários, vazio/erro e procedência. | S1; autenticação se fonte exigir. | Unidade selecionada determina seus horários; horário de atendimento não vira vaga; falha não mostra dados demo como reais. Testes de mapeamento, vazio/erro e origem. | 02, 03, 04, 05, 06. |
| **S4 — rascunho e revisão do fluxo** | Estado de seleção preservado entre Unidade → Data/Horário → preferência de rotina se aprovada → Revisão, sem gravar ainda; sem política aprovada, seguir diretamente com sem recorrência. | S3; RN006 decidida ou etapa de rotina omitida; limite RN013 respeitado; estratégia de navegação decidida. | Revisão mostra exatamente os valores escolhidos; recriação/volta não inventa seleção; sem políticas aprovadas, “sem recorrência” permanece o único modo. Testes de ViewModel/estado e navegação. | 02, 03, 04, 06. |
| **S5 — registro persistido** | Gravar uma solicitação/agendamento via operação que revalide oferta/capacidade, identidade, campos permitidos, transições e idempotência; resposta fornece ID/estado persistidos conforme DP02. Criar vínculo de rotina opcional conforme política aprovada. | S2–S4; modelo, RLS e migração de desenvolvimento aprovados. | Sucesso apenas após resposta do servidor e com texto fiel ao estado; vaga perdida não cria reserva nem confirmação indevida; repetição não duplica; nenhum registro de `doacao`/estoque muda. Testes de integração, concorrência e RLS com dois usuários/anon. | 02–06 e registro de migração/validação. |
| **S6 — Minhas Doações/Agendamentos e fechamento** | Ler agendamentos próprios da persistência, mostrar estado e navegar do sucesso/detalhe; retirar do fluxo funcional os literais fictícios. | S5. | Após reinstalação/nova sessão, agendamento próprio reaparece; outro usuário não o vê; lista vazia/erro tratados. Revisão manual em dispositivo e teste automatizado de rota/consulta. | 01–06, este roadmap e 10/12 legados se continuarem publicados. |

Cada sprint termina com `test`, `assembleDebug`, `lint` e testes relevantes à mudança. Atualização de 23/09/2026: Navigation Compose foi adotado e a navegação visual possui testes instrumentados (`connectedDebugAndroidTest`); isso não conclui S1–S6. O esquema físico continua sujeito a aprovação antes de migrations.

## Trilha da entrega acadêmica

O professor aceitou o tema **Clínica/Hospital — atendimento humano** para o Doe Sangue e sugeriu Supabase. O enunciado da Aula 07 define critérios da **entrega final**, não funcionalidade já presente: pelo menos seis tabelas relacionadas do app (sem depender de `auth.users` para a contagem), cinco FKs, 200 registros sintéticos distribuídos, constraints adequadas, Auth, consultas com filtro/busca/ordenação, leitura e escrita demonstráveis no app, GitHub organizado e relatório/apresentação em padrão ABNT. A [estratégia de população](09-banco-de-dados.md#como-popular-os-dados) separa migrações, seed do domínio e contas de teste.

Após o esquema e antes da apresentação: executar seed em ambiente de demonstração autorizado; verificar contagens e FKs; testar uma consulta e uma alteração pelo app e conferir o resultado no banco; preparar acesso do professor ao app sem senha versionada; combinar o convite de colaborador no GitHub; produzir relatório e roteiro de apresentação. A conta do professor, o ambiente Supabase e qualquer execução remota exigem confirmação de endereço, permissões e autorização específica. Não antecipar essas ações nesta Sprint 0.

## Pós-MVP — não bloquear o primeiro fluxo com extras

1. **Alterar/cancelar agendamento (RF009/CU005):** exigir regras de antecedência, transição e efeito sobre a vaga/rotina; testar auditoria e conflito.
2. **Doação efetiva/histórico (RF010/CU006):** integrar somente com registro operacional autorizado; testar que doador não confirma coleta. Sem fonte, manter fora de produção.
3. **Campanhas e estoque (RF011–RF012/CU007–CU008):** definir publicadores, medida, origem e frescor; testar publicação e RLS. Não inferir estoque a partir de agendamento.
4. **Lembretes (RF013/CU009):** definir consentimento, canal, horário e provedor; testar deduplicação, cancelamento e permissões. Telas atuais de notificação não enviam nada.
5. **Outros elementos visuais:** edição completa de perfil, recuperação/verify, mapa, contato e FAQ só entram após decisão de prioridade, conteúdo e infraestrutura. Login Google passa a integrar a etapa de Auth, não este grupo.

## Decisões de entrada e conclusão da Sprint 1

**Antes de iniciar S1:** definir com DP01 se existe fonte autorizada viável, com DP02 se a operação é reserva, solicitação sujeita à unidade ou registro de intenção, e com DP10 quem responde pelo ambiente de desenvolvimento. Sem agenda/autorização, redefinir o MVP como demonstração sintética ou registro de intenção antes de avançar. **Até concluir S1:** detalhar contratos, campos, estados, política de rotina e permissões; DP06–DP08 podem seguir como decisões pós-MVP quando não afetarem o primeiro fluxo.

| ID | Decisão a obter da responsável do produto / fonte | Por que bloqueia |
| --- | --- | --- |
| DP01 | Quem fornece, publica e atualiza unidades e horários? Existe API/integração autorizada? | Define `HorarioDisponivel`, sincronização e se o app pode mostrar vaga real. |
| DP02 | Confirmar no app cria reserva firme, solicitação sujeita à confirmação da unidade ou apenas intenção? Quem confirma? | Define estados, texto de sucesso, transações e CU003. A UI atual diz “confirmado” sem prova. |
| DP03 | Quais campos mínimos do perfil são necessários e qual fluxo de cadastro/validação por e-mail é exigido? | Minimização de dados, Auth, RLS e RF001–RF002. |
| DP04 | Quais estados/transições de agendamento, políticas de duplicidade, cancelamento e alteração são permitidos? | Constraints, auditoria, RN005/RN007/RN008 e evolução pós-MVP. |
| DP05 | O que significa recorrência: preferência, sugestão de próxima data, lembrete ou reserva automática? Há cadência aprovada e fonte oficial? | RN006/RN013; não copiar “3/6/12 meses” da tela como regra. **Regra dependente de fonte oficial / pendente de validação.** |
| DP06 | Haverá fonte operacional autorizada para doações realizadas, e quais papéis podem registrar/confirmar coleta? | `Doacao`, histórico verdadeiro e RLS; sem fonte, RF010 fica pós-MVP. |
| DP07 | Como serão publicadas campanhas e medidas de estoque, com unidade, origem e atualização? | Evita exibir protótipos como dados reais; modelo pós-MVP. |
| DP08 | Quais textos legais, consentimentos, retenção de dados e permissões de notificação são exigidos? | Cadastro, proteção de dados e lembretes. Exige revisão jurídica/organizacional, não inferência do protótipo. |
| DP09 | **Navegação decidida e implementada em 23/09/2026:** Navigation Compose 2.9.8 com rotas tipadas; ver ADR-003. Confirmação do `applicationId` continua pendente. | Argumentos de registros, deep links e preservação de histórico por aba ainda precisam de implementação/decisão; não confundir restauração de rota com rascunho persistido. |
| DP10 | Qual ambiente Supabase de desenvolvimento e quem aprova migrations/RLS? | Nenhuma operação remota ou SQL deve ocorrer sem projeto-alvo e aprovação. |

## Notas sobre legados e limites ainda presentes

- `AGENTS.md` e `docs/11-demonstracao.md` foram alinhados ao estado das 34 telas visuais; `docs/07-roadmap.md` aponta para este plano vigente. `docs/09-banco-de-dados.md` permanece como resumo anterior; revisar ou arquivar os demais legados quando o escopo for aprovado.
- O texto visual apresenta “já pode doar”, intervalos de 3/6/12 meses, “agendamento confirmado”, “doação realizada”, estoque crítico e lembrete ativo, mas não existe validação médica, confirmação operacional, persistência, fonte de estoque ou envio. O agendamento estático de 12/09/2026 já é passado na data desta revisão (16/09/2026). Não usar esses literais como regra ou dado real.
- `HomeViewModel`/fonte demo existem mas não são consumidos pelas telas; Home e perfil usam literais. Login e conclusão do agendamento continuam visuais. Navigation Compose está configurado e conectado; Supabase permanece pendente.
- Há uma modificação local anterior em `DoeSangueComponents.kt` (`navigationBarsPadding()` para a barra inferior), não relacionada a estes documentos e não commitada. Esta Sprint 0 não a altera nem a envia.
