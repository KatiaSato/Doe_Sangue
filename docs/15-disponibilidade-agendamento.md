# Disponibilidade de agendamento por unidade

Registro de decisão — 06/10/2026. A usuária aprovou a direção funcional e pediu seu registro na documentação. A implementação continuará em pequenos passos, com explicação de cada camada.

## Confirmado

**Permitir agendamento somente nas datas e nos horários disponibilizados para a unidade escolhida.**

- A disponibilidade deve ser configurada por unidade. Não bloquear sábados, domingos ou feriados de forma universal no aplicativo.
- A programação precisa permitir expediente diferente aos sábados, abertura excepcional aos domingos, fechamento em datas específicas e horários sem vagas.
- A demonstração usará somente unidades, horários e reservas sintéticos; não representa atendimento ou reserva em hemocentros reais.
- Selecionar uma data/horário preenche um rascunho. A reserva só poderá ser apresentada como concluída após a futura gravação retornar sucesso.
- A disponibilidade deve ser revalidada no servidor ao gravar, com controle de concorrência para impedir reservas acima da capacidade. Desabilitar opções na tela não garante a vaga.
- Em 07/10/2026, a usuária escolheu capacidade configurável: cada registro representa um horário de uma unidade em uma data específica e pode receber vários agendamentos até o limite definido. Capacidade total e quantidade de vagas restantes são conceitos distintos; a disponibilidade restante deverá ser calculada no servidor conforme os agendamentos que ocupam vaga.

## Implementado e validado até aqui

### Estado da integração Android — 07/10/2026

- A usuária implementou DonationTimeSlotDto, mapper com OffsetDateTime para Instant, DataSource, repository com Result e preservação de cancelamento, e injeção do caso de uso pelo AppContainer.
- SchedulingViewModel consulta ao escolher outra data, cancela a busca anterior e descarta resultados que já não correspondem à unidade/data. Trocar de unidade limpa as escolhas e horários. Seleção exige correspondência de unidade, data e hora com um registro consultado.
- ScheduleDateScreen substituiu a lista fixa por carregamento, erro com Tentar novamente, lista vazia ou horários do estado. Exibe o instante no fuso da unidade e impede Continuar durante carregamento/erro ou sem seleção.
- O relato de 08:00 aparecendo em outras datas revelou perda do limite superior na biblioteca Supabase Kotlin 3.6.0: PostgrestRequestBuilder usa mapToFirstValue no envio. A usuária agrupou gte/lt em and, preservando os dois limites da coluna inicio no pedido. A validação do ViewModel impedia selecionar o registro de outro dia, mas não corrigia a consulta.
- Os testes normais usam FakeDonationTimeSlotRepository em androidTest. Um teste remoto de leitura, explicitamente ativado, percorre os dias 18, 19 e 20 de outubro usando a consulta real. Resultados e limites em [validação](12-validacao.md).
- Ainda não há gravação de reserva, vagas restantes, bloqueio prévio de dias vazios no calendário nem ID do horário selecionado no rascunho. O título de demonstração permanece porque os dados são sintéticos.

### Histórico dos passos anteriores à ligação da consulta

- Em 07/10/2026, a usuária executou a migração `20261007000200_criar_horario_disponivel.sql` e mostrou `public.horario_disponivel` com RLS ativo em `pg_tables`. Aplicou `seed_horarios.sql` e confirmou as três ofertas sintéticas. Consultas com JOIN sob `anon` e `authenticated` retornaram somente Demo A às 08:00; SELECT permitido e INSERT/UPDATE/DELETE ausentes para ambos foram confirmados por `has_table_privilege`. A usuária também confirmou por relato o resultado esperado na consulta direta dos horários, sem JOIN, sob ambos os papéis. Constraints ainda não foram exercitadas. Evidências e limites em [validação](12-validacao.md).
- Em 07/10/2026, a usuária criou `DonationTimeSlot` em `domain/model`, com `id: String`, `centerId: String`, `startsAt: Instant` e `capacity: Int`; conteúdo conferido por inspeção. O modelo descreve um horário oferecido, sem garantir vaga restante, consultar banco ou persistir reservas. A conversão do instante para data/hora usará o fuso cadastrado na unidade; essa conversão ainda não foi implementada.
- Migração local `20261007000100_adicionar_fuso_unidade.sql` preparada e executada manualmente pela usuária no Supabase de desenvolvimento. A captura de 07/10/2026 mostra Demo A e Demo B com `fuso_horario = America/Sao_Paulo`, preservando `publicada` como true e false, respectivamente. O script impõe NOT NULL e remove o default após preencher as linhas existentes; essas propriedades do esquema ainda não foram verificadas por consulta remota. O seed local foi atualizado para informar o fuso explicitamente; sua nova versão ainda não foi executada.
- A usuária criou `DonationTimeSlotRepository` com a operação suspensa `getTimeSlots(centerId, fromInclusive, untilExclusive): Result<List<DonationTimeSlot>>`; assinatura conferida por inspeção. O contrato delimita a consulta por unidade e intervalo, mas ainda não possui implementação nem ligação à tela.
- `GetDonationTimeSlotsUseCase` foi criado e compilou em 07/10/2026; nome do arquivo conferido após correção. Recebe unidade e data, converte o início do dia e do dia seguinte em instantes pelo fuso da unidade e chama o contrato do repository. Captura somente `DateTimeException`, preservando cancelamento de corrotina. Ainda não está ligado ao ViewModel e não teve os limites de fuso exercitados em testes.
- A consulta de unidades usa o Supabase. A tabela `unidade_coleta` já existe; seu campo `horario_atendimento` é texto descritivo, não uma agenda estruturada.
- O Android recebe `fuso_horario` em `DonationCenterDto.timeZoneId` e o mapper copia o valor obrigatório para `DonationCenter.timeZoneId`. As fontes demo e fake dos testes informam o fuso explicitamente. `assembleDebug assembleDebugAndroidTest` passou em 07/10/2026; os testes instrumentados não foram executados nesta etapa. A usuária confirmou que a unidade carregou sem erro na consulta real com o DTO atualizado. A validação/conversão do identificador de fuso ainda será implementada.
- O calendário permite escolher uma data. `DonationDatePickerDialog` mantém a seleção temporária e a comunica somente ao confirmar; cancelar preserva a escolha anterior.
- `SchedulingViewModel` mantém unidade, `LocalDate` e `LocalTime` no rascunho em memória. Mudar de unidade limpa data e horário; mudar de data limpa o horário. Confirmar a mesma data preserva o horário.
- A tela permite selecionar horários de demonstração fixos e habilita Continuar após escolher unidade, data e horário. Esses horários ainda não vêm do banco nem variam conforme o dia.
- `onContinue` comunica a intenção de avançar à navegação, sem transportar data/horário. As escolhas já estão no ViewModel compartilhado pelo grafo de agendamento.
- A usuária confirmou manualmente o comportamento do calendário, da seleção de horário e da habilitação de Continuar. Os testes instrumentados foram atualizados para exigir data/horário e cobrir cancelamento do calendário, confirmação da mesma data, retorno à unidade e limpeza do horário ao mudar a data. Resultados em [validação](12-validacao.md).
- A revisão recebe o estado do ViewModel compartilhado e exibe unidade, data (`dd/MM/yyyy`) e horário (`HH:mm`) escolhidos. Valores ausentes recebem mensagens explicativas; Confirmar só fica habilitado quando os três campos estão preenchidos. Implementado em 07/10/2026, com compilação aprovada e exibição confirmada pela usuária.
- A etapa de recorrência informa que a funcionalidade ainda não está disponível e oferece somente este agendamento. A confirmação ainda navega para uma tela de sucesso com conteúdo fixo; não há gravação de agendamento nem recuperação do rascunho após morte do processo.

## Proposto para a demonstração

Exemplo de programação fictícia, ainda sem seed ou regra implementada:

| Dia | Programação de exemplo |
| --- | --- |
| Segunda a sexta | Horários pela manhã e à tarde. |
| Sábado | Horários somente pela manhã. |
| Domingo | Nenhum horário, salvo abertura especial cadastrada. |

Esses valores servem para testar a configuração de uma unidade; não são uma regra médica nem uma programação aplicável a todas as unidades.

Começar pela modelagem de `HorarioDisponivel` vinculado a `UnidadeColeta`. A agenda sintética será mantida no nosso banco. Definir o esquema e o contrato antes de substituir a lista fixa da tela. Uma grade semanal e suas exceções podem ajudar a gerar horários por data, mas a necessidade de tabelas separadas para isso ainda será avaliada.

O seed mínimo já foi criado e aplicado: três ofertas sintéticas em 19/10/2026 — Demo A às 08:00 ativa (capacidade 3), Demo A às 09:30 inativa (capacidade 2) e Demo B às 08:00 ativa (capacidade 1). Usa o fuso da unidade e evita duplicação por unidade/início, sem atualizar ofertas existentes. Ele ainda não cobre a grade semanal proposta acima. DTO, mapper, fonte de dados e repository já estão ligados à tela; próximas etapas incluem conservar a identidade do horário escolhido e planejar autenticação/persistência com capacidade validada no servidor.

## Responsabilidade de cada camada — implementação planejada

| Camada | Responsabilidade |
| --- | --- |
| `domain/` | Representar o horário disponível e o contrato de consulta; manter regras independentes de Compose e Supabase. |
| `data/` | Consultar a disponibilidade da unidade no Supabase, converter DTOs para modelos e implementar o contrato do repository. Manter dados demo separados. |
| `di/` | Montar e fornecer as dependências ao ViewModel. |
| `presentation/` | Consultar pelo repository, expor carregamento/erro/disponibilidade e validar ou limpar escolhas que deixaram de ser válidas. |
| `ui/` | Exibir os dados do estado, desabilitar datas sem disponibilidade conhecida e comunicar escolhas por callbacks. Falha de consulta deve aparecer como erro, sem ser confundida com agenda vazia. |
| Servidor/banco | Guardar a programação, proteger leitura/escrita e validar unidade, horário, capacidade e permissão na operação de reserva atômica. RLS não substitui controle de capacidade. |

Consulta de horários: tela → ViewModel → caso de uso (data/fuso para intervalo) → repository → fonte Supabase/API → banco.

Resposta: banco/API → DTO → modelo → repository → estado do ViewModel → calendário e horários.

Na futura confirmação: tela → ViewModel → repository → operação no servidor → validação e gravação → resultado para a tela.

## Pendente para ampliar a disponibilidade e gravar reservas

- Fechar campos, FKs, unicidade, limites da capacidade e estados dos agendamentos que ocupam vaga. A escolha de horário com capacidade configurável já está confirmada; a regra para reduzir capacidade abaixo das reservas existentes também precisa ser definida.
- Ampliar os testes de conversão entre data/hora local e instante, já implementada com o fuso da unidade, para identificadores inválidos e transições de horário de verão. O UTC usado pelo DatePicker é uma convenção do componente, não o fuso do atendimento.
- Definir período consultável, tratamento de datas/horários passados e antecedência para agendar, sem inventar critérios clínicos.
- Definir como cadastrar fechamentos, feriados e aberturas especiais e como representar horários esgotados.
- Definir permissões operacionais, consulta pública e reserva vinculada ao usuário autenticado. O doador não poderá editar a disponibilidade.
- Criar migrações e seeds adicionais para reservas e demais entidades após fechar a modelagem; as migrações e seeds de unidades/horários já estão versionados. Este registro não aplica nem autoriza automaticamente mudanças remotas.
- Ampliar os testes quando existir disponibilidade estruturada: sábado, domingo excepcional, fechamento, agenda vazia, falha de consulta, mudança de unidade e concorrência na reserva. A exigência de data/horário e a mudança de data já possuem cobertura no fluxo local.

## Referências que motivam a configuração por unidade

Consultadas em 06/10/2026, apenas como exemplos da variação de atendimento:

- [Postos e horários do Hemocentro Unicamp](https://www.hemocentro.unicamp.br/seja-um-doador/doacao-de-sangue/): lista unidades com atendimento aos sábados e horários diferentes.
- [Abertura especial no domingo de 27/09/2026](https://www.hemocentro.unicamp.br/noticias/hemocentro-unicamp-realiza-dia-d-de-doacao-de-sangue-neste-domingo-27-09/): exemplo de campanha em data excepcional; não comprova atendimento em todos os domingos.

Ver também [modelo de domínio](05-modelo-de-dominio.md), [banco de dados](09-banco-de-dados.md) e [integração de unidades](14-integracao-unidades.md).
