# Validação

## Formulário de criação do perfil — 10/10/2026

- A usuária confirmou manualmente em 09/10/2026 que a conta sintética autenticada abriu Perfil e recebeu a mensagem de perfil ausente. É evidência de uma consulta concluída sem perfil visível, não de criação persistida ou isolamento entre duas contas pela API.
- O formulário de nome está ligado ao ProfileViewModel. A criação valida nome vazio, envia somente ID da conta autenticada e nome normalizado via DataSource, solicita o registro inserido e apresenta Loaded após sucesso. Durante o envio bloqueia alterações, novas criações e consulta concorrente; falha preserva o nome e mostra mensagem genérica. Nenhuma migração nova foi necessária.
- Criado ProfileFlowTest: cinco testes com grafo/tela/ViewModel reais e repository controlado em memória. Cobrem nome vazio/espaços sem escrita; criação com trim e leitura ao reabrir Perfil; falha de criação com nome preservado e nova tentativa; resposta pendente bloqueando edição/envios repetidos/consulta concorrente; carregamento, falha de leitura e nova tentativa abrindo o formulário. Respostas pendentes usam CompletableDeferred, sem depender de atrasos da rede.
- Executado `.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.edu.fatec.doesangue.ui.navigation.ProfileFlowTest,br.edu.fatec.doesangue.ui.navigation.AppNavigationTest'` no Pixel_6/Android 12: BUILD SUCCESSFUL, **15 aprovados, zero falhas/erros/ignorados**, conferidos no XML (cinco de perfil e dez de navegação). Compilação do aplicativo e APK de testes concluída; diff --check sem erros.
- Validação manual posterior: em 10/10/2026, a usuária relatou resultado esperado do roteiro com a conta sintética A: criar Doador Demo A pelo aplicativo, exibir o nome e sair/voltar à aba Perfil. Captura posterior da consulta com JOIN entre perfil_doador e auth.users, filtrada pelo e-mail sintético A, confirma uma linha com o UUID da conta A, nome Doador Demo A e criado_em preenchido em 10/10/2026. A captura confirma o registro persistido e o vínculo com a conta; a consulta administrativa não comprova isolamento entre contas pela API. O agente não executou escrita remota nem recebeu credenciais nesta etapa.
- Decisão de fluxo: no cadastro real, o nome informado será usado para criar o perfil quando houver sessão autenticada; não haverá uma segunda etapa obrigatória para redigitar o nome. O formulário Missing permanece como recuperação para contas sem perfil. Cadastro real, confirmação de e-mail e recuperação de criação parcial ainda precisam ser integrados.
- Limites: constraints e isolamento entre contas via API, cancelamento e perda de resposta após INSERT ainda não foram verificados. Reabrir Perfil no teste automatizado lê memória fictícia; a evidência real acima é manual. Login, orientações e teste remoto de horários não foram repetidos nesta execução focada; resultados anteriores abaixo são históricos.
- Revisão do checkpoint: removidos dois imports duplicados do grafo principal e atualizado o comentário sobre a entrada após login. `assembleDebug` repetido com BUILD SUCCESSFUL; diff --check sem erros. Testes instrumentados não foram repetidos por esses ajustes de imports/comentários e documentação.

## Integração da tela de perfil — 09/10/2026

- Modelo, DTO, mapper, DataSource, repository, ProfileViewModel e fábrica estão conectados à tela pelo grafo principal. A UI representa carregamento, perfil encontrado, perfil ausente e erro com nova tentativa. A consulta usa o mesmo cliente do Auth e filtra pelo ID da conta autenticada; não cria perfis.
- A usuária relatou login real bem-sucedido com a conta sintética em 09/10/2026. Esta é evidência manual de autenticação, não de leitura/criação do perfil.
- AppNavigationTest, DonationRestrictionsTest e LoginFlowTest recebem FakeDonorProfileRepository. Após uma tentativa sem dispositivo conectado (aplicativo e APK de testes compilados), `connectedDebugAndroidTest` concluiu no Moto g04/Android 14: **17 aprovados, zero falhas/erros e um teste remoto ignorado**. XML confirma 18 casos totais; o console apresentou contagem extra. São dez testes de navegação, quatro de orientações, três de login e um remoto desativado por padrão.
- Limites: a suíte confirma os percursos existentes com o perfil integrado e repository fictício; ainda não há testes específicos dos quatro estados do perfil, nova tentativa ou conteúdo do nome exibido. Consulta real do perfil e criação pelo aplicativo permanecem pendentes. Nenhuma escrita remota foi feita nesta validação.

## Login Android: validação com repository fictício — 08/10/2026

- Login por e-mail/senha está ligado à tela, LoginViewModel, AuthRepository e implementação Supabase. As execuções abaixo substituem o repository e não comprovam login remoto.
- Após adaptar AppNavigationTest e DonationRestrictionsTest para preencher os campos e injetar FakeAuthRepository, `connectedDebugAndroidTest` passou no Pixel_6/Android 12: **14 aprovados, zero falhas/erros e um teste remoto ignorado**. Contagem conferida no XML; o resumo de console do Gradle apresentou uma contagem extra.
- Criado LoginFlowTest com três testes instrumentados, todos aprovados no mesmo emulador: campos vazios (ambos, senha ausente e e-mail contendo só espaços) não chamam o repository; falha mantém Login, mostra mensagem genérica e permite nova tentativa bem-sucedida; resposta pendente desabilita campos/botão e bloqueia solicitações repetidas no ViewModel, inclusive após sucesso.
- O cenário de falha/nova tentativa também verifica remoção de espaços externos somente do e-mail, preservação da senha fictícia e ausência da mensagem técnica do repository na tela. O cenário de carregamento usa CompletableDeferred para controlar a resposta, sem atrasos arbitrários ou acesso à rede.
- Execução focada: `.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.edu.fatec.doesangue.ui.navigation.LoginFlowTest'` — BUILD SUCCESSFUL, **3 aprovados, zero falhas/erros/ignorados**. Os 14 testes anteriores não foram repetidos após adicionar a classe independente; código de produção não foi alterado nesta validação.
- Pendências: login real via Supabase, sessão/restauração/logout, cancelamento da tentativa ao encerrar o ViewModel e criação/leitura do perfil pelo aplicativo. Cadastro, recuperação e provedores sociais continuam visuais. Estes testes não verificam RLS, JWT, persistência de perfil ou agendamento. Resultados das etapas anteriores abaixo são históricos.

## Perfil do doador: estrutura e permissões — 08/10/2026

- A usuária preparou `20261008000100_criar_perfil_doador.sql` em pequenos passos e relatou sua execução manual no Supabase de desenvolvimento. O agente revisou o arquivo e as capturas; não executou SQL remoto.
- Captura de `pg_tables` confirma `public.perfil_doador` com RLS ativo. A migração declara ID UUID como PK/FK para `auth.users(id)`, exclusão restrita, nome obrigatório não vazio após trim e data de criação com default now(). O preenchimento da data foi observado na inserção descrita abaixo; rejeições por constraints ainda não foram exercitadas.
- Captura de `has_column_privilege` confirma: anon sem SELECT/INSERT/UPDATE nas três colunas; authenticated com SELECT nas três, INSERT somente em id/nome_exibicao e UPDATE somente em nome_exibicao. Outra captura confirma DELETE ausente para ambos os papéis.
- Captura de `pg_policies` mostra três policies para authenticated: SELECT com USING, INSERT com WITH CHECK e UPDATE com ambas as condições, comparando id com auth.uid().
- A usuária criou duas contas sintéticas pelo painel Auth. Uma captura de LEFT JOIN confirma ambas em auth.users, sem perfil naquele momento. Contas de teste não equivalem a seed de perfis nem comprovam login realizado.
- Testes manuais no SQL Editor usaram BEGIN, SET LOCAL ROLE e request.jwt.claim.sub, com ROLLBACK ao final. Captura confirma INSERT do próprio perfil como A, com nome e criado_em retornados. Outras capturas confirmam SELECT sem WHERE, sobre dois perfis temporários: A recebeu somente A e B recebeu somente B.
- Conforme relato da usuária, UPDATE sem WHERE como A alterou somente o nome de A; consulta administrativa posterior mostrou B intacto. Ela também confirmou os resultados esperados dos blocos que capturam insufficient_privilege: A tentando inserir perfil com ID de B foi bloqueado, e consulta como anon foi bloqueada.
- Limites: são testes manuais de permissões/policies com identidade simulada administrativamente, não testes de login/JWT via API ou pelo Android. Os perfis usados nos scripts são temporários e não compõem o seed. Ainda faltam rejeições por constraints, tentativas efetivas de alterar ID/data ou excluir, consulta de TRUNCATE e criação/edição própria como B. Login Android e criação do perfil pelo aplicativo ainda não existem. Não houve nova execução Gradle nesta etapa exclusivamente SQL/documental.

## Seleção do horário pelo ID — 08/10/2026

- Inspeção: a tela envia o ID ao ViewModel, que valida o registro contra unidade/data/lista antes de guardar ID e hora. Troca de unidade/data e recarga limpam a seleção; Continuar e Confirmar exigem o ID preenchido.
- Validação manual relatada pela usuária: selecionar Demo A, 19/10/2026 e 08:00 habilita Continuar; mudar para 20/10 mostra agenda vazia e desabilita o botão; voltar para 19/10 não restaura a seleção; escolher novamente permite avançar até a revisão com os dados corretos e Confirmar habilitado.
- `test assembleDebug`: um teste local básico aprovado e compilação concluída. A primeira tentativa instrumentada encontrou o Moto g04/Android 14 com tela apagada e bloqueada, com falhas de ausência de hierarquia Compose; foi interrompida pelo agente.
- Após a usuária desbloquear o aparelho, `connectedDebugAndroidTest` terminou com BUILD SUCCESSFUL: **14 testes instrumentados aprovados**, zero falhas/erros e um teste de leitura remota ignorado por padrão. Contagem conferida no XML: dez testes de navegação, quatro de orientações e um remoto ignorado. A suíte usa repositories fictícios e verifica o fluxo, incluindo limpeza da seleção ao mudar a data.
- Limites: o teste local básico não verifica a nova lógica do ViewModel. Não foram acrescentados testes isolados para IDs inválidos ou registros de outra unidade/data. A confirmação continua visual, sem gravação de reserva. Lint e teste remoto de leitura não foram repetidos nesta etapa.

## Consulta de horários e correção do intervalo — 07/10/2026

- `test assembleDebug assembleDebugAndroidTest connectedDebugAndroidTest`: BUILD SUCCESSFUL com o argumento de leitura remota abaixo. Um teste local básico aprovado e **15 testes instrumentados** no Pixel_6/Android 12, sem falhas ou ignorados: dez de navegação, quatro de orientações e um de integração de leitura dos horários.
- `SupabaseTimeSlotsReadTest` usa o cliente do aplicativo, a API real de desenvolvimento e a tela de data/horário, com o seed sintético existente. Verificou 18/10/2026 vazio; 19/10/2026 com 08:00 e sem 09:30; seleção habilitando Continuar; 20/10 e retorno a 18/10 vazios; retorno a 19/10 sem seleção e com Continuar desabilitado. Nenhuma escrita remota foi realizada.
- O teste de integração é ignorado por padrão. No PowerShell, para executar explicitamente contra o ambiente acadêmico configurado: `.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.enableSupabaseReadTests=true'`. Depende da rede, da configuração local e do seed de 19/10/2026; não representa uma suíte independente do ambiente. A chave não é escrita no código de teste nem nos seus resultados.
- A regressão vinha do envio de filtros repetidos para a mesma coluna na biblioteca Supabase Kotlin 3.6.0: PostgrestRequestBuilder usa mapToFirstValue, descartando o segundo valor. Agrupar os limites em and preserva início inclusivo e fim exclusivo no pedido. Compilar, isoladamente, não detectava esse erro de consulta.
- O teste de navegação foi adaptado: antes de selecionar a data, 09:30 não existe na tela; anteriormente ficava visível e desabilitado. Os testes normais usam repositories fictícios para unidades e horários.
- Limites: não exercitados falha/repetição da rede, corridas de respostas com atraso controlado, transições de horário de verão, reserva/capacidade concorrente ou login real. O calendário ainda permite consultar datas sem ofertas. Lint não foi repetido neste checkpoint.

## Histórico: horários sintéticos e permissões antes da integração Android — 07/10/2026

- A usuária executou o SQL manualmente no Supabase de desenvolvimento; o agente conferiu as capturas, sem executar SQL remoto.
- `pg_tables` mostrou `public.horario_disponivel` com RLS ativo. Após `seed_horarios.sql`, a consulta administrativa mostrou três ofertas em 19/10/2026: Demo A às 08:00 (capacidade 3, ativa), Demo A às 09:30 (capacidade 2, inativa) e Demo B às 08:00 (capacidade 1, ativa). Demo A publicada; Demo B não publicada.
- Consultas com JOIN entre horários e unidades, sem WHERE, executadas com `SET LOCAL ROLE anon` e depois `authenticated`, retornaram somente Demo A às 08:00. Os testes usaram transações encerradas por ROLLBACK.
- `has_table_privilege` retornou SELECT = true e INSERT/UPDATE/DELETE = false para os dois papéis na tabela de horários. TRUNCATE não foi consultado nesta etapa.
- A usuária também confirmou por relato o resultado esperado na consulta direta a horario_disponivel, sem JOIN e sem WHERE, sob anon e authenticated: somente o horário ativo da unidade publicada.
- Limites: Constraints não foram exercitadas por inserções inválidas. Simular o papel authenticated não testa login ou JWT. Não há reserva, cálculo de vagas restantes ou consulta dos horários pelo Android.
- Fuso no Android: `assembleDebug assembleDebugAndroidTest` passou após DTO/mapper/modelo receberem o campo; a usuária confirmou carregamento da unidade sem erro. `assembleDebug` passou com `GetDonationTimeSlotsUseCase`. Limites de dia/fuso ainda não têm testes específicos. Não houve nova execução instrumentada ou de Lint nesta etapa.

## Revisão ligada ao rascunho — 07/10/2026

- `assembleDebug`: BUILD SUCCESSFUL após ligar a revisão ao estado e após adicionar a condição de habilitação de Confirmar.
- A usuária confirmou a exibição e enviou captura da revisão com Unidade Demo A, 19/10/2026 e 08:00. Isso comprova a apresentação dessas escolhas; não comprova gravação no banco nem validação de disponibilidade.
- Inspeção do código: Confirmar exige unidade, data e horário não nulos. O comportamento dos estados incompletos ainda não foi exercitado em teste automatizado.
- A suíte instrumentada e o Lint não foram repetidos nesta pequena alteração. Os 14 testes aprovados abaixo pertencem ao checkpoint anterior à ligação da revisão ao rascunho.

## Orientações e rascunho do agendamento — 07/10/2026

- `test assembleDebug assembleDebugAndroidTest lint`: sucesso na preparação desta entrega; um teste local básico aprovado e APKs gerados. Lint: zero erros e 31 avisos.
- `connectedDebugAndroidTest`: execução final aprovada no Pixel 5, Android 12/API 32; **14 testes**, sendo dez de navegação e quatro de orientações, sem falhas, erros ou ignorados.
- O fluxo exige unidade, data e horário. A cobertura nova verifica cancelamento do calendário, confirmação da mesma data, preservação ao voltar à unidade e limpeza do horário ao mudar de data. A conclusão visual também foi exercitada.
- As quatro categorias de orientações foram percorridas, com fontes e data de revisão; as condições de avaliação médica, procedimentos e preservação da prevenção permanecem nos textos verificados.
- O seletor do dia usa a data acessível do Material 3 e restringe a busca ao diálogo, evitando confusão entre o dia 15 e o horário 15:30 da tela ao fundo. As falhas anteriores desse seletor foram corrigidas; houve também uma falha do sistema do emulador, resolvida com reinicialização antes da execução final.
- Testes usam `FakeDonationCenterRepository` e dados sintéticos. Não validam Supabase remoto, reserva real, disponibilidade por unidade ou aptidão clínica. Revisão/sucesso continuam visuais e não gravam agendamento.

Ver [revisão editorial e fontes](16-revisao-regras-doacao.md) e [disponibilidade planejada](15-disponibilidade-agendamento.md). Relatórios locais permanecem em `app/build`, fora do versionamento.

## Integração de unidades — 06/10/2026

- `test assembleDebug assembleDebugAndroidTest lint`: BUILD SUCCESSFUL; um teste local básico aprovado; APK do aplicativo e dos testes gerados.
- Lint: zero erros e 31 avisos, incluindo versões/dependências, ícones, recurso não usado e anotação de preservação de enum na minificação. Não houve ampliação de escopo para corrigir esses avisos nesta etapa.
- `connectedDebugAndroidTest`: 12 testes aprovados (9 em `AppNavigationTest`, 3 em `DonationRestrictionsTest`), zero falhas, erros ou ignorados. Emulador Pixel 5, Android 12/API 32.
- Ambos os testes injetam `FakeDonationCenterRepository` no container; não consultam Supabase nem exigem chave configurada. Verificam a navegação existente, não persistência de agendamento.
- Em 03/10/2026 a usuária confirmou manualmente a exibição de Unidade Demo A do Supabase no celular. Também relatou anteriormente que os dois papéis consultam somente a unidade publicada e não possuem privilégios de escrita. Não houve teste automatizado remoto nem tentativa automatizada de escrita nesta etapa.
- Ainda pendem testes de sucesso/vazio/erro/cancelamento do ViewModel/repository, mapper, dados privados com usuários distintos e concorrência. Ver [estado e responsabilidades](14-integracao-unidades.md).

Relatórios locais (não versionados): `app/build/reports/androidTests/connected/debug/index.html`, `app/build/test-results/testDebugUnitTest` e `app/build/reports/lint-results-debug.html`.

## Histórico: consulta de restrições — substituição do fluxo anterior

- `test assembleDebug lint`: BUILD SUCCESSFUL. O teste local básico passou.
- `connectedDebugAndroidTest`: **12 testes aprovados**, sem falhas (9 de navegação
  existentes e 3 da consulta de restrições).
- Os testes novos percorrem as quatro listas, verificam fontes sem abrir sites,
  retorno à Home normal/agendada, restauração de categoria, Voltar do sistema
  e mensagem de erro quando não há navegador.
- Lint: 0 erros e 28 avisos; os avisos restantes são de dependências/recursos do projeto.
- Relatório: `app/build/outputs/androidTest-results/connected/debug`.
- Capturas das duas entradas e quatro listas revisadas em 360 dp, em
  `app/build/captures/restricoes/files`. A largura original do emulador foi restaurada.
- A abertura de todas as listas com o botão de retorno visível foi verificada
  em uma execução adicional do teste de categorias, também aprovada.

As capturas usam a UI real com áreas de toque de pelo menos 48 dp. A Home
mantém suas seções anteriores além do novo cartão. Não foram realizados
testes com TalkBack ou todas as escalas de fonte.

Os testes antigos de busca, calendário e cálculo foram removidos junto com a
funcionalidade. O resultado abaixo é histórico e não descreve a suíte atual.

## Histórico: medicamentos — 29/09/2026

- `test`: 15 testes locais aprovados (14 novos e o básico existente).
- `assembleDebug` e `assembleDebugAndroidTest`: APKs gerados.
- AndroidJUnitRunner no Pixel 5/API 32: **12 testes instrumentados aprovados**,
  sendo 9 de navegação e 3 do fluxo novo. A saída final está em
  `app/build/captures/triagem/instrumentation-final.txt`.
- Capturas revisadas em 360 dp; calendário ajustado para mostrar todas as sete
  colunas, com idioma português e bloqueio de datas futuras.
- Verificação final de `assembleDebug lint`: `BUILD SUCCESSFUL`; 0 erros e
  30 avisos no Lint.

O Gradle continua sendo a forma recomendada de executar a suíte:
`.\gradlew.bat connectedDebugAndroidTest test assembleDebug lint`.
Ver [escopo, adaptações e limites](13-triagem-medicamentos.md).

## Ambiente observado em 30/08/2026

- Windows com JDK 21.0.8.
- Android SDK com plataformas 33, 34 e 36; Build Tools 36.0.0.
- Gradle global ausente; Gradle Wrapper incluído.

## Verificações desta etapa

| Verificação | Resultado |
| --- | --- |
| Estrutura e arquivos obrigatórios | Aprovada por inspeção local |
| `gradlew.bat test` | Aprovado em 30/08/2026 |
| `gradlew.bat assembleDebug` | Aprovado em 30/08/2026 |
| `gradlew.bat lint` | Aprovado em 30/08/2026 |

Execução conjunta: `BUILD SUCCESSFUL` em 1 min 43 s. O sandbox negou escrita nas pastas globais de métricas Android e daemon Kotlin; a compilação usou o fallback sem daemon e concluiu. Supabase, migrações e RLS não foram testados porque ainda não estão implementados.

## Validação das camadas — 30/08/2026

Após criar `domain`, `data`, `presentation` e `di`, os comandos `test`, `assembleDebug` e `lint` foram executados em conjunto. Resultado: `BUILD SUCCESSFUL` em 5 min 43 s, com 51 tarefas executadas. A demora ocorreu na primeira resolução/compilação das dependências Lifecycle e corrotinas.

## Validação da UI e navegação — 30/08/2026

Após implementar as 34 telas, o tema, os componentes e o roteador, `test`, `assembleDebug` e `lint` foram executados em conjunto. A execução final, após o ajuste do cabeçalho comum, resultou em `BUILD SUCCESSFUL` em 16 s, com 51 tarefas (14 executadas e 37 atualizadas).

O APK debug foi instalado e iniciado no emulador Pixel 5. A árvore de acessibilidade confirmou a Activity em primeiro plano, os elementos das telas e a navegação `Boas-vindas → Login → Home → Perfil → Configurações`, incluindo retorno e destinos internos. A captura de pixels desse AVD retornou uma imagem preta por falha do renderizador do emulador; esse problema é externo ao app e não impediu a confirmação estrutural da interface e do fluxo.


## Navigation Compose — 23/09/2026

**Implementado:** suíte `AppNavigationTest` em `app/src/androidTest/java/br/edu/fatec/doesangue/ui/navigation`. Usa o `AppNavHost` real, `rememberNavController`, cliques na interface e verificação de destino e pilha. As dependências de teste já estavam configuradas; nenhuma biblioteca adicional foi necessária.

| Teste | Comportamento verificado |
| --- | --- |
| `splashIsRemovedFromBackStack` | Boas-vindas sem Splash como tela anterior |
| `enteringMainRemovesAuthenticationHistory` | Entrada visual na Home sem Auth no histórico |
| `passwordRecoveryReturnsToLogin` | Recuperação volta ao Login pela seta |
| `registrationCompletionRemovesRegistrationSteps` | Avanço/retorno no cadastro e conclusão sem reabrir etapas |
| `rootArrowsReturnHomeWithoutAccumulatingTabs` | Setas das cinco telas raiz retornam à Home sem acumular abas |
| `internalBackKeepsThePreviousScreen` | Sobre → Configurações → Perfil → Home |
| `schedulingCompletionClearsItsSteps` | Resultado visual → HomeScheduled sem etapas anteriores |
| `cancellationReturnsToListWithoutReopeningOldDetail` | Cancelamento visual retorna à lista sem reabrir o detalhe |
| `savedNavigationRestoresDestinationAndReturnPath` | Estado salvo restaura Sobre e o retorno para Configurações |

### Como executar

Inicie um emulador ou conecte um dispositivo de teste com depuração USB. Na raiz do projeto:

```powershell
.\gradlew.bat connectedDebugAndroidTest test assembleDebug lint
```

Também é possível executar `AppNavigationTest` pelo ícone de execução ao lado da classe no Android Studio, escolhendo um dispositivo de teste.

- Relatório instrumentado: `app/build/reports/androidTests/connected/debug/index.html`.
- Relatório unitário local: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Relatório Lint: `app/build/reports/lint-results-debug.html`.

### Limites da cobertura

`test` executa apenas o teste local básico preexistente; a cobertura de navegação vem de `connectedDebugAndroidTest`. Os nove testes não equivalem a cobertura de todos os botões das 34 telas.

A restauração automatizada usa `StateRestorationTester`: recria o estado salvo do Compose, sem simular encerramento real do processo ou reinicialização do aparelho. Rotação e botão Voltar do Android também foram exercitados na verificação interativa anterior, em Pixel 5/API 32. A suíte não comprova autenticação, persistência, rascunho, regras clínicas, Supabase, RLS ou histórico independente por aba.

### Resultado da execução

Execução em 23/09/2026 no emulador Pixel 5, Android 12L/API 32: **9 testes instrumentados aprovados, 0 falhas, 0 ignorados**. `connectedDebugAndroidTest test assembleDebug lint` terminou com `BUILD SUCCESSFUL` em 1 min 48 s. O teste unitário local passou; Lint terminou sem erros, com 27 avisos. O antigo `AppNavigator.kt` foi removido pela usuária e não há referências no código-fonte.
