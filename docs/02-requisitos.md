# Doe Sangue — requisitos

**Sprint 0 — proposta para revisão.** `IMPLEMENTADO` significa comportamento comprovado no código; `PARCIAL/UI` significa tela ou navegação sem regra/dado funcional; `PLANEJADO` significa sem implementação. O rótulo MVP é uma proposta, não autorização para implementar. Referências de regras estão em [03](03-regras-de-negocio.md) e de casos em [04](04-casos-de-uso.md).

## Requisitos funcionais

| ID | Etapa / estado atual | Requisito testável | Regras / caso |
| --- | --- | --- | --- |
| RF001 | MVP / PARCIAL/UI | Autenticar o doador por uma conta antes de criar ou consultar agendamentos privados; sessão inválida não acessa esses dados. | RN001, RN002 / CU001 |
| RF002 | MVP / PARCIAL/UI | Após autenticação, consultar e exibir os dados mínimos do próprio perfil, sem expor o de outra pessoa. Campos, criação do perfil e recuperação de falha após cadastro Auth: **DECISÃO PENDENTE**. | RN002 / CU001 |
| RF003 | MVP / PARCIAL/UI | Listar apenas unidades publicadas com identidade, endereço e horário de atendimento informados pela fonte; abrir detalhe da unidade selecionada. | RN003 / CU002 |
| RF004 | MVP / PARCIAL/UI | Após escolher uma unidade, apresentar datas e horários disponíveis recebidos de fonte autorizada; indisponibilidade/falha não pode aparecer como horário reservável. | RN003, RN004 / CU003 |
| RF005 | MVP / PARCIAL/UI | Revisar unidade, data, hora e preferência de recorrência escolhidas e, após envio autenticado, persistir **uma solicitação/agendamento com ID e estado explícito**, conforme DP02, ou mostrar falha/conflito sem mensagem de sucesso. Registro aceito não significa reserva firme enquanto essa política não for aprovada. | RN004–RN007 / CU003 |
| RF006 | MVP condicionado / PARCIAL/UI | Permitir escolher “sem recorrência” ou uma preferência aprovada e associá-la separadamente ao agendamento inicial; a escolha isolada não gera reservas futuras. Opções e cadência: **DECISÃO PENDENTE**. | RN006 / CU003 |
| RF007 | MVP / PARCIAL/UI | Listar somente agendamentos do usuário autenticado com unidade, data/hora e estado persistidos; não derivar a lista da navegação recém-percorrida. | RN002, RN007 / CU004 |
| RF008 | MVP / PARCIAL/UI | Mostrar claramente que agendamento é planejado e não é uma doação concluída; sucesso da solicitação não altera estoque nem histórico de coleta. | RN007, RN009 / CU003, CU004 |
| RF009 | Pós-MVP / PARCIAL/UI | Alterar ou cancelar agendamento existente apenas segundo política aprovada, preservando histórico de transições e evitando conflito de horário. Prazos/autoridade: **DECISÃO PENDENTE**. | RN005, RN008 / CU005 |
| RF010 | Pós-MVP / PARCIAL/UI | Consultar histórico de doações efetivas **somente** quando houver registros de origem operacional autorizada; diferenciar registro declarado, se vier a existir. | RN009 / CU006 |
| RF011 | Pós-MVP / PARCIAL/UI | Consultar campanhas publicadas com período, origem, validade e eventual unidade vinculada. | RN010 / CU007 |
| RF012 | Pós-MVP / PARCIAL/UI | Exibir informações de estoque/necessidade por unidade e tipo sanguíneo somente com medida, origem e atualização verificáveis. | RN011 / CU008 |
| RF013 | Pós-MVP / PARCIAL/UI | Oferecer lembretes de agendamento ou rotina somente após definição de canal, consentimento, horário e cancelamento. A tela de chaves não comprova envio. | RN012 / CU009 |

Em RF003–RF013 a interface oferece representações estáticas, não os comportamentos descritos. A navegação entre telas é a única parte funcional desses fluxos hoje.

## Requisitos não funcionais

| ID | Estado atual | Critério verificável |
| --- | --- | --- |
| RNF001 | IMPLEMENTADO | Aplicativo Android em Kotlin e Jetpack Compose com componentes Material 3; versões e SDK documentados em `app/build.gradle.kts` e `gradle/libs.versions.toml`. |
| RNF002 | PARCIAL | UI, estado e dados devem ter responsabilidades distintas; ao integrar uma feature, a tela observa estado do ViewModel e não acessa Supabase diretamente. Hoje há camadas exemplificadas, mas não conectadas. |
| RNF003 | PLANEJADO | Ao integrar Supabase, nenhum segredo administrativo entra no APK ou no Git; políticas RLS negam acesso a dados privados alheios e escrita operacional pelo cliente do doador. |
| RNF004 | PLANEJADO | Cada consulta/gravação funcional apresenta carregamento, conteúdo/sucesso, vazio quando aplicável e erro com recuperação; hoje a UI não trata esses estados. |
| RNF005 | PARCIAL | Dados sintéticos devem ser rotulados como tais; a fonte demo contém esse marcador, mas os literais das telas não. Em produção, unidade, disponibilidade, campanha e estoque apresentam fonte/atualização ou não são mostrados como atuais. |
| RNF006 | PLANEJADO | Testes cobrem regras de agendamento, autorização, conflito e mapeamento de dados, além do build/lint; o teste atual apenas verifica `2 + 2 = 4`. |
| RNF007 | PLANEJADO | Armazenar apenas dados pessoais necessários, protegidos por Auth/RLS; política de retenção e textos legais: **DECISÃO PENDENTE**. |

## Restrições técnicas e escopo

- **Configurado:** Kotlin, Compose, Material 3, Lifecycle ViewModel, corrotinas, `compileSdk/targetSdk 36`, `minSdk 23`, Java 17, AGP 9.2.0. Navegação atual: `AppNavigator` próprio em memória.
- **Não configurado:** Navigation Compose e bibliotecas/cliente Supabase. A migração para Navigation Compose é proposta de arquitetura, não requisito para declarar a UI atual funcional.
- Não criar regras clínicas, “próxima data apta” ou frequências fixas a partir dos números escritos nas telas. Ver [RN013](03-regras-de-negocio.md).
