# Doe Sangue

Aplicativo acadêmico Android que ajuda doadores a encontrar unidades de coleta, consultar campanhas e estoques e organizar seus agendamentos de doação.

**Projeto entregue.** As funcionalidades principais do escopo acadêmico foram concluídas em novembro de 2026. O ambiente demonstrativo usa dados sintéticos.

## Funcionalidades concluídas

- **Conta e perfil:** cadastro e login por e-mail e senha, login com Google, confirmação de e-mail, recuperação de senha e edição do perfil do doador.
- **Consulta:** busca de unidades por nome ou cidade, abertura de mapas, campanhas publicadas e estoques por unidade e tipo sanguíneo.
- **Agendamentos:** calendário com disponibilidade, escolha de horário, confirmação persistida, retomada de rascunho, remarcação e cancelamento.
- **Minhas doações:** histórico, agendamentos com filtros por situação e rotina pessoal de lembretes.
- **Notificações:** preferências por conta neste aparelho, avisos nas datas da rotina e lembrete de agendamentos elegíveis programado para 24 horas antes.
- **Calendário:** exportação opcional de um agendamento para o editor de calendário do Android. A pessoa escolhe o calendário, inclusive o Google Calendar, e confirma o salvamento. Eventos exportados são ajustados na agenda caso a reserva seja remarcada ou cancelada.

## Stack

- Android nativo, Kotlin, Jetpack Compose e Navigation Compose.
- MVVM com ViewModels, StateFlow e repositories.
- Supabase Auth, PostgreSQL, PostgREST, Supabase Kotlin 3.6.0 e Ktor/OkHttp 3.4.3.
- Credential Manager para login com Google e WorkManager para notificações.
- `applicationId` `br.edu.fatec.doesangue`, Android mínimo API 23 e SDK 36 para compilação e destino.
- AGP 9.3.3, Kotlin 2.3.21 e Gradle 9.5.0. O código tem Java 17 como alvo e o Gradle pode executar com JDK 21.

## Abrir e executar

1. Abra esta pasta no Android Studio e sincronize o Gradle com o SDK 36.
2. Preserve o valor de `sdk.dir` no `local.properties` da raiz e adicione a URL e a chave publicável do seu projeto Supabase. Para habilitar o botão Google, configure também o ID público do cliente OAuth Web e habilite o provedor no Supabase.
3. Selecione um aparelho ou emulador com API 23 ou superior e execute o módulo `app`.

```properties
SUPABASE_URL=https://SEU_PROJETO.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_SUBSTITUA_PELO_VALOR_DO_PAINEL
GOOGLE_WEB_CLIENT_ID=SEU_CLIENTE_WEB.apps.googleusercontent.com
```

O Git ignora o `local.properties`. A URL, a chave publicável e o ID público configurado são incorporados ao APK. O segredo OAuth fica no Supabase. Para preparar um ambiente de demonstração, consulte as instruções de [migrações e dados sintéticos](supabase/README.md).

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

A segunda linha executa os testes instrumentados em um aparelho ou emulador conectado.

## Documentação

- [Validação e evidências](docs/12-validacao.md)
- [Sessão e retomada de rascunho](docs/42-sessao-durante-uso.md) · [Rascunho do agendamento](docs/43-retomada-rascunho-agendamento.md)
- [Remarcação e rotina](docs/44-remarcacao-agendamento.md) · [Rotina pessoal](docs/45-rotina-lembretes.md)
- [Login com Google](docs/47-login-google.md)
- [Notificações da rotina](docs/54-notificacoes-rotina.md) · [Avisos de agendamento e calendário](docs/55-avisos-agendamento-e-calendario.md)
