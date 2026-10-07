# Disponibilidade de agendamento por unidade

Registro de decisão — 06/10/2026. A usuária aprovou a direção funcional e pediu seu registro na documentação. A implementação continuará em pequenos passos, com explicação de cada camada.

## Confirmado

**Permitir agendamento somente nas datas e nos horários disponibilizados para a unidade escolhida.**

- A disponibilidade deve ser configurada por unidade. Não bloquear sábados, domingos ou feriados de forma universal no aplicativo.
- A programação precisa permitir expediente diferente aos sábados, abertura excepcional aos domingos, fechamento em datas específicas e horários sem vagas.
- A demonstração usará somente unidades, horários e reservas sintéticos; não representa atendimento ou reserva em hemocentros reais.
- Selecionar uma data/horário preenche um rascunho. A reserva só poderá ser apresentada como concluída após a futura gravação retornar sucesso.
- A disponibilidade deve ser revalidada no servidor ao gravar, com controle de concorrência para impedir reservas acima da capacidade. Desabilitar opções na tela não garante a vaga.

## Implementado e validado até aqui

- A consulta de unidades usa o Supabase. A tabela `unidade_coleta` já existe; seu campo `horario_atendimento` é texto descritivo, não uma agenda estruturada.
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

## Responsabilidade de cada camada — implementação planejada

| Camada | Responsabilidade |
| --- | --- |
| `domain/` | Representar o horário disponível e o contrato de consulta; manter regras independentes de Compose e Supabase. |
| `data/` | Consultar a disponibilidade da unidade no Supabase, converter DTOs para modelos e implementar o contrato do repository. Manter dados demo separados. |
| `di/` | Montar e fornecer as dependências ao ViewModel. |
| `presentation/` | Consultar pelo repository, expor carregamento/erro/disponibilidade e validar ou limpar escolhas que deixaram de ser válidas. |
| `ui/` | Exibir os dados do estado, desabilitar datas sem disponibilidade conhecida e comunicar escolhas por callbacks. Falha de consulta deve aparecer como erro, sem ser confundida com agenda vazia. |
| Servidor/banco | Guardar a programação, proteger leitura/escrita e validar unidade, horário, capacidade e permissão na operação de reserva atômica. RLS não substitui controle de capacidade. |

Consulta: tela → ViewModel → repository → fonte Supabase/API → banco.

Resposta: banco/API → DTO → modelo → repository → estado do ViewModel → calendário e horários.

Na futura confirmação: tela → ViewModel → repository → operação no servidor → validação e gravação → resultado para a tela.

## Pendente antes da implementação no banco

- Definir se cada registro representa uma vaga individual ou um horário com capacidade para várias pessoas; fechar campos, FKs, unicidade e estados.
- Definir o fuso da unidade e a conversão entre data/hora local e instante persistido. O UTC usado pelo DatePicker é uma convenção do componente, não uma decisão sobre o fuso do atendimento.
- Definir período consultável, tratamento de datas/horários passados e antecedência para agendar, sem inventar critérios clínicos.
- Definir como cadastrar fechamentos, feriados e aberturas especiais e como representar horários esgotados.
- Definir permissões operacionais, consulta pública e reserva vinculada ao usuário autenticado. O doador não poderá editar a disponibilidade.
- Criar migração e seed sintéticos versionados após fechar a modelagem. Este registro não aplica nem autoriza automaticamente mudanças remotas.
- Ampliar os testes quando existir disponibilidade estruturada: sábado, domingo excepcional, fechamento, agenda vazia, falha de consulta, mudança de unidade e concorrência na reserva. A exigência de data/horário e a mudança de data já possuem cobertura no fluxo local.

## Referências que motivam a configuração por unidade

Consultadas em 06/10/2026, apenas como exemplos da variação de atendimento:

- [Postos e horários do Hemocentro Unicamp](https://www.hemocentro.unicamp.br/seja-um-doador/doacao-de-sangue/): lista unidades com atendimento aos sábados e horários diferentes.
- [Abertura especial no domingo de 27/09/2026](https://www.hemocentro.unicamp.br/noticias/hemocentro-unicamp-realiza-dia-d-de-doacao-de-sangue-neste-domingo-27-09/): exemplo de campanha em data excepcional; não comprova atendimento em todos os domingos.

Ver também [modelo de domínio](05-modelo-de-dominio.md), [banco de dados](09-banco-de-dados.md) e [integração de unidades](14-integracao-unidades.md).
