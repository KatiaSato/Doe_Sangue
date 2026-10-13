# Validação

## Mostrar/ocultar senha no login — 13/10/2026

- A usuária implementou estado local `passwordVisible`, transformação condicional e botão textual Mostrar/Ocultar em `LoginScreen`. Senha inicialmente oculta; botão desabilitado durante o envio. A alteração preserva o valor digitado e o callback existente. O ícone de olho foi adiado por decisão da usuária.
- `assembleDebug`: BUILD SUCCESSFUL. A usuária confirmou o roteiro manual de ocultação inicial e alternância sem perda de texto; sem captura adicional.
- `connectedDebugAndroidTest` focado em `LoginFlowTest`, Pixel_5/Android 12: **três aprovados, zero falhas/erros/ignorados**, conferidos no XML; BUILD SUCCESSFUL. Suíte existente, sem novos testes, com repository fictício: valida campos vazios, falha/nova tentativa com envio da senha sem alterações e bloqueio/duplicidade durante o envio. A alternância Mostrar/Ocultar tem evidência manual; os testes existentes não acionam esse botão. Não houve validação de login remoto nesta rodada.
- `git diff --check` passou. Nenhuma mudança no banco, migração, repository ou ViewModel. Diagrama em [visibilidade da senha](19-diagrama-sequencia-visibilidade-senha.md).

## Detalhes da reserva pelo ID — 13/10/2026

- Diagrama da etapa apresentado antes do código em [detalhe pelo ID](18-diagrama-sequencia-detalhe.md). A usuária implementou estado/ViewModel, fábrica que recebe ID, passagem da função pela navegação, rota tipada e tela. O botão de cada cartão emite seu ID; o destino usa `toRoute()` e fornece o ID ao ViewModel. A consulta reutiliza `getMyAppointments()` e localiza o ID na lista própria, com Loading/Loaded/NotFound/Error. Dados exibidos vêm do servidor, com fuso da unidade e estado preservados. Detalhe/cancelamento visual antigo permanece separado; nenhuma nova operação de escrita ou migração.
- O agente adaptou oito roteiros existentes à função de fábrica recebida por AppNavHost e preparou `AppointmentDetailFlowTest`, com sete cenários. `compileDebugKotlin compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL. O novo roteiro também compilou na preparação da execução.
- A primeira tentativa de `connectedDebugAndroidTest` não iniciou os testes: o dispositivo estava offline. Após reconectar e conferir `emulator-5554` online, a execução focada de `AppointmentDetailFlowTest` passou no Pixel_5/Android 12: **sete aprovados, zero falhas/erros/ignorados**, conferidos no XML; BUILD SUCCESSFUL. Cobre seleção do segundo ID com consulta nova e dados atualizados, fuso/cancelado/Voltar, carregamento/duplicidade, erro genérico/nova tentativa para o mesmo ID, ID ausente/lista vazia, saída pendente/cancelamento/troca de ID, troca A/B com ID alheio/ID próprio e restauração do ID tipado acima de 32 bits. A restauração é do estado salvo do Compose/navegação, sem morte real do processo ou renovação da sessão.
- Execução complementar no mesmo Pixel_5/Android 12: `AppointmentListFlowTest` com seis aprovados e `AppNavigationTest` com dez aprovados; zero falhas/erros/ignorados em ambas, conferidos no XML; BUILD SUCCESSFUL. São 23 aprovações somando os dois comandos da etapa (7 + 16); as demais suítes de Auth/Perfil/Criação não foram repetidas neste recorte.
- `lint` em execução separada: BUILD SUCCESSFUL, zero erros e 31 avisos; contagem no SARIF. Repositories e sessões dos testes instrumentados são fictícios, sem chamadas ao backend real.
- Verificação manual seguinte, orientada para a conta sintética A: primeira captura confirma a listagem de 23 com o botão “Ver detalhes”; após orientação de clique, segunda captura mostra o cartão do detalhe com Agendamento 23, Unidade Demo A, 19/10/2026 às 08:00 e estado Agendado. Após orientação de Voltar pela seta, a usuária relatou retorno à lista mantendo o cartão de 23, como esperado. Percurso lista → detalhe → lista concluído nesse recorte. O retorno foi relatado, sem captura adicional; os dados do detalhe têm evidência visual, sem inspeção de JWT/tráfego. Preservar a reserva 23.

## Listagem de agendamentos no Android — 13/10/2026

- A usuária implementou `AppointmentsUiState`, `AppointmentsViewModel`, fábrica no AppContainer e ligação por DoeSangueApp/AppNavHost/MainNavGraph à tela. A consulta inicia no ViewModel, impede consultas simultâneas, permite nova tentativa e verifica cancelamento antes de publicar o resultado. A tela representa carregamento, erro, vazio e cartões com unidade, ID, data/hora no fuso da unidade e estado SCHEDULED/CANCELLED. Não infere coleta ou comparecimento. O callback antigo de detalhes foi retirado desta listagem; detalhes ainda precisam receber a identidade da reserva.
- O agente adaptou os sete roteiros instrumentados que chamam AppNavHost ao novo argumento de fábrica. `FakeAppointmentRepository` retorna lista vazia, sem rede; o repository de `AppointmentCreationFlowTest` permanece restrito à criação. Novo roteiro de leitura usa repository controlado e dados sintéticos.
- `compileDebugKotlin compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL. `connectedDebugAndroidTest` focado em `AppointmentListFlowTest`, no Pixel_6/Android 12: **seis aprovados, zero falhas/erros/ignorados**, conferidos no XML. Cobre consulta pendente/duplicidade, erro genérico/nova tentativa, vazio sem cartão do protótipo, fuso com mudança do dia e reserva passada/cancelada, cancelamento ao sair/consulta nova ao retornar e descarte da lista de A após logout/login como B.
- Na suíte `AppNavigationTest`, nove testes passaram e um falhou ao procurar o botão antigo “Ver detalhes”, removido da listagem. O roteiro foi adaptado à entrada visual “Meu agendamento” pelo Perfil, mantendo a verificação do histórico após cancelamento. Repetição somente de `prototypeCancellationReturnsToProfileWithoutReopeningOldDetail`: um aprovado, zero falhas/erros/ignorados, conferido no XML; BUILD SUCCESSFUL. São nove aprovações da primeira execução e uma da repetição focada; a suíte completa não foi repetida após o ajuste do teste. A produção não foi alterada para corrigir esse teste obsoleto. `lint` concluiu sem erros, com 31 avisos no relatório SARIF.
- Esses testes automatizados exercitam o aplicativo com repositories fictícios. Na verificação manual seguinte, orientada para a conta sintética A, captura enviada pela usuária confirma a listagem no emulador com Agendamento 23, Unidade Demo A, 19/10/2026 às 08:00 e estado Agendado. Após orientação de logout de A e login como B, uma segunda captura confirma “Você ainda não possui agendamentos.”, sem a reserva 23. Após logout de B e retorno para A, terceira captura confirma a reserva 23 novamente com os mesmos dados. Roteiro A → B → A concluído nesse recorte de leitura. São evidências visuais do roteiro com sessões reais no aplicativo; não incluem inspeção de token ou tráfego HTTP nem comprovam todas as combinações de acesso à API. Nenhuma migração foi reaplicada nem registro remoto alterado pelo agente.

## Consulta dos próprios agendamentos — etapa iniciada em 12/10/2026

- A usuária implementou AppointmentSummary, contrato getMyAppointments, DTO/mapper, DataSource RPC e método do repository em pequenos passos. O agente adaptou somente os dois repositories fictícios ao contrato; suas leituras ainda retornam falha explícita de consulta não configurada. `compileDebugKotlin compileDebugAndroidTestKotlin` terminou com BUILD SUCCESSFUL; não executou a RPC nem testes funcionais de listagem.
- A usuária concluiu a migração `20261012000200_consultar_meus_agendamentos.sql` e relatou resultado esperado após aplicação manual no desenvolvimento. Não reaplicar. Revisão local conferiu STABLE, SECURITY DEFINER, search_path vazio, rejeição de identidade ausente com 42501, filtro obrigatório por auth.uid(), JOINs e projeção compatível com o DTO; EXECUTE revogado de public/anon/authenticated e concedido somente a authenticated.
- Captura de `supabase/tests/consultar_meus_agendamentos_configuracao.sql` confirma anon sem EXECUTE e authenticated com EXECUTE; SECURITY DEFINER=true, volatilidade=s (STABLE), search_path vazio em ambas as linhas. São metadados, não tentativas de acesso.
- Captura de `consultar_meus_agendamentos_leitura_isolada.sql` confirma A vê somente A, B vê somente B, reserva/unidade/início/fuso conferidos e reserva 23 preservada. O roteiro verifica toda a resposta contra a identidade, objetos completos das fixtures canceladas e visibilidade de 23 somente para A; termina em ROLLBACK, incluindo o perfil B se criado. É simulação administrativa, não validação de JWT/login pela API.
- Captura de `consultar_meus_agendamentos_acesso_vazio.sql` confirma lista vazia para B sem reservas, authenticated sem identidade bloqueado por 42501, execução como anon bloqueada e reserva 23 preservada. O script verifica a mensagem específica da validação de identidade e termina em ROLLBACK, sem alterar dados do domínio. Identidades são simuladas administrativamente.
- Captura de `consultar_meus_agendamentos_oferta_oculta.sql` confirma os seis resultados: reserva própria visível com horário inativo ou unidade não publicada; reserva passada/cancelada visível com estado preservado; ofertas ocultas continuam ocultas diretamente; B não recebe reservas de A; reserva 23 preservada. As fixtures representam situações existentes por preparação administrativa, não pedidos de criação aceitos nessas condições. O roteiro confere a resposta completa contra os dados preparados e termina em ROLLBACK; nenhuma oferta existente é alterada. É simulação administrativa, sem JWT pela API.
- Captura de `consultar_meus_agendamentos_conferencia_final.sql` confirma a massa após os roteiros: duas unidades, três horários, um perfil e um agendamento (sete registros do domínio), zero unidades temporárias do roteiro, zero outras reservas no horário de 23 e estado de 23 `agendado`. Limpeza/preservação conferidas por leitura administrativa; sequências podem ter avançado. Etapa SQL de leitura concluída nesse recorte. Integração de ViewModel/tela e isolamento pela API continuam pendentes.
- Teste local focado `testDebugUnitTest --tests=br.edu.fatec.doesangue.data.supabase.mapper.AppointmentSummaryMapperTest`: BUILD SUCCESSFUL, três aprovados, zero falhas/erros/ignorados, conferidos no XML. Verifica decodificação da lista JSON aninhada, IDs acima de 32 bits, precisão e offset dos instantes, preservação de CANCELLED e rejeição de estado desconhecido. Sem rede; não comprova a resposta HTTP real. `git diff --check` sem erros. Retomada em [checkpoint de leitura](checkpoints/2026-10-12-consulta-agendamentos.md).
- Checagem final antes do commit autorizado: `lint` terminou com BUILD SUCCESSFUL, 31 avisos e zero erros. Revisão estática independente do recorte de leitura não encontrou bloqueios em identidade/permissões, DTO/mapper ou preservação de cancelamento; sem repetir testes remotos ou ampliar a suíte instrumentada.

## Confirmação de agendamento no Android — 12/10/2026

- A usuária concluiu a ligação da chamada RPC ao repository/ViewModel, revisão e navegação para sucesso. Aplicativo e APK de testes compilaram. O agente acrescentou `FakeAppointmentRepository` e sua injeção nos seis roteiros existentes para que confirmar nos testes não use o backend real.
- `connectedDebugAndroidTest` focado em `AppNavigationTest`, no Pixel_6/Android 12: **dez aprovados**, zero falhas/ignorados conforme console, BUILD SUCCESSFUL. Inclui confirmação e saída para Home com histórico limpo, agora usando a resposta do repository fictício.
- Novo `AppointmentCreationFlowTest`: **seis aprovados, zero falhas/erros/ignorados**, conferidos no XML da execução final, BUILD SUCCESSFUL no mesmo emulador. Cobre dados escolhidos/ID retornado e novo fluxo com outra chave; espera com Voltar, duplicidade e alterações bloqueados; perda de resposta depois de persistência simulada e recuperação do mesmo registro/chave; troca de horário após erro com outra chave; retorno cancelado sem sucesso/nova chamada; rascunho incompleto sem chamada ao repository.
- A primeira execução dos seis cenários falhou no helper do próprio teste: `performScrollTo` foi aplicado ao botão de boas-vindas, fora de área rolável. Corrigido somente o roteiro para rolar os controles apropriados; a execução final acima passou. Não houve alteração de produção pelo agente nesta validação.
- `git diff --check` sem erros. Na checagem final antes do commit, `lint` terminou com BUILD SUCCESSFUL: 31 avisos e zero erros. Testes locais de unidade e suíte instrumentada completa não foram repetidos nesta rodada; a validação instrumentada foi focada nos fluxos alterados.
- **Limites dos testes automatizados:** todos os repositories desses testes são fictícios, sem chamadas remotas. A recuperação simulada demonstra a conservação da chave no ViewModel, não a implementação de idempotência do servidor. Isolamento A/B pela API, perda real de rede e recuperação após morte do processo continuam pendentes. Os testes SQL históricos abaixo são evidências separadas; migrações não foram reaplicadas nem reservas criadas remotamente pelo agente.
- **Confirmação manual pelo aplicativo:** a usuária informou o ID 23 após o percurso com a conta sintética A. Captura da consulta administrativa com JOIN confirma uma linha persistida: ID 23, Doador Demo A, Unidade Demo A, início local `2026-10-19 08:00:00`, estado `agendado`, criado_em `2026-10-12 23:14:35.586383+00`. Em conjunto com o ID mostrado no aplicativo, é evidência do percurso Android → RPC autenticada → banco → retorno ao aplicativo. A consulta administrativa não comprova isolamento de leitura A/B pela API, recuperação real com a mesma chave nem contagem total da massa. Preservar a reserva sintética 23, sem cancelamento/limpeza automáticos.
- **Segunda intenção para o mesmo doador/horário:** após orientação para iniciar outro fluxo com A e selecionar Demo A em 19/10/2026 às 08:00, captura mostra a revisão com os dados e a mensagem genérica de falha, sem sucesso. Consulta administrativa filtrada pelo doador/horário da reserva 23 mostra somente ID 23, `agendado`, com criado_em original preservado. Evidência de ausência de segunda reserva nesse cenário; a mensagem genérica não identifica o erro SQL retornado. Não confundir nova intenção/chave com recuperação idempotente usando a mesma chave.

## Função de reserva — 12/10/2026

- A usuária relatou execução manual de `20261012000100_criar_funcao_agendamento.sql` no Supabase com “Success. No rows returned”. Não reaplicar. A migração cria a função e suas permissões, sem chamar a operação nem inserir reservas.
- Revisão local conferiu identidade pela sessão, recuperação idempotente, bloqueios, isolamento READ COMMITTED, oferta/início futuro, duplicidade, capacidade, INSERT/retorno e concessão de EXECUTE somente a authenticated. `git diff --check` passou. Não houve execução remota pelo agente.
- Captura da primeira consulta de `supabase/tests/criar_agendamento_configuracao.sql` confirma SECURITY DEFINER=true, volatilidade=v e configuração search_path vazio. A usuária relatou resultado esperado na segunda consulta: anon sem EXECUTE/INSERT direto; authenticated com EXECUTE e sem INSERT direto. São metadados de privilégios, não tentativas de escrita direta.
- Captura da execução de `supabase/tests/criar_agendamento_criacao_idempotencia.sql` confirma os três resultados esperados: reserva criada corretamente, repetição retorna o mesmo registro e somente uma reserva. O roteiro usa horário temporário futuro com capacidade 1, papel authenticated/identidade A simulada, verifica retorno completo/defaults e repete a mesma chave com a vaga ocupada. Confere contagem também como administrador e termina em ROLLBACK; sequências podem avançar. É teste sequencial, sem comprovar concorrência ou JWT pela API.
- Captura de `supabase/tests/criar_agendamento_pedidos_duplicados.sql` confirma chave reutilizada para outro horário bloqueada, reserva ativa duplicada bloqueada e primeira reserva preservada. Duas ofertas temporárias futuras com capacidade 3 isolam duplicidade de falta de vagas. O script exige mensagens específicas, preservação do registro original e contagem administrativa de uma reserva; usa identidade A simulada e ROLLBACK.
- Captura de `supabase/tests/criar_agendamento_disponibilidade.sql` confirma os quatro bloqueios: horário inativo, unidade não publicada, início passado e capacidade 1 ocupada por B (bloqueado para A). B reserva pela função; o roteiro exige que A não veja a reserva de B pela RLS e receba falta de vagas. Confere como administrador que somente a reserva de B existe. Perfil B é criado somente se ausente, sem alterar o existente; ROLLBACK desfaz os dados do teste. Teste sequencial com identidades simuladas, sem comprovar concorrência ou JWT pela API.
- Captura de `supabase/tests/criar_agendamento_cancelados.sql` confirma os quatro resultados: chave antiga retorna o cancelado mesmo com oferta fechada/passada; nova chave cria outra reserva ativa; antiga permanece cancelada; capacidade 1 tem somente uma reserva ativa. O roteiro cria pela função e cancela administrativamente somente o registro temporário, confere um ativo/um cancelado e termina em ROLLBACK. Não implementa cancelamento no aplicativo.
- Captura de `supabase/tests/criar_agendamento_validacao_acesso.sql` confirma oito grupos bloqueados e dados preservados: horário nulo/zero/negativo e chave nula; horário inexistente; identidade ausente; B sem perfil; INSERT/UPDATE/DELETE diretos como A; EXECUTE como anon. O roteiro exige mensagens específicas das validações e insufficient_privilege nas tentativas diretas, sem remover perfis existentes. Usa papéis/identidades simulados e ROLLBACK, sem verificar JWT pela API; não testa TRUNCATE neste roteiro.
- Captura de `supabase/tests/criar_agendamento_isolamento.sql` confirma REPEATABLE READ e SERIALIZABLE bloqueados. Em duas transações independentes, o roteiro chama como authenticated/identidade A simulada e exige a mensagem de READ COMMITTED. Ambas terminam em ROLLBACK; não prepara novos registros. Não comprova coordenação sob concorrência.
- Captura da preparação da concorrência confirma oferta ID 13, início 2026-10-19 15:44:43.092005+00 e capacidade 1. `supabase/tests/concorrencia/preparar.sql` usa COMMIT para compartilhar a oferta entre as sessões; não repetir. Essa oferta adicional foi removida ao final, conforme evidência de limpeza abaixo.
- Capturas de `sessao_a.sql`/`sessao_b.sql` confirmam o cenário simultâneo: A retorna reserva ID 22 com b_aguardou_bloqueio_de_a=true; B retorna falta de vagas e somente uma reserva ativa, de A. A exige observar outro backend B bloqueado por si via pg_stat_activity/pg_blocking_pids antes de COMMIT, com espera limitada a 35 segundos. B exige prontidão de A, negação específica de reserva e contagem administrativa após o COMMIT de A; termina com ROLLBACK (incluindo perfil B, se criado). Evidência manual de duas sessões disputando uma vaga; não prova de todas as combinações ou JWT pela API.
- Captura da execução de `limpar.sql` confirma oferta_removida=true e reservas_do_teste_removidas=true. O roteiro confere oferta 13 por início/unidade/capacidade e reserva ativa de A, ID 22; exclui especificamente essas linhas e usa COMMIT. Oferta e reserva extras foram removidas. Não foi repetida contagem global da massa. [Checkpoint para a nova conversa](checkpoints/2026-10-12-reserva-supabase.md) registra estado, evidências, limites e próximo passo.

## Estrutura e permissões de agendamentos — 11/10/2026

- A usuária criou AppointmentStatus, Appointment, AppointmentRepository e a migração de agendamento em pequenos passos. `compileDebugKotlin` passou; diff --check sem erros. Não houve alteração do fluxo de confirmação nem gravação de reservas pelo aplicativo.
- Aplicação manual da migração no desenvolvimento relatada sem erros. Captura de pg_tables confirma public.agendamento com rowsecurity=true.
- Captura de has_table_privilege confirma anon sem SELECT/INSERT/UPDATE/DELETE/TRUNCATE; authenticated possui somente SELECT entre esses privilégios.
- Captura de pg_policies mostra uma única policy de SELECT para authenticated, condicionada a doador_id = (SELECT auth.uid()), sem WITH CHECK. É evidência da definição, ainda não do isolamento com registros.
- Captura de has_sequence_privilege confirma ausência de USAGE/SELECT/UPDATE na sequência public.agendamento_id_seq para anon e authenticated.
- Captura de pg_constraint mostra cinco restrições: PK em id, FKs para perfil/horário, CHECK de estado agendado/cancelado e UNIQUE de doador/chave de idempotência. Captura de pg_indexes mostra os quatro índices esperados; consulta compacta a pg_index confirma filtro estado='agendado' nos dois índices parciais, com unicidade somente em agendamento_ativo_doador_horario.
- Testes administrativos manuais: captura de INSERT RETURNING mostra ID, estado padrão agendado e criado_em gerados pelo banco. Após ROLLBACK, a usuária confirmou COUNT(*)=0. Relatou resultado esperado no teste que rejeita estado inválido pelo CHECK nomeado e no teste que rejeita chave de idempotência repetida para o mesmo doador (duas linhas canceladas, isolando a constraint do índice parcial).
- A usuária confirmou resultado exatamente esperado de `supabase/tests/agendamento_reserva_duplicada.sql`: duplicidade ativa bloqueada pelo índice nomeado, com novas chaves, e registro cancelado aceito ao lado do ativo. O script termina em ROLLBACK. É teste administrativo, sem comprovar capacidade ou identidade via API.
- A usuária relatou execução com resultado esperado de `supabase/tests/agendamento_chaves_estrangeiras.sql`: doador inexistente/horário válido e doador válido/horário inexistente rejeitados pelas respectivas FKs nomeadas. Usa registros cancelados e novas chaves para isolar as FKs e termina em ROLLBACK.
- `supabase/tests/agendamento_leitura_isolada.sql` prepara dois agendamentos cancelados temporários e perfil B apenas se ausente; preserva perfis existentes. Troca o papel para authenticated e simula cada identidade, verificando auth.uid() e leitura apenas própria entre os dois IDs. Como anon, exige negação de SELECT. Usa configurações locais e ROLLBACK; é simulação administrativa, sem login ou verificação de JWT pela API. Referências: [RLS no Supabase](https://supabase.com/docs/guides/database/postgres/row-level-security) e [SET ROLE no PostgreSQL](https://www.postgresql.org/docs/current/sql-set-role.html).
- Primeira execução da leitura isolada falhou com 42704 na compilação do bloco anon: o agente havia escrito insufficient_privileges, condição inexistente. Corrigido para insufficient_privilege (42501), conforme o catálogo oficial do PostgreSQL. Após a orientação de ROLLBACK de eventual transação abortada e repetição do arquivo completo, a captura fornecida pela usuária mostra os três resultados esperados: A vê somente A; B vê somente B; visitante com consulta bloqueada. É evidência manual de isolamento por RLS e bloqueio de SELECT para anon neste roteiro SQL.
- Pendente: implementar e testar operação de reserva com idempotência/capacidade/concorrência e integrar Android; validar autenticação/isolamento A/B pela API. As escritas diretas foram verificadas por metadados de privilégios, não por tentativas como authenticated neste teste de leitura. Nenhuma consulta ou escrita remota foi executada pelo agente; a evidência remota desta etapa é o relato e as capturas fornecidas pela usuária. Não foram repetidos testes instrumentados ou Lint nesta etapa.

## Consulta da sessão na abertura — 10/10/2026

- A usuária implementou restoreSession no contrato, DataSource e repository em pequenos passos. O DataSource aguarda a inicialização do SDK e consulta SessionStatus: Authenticated retorna o ID do usuário, NotAuthenticated retorna null, outros estados geram falha. O repository preserva CancellationException e representa as demais exceções em Result.failure. Não há armazenamento manual de senha ou tokens neste código.
- SessionUiState distingue Loading/Authenticated/Unauthenticated/Error. SessionViewModel inicia a consulta, impede consultas simultâneas e permite nova tentativa após erro. A fábrica usa o mesmo AuthRepository do login/logout; a instância do ViewModel pertence à entrada Splash no grafo.
- SplashScreen recebe estado e callbacks; mostra carregamento ou mensagem genérica com nova tentativa. Somente após resultado de autenticação/ausência de sessão e o atraso visual de 1,8 segundo comunica a conclusão. AppNavHost escolhe Main ou Auth, remove Splash do histórico e usa launchSingleTop. As implementações fictícias e cinco chamadas dos testes foram adaptadas ao contrato/fábrica.
- Criado SessionRestoreFlowTest: cinco cenários com grafo, tela e ViewModel reais e repositories fictícios. Cobrem sessão encontrada abrindo Home e logout posterior; ausência de sessão abrindo Welcome; consulta pendente sem navegação prematura e sem chamadas simultâneas; erro genérico com botão visível e nova tentativa bem-sucedida; tentativa pendente ocultando botão, segunda falha permitindo nova consulta e resultado sem sessão. Verificam remoção da Splash do histórico. Respostas pendentes usam CompletableDeferred, sem rede.
- A primeira compilação do novo arquivo de testes encontrou dois imports indevidos do agente para métodos que já pertencem à interação semântica; corrigidos. A produção compilou nessa execução. Depois, `connectedDebugAndroidTest` focado em SessionRestoreFlowTest no Pixel_6/Android 12 concluiu com BUILD SUCCESSFUL: **cinco aprovados, zero falhas/erros/ignorados**, conferidos no XML. Aplicativo e APK de testes compilaram.
- Como a Splash é comum aos percursos existentes, repetido `connectedDebugAndroidTest` sem filtro no mesmo emulador: BUILD SUCCESSFUL, **32 aprovados, zero falhas/erros e um teste remoto ignorado**. XML confirma 33 casos totais; o console informou 34. Distribuição: dez de navegação, quatro de orientações, três de login, cinco de perfil, cinco de logout e cinco de restauração; SupabaseTimeSlotsReadTest permanece desativado por padrão. Diff --check sem erros.
- Arquivos alterados pelo agente nesta validação: SessionRestoreFlowTest.kt, este documento e AGENTS.md. O código de produção e as adaptações dos testes existentes foram implementados pela usuária em pequenos passos.
- Validação manual posterior: em 10/10/2026, a usuária confirmou que o roteiro com Supabase real ocorreu como previsto e sem erros: conta sintética A conectada, parar e executar novamente abriu Home com Doador Demo A no Perfil; após Sair da conta, parar e executar novamente abriu boas-vindas. É evidência manual da restauração na reabertura e da ausência de sessão após logout; o agente não inspecionou tokens ou armazenamento do dispositivo.
- Limites: testes em memória não verificam armazenamento/renovação de tokens, expiração, revogação ou recriação do processo. A nova consulta é realizada na abertura; ainda não há um observador global de mudanças da sessão durante o uso. Nenhuma consulta ou escrita remota foi executada pelo agente nesta validação. Lint não foi repetido. Esta atualização documental não exige repetir os testes.

## Logout: correção e nova validação — 10/10/2026

- A usuária acrescentou enabled a SettingsRow e condicionou os seis itens de Configurações a canNavigate. Enquanto o logout está pendente ou já concluído, a seta fica ausente, a barra inferior não encaminha navegação e BackHandler intercepta o Voltar do Android. Falha libera novamente os controles. A navegação após sucesso continua no grafo, sem NavController no ViewModel.
- O teste de regressão foi ampliado: verifica Sobre o app desabilitado, tenta clicar nele e nas abas Início/Agendar, confirma ausência da seta e pressiona Voltar. A tela deve continuar em Configurações até a resposta; após sucesso deve estar em Welcome com o histórico de Main removido.
- Repetido `.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=br.edu.fatec.doesangue.ui.navigation.LogoutFlowTest'` no Pixel_6/Android 12: BUILD SUCCESSFUL, **cinco aprovados, zero falhas/erros/ignorados**, conferidos no XML. Aplicativo e APK de testes compilaram; diff --check sem erros. A falha registrada na execução inicial abaixo foi resolvida neste cenário.
- Arquivos alterados pelo agente nesta validação: LogoutFlowTest.kt, este documento e AGENTS.md. As correções de produção em ProfileScreens.kt foram implementadas pela usuária em pequenos passos.
- Validação manual posterior: em 10/10/2026, a usuária confirmou que o roteiro no aplicativo com Supabase real ocorreu sem erros: login como conta sintética A, Perfil → Configurações → Sair da conta, retorno a boas-vindas, novo login como A e perfil Doador Demo A preservado. É evidência manual do comportamento de logout e nova autenticação no aplicativo; não houve inspeção independente de tokens pelo agente.
- Limites: os testes automatizados usam repositories fictícios, sem rede. Troca de contas pela API permanece pendente de validação manual. Restauração da sessão ao abrir o aplicativo ainda não está ligada à navegação. Demais testes instrumentados e Lint não foram repetidos nesta execução focada; nenhuma escrita remota foi executada pelo agente. Esta atualização documental não exige repetir os testes.

## Logout: validação inicial — 10/10/2026

- A usuária conectou AuthRepository, DataSource/repository Supabase, LogoutUiState/ViewModel, fábrica e Configurações. O DataSource usa SignOutScope.LOCAL; sucesso solicita a entrada no grafo Auth e remoção de Main. Ainda não há validação manual do logout remoto.
- Criado LogoutFlowTest com cinco cenários no grafo real e repositories sintéticos, sem rede. A primeira execução falhou no auxiliar de interação do teste, que tentava rolar a tela de boas-vindas sem rolagem; corrigido para clicar diretamente nesse botão.
- Após a correção do teste, `connectedDebugAndroidTest` focado em LogoutFlowTest no Pixel_6/Android 12 compilou aplicativo e APK de testes: **quatro aprovados e uma falha**, sem testes ignorados. Passaram saída com limpeza do histórico e novo login/logout, falha com nova tentativa e mensagem genérica, bloqueio de solicitações repetidas durante resposta pendente e troca A/B sem exibir o perfil anterior.
- Falha pendente: iniciar logout e abrir Sobre o app antes da resposta encerra a sessão fictícia, mas não retorna a Welcome. O observador de sucesso está na composição de Configurações, que deixa de acompanhar o estado ao navegar. Corrigir a navegação durante a operação e repetir a validação antes de concluir a etapa.
- Limites: testes em memória não comprovam encerramento remoto da sessão, RLS ou isolamento pela API. Demais classes instrumentadas e Lint não foram repetidos nesta execução focada. Nenhuma alteração de produção foi feita pelo agente para corrigir a falha nesta etapa; a correção será acompanhada pela usuária.

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
