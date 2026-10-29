# Refinamentos após a avaliação do professor — proposta visual

Em 29/10/2026, Katia informou que o projeto foi aceito pelo professor e trouxe sugestões de busca, filtros e organização. É um aceite relatado pela usuária; não representa conclusão de Google, notificações ou validação adicional de todos os fluxos.

Esta etapa produz **somente propostas no Figma**, em uma página nova do arquivo existente. Nenhuma implementação Android, configuração de Auth, consulta remota, migração, seed, alteração de dados ou publicação Git foi feita nesta etapa.

## Local da proposta

Arquivo: **Doe Sangue — Android App — Education**, `OyI66BkEa9qPpGZ1mNJIWj`.

Página nova: **Refinamentos — avaliação professor**, `61:649`.

- [Agendamentos e filtros](https://www.figma.com/design/OyI66BkEa9qPpGZ1mNJIWj?node-id=61-1220)
- [Lembretes na terceira aba](https://www.figma.com/design/OyI66BkEa9qPpGZ1mNJIWj?node-id=61-1615)
- [Agendar com busca de unidade/cidade](https://www.figma.com/design/OyI66BkEa9qPpGZ1mNJIWj?node-id=61-1710)
- [Home com atalhos de estoque por nível](https://www.figma.com/design/OyI66BkEa9qPpGZ1mNJIWj?node-id=66-547)
- [Estoque agrupado por unidade](https://www.figma.com/design/OyI66BkEa9qPpGZ1mNJIWj?node-id=66-686)

As páginas **Proposta — Restrições** e **Proposta — Início** foram preservadas. A Home aprovada original, `33:56`, continua com 390 × 1010 e com o bloco original de tipos sanguíneos. A nova Home é uma cópia de proposta, não uma substituição.

## Decisões e limites pedidos pela usuária

A referência inicial repetia Unidades. Após esclarecimento, a busca por **nome da unidade ou cidade** foi solicitada para **Agendar, Agendamentos e Estoque de sangue**. A tela Unidades já pesquisa nome/cidade; não precisa receber outra busca equivalente.

Katia ressaltou que não devemos mudar muito agora, pois o banco já está pronto. A proposta mantém tabelas, estados persistidos, relacionamentos, permissões e regras de reserva. A implementação, se aprovada, deve priorizar transformação e filtragem dos resultados existentes no aplicativo.

### Agendamentos

- Abas Histórico, Agendamentos e **Lembretes**, com a rotina fora do final da lista.
- Busca por nome/cidade combinada com Todos, Agendados, Cancelados e Remarcados.
- Cartões compactos: dia/mês em destaque, horário, unidade, cidade e situação em texto e cor.
- Cartão como acesso aos detalhes, removendo a repetição de botões grandes “Ver detalhes”.
- Próximos primeiro; registros anteriores continuam na lista com rolagem. O exemplo Todos possui sete cartões, não apenas os três visíveis inicialmente.
- Remarcado é a apresentação do registro anterior ligado à reserva substituta. Não propõe um terceiro estado persistido. A reserva nova aparece em Agendados.

### Lembretes

A terceira aba apresenta a rotina atual, próxima data do planejamento, intervalo, editar, pausar e excluir. Preserva a distinção entre planejamento pessoal e agendamento/aptidão. A proposta não passa a entregar notificações; essa integração continua pendente e acompanhada.

### Agendar

Busca na etapa de escolha da unidade, antes de data/horário. Exemplos mostram lista inicial e resultado por cidade. Não altera disponibilidade, capacidade, confirmação, idempotência ou remarcação.

### Estoque de sangue

A ideia da usuária foi trocar o resumo por tipos sanguíneos na Home por três atalhos: **Crítico, Alerta e Estável**. Cada atalho abre a consulta com o nível correspondente selecionado.

Na consulta, os tipos desse nível ficam agrupados dentro de cada unidade, com nome, cidade, atualização e fonte. Há busca por unidade/cidade e indicação de filtro por tipo sanguíneo. O acesso à unidade e seus horários conecta a consulta ao agendamento sem acrescentar uma classificação global à unidade.

Uma unidade pode ter um tipo crítico e outro estável. Portanto, pode aparecer em mais de um nível. Os atalhos não afirmam que toda a unidade tem uma única criticidade e não representam contagens de bolsas, quantidade disponível ou avaliação clínica.

Os textos, datas, combinações de tipos e resultados no Figma são **exemplos sintéticos de interface**, não uma leitura atual do banco.

## Telas editáveis

| Nº | Exemplo | Node ID |
| --- | --- | --- |
| 01 | Agendamentos — Todos, com rolagem | `61:1220` |
| 02 | Cancelados | `61:1364` |
| 03 | Remarcados | `61:1501` |
| 04 | Lembretes — rotina ativa | `61:1615` |
| 05 | Agendar — escolha da unidade | `61:1710` |
| 06 | Agendar — busca por cidade | `61:1820` |
| 07 | Agendamentos — busca e situação combinadas | `61:1909` |
| 08 | Estoque — Crítico | `66:686` |
| 09 | Estoque — busca por cidade | `66:805` |
| 10 | Home — atalhos por nível | `66:547` |
| 11 | Estoque — Alerta | `69:711` |
| 12 | Estoque — Estável | `69:820` |
| 13 | Agendados sem busca preenchida | `69:929` |

## Reuso e conferência

Preservados Poppins, estilos de texto, variáveis de cor/espaçamento/raio, botão e navegação existentes. Chips reutilizam o Filter chip do Material 3 com tratamento visual da marca. Nove componentes locais de proposta organizam busca, cartões de agendamento, cartão de unidade, abas e estoque agrupado. Textos, vetores e instâncias permanecem editáveis; as telas não são imagens achatadas.

Os 13 exemplos foram revisados por capturas do Figma. Corrigidos, durante a montagem, alturas de componentes, largura dos quatro filtros e preenchimento do chip selecionado. Auditoria conferiu filtros dentro de 350 px, telas de 390 px de largura, ausência de placeholders e lista completa de sete agendamentos em conteúdo rolável. Cabeçalho e navegação inferior ficam fora da área de rolagem dessas telas.

Existem ligações de protótipo entre situações de agendamento, abas Agendamentos/Lembretes, atalhos da Home e níveis de estoque. Exemplos de busca usam telas com texto previamente preenchido. **Não é uma implementação de pesquisa livre, calendário, detalhes, edição da rotina ou filtro de tipo sanguíneo executável no Figma.** Não foram executados testes Android/banco para esta mudança exclusivamente visual.

## Preparação para uma possível implementação

`AppointmentSummary` e `BloodStock` já carregam a identificação da unidade; `DonationCenter` oferece cidade. A cidade não está diretamente nos dois primeiros modelos atuais. Associar resultados ao catálogo já existente é uma possibilidade a conferir no passo de implementação, incluindo unidade indisponível/oculta e falha de consulta, sem esconder o agendamento próprio. Não reaplicar migrações nem alterar RPCs apenas para reproduzir este desenho.

Após aprovação visual, preparar o diagrama e o mapa dos arquivos novos/modificados, com responsabilidades e validações, antes de escrever produção. A aprovação destas telas ainda está pendente. Google e notificações continuam em tutoria; nenhuma etapa foi iniciada automaticamente.

Arquivos locais desta etapa: este documento novo e o registro de continuidade em `AGENTS.md`. Sem novo commit/push autorizado.
