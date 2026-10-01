# Doe Sangue — visão do produto

**Sprint 0 — proposta para revisão (16/09/2026).** Este documento distingue intenção do produto de software existente. Os demais documentos desta série são [requisitos](02-requisitos.md), [regras](03-regras-de-negocio.md), [casos de uso](04-casos-de-uso.md), [domínio e banco](05-modelo-de-dominio.md), [arquitetura](06-arquitetura.md) e [roadmap do MVP](07-desenvolvimento_roadmap_MVP.md).

## Problema e visão

O doador precisa encontrar uma unidade, identificar um horário possível e acompanhar o que agendou sem confundir uma intenção de doar com uma doação realizada. A visão é concentrar essa jornada em um aplicativo Android claro, mantendo origem e atualização dos dados visíveis. O app **não determina aptidão clínica**, não substitui a triagem do hemocentro e não comprova uma coleta.

**Objetivo principal:** permitir que uma pessoa autenticada registre uma solicitação para unidade e horário oferecidos por fonte autorizada e depois consulte o registro e seu estado. O significado de confirmação ou reserva depende de DP02. **Público-alvo:** pessoas interessadas em doar sangue e acompanhar a própria jornada. **Proposta de valor:** organização e acesso a informações confiáveis em um só lugar, sem prometer disponibilidade, confirmação de coleta ou estoque em tempo real antes de existir integração autorizada.

O projeto também documenta a transição de Java/XML para Kotlin, Jetpack Compose e UI declarativa em um trabalho acadêmico/portfólio. Boa separação de responsabilidades importa; complexidade sem uso concreto, não.

## Enquadramento acadêmico confirmado

O professor aceitou o Doe Sangue no tema **Clínica/Hospital — atendimento humano** e sugeriu o uso do **Supabase** como backend. Essas decisões definem o enquadramento do projeto, mas não significam que a integração já exista. A entrega acadêmica deverá demonstrar o banco relacional, dados sintéticos, autenticação e operações reais descritos no enunciado da Aula 07; ver [banco de dados](09-banco-de-dados.md) e [roadmap](07-desenvolvimento_roadmap_MVP.md).

## Escopo proposto

| Etapa | Conteúdo | Condição |
| --- | --- | --- |
| **MVP proposto** | Conta/autenticação por e-mail/senha e Google; unidades publicadas; disponibilidade de data/horário; selecionar unidade → horário → escolher preferência de recorrência somente se aprovada → revisar → persistir solicitação com estado explícito → consultar os próprios registros. | Exige definir origem/autoria dos horários, estados e permissões antes de implementar. A preferência de recorrência não reserva datas futuras automaticamente. |
| **Pós-MVP candidato** | Alterar/cancelar com política aprovada; histórico de doações efetivamente confirmadas por fonte autorizada; campanhas e estoques publicados com procedência; lembretes/notificações; informações úteis com revisão de conteúdo. | Cada item depende de dados, autorização e critérios próprios. Telas existentes não bastam como comprovação. |
| **Fora do MVP** | Triagem/diagnóstico, cálculo automático de elegibilidade, confirmação de coleta pelo doador, atualização de estoque a partir de agendamento, painel operacional completo, mapa/geolocalização e mensagens de contato. | Não ampliar o MVP apenas porque há elementos visuais correspondentes. |

## Estado verificado no repositório

- **Implementado (atualizado em 23/09/2026):** app Android Kotlin/Compose/Material 3; tema e componentes; 34 rotas/telas visuais; Navigation Compose 2.9.8 com rotas tipadas e grafos Auth/Main/Scheduling; um recorte isolado de modelos, fonte sintética, repository, use case e `HomeViewModel` com `StateFlow`.
- **Parcial:** o fluxo de agendamento avança visualmente por unidade, data/horário, recorrência e confirmação; o botão de entrada leva à Home; a conclusão troca para uma Home com agendamento fictício. Isso **não** autentica, seleciona dados nem persiste agendamento. O `HomeViewModel` e a fonte demo não são consumidos pela UI.
- **Não implementado:** cliente Supabase, banco/migrações, autenticação real, consulta de disponibilidade, gravação/cancelamento de agendamento, histórico real, busca, notificações e integração com unidades. Há testes instrumentados dos fluxos visuais em `AppNavigationTest`; eles não demonstram regras de domínio, autenticação ou persistência. Ver [validação](12-validacao.md).

Nomes, endereços, distâncias, estoques, campanhas, datas, horários, perfil e doações apresentados nas telas são **literais fictícios de interface**; há ainda uma fonte demo separada, também sintética. Nenhum deles deve ser mostrado como dado real. Uma integração futura precisará de fonte identificada, data de atualização e autorização de publicação. Até lá, demonstrações devem indicar explicitamente a simulação.

## Limites e decisões pendentes

**DECISÃO PENDENTE:** quem publica unidades e horários, se o app pode efetuar uma reserva real ou apenas solicitar atendimento, qual política de recorrência é autorizada e se haverá uma fonte operacional para doações confirmadas. A lista consolidada de decisões está no [roadmap](07-desenvolvimento_roadmap_MVP.md#decisões-de-entrada-e-conclusão-da-sprint-1).
