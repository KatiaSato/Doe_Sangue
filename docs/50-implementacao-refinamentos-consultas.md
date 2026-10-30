# Refinamentos de consultas — implementação autorizada em 29/10/2026

## Correção — seleção da unidade, 30/10/2026

Usuária relata que tocar na unidade parece não fazer nada. A leitura do código mostra que o cartão seleciona a unidade sem avançar automaticamente; o botão Continuar ficava depois de toda a lista, fora da vista com o catálogo ampliado. Isso comprova um obstáculo de apresentação, ainda sem reprodução do relato no aparelho. Nenhuma relação estabelecida com datas vencidas, rede ou banco.

Fluxo do ajuste, antes do código: **toque no cartão → callback existente → ViewModel seleciona unidade → resumo e Continuar fixos acima da navegação inferior → próximo passo Data e horário**. A lista continua rolável; não haverá avanço automático ou alteração de reserva/rascunho/regras.

| Arquivo existente | Responsabilidade no ajuste |
| --- | --- |
| `ui/screens/scheduling/SchedulingScreens.kt` | Usar o rodapé já suportado por DsScreen para manter seleção e Continuar visíveis, preservando a barra inferior |
| `ui/navigation/RefinementScreensTest.kt` em androidTest | Conferir seleção real com catálogo longo, botão sem rolagem, filtros e bloqueio em erro/carregamento |
| `ui/navigation/SchedulingOriginFlowTest.kt` em androidTest | Conferir o grafo e ViewModel existentes com 14 unidades; avanço e retorno preservam a seleção |

Implementado somente no rodapé de ScheduleCenterScreen, reutilizando DsScreen/DsBottomBar. Critérios de habilitação preservados: consulta concluída, sem erro e seleção pertencente ao catálogo; a busca não apaga a seleção. Capturas sintéticas de lista longa e fonte1,5 revisadas e legíveis. Testes novos verificam seleção da primeira/última unidade, botão sem rolagem e bloqueios em carregamento/erro/seleção ausente. O teste de grafo verifica unidade → data e retorno com a mesma seleção.

**Validação:** APKs/Lint aprovados em3m3, zero erros/34 avisos; última recompilação de testes/Lint em1m38.28 Android distintos aprovados no Pixel_5 temporário/Android12/API32:3 de seleção/busca (2 novos),8 de origem/navegação,6 de criação e11 de remarcação/rotina, todos com fontes/respostas fictícias. A primeira rodada teve27 aprovados e1 timeout após dois comandos Voltar seguidos (261,021s). Acrescentada espera explícita e conferência da etapa intermediária no teste; o caso passou em8,095s sem nova alteração de produção. Não somar a repetição nem declarar que a rodada inicial inteira passou. Nenhuma nova suíte JVM necessária para este ajuste de composição UI.

APK de produção e teste instalados `-r` no Moto g04, preservando dados. A conexão do celular caiu antes de iniciar a instrumentação; testes concluídos no emulador existente em modo read-only/no-snapshot/no-window, encerrado ao final sem salvar a base. Capturas ignoradas em `captures/2026-10-30-selecao-unidade/`; logs e `result-final.json` em `app/build/reports/scheduling-selection/`.

Sem nova camada, SQL, remoto, Auth, e-mail, reserva real ou mudança nas datas/ofertas. Banco, rascunhos, applicationId e configurações preservados. Conferência da usuária no aplicativo ainda pendente; sem commit/push autorizado. F05/F15 continuam como próximas etapas guiadas após esta correção.

**Fechamento Git autorizado em29/10/2026:** após o aceite visual, a usuária pediu versionamento no GitHub. Incluir o conjunto completo de produção, testes, recursos e documentação, com commit/push normais em `origin/main`. Preservar arquivos ignorados e dados; não repetir testes/SQL. As menções abaixo à ausência de autorização Git descrevem os momentos anteriores a este pedido. Próximas etapas serão discutidas depois, sem início automático.

**Aceite visual em29/10/2026:** após a entrega, Katia informou “ficou muito bom”. Recorte encerrado com aprovação visual por relato, sem atribuir cenários manuais adicionais. Evidências e limitações dos testes abaixo mantidas; nenhuma nova execução de testes/SQL e nenhum commit/push autorizado por esse aceite.

Katia autorizou implementar as propostas atuais caso o banco não comportasse um filtro sanguíneo fiel em todas as guias. A inspeção das migrações/modelos confirma tipo no perfil, campanhas e estoques, mas não como atributo de agendamento, histórico ou rotina. Não atribuir o tipo atual do perfil a registros antigos. Seguir a alternativa autorizada: propostas de docs49, filtro de tipo no estoque e campanhas existente, sem novos estados/tabelas/migrações.

## Fluxo antes do código

```text
Pessoa escolhe busca, situação, nível ou tipo
                  |
                  v
Tela Compose guarda os filtros e a aba visível
                  |
                  v
Funções de apresentação filtram/agrupam os resultados existentes
                  ^
                  |
ViewModel consulta o repository atual e publica estado
         |                         |
         v                         v
Reservas/estoques privados     Catálogo público de unidades
         |                         |
         +---- associação por ID --+--> cidade, quando disponível

Lembretes --> rota própria --> ViewModel e operações de rotina existentes
Home --> atalho de nível --> mesma consulta de estoque, filtro inicial
```

Catálogo lento, oculto ou indisponível não pode esconder uma reserva própria. Nome e reservas continuam disponíveis; cidade ausente fica sem valor inventado. Recarregamentos descartam respostas antigas. Filtros não criam reservas, alteram capacidade, reativam cancelados ou sugerem aptidão.

## Mapa de arquivos

Todos os caminhos Kotlin abaixo são relativos a `app/src/main/java/br/edu/fatec/doesangue/`.

| Arquivo | Situação/camada | Responsabilidade e entrada/saída |
| --- | --- | --- |
| `presentation/common/CatalogFilters.kt` | Novo, apresentação | Normalização e projeções puras; listas + filtros → resultados e grupos |
| `ui/components/CatalogSearchField.kt` | Novo, UI | Campo reutilizável e filtro de tipo; texto/seleção → callbacks |
| `presentation/appointments/AppointmentsUiState.kt` e `AppointmentsViewModel.kt` | Existentes, apresentação | Acrescentar catálogo auxiliar, sem bloquear leitura privada |
| `presentation/stock/BloodStockViewModel.kt` | Existente, apresentação | Catálogo auxiliar independente da consulta de estoque |
| `presentation/centers/CentersViewModel.kt` | Existente, apresentação | Reusar a normalização de busca |
| `ui/screens/donations/DonationScreens.kt` | Existente, UI | Abas, filtros, lista eficiente e cartões compactos |
| `ui/screens/routines/ReminderOverviewScreen.kt` | Novo, UI | Terceira aba e resumo/ações da rotina existente |
| `ui/screens/scheduling/SchedulingScreens.kt` | Existente, UI | Buscar unidades sem apagar seleção/rascunho |
| `ui/screens/stock/StockScreen.kt` | Existente, UI | Busca + nível + tipo, agrupados por unidade, fonte/data preservadas |
| `ui/screens/home/HomeStockPreview.kt` e `HomeScreens.kt` | Existentes, UI | Atalhos Crítico/Alerta/Estável para a consulta filtrada |
| `ui/navigation/AppRoute.kt`, `MainNavGraph.kt`, `ui/components/DsBottomBar.kt` | Existentes, navegação | Aba Lembretes e entrada de estoque com nível; voltar e seleção inferior |
| `di/AppContainer.kt` | Existente, composição | Fornecer repository de unidades aos dois ViewModels |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Existentes, configuração | Explicitar Foundation Layout na versão Compose já usada em execução, evitando compilação contra a transitiva antiga |
| `docs/figma/refinamentos/assets/`, `app/src/main/res/drawable-nodpi/` | Novos recursos | Ícones exatos exportados do Figma e versões raster para Android |
| `app/src/test/…/CatalogFiltersTest.kt`, `app/src/androidTest/…` | Testes novos/adaptados | Combinação de filtros, acentos, catálogo ausente, navegação, retorno e fonte ampliada |

Arquivos do mapa implementados. Proposta Figma e código de contexto consultados para agendamentos, estoque, atalhos e rotina; traduzidos para Compose com Poppins, tokens/componentes existentes e dimensões adaptáveis. Sem nova versão do Compose ou imagem de tela inteira como implementação. Foundation Layout já fazia parte do runtime; sua versão de compilação foi explicitada após a incompatibilidade encontrada nos testes. `HomeStyle.kt` recebeu somente atualização de comentário sobre esse reuso; FAQ em `ProfileScreens.kt` passou a orientar o toque no cartão, em vez do botão removido.

## Ordem e validação

1. Funções puras e catálogo auxiliar, preservando falhas independentes.
2. Busca ao agendar e filtros/listas, com estados vazios e limpeza.
3. Terceira aba de lembretes, navegação e ações existentes.
4. Atalhos e agrupamento do estoque, com filtro dos oito tipos conhecidos e Todos.
5. Testes locais/Android com dados fictícios, compilação/Lint e conferência visual.

Não executar SQL remoto, reaplicar massa ou testar com reservas/contas reais. Sem novo commit/push autorizado. Google e notificações continuam fora deste recorte.

## Evidências e conclusão

Implementação técnica concluída e aprovada visualmente por relato da usuária em29/10/2026. Busca/filtros ficam no estado salvo de cada tela; detalhe/retorno preservam a consulta. A seleção da unidade continua válida quando uma busca a oculta e é identificada explicitamente. Reservas próprias são exibidas antes da conclusão do catálogo; retry de cidade não repete a consulta privada. Respostas canceladas/antigas são descartadas.

Lembretes usa a rotina e as operações já existentes, com confirmação para excluir e bloqueios de navegação/gravação enquanto salva. Trocar as três abas não acumula páginas; o Voltar do Android acompanha a seta, inclusive quando a seção é a única tela da pilha. Com fonte ampliada, as abas passam a rolar horizontalmente para não dividir Agendamentos no meio da palavra. Os filtros de situação podem ocupar mais de uma linha conforme a largura/fonte. Datas relacionadas da remarcação aparecem quando ambas as reservas estão na lista; sem a relacionada, o ID permanece como referência.

### Verificações aprovadas

- **96 JVM**, incluindo sete testes novos de normalização, situações, ordenação, metadados e combinação dos filtros. Zero falhas/erros nos XMLs.
- **98 Android distintos** no Moto g04/Android14: 15 novos (9 telas, 2 navegação, 4 catálogo/VM) e 83 de regressão (Home12, lista6, detalhe14, unidades14, origem8, remarcação/rotina11, campanhas4, sessão14). Repetições não foram somadas.
- APKs de produção/testes e Lint aprovados. Última compilação/análise de produção:39s, zero erros e34 avisos. O aviso adicional é de atualização disponível para a dependência Foundation Layout explicitada; nenhuma checagem foi desativada e não houve atualização da versão já usada em execução. Recompilações posteriores do auxiliar de captura afetaram somente testes.
- Capturas sintéticas revisadas de agendamentos, estoque, Home e rotina, incluindo fonte1,5. Seis ícones locais conferidos não vazios, com os slots de16/20/32dp; não há URL temporária de imagem no aplicativo. APKs instalados com `-r`, sem limpar dados.

### Falhas intermediárias e limites

O primeiro Lint teve falha interna `AsyncExecutionService`; a análise passou sem cache de configuração, mantendo os detectores. Uma compilação intermediária encontrou uma ligação antiga `routineContent`; o grafo foi corrigido e suas rotas passaram nos testes posteriores.

A primeira rodada Android14 expôs incompatibilidade real: Foundation Layout1.7.2 na compilação e1.9.5 em execução, confirmadas por `dependencyInsight`. A dependência1.9.5 foi explicitada na versão `composeUi` existente. As rodadas posteriores passaram pelo FlowRow sem `NoSuchMethodError`. Outra rodada15 encontrou o Voltar do Android encerrando a seção; os handlers passaram a usar a mesma saída da seta, como o Perfil já fazia. Os testes foram adaptados à remoção do botão Ver detalhes, ao agrupamento dos metadados e à nova localização da rotina, preservando as verificações de comportamento.

Na rodada longa98, **54 casos concluíram com sucesso e o processo caiu no caso55**, sem resultado final da suíte. `exit-info` e o buffer de crash do PID de teste registraram **SIGSEGV na RenderThread**. Não se estabeleceu uma causa conclusiva nem se atribuiu o evento a falta de memória ou ação da usuária. O caso interrompido passou isoladamente (8,474s); lotes finais de37 (120,387s) e29 (99,942s) passaram, incluindo todas as classes restantes e repetindo os novos casos/unidades. A união dos casos aprovados soma98, não54+37+29. A rodada adicional de capturas passou pelos nove testes de tela (25,741s), sem aumentar esse total. Não há garantia geral sobre falhas nativas ou outros dispositivos a partir desses resultados.

Todos os cenários usam fontes, contas, reservas e respostas fictícias em memória. Não houve SQL, seed, migração, e-mail, consulta/renovação de sessão real ou criação/cancelamento de reserva real nesta etapa. Não houve nova leitura de contagem remota;318 permanece a última massa conferida anteriormente, não uma contagem atualizada por esta alteração.

Evidências ignoradas em `app/build/reports/refinements/` (logs de build, dependências, instrumentação e falha nativa) e `captures/2026-10-29-refinamentos-app/`. As capturas finais usam o prefixo `refinements_`; há uma captura intermediária anterior da fonte ampliada, distinguida pelo nome sem esse prefixo. Aceite visual registrado em29/10/2026. Nenhum commit/push foi feito; a próxima etapa depende de escolha da usuária. Google e notificações continuam fora deste recorte.
