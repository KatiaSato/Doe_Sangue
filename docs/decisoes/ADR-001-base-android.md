# ADR-001 — Base Android e versões

- **Status:** proposta aceita para a base inicial.
- **Data:** 30/08/2026.

## Contexto

O stack Kotlin/Compose está confirmado; versões, SDK mínimo e identificador não estavam recuperados. O ambiente tem JDK 21 e SDK 36.

## Decisão

Usar AGP 9.2.0, Gradle 9.4.1, Kotlin/Compose Compiler 2.3.21, `compileSdk/targetSdk 36`, `minSdk 23` e bytecode Java 17. O catálogo centraliza versões. `br.edu.fatec.doesangue` é provisório.

As bibliotecas AndroidX foram fixadas em versões estáveis compatíveis com SDK 36 (Core 1.17.0, Activity Compose 1.12.2, Compose UI 1.9.5 e Material 3 1.3.2). As versões AndroidX de agosto de 2026 exigem SDK 37, ausente no ambiente.

## Consequências

Exige Android Studio compatível com AGP 9.2. A confirmação institucional do `applicationId` continua pendente.
