# Telas e navegação

## Consulta de restrições — 29/09/2026

Nas duas Homes, **Antes de doar → Consultar restrições** abre quatro categorias:
medicamentos, idade/peso, doenças/sintomas e vacinas/procedimentos.
Cada categoria abre uma lista com fonte oficial e orientação para contatar
o hemocentro. Fontes abrem no navegador, com mensagem se ele estiver indisponível.

Rotas tipadas: `AppRoute.Restrictions` e `AppRoute.RestrictionDetail(category)`.
Seta e Voltar seguem lista → categorias → Home de origem, inclusive Home agendada.
O argumento da rota permite restaurar a categoria selecionada.
Busca, calendário, cálculo fictício e resultados individuais anteriores foram removidos.
Ver [implementação, fontes e limites](13-triagem-medicamentos.md).

## Referências confirmadas

Nome visual: **Doe Sangue**. Frase: **Um pequeno ato que salva vidas**. Figma fornecido: arquivo “Doe Sangue — Android App — Education”, nó `9:8`.

## Implementado

Foram implementadas as 34 telas selecionadas do Figma: splash, boas-vindas, login, recuperação, verificação de e-mail, cadastro em quatro etapas, Home versão 2 e Home com agendamento, seleção de unidade/data/recorrência, confirmação e sucesso, histórico e agendamentos de doações, detalhe/edição/cancelamento, campanhas e detalhe, unidades e detalhe, perfil, configurações, edição de perfil, segurança, notificações, perguntas frequentes, contato e sobre.

A Home versão 1 foi deliberadamente excluída após a escolha da versão 2. A única adaptação fora do desenho original foi inserir a entrada **Configurações** no Perfil, decisão aprovada explicitamente pela responsável do produto.

Fluxo principal: `Splash → Boas-vindas → Login ou Cadastro → Home`. A barra inferior conecta `Início`, `Agendar`, `Doações`, `Unidades` e `Perfil`. Fluxos internos usam avanço e retorno em pilha; conclusões retornam à raiz correspondente.

## Limites atuais

Campos, seleções, chaves e botões dependentes de dados são visuais. Não existem autenticação, persistência, acesso à localização, envio de formulário, notificações ou integração externa. Estados reais de carregamento, vazio e erro serão acrescentados quando a camada funcional for conectada.

## Navegação implementada — 23/09/2026

| Arquivo em `ui/navigation` | Responsabilidade |
| --- | --- |
| `AppRoute.kt` / `AppGraph.kt` | Identificadores tipados de telas e grupos |
| `DoeSangueApp.kt` | Criar o controlador com `rememberNavController` |
| `AppNavHost.kt` | Conectar Splash/Auth/Main e controlar trocas de seção |
| `AuthNavGraph.kt` | Boas-vindas, login, cadastro e recuperação |
| `MainNavGraph.kt` | Home, doações, unidades, campanhas, perfil e acompanhamento |
| `SchedulingNavGraph.kt` | Fluxo visual para iniciar um agendamento, dentro de Main |
| `NavigationBack.kt` | Retorno da seta: tela anterior ou Home quando não houver anterior |

**Comportamentos implementados:**
- Splash sai do histórico ao abrir Auth; entrar em Main retira Auth.
- Finalizar o cadastro visual leva ao Login e retira as etapas do cadastro.
- Trocar de aba remove o percurso anterior. Não há pilhas independentes por aba.
- As setas de Unidades, Perfil, Histórico, Agendamentos e seleção de unidade usam o retorno seguro à Home. As telas internas retornam pela pilha.
- Ao concluir o agendamento visual, HomeScheduled substitui o percurso anterior de Main.
- No cancelamento visual, o detalhe e a confirmação de cancelamento saem da pilha; Voltar pode retornar à lista de origem.
- A restauração do estado salvo da navegação não equivale à persistência de dados.

**Pendente:** IDs nos detalhes, deep links, rascunho compartilhado por ViewModel, autenticação e operações reais. A simulação de sucesso/cancelamento não comprova nenhuma gravação.

Os testes instrumentados estão em `app/src/androidTest/java/br/edu/fatec/doesangue/ui/navigation/AppNavigationTest.kt`. O procedimento e seus limites estão em [validação](12-validacao.md).
