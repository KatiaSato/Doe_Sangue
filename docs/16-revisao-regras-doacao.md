# Revisão das orientações de doação — 06/10/2026

## Confirmado

A usuária solicitou pesquisa em fontes oficiais, implementação das mudanças, commit e push. A Portaria GM/MS nº 11.685, de 2 de julho de 2026, tem vigência em 30/09/2026 segundo a Anvisa. O conteúdo do Android foi revisado após essa entrada em vigor.

As telas são informativas. Não calculam aptidão, data de liberação ou prazo individual, nem orientam suspensão de tratamento. O serviço de hemoterapia avalia o candidato. A revisão não cria coleta de dados clínicos pessoais, tabelas ou migrações.

## Implementado

- `RestrictionContent.kt`: faixa etária com a possibilidade de continuidade após o limite geral mediante avaliação médica; primeira doação em item próprio; orientações de procedimentos com as condições aplicáveis; itens de PrEP/PEP e injetáveis GLP-1, mantendo a orientação de preservar o tratamento.
- `RestrictionScreens.kt`: fontes atualizadas e data de revisão visível. A data indica revisão editorial, não atualização automática nem validade clínica individual.
- As orientações gerais de doenças/sintomas permanecem sem classificação automática. Não se tentou reproduzir toda a norma ou todos os impedimentos em um catálogo exaustivo.
- `SchedulingScreens.kt`: retirada da cadência fixa como escolha recomendada. A etapa oferece somente este agendamento e informa que recorrência ainda não está disponível. Revisão e cartões não exibem rotina trimestral. A edição informa a indisponibilidade, sem simular gravação de recorrência.
- `DonationScreens.kt`: retirada de datas futuras sugeridas pelo protótipo e indicação de ausência de lembrete de recorrência.
- Testes de restrições atualizados para as fontes, rolagem até a data de revisão e preservação das condições nos textos. Teste de conclusão do agendamento adaptado à exigência de data/horário; teste adicional cobre cancelamento do calendário, manutenção do rascunho e limpeza do horário ao trocar de data.

## Referências e rastreabilidade

Consultadas em 06/10/2026:

| Referência oficial | Uso na revisão |
| --- | --- |
| [Anvisa — vigência e transição normativa](https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2026/ministerio-da-saude-atualiza-procedimentos-hemoterapicos-e-anvisa-orienta-periodo-de-transicao) | Data de vigência e distinção entre portaria e regulação sanitária complementar. |
| [Portaria no repositório do Ministério da Saúde](https://bvsms.saude.gov.br/bvs/saudelegis/gm/2026/prt11685_03_07_2026.html) | Anexo IV-B: arts. 38–40 (frequência, idade e peso), 65–67 (procedimentos e exposições); anexos 2 e 3 (procedimentos e medicamentos). |
| [Hemominas — comunicado de 29/09/2026](https://portal.hemominas.mg.gov.br/hemominas-adota-novos-criterios-para-doacao-de-sangue) | Explicação pública das condições de procedimentos estéticos, piercings e endoscopia e dos cuidados com injetáveis GLP-1. |
| [Hemocentro Unicamp — critérios](https://www.hemocentro.unicamp.br/perguntas-frequentes/criterios-para-doacao-de-sangue/) | Fonte mantida na categoria geral de doenças e sintomas. |

A reprodução da portaria na BVS informa que não substitui o Diário Oficial. O acesso direto ao DOU falhou na ferramenta de pesquisa; foram utilizados o repositório oficial do Ministério e os comunicados da Anvisa e Hemominas. Resumos de notícias não devem substituir condições e exceções da norma.

## Limites e pendências

- Figma remoto não alterado. Os arquivos em `docs/figma/restricoes` são registros históricos do desenho anterior, não o conteúdo editorial atual do Android.
- Recorrência continua pendente de definição. Os intervalos gerais da norma não justificam uma recomendação universal ou uma confirmação de aptidão pelo app; preservar RN013.
- Calendário, horários sintéticos e disponibilidade de unidade são distintos de triagem clínica. A agenda do banco ainda será implementada conforme [decisão de disponibilidade](15-disponibilidade-agendamento.md).
- Revisão, sucesso, histórico e outros conteúdos de demonstração ainda não representam reservas ou coletas persistidas. Esta alteração não conclui essas funcionalidades.
- Revalidar fontes antes de novas mudanças editoriais; não converter prazos em cálculos automáticos sem nova decisão explícita e validação da política.
- Resultados da execução dos testes em [validação](12-validacao.md).
