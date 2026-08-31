# Doe Sangue

Aplicativo acadêmico Android para gestão e incentivo à doação de sangue. O app é voltado ao doador e pretende permitir consulta de unidades e necessidades, agendamento, status, histórico e campanhas.

> Estado atual: as 34 telas da versão 2 do Figma e sua navegação visual estão implementadas em Jetpack Compose. Campos e controles são apenas representações visuais; não há autenticação real, persistência, conexão com Supabase nem regras médicas implementadas.

## Stack confirmada

- Android nativo, Kotlin e Jetpack Compose.
- Arquitetura MVVM com Repository preparada em camadas; a UI visual ainda não consome ViewModels.
- Supabase Authentication, PostgreSQL e PostgREST na etapa de integração.
- Dados sintéticos para a demonstração acadêmica.

## Configuração proposta

- `applicationId`: `br.edu.fatec.doesangue` (a confirmar).
- `minSdk 23`, `targetSdk 36`, `compileSdk 36`.
- JDK 17 como alvo; o Gradle pode executar pelo JDK 21 instalado.
- AGP 9.2.0, Kotlin 2.3.21 e Gradle 9.4.1.

## Abrir e executar

1. Abra esta pasta no Android Studio.
2. Aguarde a sincronização do Gradle e confirme o SDK 36.
3. Escolha um emulador/dispositivo com API 23 ou superior.
4. Execute a configuração `app`.

```powershell
.\gradlew.bat test assembleDebug lint
```

O Android Studio gera `local.properties` com o caminho do SDK; esse arquivo não deve ser versionado.

## Supabase

A integração ainda não existe. URL e chave cliente publicável deverão vir de configuração local não versionada. Nunca incluir `service_role`, senha do banco, tokens ou dados pessoais no aplicativo ou no Git.

## Escopo visual atual

- Splash, boas-vindas, autenticação e cadastro em quatro etapas.
- Home versão 2, agendamento, doações, campanhas, unidades, perfil e configurações.
- Fluxos de detalhe, edição e cancelamento presentes no Figma.
- Navegação inferior entre Início, Agendar, Doações, Unidades e Perfil.
- Entrada de Configurações adicionada ao Perfil por decisão explícita de produto.

Botões que dependeriam de backend, permissões do dispositivo ou integrações externas permanecem sem efeito propositalmente.

## Documentação

Os documentos em `docs/` usam **Confirmado**, **Proposto**, **Implementado** e **Pendente** para distinguir requisitos, escolhas atuais e código pronto.
# Doe_Sangue
