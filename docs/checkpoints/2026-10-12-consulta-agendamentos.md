# Checkpoint — consulta dos próprios agendamentos — 12/10/2026

## Retomada obrigatória em modo de tutoria

Projeto: `C:\Users\katia\Documents\Apps\DoeSangue`. Ler AGENTS.md, este registro e os arquivos diretamente relacionados ao próximo passo. A usuária escreve produção; o agente confere, explica e fornece somente um trecho curto por resposta. Não criar/editar Kotlin de produção ou migrações automaticamente. Testes e documentação podem ser preparados pelo agente. A usuária preferiu continuar na mesma conversa; este checkpoint não autoriza abrir outra nem implementar os passos pendentes.

## Estado local e Git

- Branch `main`; base da etapa `7e8eade` (confirmação de agendamento integrada ao Supabase). A usuária autorizou commit/push da consulta após a conferência final; verificar versão e sincronização no Git ao retomar.
- Conteúdo autorizado para registro: AppointmentSummary, getMyAppointments no contrato/repository, DTO/mapper, RPC no DataSource; migração de leitura, roteiros de teste, teste local e documentação. Não incluir implementação de ViewModel/tela, ainda pendente.
- Aplicativo e testes compilaram com `compileDebugKotlin compileDebugAndroidTestKotlin`. Na checagem final, Lint passou com 31 avisos e zero erros. Os 16 testes instrumentados da etapa de criação são históricos; não foram repetidos nesta etapa de leitura.
- O agente adaptou dois repositories fictícios de androidTest para o contrato: leituras retornam falha explícita porque a listagem ainda não faz parte desses roteiros. Configurar respostas fictícias adequadas antes de ligar a tela; não deixar testes recorrerem ao Supabase real.
- Teste local de contrato/mapper em `app/src/test/java/br/edu/fatec/doesangue/data/supabase/mapper/AppointmentSummaryMapperTest.kt`: três aprovados, zero falhas/erros/ignorados, conferidos no XML, BUILD SUCCESSFUL. JSON aninhado/lista, IDs bigint, precisão/offsets, cancelado e estado desconhecido, sem rede. Evidência em docs/12-validacao.md.

## Banco remoto e evidências

- Migração `supabase/migrations/20261012000200_consultar_meus_agendamentos.sql` aplicada manualmente conforme relato da usuária. É a sétima; não reaplicar. Histórico da CLI ainda não conciliado; não usar db push/reset para resolver automaticamente.
- `consultar_meus_agendamentos()` é STABLE/SECURITY DEFINER com search_path vazio. Exige auth.uid() e filtra somente reservas desse doador. Não recebe identidade do cliente. Retorna `agendamento` JSONB com campos explícitos, `nome_unidade`, `inicio` e `fuso_horario`, incluindo canceladas, passadas e ofertas ocultas. Consulta pública mantém suas policies.
- Captura de configuração: anon sem EXECUTE, authenticated com EXECUTE, SECURITY DEFINER=true, volatilidade=s, search_path vazio.
- Captura de leitura isolada: A vê somente A, B somente B, reserva/unidade/início/fuso conferidos, reserva 23 preservada. Fixtures canceladas e perfil B, se criado, ficam em ROLLBACK.
- Captura de acesso/vazio: B sem reservas recebe vazio; authenticated sem identidade bloqueado por 42501/mensagem específica; anon bloqueado; reserva 23 preservada.
- Captura de oferta oculta: reservas próprias visíveis com horário inativo/unidade não publicada; passada/cancelada mantém estado; consulta pública mantém ofertas ocultas; B não recebe A; reserva 23 preservada. Fixtures novas de unidade/horário/reserva ficam em ROLLBACK, sem alterar ofertas existentes.
- Todas essas leituras são simulações administrativas de identidade/papel, sem login/JWT pela API. Não extrapolar para a integração Android ou todas as combinações de concorrência.
- Captura da conferência final pós-ROLLBACK confirma 2 unidades, 3 horários, 1 perfil, 1 agendamento; zero unidades temporárias do roteiro, zero outras reservas no horário de 23 e estado de 23 agendado. Etapa SQL concluída nesse recorte e limpeza conferida. Não apagar registros nem resetar sequências automaticamente.

## Registro persistido a preservar

Reserva 23, criada pelo aplicativo com a conta sintética A: Doador Demo A, Unidade Demo A, 19/10/2026 às 08:00 local, estado agendado, criado_em 2026-10-12 23:14:35.586383+00. Segunda intenção não criou outra reserva. Não cancelar/excluir essa linha como limpeza dos testes.

A massa atual foi confirmada por captura da conferência final: duas unidades, três horários, um perfil e a reserva 23, totalizando sete registros do domínio. As duas contas Auth são separadas dessa massa; não registrar credenciais neste checkpoint.

## Próximo passo e pendências

1. Conferência final concluída e evidências/contagem atualizadas. A usuária autorizou commit/push antes de avançar produção; conferir a conclusão no Git ao retomar.
2. Primeiro trecho didático da próxima integração: criar estado imutável de listagem em `presentation/appointments`, depois ViewModel dependente de AppointmentRepository, fábrica, navegação e tela, um passo por resposta. Reutilizar AppContainer; não levar NavController ao ViewModel.
3. Hoje a tela de agendamentos, detalhes e cancelamento continuam visuais. Confirmar já grava; listar ainda não lê pelo aplicativo. As escolhas da reserva persistida devem ser consultadas no servidor, sem depender do rascunho em memória.
4. Validar carregamento/erro/nova tentativa/vazio/lista e troca de conta com repositories fictícios. Depois verificar leitura real da reserva 23 e isolamento A/B com sessão/JWT pela API.
5. Permanecem pendentes: recuperação real após perda de resposta/morte do processo, cancelamento persistido, cadastro real, recorrência e ampliação da modelagem/seed acadêmico. Entrega 25/11/2026 exige pelo menos seis tabelas relacionadas, cinco FKs e 200 registros sintéticos; quatro tabelas do domínio estão aplicadas, sem seed ampliado.

Ver [validação](../12-validacao.md), [disponibilidade](../15-disponibilidade-agendamento.md) e [Supabase](../../supabase/README.md).
