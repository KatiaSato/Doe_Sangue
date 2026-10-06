# Doe Sangue

Aplicativo acadêmico Android para gestão e incentivo à doação de sangue. O app é voltado ao doador e pretende permitir consulta de unidades e necessidades, agendamento, status, histórico e campanhas.

> Estado em 06/10/2026: a escolha de unidade no agendamento consulta o Supabase e mantém a seleção em um ViewModel. A base visual e a navegação Compose estão implementadas. Autenticação, calendário interativo, horários e gravação do agendamento continuam pendentes; as telas de confirmação ainda são demonstrativas. Veja o [registro desta integração](docs/14-integracao-unidades.md).

## Stack confirmada

- Android nativo, Kotlin e Jetpack Compose.
- MVVM com Repository e StateFlow conectado à escolha de unidade.
- PostgreSQL e PostgREST integrados à consulta de unidades; Supabase Auth pendente.
- Dados sintéticos para a demonstração acadêmica.

## Configuração proposta

- `applicationId`: `br.edu.fatec.doesangue` (a confirmar).
- `minSdk 23`, `targetSdk 36`, `compileSdk 36`.
- JDK 17 como alvo; o Gradle pode executar pelo JDK 21 instalado.
- AGP 9.3.3, Kotlin 2.3.21 e Gradle 9.5.0.

## Abrir e executar

1. Abra esta pasta no Android Studio.
2. Aguarde a sincronização do Gradle e confirme o SDK 36.
3. Escolha um emulador/dispositivo com API 23 ou superior.
4. Configure a URL e a chave publicável conforme a seção abaixo e execute `app`.

```powershell
.\gradlew.bat test assembleDebug lint
```

O Android Studio gera `local.properties` com o caminho do SDK; esse arquivo não deve ser versionado.

## Supabase

A consulta usa Supabase Kotlin 3.6.0, Ktor/OkHttp 3.4.3 e desugaring para manter `minSdk 23`. Preserve `sdk.dir` no `local.properties` da raiz e acrescente os valores do seu projeto de desenvolvimento, sem aspas:

```properties
SUPABASE_URL=https://SEU_PROJETO.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_SUBSTITUA_PELO_VALOR_DO_PAINEL
```

Os valores acima são exemplos. O arquivo é ignorado pelo Git; Gradle lê suas propriedades e gera os campos de `BuildConfig`. Após alterar os valores, recompile o aplicativo. A chave publicável ficará no APK e não é segredo: permissões e RLS protegem os dados. Nunca incluir `sb_secret_`, `service_role`, senha do banco ou tokens de usuário no Android.

Sem configuração, a compilação e os testes com fake são possíveis, mas abrir o agendamento no aplicativo normal falha na validação do cliente. Consulte [SQL, seed e aplicação manual](supabase/README.md) antes de preparar outro ambiente. O aplicativo não executa migrações.

## Escopo visual atual

- Splash, boas-vindas, autenticação e cadastro em quatro etapas.
- Home versão 2, agendamento, doações, campanhas, unidades, perfil e configurações.
- Fluxos de detalhe, edição e cancelamento presentes no Figma.
- Navegação inferior entre Início, Agendar, Doações, Unidades e Perfil.
- Entrada de Configurações adicionada ao Perfil por decisão explícita de produto.

A consulta, seleção de unidade e repetição da consulta são funcionais. As demais ações de backend permanecem visuais; a tela de sucesso ainda não comprova um agendamento gravado.

## Documentação

Os documentos em `docs/` usam **Confirmado**, **Proposto**, **Implementado** e **Pendente** para distinguir requisitos, escolhas atuais e código pronto.

## Navegação e testes — atualização de 23/09/2026

**Implementado:** Navigation Compose 2.9.8, rotas tipadas e grafos Auth/Main/Scheduling. As telas enviam callbacks; as decisões de navegação ficam em `ui/navigation`. As setas das raízes retornam à Home quando não há tela anterior. Histórico independente por aba e persistência do rascunho continuam pendentes.

Com um emulador ou dispositivo de teste conectado, execute:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Os testes em `app/src/androidTest` exercitam a navegação e o estado salvo do Compose, com `FakeDonationCenterRepository` injetado explicitamente e sem consulta ao Supabase. Em 06/10/2026, 12 testes passaram no Pixel 5/API 32. `test` executa somente os testes locais em `app/src/test`; não substitui a suíte instrumentada. Consulte [telas e navegação](docs/10-telas-e-navegacao.md), [ADR-003](docs/decisoes/ADR-003-navegacao-compose.md) e [validação](docs/12-validacao.md).
