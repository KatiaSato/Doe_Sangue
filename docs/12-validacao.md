# Validação

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
