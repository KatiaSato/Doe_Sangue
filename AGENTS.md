# Orientações para agentes — Doe Sangue

## Objetivo e estado

Projeto acadêmico Android do aplicativo Doe Sangue. A base Compose e a navegação existem; unidades e horários sintéticos do agendamento são consultados no Supabase. Unidade, data e horário são mantidos no rascunho em StateFlow e exibidos na revisão. Login por e-mail/senha e criação/leitura do perfil estão integrados ao Supabase; cadastro, recuperação de senha e provedores sociais continuam visuais. Recorrência ainda é visual; a confirmação está ligada à função de reserva e a primeira gravação sintética pelo aplicativo foi confirmada. Explique decisões em português e mantenha a solução compreensível para apresentação acadêmica. A usuária implementa acompanhando pequenos passos; não entregar funcionalidades inteiras prontas sem solicitação. Ver `docs/14-integracao-unidades.md` e `docs/15-disponibilidade-agendamento.md`.

## Modo de tutoria — obrigatório ao retomar

- **A usuária escreve o código de produção. O agente orienta e revisa.** Não criar, preencher ou editar arquivos de produção automaticamente, mesmo que a mudança seja pequena ou envolva apenas um DTO, mapper ou DataSource.
- Mostrar um único passo curto por resposta: indicar o arquivo/local, fornecer o trecho necessário e explicar sua função. Aguardar a usuária implementar e responder antes de conferir e apresentar o próximo passo.
- Orientação da usuária em 13/10/2026: antes de iniciar cada nova etapa, apresentar um diagrama de sequência para explicar quem conversa com quem, quais chamadas são feitas e como as respostas retornam. Relacionar os participantes aos arquivos e às camadas do projeto. Distinguir o fluxo já implementado do fluxo proposto para a etapa; salvar o diagrama na documentação e manter a tutoria em pequenos passos depois dessa visão inicial. A próxima etapa, detalhes da reserva pelo ID, também deve começar pelo diagrama.
- “Vamos”, “continuar”, “prosseguir”, “feito” e pedidos de checkpoint mantêm esse modo; não autorizam o agente a implementar produção. Só alterar produção diretamente quando a usuária pedir explicitamente que o agente faça aquela alteração.
- Documentação, roteiros de teste, verificações e operações Git autorizadas podem ser executados pelo agente. Isso não autoriza avançar automaticamente a implementação do aplicativo.
- Preservar arquivos gerados por outras conversas. Antes de apagar/reverter ou decidir que esses arquivos substituem as etapas de aprendizado, combinar a retomada com a usuária.

## Stack e estrutura

- Kotlin, Android nativo, Jetpack Compose.
- Arquitetura planejada: MVVM com Repository, StateFlow e corrotinas.
- Backend: PostgreSQL/PostgREST integrado a unidades, horários e perfil; Supabase Auth integrado ao login por e-mail/senha.
- Tema acadêmico aceito pelo professor: Clínica/Hospital — atendimento humano. Supabase foi sugerido por ele; ambiente de desenvolvimento acadêmico confirmado pela usuária, somente dados sintéticos e sem reservas em hemocentros reais.
- Autenticação planejada: e-mail/senha e Google. Para e-mail/senha, mínimo de 8 caracteres com maiúscula, minúscula, número e símbolo, aplicado no Auth e explicado na UI.
- `domain/`: modelos, contratos e casos de uso sem dependência da UI.
- `data/`: fontes de dados e implementações dos repositories; demo e Supabase devem permanecer separados.
- `presentation/`: ViewModels e estados imutáveis, sem componentes Compose.
- `ui/`: componentes e telas Compose; `di/`: composição manual inicial.
- `docs/`: documentação; `supabase/`: migração de `unidade_coleta` e seed sintético versionados.

## Comandos reais

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

## Regras de trabalho e segurança

- Leia este arquivo e os documentos relacionados antes de escrever.
- Preserve a separação entre Confirmado, Proposto, Implementado e Pendente.
- Não invente regras médicas, critérios de aptidão ou intervalos de doação.
- Agendamento não comprova comparecimento, coleta, aptidão ou alteração de estoque.
- Use somente dados sintéticos; não registre credenciais, tokens ou dados pessoais em logs.
- Entrega acadêmica final: pelo menos 6 tabelas relacionadas, 5 FKs e 200 registros sintéticos distribuídos; distinguir seed do domínio de contas aptas a fazer login. Ver `docs/09-banco-de-dados.md`.
- Supabase Auth guarda credenciais; nunca criar coluna de senha no domínio.
- Nunca incluir `service_role`, senha do banco ou segredo no Android.
- RLS deve impedir acesso privado de terceiros, elevação de privilégios, alteração de estoque e confirmação de coleta pelo doador.
- Não aplicar migrações remotas, publicar, fazer deploy ou alterar produção sem autorização explícita.
- Não modificar o Figma sem solicitação explícita.

## Fontes e pendências

- Entrega confirmada pela usuária para a última quarta-feira de novembro de 2026: 25/11/2026. Apresentação focada em banco de dados, na disciplina de persistência. Priorizar modelagem, integridade, permissões, operações persistidas e requisitos acadêmicos; continuar o ensino em pequenos passos.
- Briefing local fornecido em 30/08/2026.
- Figma: `Doe Sangue — Android App — Education`, nó `9:8`; requer inventário antes das telas.
- Enunciado acadêmico recebido: `Aula 07 - LBD - M - Projeto Integrador Especificação e Critérios - 16.09.pdf` (fornecido pela usuária, fora do repositório).
- Pendentes: recorrência, cardinalidades finais, matriz de permissões completa, distribuição do seed, conta do professor e confirmação do `applicationId`.

## Atualização de navegação — 23/09/2026

- Navigation Compose 2.9.8 e rotas tipadas estão implementados; `AppNavHost` conecta Auth/Main/Scheduling. O roteador próprio foi substituído.
- A seta das raízes usa `navigateBackOrHome`; decisões de navegação permanecem em `ui/navigation`, sem NavController nos ViewModels.
- Testes instrumentados: `.\gradlew.bat connectedDebugAndroidTest`, com emulador/dispositivo conectado. Ver `docs/12-validacao.md` e ADR-003.
- Trocar de aba ainda remove o percurso anterior. Rascunho completo, argumentos de registros, deep links e autenticação real continuam pendentes.

## Integração de unidades — 06/10/2026

- `SchedulingViewModel` depende de `DonationCenterRepository`; `AppContainer` injeta a implementação Supabase por padrão e aceita substituição explícita nos testes.
- DTO e mapper ficam em `data/supabase`. O repository preserva `CancellationException` e retorna outras exceções em `Result.failure`.
- `local.properties` fornece URL/chave publicável ao `BuildConfig`; não exibir valores nem versionar esse arquivo. Chave publicável pode estar no APK; nenhum segredo administrativo pode estar nele.
- RLS/grants da tabela permitem aos papéis `anon` e `authenticated` somente leitura das unidades publicadas. SQL foi aplicado manualmente pela usuária; histórico da CLI ainda não foi conciliado. Não reaplicar automaticamente a migração.
- Em 06/10/2026: compilação, teste local, Lint e 12 testes instrumentados passaram; Lint tem 31 avisos e zero erros. A consulta real no celular foi confirmada pela usuária em 03/10/2026.
- Data e horário já preenchem o rascunho; a revisão passou a exibir as escolhas em 07/10/2026 e exige os três campos para habilitar Confirmar. Consultar resultados atuais em `docs/12-validacao.md`. Próximos passos: modelar disponibilidade sintética, depois autenticação e persistência. Agendamento ainda não é gravado.

## Disponibilidade por unidade — decisão de 06/10/2026

- Permitir agendamento somente nas datas/horários disponibilizados para a unidade escolhida. Não bloquear sábados, domingos ou feriados universalmente; representar aberturas especiais e fechamentos na programação.
- `horario_atendimento` é texto descritivo, não fonte estruturada de disponibilidade. Modelar horários ligados à unidade antes de implementar a regra no calendário.
- A futura gravação deve revalidar disponibilidade/capacidade no servidor, com controle de concorrência; o estado da tela não garante reserva.
- Em 07/10/2026, a usuária escolheu um registro por horário oferecido, com capacidade configurável para vários agendamentos. Capacidade total não equivale a vagas restantes; o servidor deverá calcular e revalidar a disponibilidade.
- Em 07/10/2026, a usuária aplicou manualmente a migração de `fuso_horario` e mostrou Demo A/B com `America/Sao_Paulo` e publicação preservada. DTO, mapper e domínio já transportam `timeZoneId`; aplicativo e APK de testes compilaram, e a usuária confirmou a consulta real sem erro. O caso de uso já converte o dia para instantes no fuso da unidade, e a tela converte os instantes recebidos para horários locais. Não reaplicar a migração. Ver `supabase/README.md` para ordem e evidências.
- Programação de segunda a sexta, sábado pela manhã e domingo excepcional é apenas exemplo sintético proposto. Esquema, permissões de leitura e seed mínimo de horários já existem; ampliação da programação, limites operacionais da capacidade e antecedência permanecem pendentes.
- A tabela `horario_disponivel` foi criada manualmente pela usuária em 07/10/2026; captura de `pg_tables` confirma RLS ativo. O seed de horários foi aplicado: três ofertas sintéticas. Consultas com JOIN sob `anon` e `authenticated` retornaram somente Demo A às 08:00; `has_table_privilege` confirmou SELECT e ausência de INSERT/UPDATE/DELETE para ambos. Ver limites da evidência em `docs/12-validacao.md`. Não reaplicar a migração. Nenhuma tabela de reservas foi criada. Ler `docs/15-disponibilidade-agendamento.md` antes de continuar.

## Consulta de horários — evolução de 07/10/2026

- DTO, mapper, DataSource, repository e caso de uso de horários estão ligados ao SchedulingViewModel via AppContainer. Ao mudar a data, consulta o intervalo no fuso da unidade; ao mudar a unidade, cancela e limpa o rascunho. A tela mostra carregamento, erro com nova tentativa, vazio ou horários retornados; o ViewModel valida a escolha contra unidade/data/lista.
- Supabase Kotlin 3.6.0: agrupar gte/lt da coluna inicio dentro de and no DataSource. O envio de parâmetros dessa versão usa somente o primeiro valor por chave quando as condições ficam diretamente no filter; isso descartava o limite superior. Não remover o agrupamento sem um teste do pedido HTTP ou de integração que cubra dias adjacentes.
- AppNavigationTest e DonationRestrictionsTest injetam repositories fictícios de unidades e horários. SupabaseTimeSlotsReadTest é uma exceção explícita, somente leitura, ativada por enableSupabaseReadTests=true contra o seed de desenvolvimento. Resultados em docs/12-validacao.md.
- Calendário ainda permite escolher qualquer data para consultar; não há bloqueio antecipado de dias vazios. Capacidade restante, persistência de reservas e autenticação continuam pendentes.

## Identidade do horário no rascunho — 08/10/2026

- A tela comunica o ID do horário ao ViewModel por `onSelectTimeSlot`. `selectTimeSlot` valida o registro na lista da unidade/data atuais e guarda `selectedTimeSlotId` junto de `selectedTime` para exibição.
- Mudar de unidade/data ou recarregar horários limpa o ID e a hora. Continuar e Confirmar exigem o ID preenchido. A usuária confirmou o percurso manual de seleção, troca de data, retorno sem seleção e revisão; resultados automatizados em `docs/12-validacao.md`.
- O ID identifica a oferta para a futura persistência; não constitui reserva nem garante capacidade disponível. Confirmar ainda é navegação visual.

## Perfil do doador — 08/10/2026

- A usuária aplicou manualmente `20261008000100_criar_perfil_doador.sql`. Não reaplicar. Perfil mínimo com UUID PK/FK para auth.users, nome de exibição e criado_em; sem senha ou criação automática após cadastro.
- Capturas confirmam RLS ativo, policies de SELECT/INSERT/UPDATE para acesso próprio, privilégios por coluna e DELETE negado aos papéis anon/authenticated. Testes SQL manuais confirmaram leitura isolada A/B e criação própria como A; edição própria preservando B, inserção em nome de B bloqueada e consulta anon bloqueada tiveram resultado esperado conforme relato. Ver `docs/12-validacao.md` para limites.
- Duas contas sintéticas existem no Auth, criadas pelo painel; perfis dos testes foram usados em transações com ROLLBACK. Não versionar credenciais nem confundir simulação administrativa da identidade com login real. Próximo passo didático: autenticação Android e ligação do perfil, em pequenos passos. Constraints, seed persistente de perfis e gravação de agendamento permanecem pendentes.

## Login e persistência do perfil — 10/10/2026

- AuthRepository/LoginViewModel estão ligados ao login; a navegação abre Main somente após sucesso. A senha não fica no estado do ViewModel nem em logs. O cliente Supabase é compartilhado entre Auth e PostgREST; factories e repositories aceitam substituições nos testes.
- Perfil mínimo: DonorProfile, DTO/mapper, DataSource e repository; ProfileViewModel representa Loading/Loaded/Missing/Error. O formulário Missing cria o próprio perfil, validando nome vazio, preservando o texto em falhas e bloqueando edição/envios repetidos/consulta concorrente durante a gravação. O ID vem da conta autenticada; criado_em vem do banco. RLS continua sendo a proteção no servidor.
- A usuária confirmou login real e criação/leitura com a conta sintética A. Captura da consulta com JOIN para auth.users confirma um perfil persistido com nome Doador Demo A e data preenchida. Massa confirmada: duas unidades, três horários e um perfil (seis registros do domínio); duas contas Auth são separadas dessa contagem.
- No cadastro real, usar o nome já informado para criar o perfil quando houver sessão autenticada. Missing é recuperação de contas sem perfil, não uma segunda etapa obrigatória do cadastro. Cadastro real e confirmação de e-mail ainda precisam ser conectados; sessão/restauração/logout e perda de resposta após INSERT permanecem pendentes.
- Validação atual: cinco testes de perfil e dez de navegação aprovados em 10/10 no Pixel_6/Android 12. Suíte de 09/10: 17 aprovados e um remoto ignorado no Moto g04/Android 14. Testes usam dados fictícios; ver docs/12-validacao.md para evidências e limites. Ainda faltam isolamento entre contas via API, cancelamento, seed ampliado e persistência de agendamentos. Seções anteriores descrevem checkpoints históricos.

## Logout — 10/10/2026

- AuthRepository/DataSource/repository, LogoutUiState/ViewModel e fábrica estão ligados a Configurações. Supabase usa SignOutScope.LOCAL; sucesso abre Auth removendo Main, falha apresenta mensagem genérica e permite nova tentativa. Nenhum NavController foi levado ao ViewModel.
- Durante a saída, SettingsRow não aceita cliques, a seta fica ausente, as abas não encaminham navegação e BackHandler intercepta Voltar. Isso evita abandonar o observador de sucesso em Configurações antes da conclusão.
- LogoutFlowTest no Pixel_6/Android 12: cinco aprovados, zero falhas/erros/ignorados. Usa repositories fictícios e cobre histórico removido, novo login/logout, falha/nova tentativa, duplicidade, troca A/B e navegação durante a resposta pendente. Ver docs/12-validacao.md para a falha inicial e sua correção.
- A usuária confirmou manualmente o roteiro com Supabase real: logout da conta sintética A retorna a boas-vindas; novo login como A mantém Doador Demo A no Perfil. Troca A/B pela API ainda precisa de validação manual. Restauração de sessão ao abrir o aplicativo permanece pendente; não assumir que estas etapas foram concluídas a partir dos testes em memória.

## Consulta da sessão na abertura — 10/10/2026

- AuthRepository.restoreSession retorna Result<AuthenticatedUser?>. DataSource aguarda inicialização do SDK e distingue Authenticated/NotAuthenticated/falha; repository preserva CancellationException. A nova operação não armazena manualmente credenciais ou tokens.
- SessionUiState/SessionViewModel e fábrica estão ligados à entrada Splash. O ViewModel consulta na criação, impede consultas simultâneas e permite nova tentativa após erro. Splash recebe estado/callbacks; só sinaliza conclusão após resultado e atraso visual de 1,8 segundo. AppNavHost escolhe Main ou Auth e remove Splash do histórico. Mensagem de erro e nova tentativa são visíveis na Splash.
- SessionRestoreFlowTest cobre cinco cenários de abertura com repositories fictícios, sem rede. Execução focada: cinco aprovados. Suíte ampliada no Pixel_6/Android 12: 32 aprovados, zero falhas/erros e um remoto ignorado, confirmados no XML. Ver docs/12-validacao.md para evidências e limites.
- A usuária confirmou manualmente o roteiro com Supabase real: reabrir com conta sintética A conectada abriu Home com perfil Doador Demo A; reabrir após logout abriu boas-vindas. O agente não inspecionou armazenamento ou tokens. Testes em memória não verificam renovação, expiração, revogação ou recriação do processo. Não há observador global de mudanças de sessão durante o uso; a integração nova decide a entrada na abertura. Seções anteriores são checkpoints históricos.

## Estrutura inicial de agendamentos — 11/10/2026

- A primeira execução de agendamento_leitura_isolada.sql falhou por erro do agente no nome insufficient_privileges. Corrigido para insufficient_privilege. Após orientação de ROLLBACK de eventual transação abortada e repetição, captura confirma A vê somente A, B vê somente B e consulta anon bloqueada. É simulação administrativa de identidade/papel, sem login nem validação de JWT pela API.
- Testes manuais posteriores: captura confirma INSERT com ID/estado/criado_em automáticos; COUNT(*)=0 após ROLLBACK foi relatado. CHECK de estado inválido, UNIQUE de idempotência com linhas canceladas, reserva ativa duplicada/cancelado coexistente e as duas FKs tiveram resultado esperado conforme relato. Scripts em supabase/tests usam ROLLBACK. Capacidade/concorrência, gravação pelo app e autenticação/isolamento A/B pela API continuam pendentes. Evidências em docs/12-validacao.md.
- A usuária criou AppointmentStatus, Appointment e AppointmentRepository no domínio; ainda sem implementação nem gravação pelo botão Confirmar.
- `20261011000100_criar_agendamento.sql` preparada localmente, com perfil/horário obrigatórios por FK, estados agendado/cancelado, idempotência por doador, unicidade parcial de reserva ativa, índice de contagem e RLS de leitura própria. Escrita direta e acesso à sequência de IDs foram revogados para public/anon/authenticated.
- A usuária relatou aplicação manual sem erros da migração no desenvolvimento. Não reaplicar. Capturas confirmam a quarta tabela, RLS ativo, grants da tabela, definição da policy própria e sequência sem USAGE/SELECT/UPDATE para anon/authenticated. Cinco constraints e quatro índices estão presentes; filtros e unicidade dos índices parciais foram conferidos por captura. Testes manuais de comportamento e isolamento estão descritos acima; compileDebugKotlin passou para os modelos/contrato. Ver docs/09-banco-de-dados.md e docs/12-validacao.md. Capacidade e concorrência exigem operação no servidor ainda pendente; agendamento não comprova coleta.

## Função de reserva — 12/10/2026

- A usuária pediu um checkpoint para iniciar outra conversa depois de concluir esta etapa do banco e realizar os testes no Supabase. Preparar registro com estado local/remoto, evidências, pendências e próximo passo; não tratar a modelagem acadêmica inteira como concluída nem incluir credenciais.
- Checkpoint concluído após testes e limpeza confirmados: `docs/checkpoints/2026-10-12-reserva-supabase.md`. Retomar por ele e pelos arquivos diretamente relacionados, em pequenos passos. Próximo passo: DTO/mapper do agendamento para futura chamada RPC no Android. Commit/push autorizados pela usuária antes da nova conversa; conferir versão e sincronização no Git ao retomar. Não reaplicar migrações nem repetir automaticamente o teste histórico.
- A usuária concluiu `20261012000100_criar_funcao_agendamento.sql`, com aplicação manual relatada no Supabase: Success. No rows returned. O INSERT com RETURN QUERY/RETURNING substitui o bloqueio provisório; após revogar EXECUTE de public/anon/authenticated, o script concede execução somente a authenticated. Função e permissões revisadas e exercitadas nos roteiros abaixo; limpeza confirmada. Não reaplicar a migração.
- VOLATILE está explícito e a função exige isolamento READ COMMITTED antes dos bloqueios. A contagem após aguardar o horário precisa observar reservas já confirmadas por outras transações; isolamento com snapshot anterior não é aceito. Captura do teste de isolamento confirma rejeição de REPEATABLE READ e SERIALIZABLE. Capturas A/B confirmam espera concorrente e capacidade 1 respeitada; captura final confirma oferta/reserva de teste removidas.
- O rascunho define criar_agendamento(horário, chave) como SECURITY DEFINER com search_path vazio. Identidade vem de auth.uid(); valida parâmetros, exige perfil próprio e bloqueia sua linha com FOR UPDATE para coordenar pedidos desse doador.
- Captura da consulta de configuração confirma SECURITY DEFINER=true, volatilidade=v e search_path vazio. A usuária relatou privilégios esperados: anon sem EXECUTE/INSERT direto; authenticated com EXECUTE e sem INSERT direto. Captura confirma criação/repetição sem duplicação no roteiro supabase/tests/criar_agendamento_criacao_idempotencia.sql, com oferta temporária capacidade 1 e ROLLBACK. É simulação administrativa e sequencial, sem JWT pela API nem concorrência. Captura confirma bloqueios de chave reutilizada em outro horário e reserva ativa duplicada, preservando o original. Captura confirma quatro bloqueios em supabase/tests/criar_agendamento_disponibilidade.sql: inativo, unidade não publicada, passado e única vaga ocupada por B (A não vê essa reserva). Captura confirma os quatro resultados do teste de cancelados: retorna antigo cancelado, aceita nova chave, preserva cancelado e mantém somente um ativo para capacidade 1. Cancelamento é administrativo apenas no roteiro, não funcionalidade do app. Captura confirma oito bloqueios e dados preservados no teste de validação/acesso, inclusive INSERT/UPDATE/DELETE diretos e execução como anon. Captura confirma REPEATABLE READ/SERIALIZABLE bloqueados no teste de isolamento. Captura da preparação com COMMIT confirma oferta temporária ID 13, início 2026-10-19 15:44:43.092005+00, capacidade 1. Capturas dos roteiros A/B em supabase/tests/concorrencia/ confirmam reserva A ID 22, espera de B pelo bloqueio=true e B sem vagas, com somente uma reserva ativa de A. B termina em ROLLBACK, incluindo perfil temporário. Captura final confirma oferta_removida=true e reservas_do_teste_removidas=true para oferta 13/reserva 22. É cenário controlado de duas sessões/uma vaga, sem JWT pela API. Checkpoint preparado; a modelagem acadêmica completa e integração Android continuam pendentes. Ver docs/12-validacao.md.
- Busca a chave somente para esse doador; retorna o registro existente com estado atual, rejeitando reutilização para outro horário. RETURN NEXT adiciona a linha e RETURN encerra. Novo pedido após cancelamento precisa de nova chave; a futura UI deve respeitar o estado retornado.
- A função busca oferta ativa/publicada, com FOR UPDATE no horário e FOR SHARE na unidade. Confere início futuro com clock_timestamp(), rejeita outra reserva ativa desse doador e compara a contagem de todos os agendamentos ativos com a capacidade. Recuperação idempotente acontece antes dessas validações de nova reserva. INSERT usa doador da sessão, horário validado, estado agendado e chave recebida; ID/criado_em vêm dos defaults. Criação/repetição, negações, cancelados, isolamento e cenário concorrente A/B com uma vaga verificados por capturas/relatos dos roteiros manuais; limpeza confirmada por captura. Decisões e responsabilidades em docs/15-disponibilidade-agendamento.md.

## Confirmação no Android — 12/10/2026

- DTO/mapper, DataSource RPC e repository estão ligados via AppContainer ao SchedulingViewModel. A usuária implementou a confirmação em pequenos passos; o botão agora chama `confirmAppointment`. Horário/chave são enviados, identidade e capacidade são decididas no servidor.
- A chave é reutilizada após erro; mudar a seleção ou recarregar limpa o pedido local. Pendência bloqueia envio/alterações/Voltar. Apenas SCHEDULED abre sucesso com ID retornado e dados escolhidos; CANCELLED permanece na revisão. Rascunho/chave não sobrevivem à morte do processo.
- Validação focada no Pixel_6/Android 12: dez testes de navegação e seis de confirmação aprovados, sem falhas; os seis novos foram conferidos no XML. Todos usam repositories fictícios. A usuária confirmou pelo aplicativo o ID 23; captura administrativa mostra reserva persistida do perfil sintético A, Demo A, 19/10/2026 às 08:00, agendado. Segunda intenção para o mesmo doador/horário exibiu erro na revisão; captura posterior confirma somente a reserva 23, agendada e com criado_em preservado. Preservar a reserva. Não houve gravação remota pelo agente. Ver docs/12-validacao.md e docs/15-disponibilidade-agendamento.md; isolamento A/B pela API, recuperação real com a mesma chave e perda real de resposta continuam pendentes. Próxima integração funcional: consulta dos agendamentos persistidos, ainda sem implementação.
- Checkpoints anteriores que dizem que Confirmar é somente visual são históricos. Listagem/detalhe/cancelamento, recorrência e cadastro real continuam pendentes; não assumir que já foram integrados a partir da confirmação.


## Consulta dos próprios agendamentos — etapa iniciada em 12/10/2026

- A usuária criou AppointmentSummary, contrato getMyAppointments, DTO/mapper e chamada RPC no DataSource/repository. Aplicativo e testes compilaram; dois repositories fictícios foram adaptados pelo agente, com leitura ainda não configurada. ViewModel/tela pendentes.
- A usuária relatou aplicação manual da migração 20261012000200_consultar_meus_agendamentos.sql com resultado esperado. Não reaplicar. Capturas confirmam configuração (SECURITY DEFINER/STABLE/search_path vazio, EXECUTE somente authenticated), leitura própria A/B com formato completo, acesso/vazio e oferta oculta/passada/cancelada com policies públicas preservadas. Reserva 23 preservada. São identidades simuladas, sem JWT pela API. Captura da conferência final pós-ROLLBACK confirma 2 unidades, 3 horários, 1 perfil, 1 agendamento, zero unidades temporárias e zero outras reservas no horário de 23; sete registros do domínio. Três testes locais do JSON/mapper passaram, conferidos no XML; sem HTTP real. Lint final: 31 avisos e zero erros. Etapa SQL concluída nesse recorte. Retomada em docs/checkpoints/2026-10-12-consulta-agendamentos.md. Preservar reserva 23. Próxima integração: estado/ViewModel/tela de listagem, um passo por resposta. A usuária autorizou commit/push desta etapa; conferir sincronização no Git ao retomar. Ver docs/12-validacao.md e supabase/README.md.

## Listagem no Android — 13/10/2026

- A usuária implementou estado/ViewModel em `presentation/appointments`, fábrica e passagem da dependência pela navegação. A tela recebe estado e nova tentativa; mostra carregamento, erro, vazio ou reservas com data/hora no fuso da unidade, unidade, ID e estado. Consulta inicia no ViewModel, impede duplicidade e respeita cancelamento. O callback antigo de detalhes foi retirado da listagem; detalhe por ID e cancelamento persistido continuam pendentes.
- Aplicativo e testes compilaram. O agente adaptou sete chamadas instrumentadas de AppNavHost, configurou lista vazia no repository fictício de navegação e acrescentou `AppointmentListFlowTest`. Seis testes passaram no Pixel_6/Android 12, conferidos no XML: carregamento/duplicidade, erro/nova tentativa, vazio, fuso/estado cancelado, saída/retorno e troca A/B. Esses testes usam repositories fictícios, sem rede. Lint: 31 avisos e zero erros. Ver docs/12-validacao.md para o ajuste do roteiro antigo de cancelamento visual e sua repetição aprovada. Preservar a reserva 23 e não reaplicar SQL. A usuária autorizou commit/push da etapa em 13/10; conferir versão e sincronização no Git ao retomar.

- Na verificação manual da listagem em 13/10, após orientação para entrar como conta sintética A, captura do emulador confirma reserva 23, Unidade Demo A, 19/10/2026 às 08:00 e estado Agendado. Após orientação de logout de A e login como B, segunda captura mostra lista vazia e ausência de 23. Após logout de B e retorno para A, terceira captura confirma 23 novamente com os mesmos dados. Roteiro de leitura A → B → A com sessões reais concluído nesse recorte; não é auditoria completa de acesso à API. Ver docs/12-validacao.md para os limites das capturas; nenhuma credencial, token ou tráfego HTTP foi inspecionado. Próximo passo funcional após registro no Git: detalhes vinculados ao ID da reserva, em modo de tutoria.

## Diagrama e implementação dos detalhes — 13/10/2026

- A usuária pediu diagramas antes de cada nova etapa. O fluxo implementado da listagem está em `docs/17-diagrama-sequencia-listagem.md`; o diagrama de `docs/18-diagrama-sequencia-detalhe.md` foi apresentado como proposta antes da implementação guiada.
- A usuária implementou rota tipada `AppointmentDetailById(appointmentId)`, estado/ViewModel próprio do destino, função de fábrica por ID e nova tela. O botão “Ver detalhes” entrega o ID à navegação. O ViewModel reutiliza `AppointmentRepository.getMyAppointments()`/RPC existente e procura o ID na lista própria; representa Loading/Loaded/NotFound/Error, impede consultas simultâneas e verifica cancelamento. A tela mostra unidade, ID, data/hora no fuso e estado. Acessos visuais antigos do Perfil/Home continuam separados; cancelamento persistido, alteração e recorrência ainda pendentes. Nenhuma nova migração ou RPC, endereço fixo ou lembrete inventado.
- Aplicativo e testes compilaram; Lint passou com 31 avisos e zero erros. O agente adaptou oito chamadas instrumentadas e acrescentou `AppointmentDetailFlowTest`: sete aprovados no Pixel_5/Android 12, zero falhas/erros/ignorados, conferidos no XML. Complemento: seis testes de listagem e dez de navegação aprovados no mesmo emulador, sem falhas/erros/ignorados; 23 aprovações em duas execuções. A primeira tentativa não iniciou por emulador offline; conexão restabelecida antes das execuções aprovadas. Testes usam repositories fictícios, incluindo ID alheio/ID próprio após troca A/B; não é prova de isolamento real da API. Na verificação manual orientada para A, capturas confirmam botão na listagem e dados no detalhe de 23: Unidade Demo A, 19/10/2026 às 08:00, Agendado. A usuária relatou retorno pela seta à lista mantendo 23, como esperado; percurso concluído nesse recorte. Preservar a reserva. Ver docs/12-validacao.md. Produção continua sob responsabilidade da usuária e em pequenos passos. A usuária autorizou commit/push do detalhe e diagramas em 13/10; conferir versão e sincronização no Git ao retomar.

## Cancelamento no banco — 14/10/2026

- Retomada atual em `docs/checkpoints/2026-10-14-cancelamento-supabase.md`. A usuária escreve produção; antes da próxima integração Android, apresentar o diagrama com SQL implementado e Android proposto e orientar um passo curto por resposta. Não implementar Kotlin automaticamente a partir do checkpoint.
- Prazo aprovado: cancelamento próprio antes do início, sem antecedência adicional. Oitava migração `20261014000100_criar_cancelamento_agendamento.sql` aplicada manualmente; não reaplicar. Tabela `agendamento_cancelamento` com FK única à reserva, FK ao autor e instante; RLS/grants/sequência/constraints conferidos por capturas. Função VOLATILE/SECURITY DEFINER/search_path vazio, EXECUTE somente authenticated; exige READ COMMITTED, coordena perfil → horário → reserva, valida dono e prazo após bloqueios, grava estado/evento atomicamente e retorna registro próprio já cancelado sem novo evento.
- Capturas de cinco roteiros sequenciais confirmam sucesso/repetição, prazo/oferta oculta, validação/acesso A/B/anon, atomicidade sob conflito do evento e rejeição de REPEATABLE READ/SERIALIZABLE. São identidades/papéis administrativos simulados, sem login/JWT pela API. Concorrência controlada: oferta temporária 23, início 2026-10-21 21:31:31.436406+00, capacidade 1, reserva A=37; A cancela e observa B esperando=true; B cria 38 depois do COMMIT de A, com apenas B ativo e um evento de A. B termina em ROLLBACK; captura de limpeza confirma oferta/reservas/eventos removidos=true. Não repetir o cenário histórico.
- Captura final confirma 2 unidades, 3 horários, 1 perfil, 1 agendamento, 0 eventos; sete registros do domínio. Reserva original 23 agendada, com dono/unidade/início/fuso/criado_em conferidos=true. Horário temporário 23 é outra entidade; está removido, assim como reservas 37/38/eventos. Preservar a reserva original 23. Cinco tabelas e cinco FKs do domínio, além da FK para Auth; sexta tabela e seed de 200 registros ainda pendentes.
- Cancelamento no Android ainda não implementado. Próximo passo após registro no Git autorizado: contrato/repository/DataSource RPC e depois estado/ação no detalhe, mantendo navegação em ui/navigation e atualização da lista ao voltar. Reutilizar DTO/mapper da reserva; telas de cancelamento visuais antigas continuam separadas. Sem repetir Gradle nesta etapa SQL. Commit/push do recorte de 14/10 ainda pendentes na criação do checkpoint; conferir Git ao retomar. Ver `docs/12-validacao.md` para evidências e limites.

## Cancelamento no Android — 14/10/2026

- Diagrama apresentado como proposta antes do código em `docs/21-diagrama-sequencia-cancelamento-android.md`. A usuária implementou contrato, RPC/repository, estado/ViewModel e confirmação no detalhe por ID. Reutiliza AppointmentDto/mapper; identidade e prazo são decididos no servidor. Preserva CancellationException, dados em falha e ID na repetição; só mostra sucesso para o ID solicitado com estado CANCELLED.
- Envio impede consulta/cancelamento simultâneos e saída pela seta/Voltar/abas. A lista recarrega em ON_RESUME após a primeira abertura, com indicador no SavedStateHandle; a consulta inicial permanece no ViewModel e NavController fica em ui/navigation. Rotas antigas visuais continuam separadas.
- O agente adaptou quatro implementações fictícias do contrato e acrescentou sete cenários em AppointmentDetailFlowTest; 14 testes de detalhe/cancelamento e seis de listagem passaram no Pixel_5/Android 12, zero falhas/erros/ignorados conferidos nos XML. LintDebug passou com 31 avisos/zero erros. São dados/sessão em memória, sem API; perda de resposta e restauração são simuladas.
- Roteiro real concluído pela usuária: preparação específica com COMMIT criou oferta 24/reserva 39. Após orientação de cancelamento pelo app como A, captura mostra 39 Cancelado e 23 Agendado na lista. Consulta administrativa confirmou 39 cancelado, exatamente um evento, autor_confere=true e instante 2026-10-15 02:28:24.572206+00; original 23 agendado, zero eventos e criado_em original preservado. Captura da limpeza específica confirma oferta/reserva/evento removidos e 23 preservada. Não repetir os roteiros nem reaplicar migrações. Sem inspeção de JWT/tráfego, perda real de resposta ou auditoria completa de API. Massa anterior de sete registros preservada no escopo do roteiro, sem nova contagem global. Commit/push da integração Android autorizados pela usuária após a limpeza; conferir versão e sincronização no Git ao retomar; modelagem/seed acadêmicos permanecem pendentes. Ver docs/12-validacao.md e atualização final do checkpoint de 14/10; continuar em tutoria.

## Campanhas por unidade — decisão de 15/10/2026

- **Recorte SQL concluído:** captura final confirma 2 unidades, 3 horários, 1 perfil, 1 agendamento, 0 eventos, 0 campanhas, 0 unidades temporárias, total de sete registros e 23 agendado com dados conhecidos conferidos=true. Cinco consultas de catálogo e três testes comportamentais (leitura direta/JOIN, integridade, escrita/sequência negadas) tiveram os resultados esperados por captura; papéis simulados no SQL Editor, sem JWT/API. Migração aplicada manualmente; não reaplicar nem repetir automaticamente os roteiros históricos. Ainda sem seed persistente de campanhas ou camadas Android; 200 registros continuam pendentes. Retomar por `docs/checkpoints/2026-10-15-campanhas-supabase.md` e diagrama 22, mantendo tutoria. Commit/push autorizados pela usuária em 15/10 após a conferência final; incluir a migração completa (estava AM) e conferir commit/sincronização no Git ao retomar. Depois seed mínimo sintético e integração Android em pequenos passos. Itens abaixo são históricos desta etapa.

- Última validação: captura de `campanha_integridade.sql` confirma defaults, CHECKs, nove NOT NULL, FK/RESTRICT, identidade/PK e preservação de campanhas/unidades/23; fixtures em ROLLBACK. Próximo passo: executar `supabase/tests/campanha_escrita_bloqueada.sql` inteiro como postgres (preparado, ainda sem execução), depois conferência final. Não reaplicar a migração nem executar seed. Integração Android ainda pendente. Histórico abaixo conserva as etapas anteriores.

- Progresso mais recente: captura confirma nove colunas NOT NULL com identidade/defaults esperados e policy SELECT para anon/authenticated com with_check NULL (qual cortada na imagem). Teste `campanha_leitura_publicada.sql` teve resultado esperado por captura: leitura direta/JOIN sob os dois papéis retorna somente campanha publicada de unidade publicada. Simulação administrativa, fixtures em ROLLBACK; não é API/seed persistente. Próximo roteiro é `supabase/tests/campanha_integridade.sql`, preparado e ainda não executado, seguido de negações de escrita e conferência final. Preservar reserva 23 e não reaplicar migrações; produção continua escrita pela usuária em pequenos passos. Itens abaixo conservam estados anteriores.

- Evolução posterior: a usuária concluiu e relatou aplicação manual de `20261015000100_criar_campanha.sql`; não reaplicar. Capturas confirmam tabela/RLS, SELECT sem INSERT/UPDATE/DELETE/TRUNCATE para anon/authenticated, sequência sem USAGE/SELECT/UPDATE e PK/FK RESTRICT/quatro CHECKs. Agora são seis tabelas/seis FKs do domínio, além de Auth; 200 registros continuam pendentes. Próximo passo: consulta somente leitura `supabase/tests/campanha_policy.sql`, depois colunas e testes comportamentais em ROLLBACK. Ainda sem seed persistente ou integração Android de campanhas. A migração apareceu AM no Git: em futuro commit autorizado, incluir a versão completa revisada. Os bullets abaixo conservam a proposta inicial e o começo da tutoria.

- Retomada confirmou main limpa e sincronizada em `3b9846189c596515cf945fc78fb1c6bcddfdb31d` (cancelamento Android concluído, commit/push feitos). Não repetir fixtures de cancelamento; preservar 23.
- A usuária aprovou campanhas sintéticas como próxima tabela, cada campanha ligada a exatamente uma unidade; unidade pode ter zero ou várias campanhas. Publicação manual no Supabase, consulta pelo app. Não cria reservas nem comprova coleta/estoque/aptidão. Recorrência continua pendente.
- Visão inicial e diagrama proposto salvos em `docs/22-diagrama-sequencia-campanhas.md`, antes do código. Hoje há apenas tela/rotas de campanhas com dados fixos; tabela e camadas funcionais ainda não existem. Primeiro passo orientado: a usuária cria `20261015000100_criar_campanha.sql` com transação, PK e FK para unidade, sem executar o rascunho incompleto. Campos, período, fonte, publicação, RLS/grants e testes virão em pequenos passos. Nenhum SQL remoto ou teste de aplicação foi executado pelo agente nesta preparação documental. Ainda são cinco tabelas/cinco FKs do domínio aplicadas; com a nova tabela serão seis/seis, além de Auth, e seed de 200 registros continua pendente.

## Revisão editorial — 06/10/2026

- Orientações de idade, procedimentos e medicamentos revisadas após a Portaria GM/MS nº 11.685/2026, com fontes e data visíveis. Ver `docs/16-revisao-regras-doacao.md` para referências e limites.
- Não reintroduzir a cadência trimestral nem datas futuras como recomendação universal. Recorrência ainda não está implementada; preservar a distinção entre agendamento e aptidão.
- Inventários locais do Figma são históricos; os textos atuais estão em `RestrictionContent.kt`. Figma remoto não foi alterado.
