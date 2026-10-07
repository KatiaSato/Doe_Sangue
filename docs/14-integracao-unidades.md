# Consulta de unidades — 06/10/2026

## Confirmado

- Projeto acadêmico de desenvolvimento no Supabase, com unidades e futuros horários sintéticos. Nenhuma reserva real em hemocentros.
- A usuária aplicou o SQL e o seed pelo painel e informou que a leitura com os papéis `anon` e `authenticated` retorna somente a unidade publicada. A consulta de privilégios retornou SELECT permitido e ausência de INSERT/UPDATE/DELETE/TRUNCATE para ambos.
- Em 03/10/2026, a usuária confirmou que `Unidade Demo A` apareceu na tela de agendamento no celular. É uma consulta real com conteúdo fictício. A ausência de Demo B no aplicativo não foi confirmada separadamente.

## Implementado e responsabilidades

| Arquivo/classe | Responsabilidade |
| --- | --- |
| `app/build.gradle.kts` | Ler a configuração local e gerar URL/chave publicável em `BuildConfig`; habilitar bibliotecas e desugaring. |
| `SupabaseClientFactory.kt` | Validar formatos básicos e criar cliente com PostgREST. Não comprova conectividade. |
| `SupabaseDonationCenterDataSource` | Consultar `unidade_coleta` pela API, decodificar DTOs e convertê-los para domínio. |
| `DonationCenterDto` | Representar nomes e tipos do JSON; ID `Long`, telefone e horário opcionais. |
| `DonationCenterMapper.kt` | Converter ID para `String` e campos opcionais nulos para texto vazio. |
| `DonationCenterRepository` | Definir o contrato de consulta sem depender do Supabase. |
| `SupabaseDonationCenterRepository` | Retornar lista ou falha; propagar cancelamento de corrotina. |
| `AppContainer` | Montar cliente/fonte/repository com `lazy` e fornecer a fábrica do ViewModel; aceitar repository alternativo explícito. |
| `SchedulingViewModel` | Consultar pelo contrato, expor carregamento/erro/lista e manter a unidade selecionada em StateFlow. |
| `ScheduleCenterScreen` | Exibir os estados, permitir repetir a consulta e selecionar unidade; liberar Continuar somente com seleção válida. |
| `FakeDonationCenterRepository` | Fornecer a unidade fixa em `androidTest`, sem entrar no APK distribuído. |

Pedido: tela → ViewModel → repository → fonte de dados → cliente/API → banco.
Resposta: JSON → DTO → mapper → domínio → repository → estado do ViewModel → Compose.

O ViewModel pertence ao grafo Scheduling; a seleção permanece durante a navegação entre suas etapas enquanto essa instância existir. Não há `SavedStateHandle` para restaurar o rascunho após morte do processo.

O contrato antigo `DonationOverviewRepository`, sua fonte demo e seu caso de uso continuam no projeto, mas não alimentam mais a seleção de unidade. Home, estoque e tela geral de unidades não passaram a consumir Supabase por esta mudança. O fake é uma escolha explícita dos testes, não um fallback quando a rede falha.

## Banco e configuração

Ver [banco de dados](09-banco-de-dados.md), [SQL/seed](../supabase/README.md) e [configuração local](../README.md#supabase).

Existe uma tabela de domínio e dois registros sintéticos no seed, sem FKs nesta etapa. Isso ainda não atende ao mínimo acadêmico de seis tabelas, cinco FKs e 200 registros. O app tem somente leitura dessa tabela, sem autenticação implementada: o botão Entrar ainda é navegação visual.

O cliente exige URL HTTPS e chave com prefixo publicável. Valores ausentes passam pela compilação como texto vazio, mas impedem criar o cliente ao abrir o agendamento normal. Os testes injetam o fake e não dependem desses valores.

## Validado

Os testes automatizados abaixo correspondem ao checkpoint de integração de unidades, anterior à seleção de data/horário. Para a evolução posterior, ver [disponibilidade de agendamento](15-disponibilidade-agendamento.md) e os resultados atuais em [validação](12-validacao.md).

- `test assembleDebug assembleDebugAndroidTest lint`: sucesso em 06/10/2026; um teste local básico aprovado; 31 avisos no Lint, zero erros.
- `connectedDebugAndroidTest`: 12 testes aprovados no Pixel 5 com Android 12/API 32, usando o fake: nove de navegação e três de restrições; zero falhas, erros ou testes ignorados.
- Essas verificações não são testes automatizados do Supabase, da RLS, de falha de rede ou de concorrência. Evidências remotas acima são relatos da execução manual pela usuária.

## Pendente e próximo passo

1. Calendário e seleção de horário já preenchem o rascunho em memória. Em 07/10/2026, a revisão passou a receber o estado do mesmo ViewModel e a exibir unidade, data e horário escolhidos; Confirmar exige os três campos. A lista de horários ainda é fixa. A cobertura automatizada da exibição desses valores e dos estados incompletos da revisão continua pendente; os testes anteriores cobrem seleção e troca da data.
2. Modelar e implementar disponibilidade sintética por unidade no banco, conforme a [decisão registrada](15-disponibilidade-agendamento.md): dias e horários configuráveis, exceções e revalidação no servidor, sem bloqueio universal de fim de semana nem regras médicas inventadas.
3. Implementar Auth, tabelas relacionadas, permissões e gravação/consulta do agendamento. A tela de sucesso atual ainda é visual.
4. Adicionar testes de mapper, sucesso/vazio/erro/cancelamento do fluxo e RLS com usuários distintos quando houver dados privados. Reconciliar seleção ao recarregar uma lista é outra melhoria pendente.
5. Completar o esquema e a massa acadêmica; alinhar o histórico de migrações antes de adotar a CLI em um banco já criado manualmente.

Agendamento não comprova comparecimento, aptidão, coleta ou alteração de estoque. Recorrência continua pendente de definição.
