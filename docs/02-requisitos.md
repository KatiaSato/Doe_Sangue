# Requisitos

## Funcionais confirmados

| ID | Requisito | Critério de aceitação resumido |
| --- | --- | --- |
| RF-01 | Conta e perfil | Doador consulta e altera somente dados próprios permitidos. |
| RF-02 | Consultar unidades | Exibir nome, endereço, horário e contato. |
| RF-03 | Consultar necessidade | Exibir tipo, medida, unidade, origem e atualização. |
| RF-04 | Agendar doação | Criar solicitação sem confirmar coleta. |
| RF-05 | Acompanhar status | Exibir situação e mudanças autorizadas. |
| RF-06 | Histórico | Separar registro operacional de informação declarada. |
| RF-07 | Campanhas | Listar campanhas e detalhes públicos. |
| RF-08 | Recorrência | Pendente de definição; não implementar automaticamente. |

## Não funcionais

| ID | Requisito |
| --- | --- |
| RNF-01 | Android nativo em Kotlin e Compose. |
| RNF-02 | MVVM com Repository, separando UI, estado e dados. |
| RNF-03 | Credenciais no Supabase Auth; nenhum segredo administrativo no cliente. |
| RNF-04 | Minimizar dados pessoais e usar somente seed sintético. |
| RNF-05 | Telas futuras representam carregamento, vazio, conteúdo e erro. |

## Implementado

RNF-01 parcialmente: projeto Compose criado. Os RFs ainda não foram implementados.

