# Telas e navegação

## Referências confirmadas

Nome visual: **Doe Sangue**. Frase: **Um pequeno ato que salva vidas**. Figma fornecido: arquivo “Doe Sangue — Android App — Education”, nó `9:8`.

## Implementado

Foram implementadas as 34 telas selecionadas do Figma: splash, boas-vindas, login, recuperação, verificação de e-mail, cadastro em quatro etapas, Home versão 2 e Home com agendamento, seleção de unidade/data/recorrência, confirmação e sucesso, histórico e agendamentos de doações, detalhe/edição/cancelamento, campanhas e detalhe, unidades e detalhe, perfil, configurações, edição de perfil, segurança, notificações, perguntas frequentes, contato e sobre.

A Home versão 1 foi deliberadamente excluída após a escolha da versão 2. A única adaptação fora do desenho original foi inserir a entrada **Configurações** no Perfil, decisão aprovada explicitamente pela responsável do produto.

Fluxo principal: `Splash → Boas-vindas → Login ou Cadastro → Home`. A barra inferior conecta `Início`, `Agendar`, `Doações`, `Unidades` e `Perfil`. Fluxos internos usam avanço e retorno em pilha; conclusões retornam à raiz correspondente.

## Limites atuais

Campos, seleções, chaves e botões dependentes de dados são visuais. Não existem autenticação, persistência, acesso à localização, envio de formulário, notificações ou integração externa. Estados reais de carregamento, vazio e erro serão acrescentados quando a camada funcional for conectada.
