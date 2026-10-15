# Disponibilidade de agendamento por unidade

Registro de decisão — 06/10/2026. A usuária aprovou a direção funcional e pediu seu registro na documentação. A implementação continuará em pequenos passos, com explicação de cada camada.

## Cancelamento no servidor — 14/10/2026

**Evolução posterior no Android:** a usuária integrou o cancelamento ao detalhe por ID, confirmação, bloqueio de saída durante envio, erro/nova tentativa e atualização da lista ao retomar. Vinte testes focados passaram com repositories fictícios, LintDebug com 31 avisos/zero erros. Roteiro real: reserva temporária 39 cancelada no aplicativo, lista preservando 23 Agendado e evento/autor/instante de 39 conferidos administrativamente. Limpeza confirmou oferta 24/reserva 39/evento removidos e 23 preservada. Não repetir os roteiros; JWT/tráfego e perda real de resposta não foram inspecionados. O parágrafo seguinte conserva o checkpoint anterior do banco.

A usuária aprovou cancelamento próprio antes do início, sem antecedência adicional, e implementou/aplicou `cancelar_agendamento(bigint)` com registro em `agendamento_cancelamento`. Prazo pelo relógio do servidor após bloqueios perfil → horário → reserva; estado/evento na mesma transação, repetição preservando o resultado e liberação da ocupação ao deixar `agendado`. Capturas confirmam regras/acesso/atomicidade, isolamento rejeitado e concorrência A-cancelamento/B-criação com uma vaga. Limpeza e massa final conferidas: sete registros originais, sem eventos persistentes do teste, reserva 23 agendada. São identidades administrativas simuladas; integração Android e JWT pela API ainda pendentes. Ver [checkpoint](checkpoints/2026-10-14-cancelamento-supabase.md) e [validação](12-validacao.md). Não reaplicar a migração nem repetir o cenário histórico.

## Confirmado

**Permitir agendamento somente nas datas e nos horários disponibilizados para a unidade escolhida.**

- A disponibilidade deve ser configurada por unidade. Não bloquear sábados, domingos ou feriados de forma universal no aplicativo.
- A programação precisa permitir expediente diferente aos sábados, abertura excepcional aos domingos, fechamento em datas específicas e horários sem vagas.
- A demonstração usará somente unidades, horários e reservas sintéticos; não representa atendimento ou reserva em hemocentros reais.
- Selecionar uma data/horário preenche um rascunho. A reserva só poderá ser apresentada como concluída após a futura gravação retornar sucesso.
- A disponibilidade deve ser revalidada no servidor ao gravar, com controle de concorrência para impedir reservas acima da capacidade. Desabilitar opções na tela não garante a vaga.
- Em 07/10/2026, a usuária escolheu capacidade configurável: cada registro representa um horário de uma unidade em uma data específica e pode receber vários agendamentos até o limite definido. Capacidade total e quantidade de vagas restantes são conceitos distintos; a disponibilidade restante deverá ser calculada no servidor conforme os agendamentos que ocupam vaga.

## Integração da confirmação no Android — 12/10/2026

Atualização de 13/10/2026: a usuária ligou a consulta de agendamentos ao estado/ViewModel, fábrica e navegação. A listagem representa carregamento, erro/nova tentativa, vazio e cartões com dados retornados, convertendo o início pelo fuso da unidade e preservando o estado da reserva. Seis testes instrumentados com repositories fictícios passaram, incluindo troca de conta e cancelamento de consulta ao sair. Três capturas do roteiro manual com sessões reais confirmam A vendo a reserva 23, B recebendo vazio e o retorno para A mostrando 23 com os mesmos dados. Não houve inspeção de JWT/tráfego nem auditoria completa da API.

Depois, o detalhe por ID foi implementado em pequenos passos, precedido pelo [diagrama da etapa](18-diagrama-sequencia-detalhe.md). O novo destino reutiliza a consulta própria e procura o ID recebido pela rota. Sete testes do detalhe, seis de listagem e dez de navegação passaram no Pixel_5/Android 12, com dados fictícios. Captura confirma o detalhe de 23 com unidade/data/hora/estado esperados; retorno pela seta à lista confirmado por relato. Cancelamento persistido, alteração e recorrência permanecem pendentes. Ver [validação](12-validacao.md). O relato abaixo registra a etapa anterior de 12/10.

Para a etapa posterior de listagem, a usuária preparou AppointmentSummary, DTO/mapper, contrato e chamada RPC `consultar_meus_agendamentos`; aplicativo e testes compilaram. A função foi aplicada manualmente conforme relato. Capturas confirmam configuração/permissões, leitura própria A/B e formato completo, retorno vazio e negações, leitura de reservas próprias com oferta oculta/passada/cancelada sem ampliar a consulta pública. Reserva 23 preservada; os testes simulam identidades administrativamente e usam ROLLBACK. Conferência final confirma sete registros do domínio (2 unidades, 3 horários, 1 perfil, 1 agendamento), sem unidades temporárias nem outras reservas no horário de 23. Etapa SQL concluída nesse recorte; ViewModel/tela e JWT pela API permanecem pendentes. Evidências em [validação](12-validacao.md) e retomada em [checkpoint de leitura](checkpoints/2026-10-12-consulta-agendamentos.md).

- Implementado localmente: DTO/mapper do agendamento, chamada RPC `criar_agendamento`, repository com preservação de cancelamento e injeção pelo AppContainer. O DataSource envia somente horário e chave; identidade, capacidade e gravação continuam sob responsabilidade do servidor. Exige sessão e exatamente um registro na resposta.
- A usuária integrou `confirmAppointment` ao ViewModel e ao botão Confirmar, em pequenos passos. A escolha precisa corresponder à unidade/data/lista atuais. O estado representa gravação pendente, erro ou registro retornado; a navegação observa o resultado sem levar NavController ao ViewModel.
- A chave UUID é criada uma vez por pedido e preservada nas tentativas após erro, inclusive quando a gravação pode ter ocorrido antes da perda da resposta. Alterar unidade/data/horário ou recarregar a consulta reinicia o pedido local. Durante a gravação, alterações e novos envios são bloqueados; a revisão impede Voltar.
- Apenas `SCHEDULED` abre sucesso e remove a revisão do histórico. `CANCELLED` permanece na revisão com mensagem e botão desabilitado; para iniciar outra intenção é preciso retornar e alterar a seleção. A tela de sucesso mostra ID retornado e unidade/data/hora do rascunho. Não promete lembretes automáticos.
- Esta seção substitui as pendências de integração Android dos checkpoints históricos abaixo. Evidências em [validação](12-validacao.md): após a confirmação pelo aplicativo, a usuária informou ID 23 e mostrou consulta administrativa com a reserva de Doador Demo A, Demo A, 19/10/2026 às 08:00, estado agendado. Uma segunda intenção para o mesmo doador/horário permaneceu na revisão com erro; consulta posterior mostrou somente a reserva 23, preservada. Criação persistida e ausência de duplicação nesse cenário foram confirmadas; isolamento A/B pela API, recuperação real com a mesma chave e capacidade no percurso Android ainda precisam de validação. Os testes fictícios não comprovam RLS ou capacidade no Supabase.
- A chave e o rascunho permanecem somente na memória do ViewModel: recuperação após morte do processo é pendente. Listagem/detalhe/cancelamento do aplicativo e recorrência ainda são visuais; não representam operações persistidas.

## Função de reserva — 12/10/2026

- `supabase/migrations/20261012000100_criar_funcao_agendamento.sql` aplicada manualmente, conforme relato de sucesso. Não reaplicar. SECURITY DEFINER, search_path vazio, volatilidade e privilégios conferidos por captura/relato; execução concedida somente a authenticated. INSERT com RETURN QUERY/RETURNING define identidade/estado no servidor; ID/criado_em vêm dos defaults. Criação, idempotência, negações, cancelados e concorrência no cenário A/B-capacidade 1 foram exercitados nos roteiros manuais; detalhes e limites em docs/12-validacao.md. Limpeza confirmada por captura; Android ainda não chama a função. Retomada em docs/checkpoints/2026-10-12-reserva-supabase.md.
- VOLATILE explícito e exigência de READ COMMITTED permitem atualizar a contagem depois de aguardar o bloqueio. Captura confirma REPEATABLE READ e SERIALIZABLE rejeitados. No teste simultâneo, A observou B aguardando seu bloqueio; após o COMMIT de A, B recebeu falta de vagas. Não extrapolar para todas as combinações de concorrência ou autenticação pela API.
- Etapas conferidas por leitura: identidade pela sessão, parâmetros obrigatórios, perfil próprio com FOR UPDATE e recuperação por doador/chave de idempotência. Reutilizar a chave para outro horário causa falha. Retorno de um registro existente preserva seu estado atual, inclusive cancelado; um novo pedido precisa de nova chave.
- A busca da oferta ativa de unidade publicada já está no rascunho, com FOR UPDATE OF horario e FOR SHARE OF unidade, mantendo essas linhas protegidas até o fim da transação. Para nova reserva, exige início futuro comparado a clock_timestamp() e rejeita outro agendamento ativo do próprio doador. Conta todos os registros agendados do horário e rejeita ocupação maior ou igual à capacidade; cancelados ficam fora da contagem. Recuperação pela mesma chave acontece antes dessas validações, preservando o registro existente. Permissões finais também estão no arquivo local. Captura do roteiro manual confirma criação e repetição idempotente com somente uma reserva, usando oferta temporária com capacidade 1 e ROLLBACK. Capturas posteriores confirmam negações, cancelados e disputa simultânea A/B por uma vaga; evidências e limites em docs/12-validacao.md. Captura final confirma oferta 13/reserva 22 removidas. Próximo passo: integração da gravação ao Android, em pequenos passos; não reaplicar a migração.

## Implementado e validado até aqui

### Identidade do horário selecionado — 08/10/2026

- `SchedulingUiState` conserva `selectedTimeSlotId` no rascunho. `selectedTime` permanece para exibir a hora local da unidade.
- O clique envia `slot.id` por `onSelectTimeSlot`; a navegação liga o callback a `SchedulingViewModel.selectTimeSlot`. O ViewModel localiza o registro na lista consultada e valida unidade/data antes de atualizar ID e hora juntos. Não aceita seleção durante carregamento ou erro.
- Troca de unidade, troca de data e nova consulta limpam ID e hora. Os botões Continuar e Confirmar exigem o ID preenchido; o chip identifica a seleção pelo ID.
- A usuária confirmou o percurso manual: Demo A em 19/10/2026 às 08:00 habilita Continuar; 20/10 fica vazio e desabilitado; retornar a 19/10 exige nova seleção; a revisão mostra os dados e habilita Confirmar. Resultados e limites em [validação](12-validacao.md).
- Ainda não há gravação de reserva, cálculo de vagas restantes ou bloqueio prévio de dias vazios. O ID guardado prepara a futura referência ao registro no banco; a confirmação permanece visual.

### Estado da integração Android — 07/10/2026

- A usuária implementou DonationTimeSlotDto, mapper com OffsetDateTime para Instant, DataSource, repository com Result e preservação de cancelamento, e injeção do caso de uso pelo AppContainer.
- SchedulingViewModel consulta ao escolher outra data, cancela a busca anterior e descarta resultados que já não correspondem à unidade/data. Trocar de unidade limpa as escolhas e horários. Seleção exige correspondência de unidade, data e hora com um registro consultado.
- ScheduleDateScreen substituiu a lista fixa por carregamento, erro com Tentar novamente, lista vazia ou horários do estado. Exibe o instante no fuso da unidade e impede Continuar durante carregamento/erro ou sem seleção.
- O relato de 08:00 aparecendo em outras datas revelou perda do limite superior na biblioteca Supabase Kotlin 3.6.0: PostgrestRequestBuilder usa mapToFirstValue no envio. A usuária agrupou gte/lt em and, preservando os dois limites da coluna inicio no pedido. A validação do ViewModel impedia selecionar o registro de outro dia, mas não corrigia a consulta.
- Os testes normais usam FakeDonationTimeSlotRepository em androidTest. Um teste remoto de leitura, explicitamente ativado, percorre os dias 18, 19 e 20 de outubro usando a consulta real. Resultados e limites em [validação](12-validacao.md).
- Neste checkpoint ainda não havia ID do horário selecionado no rascunho; ele foi acrescentado em 08/10/2026, conforme seção acima. O título de demonstração permanece porque os dados são sintéticos.

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

O seed mínimo já foi criado e aplicado: três ofertas sintéticas em 19/10/2026 — Demo A às 08:00 ativa (capacidade 3), Demo A às 09:30 inativa (capacidade 2) e Demo B às 08:00 ativa (capacidade 1). Usa o fuso da unidade e evita duplicação por unidade/início, sem atualizar ofertas existentes. Ele ainda não cobre a grade semanal proposta acima. DTO, mapper, fonte de dados e repository já estão ligados à tela, e o rascunho conserva a identidade do horário escolhido. Próximas etapas incluem planejar autenticação/persistência com capacidade validada no servidor.

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
