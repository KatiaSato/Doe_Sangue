# ADR-003 — Navigation Compose e responsabilidades

- **Status:** adotado e implementado para navegação visual.
- **Data:** 23/09/2026.

## Contexto

O protótipo usava uma lista em memória e um `when` para escolher a tela. A evolução pede gerenciamento de pilha e restauração, com espaço para argumentos tipados. A implementação guiada adotou Navigation Compose 2.9.8 e Kotlin Serialization, preservando `minSdk 23` e `compileSdk 36`.

## Decisão

Usar um único controlador em `DoeSangueApp`, um `AppNavHost` e grafos Auth, Main e Scheduling. O último é aninhado em Main. Rotas e decisões de navegação pertencem a `ui/navigation`; telas recebem callbacks; ViewModels não recebem NavController; domínio e dados não conhecem destinos.

Splash e Auth saem da pilha após suas transições. Na política atual, trocar de aba encerra o percurso anterior de Main. A seta de uma raiz retorna à Home quando não há tela anterior; a verificação ocorre antes de remover qualquer destino. Retornos internos continuam pela pilha. O roteador próprio foi substituído.

## Consequências e limites

Navigation 2 está em manutenção; a escolha atual atende à migração incremental, sem implicar atualização de SDK ou adoção de Navigation 3. A suíte instrumentada verifica os fluxos reais do protótipo e a restauração de estado salvo do Compose. Histórico por aba, deep links, IDs de registros e rascunho funcional continuam pendentes. Autenticação, criação e cancelamento permanecem simulações visuais.

## Referências

- [Testes de navegação Compose](https://developer.android.com/guide/navigation/testing/compose)
- [Notas de versão Navigation](https://developer.android.com/jetpack/androidx/releases/navigation)
- [Validação local](../12-validacao.md)
