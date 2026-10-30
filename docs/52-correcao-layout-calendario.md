# Calendário — sobreposição de dias, 30/10/2026

A usuária anexou novembro/2026 com e sem dia escolhido: datas da primeira semana sobrepostas aos nomes dos dias da semana. Reprodução por teste de geometria antes de nova produção; preservar regras de disponibilidade e digitação manual.

## Causa e fluxo proposto antes do código

Fonte da versão Material3 instalada (1.3.2): DatePickerDialog limita a superfície a568dp; nosso Column acrescenta alternância/manual e mensagens acima do DatePicker. A grade do Month exige6×48dp enquanto o espaço restante é menor, permitindo que seu desenho invada o cabeçalho semanal.

Fluxo do ajuste: **diálogo limita janela → conteúdo vertical rolável mede o calendário na altura natural → dias da semana e grade ocupam linhas distintas → botões permanecem no rodapé do diálogo**. Não diminuir a fonte, desativar acessibilidade, alterar a biblioteca ou consultar o banco.

| Arquivo | Papel |
| --- | --- |
| `ui/components/DonationDatePickerDialog.kt` | Acrescentar rolagem ao Column que contém controles e DatePicker, preservando estado/callbacks |
| `ui/navigation/CalendarLayoutTest.kt` em androidTest, novo | Medir separação entre cabeçalhos semanais e primeira linha; meses de4/5/6 semanas, fontes1/1,15/1,5, data escolhida, navegação por mês/ano |
| `AvailabilityCalendarTest.kt` e `ManualSchedulingDateTest.kt` existentes | Regredir disponibilidade, erros/retry, confirmação, entrada manual e cancelamento |

## Reprodução antes da correção

Captura sintética inicial reproduziu o defeito, mas a primeira comparação de boundsInRoot passou porque os limites de acessibilidade eram recortados. Medição corrigida para posição e tamanho sem recorte (sem mudar o comportamento esperado): no Moto g04/Android14, fonte1,15, o cabeçalho terminava em y583px e o primeiro dia começava em y506px, **77px de sobreposição**. Teste vermelho em4,132s, antes de alterar produção. Captura anterior mantida como prova visual, não aprovação do layout.

## Correção e validação concluídas tecnicamente

Aplicado somente Modifier.verticalScroll ao Column externo do conteúdo do diálogo. Ele mede o calendário na altura natural e recorta/rola o corpo quando necessário, enquanto Cancelar/Confirmar data permanecem no rodapé. Nenhuma alteração de fonte de produção, regra, parser, consulta, banco ou rotina.

**18 Android distintos aprovados no Moto g04/Android14**:8 novos de layout e10 de regressão (AvailabilityCalendarTest5/ManualSchedulingDateTest5). Meses de4,5,6 semanas, com/sem seleção, fontes reais1/1,15/1,5 dentro da janela, seleção do último dia por gesto vertical, navegação mensal/anual e botões acessíveis. Entrada manual, erros, retry, cancelamento e disponibilidade preservados. Sem nova suíte JVM por ser ajuste de composição visual.

Rodada completa intermediária17/18 em44s: o caso de ano procurava texto literal2027, mas Material3 expõe uma frase de acessibilidade; corrigido somente o seletor, caso isolado aprovado7,724s. Última rodada dos8 layouts aprovada23,893s. União com os10 de regressão=18, não somar repetições. Uma tentativa anterior foi interrompida pela conexão ADB e o helper genérico de rolagem atingia o paginador horizontal; teste passou a usar gesto vertical real no corpo, preservando a exigência de selecionar o último dia.

Os testes também passaram a **verificar a escala de fonte na janela**. O override aplicado apenas ao pai não atingia o contexto do Dialog: o teste acusou1,15 ao esperar1/1,5. Host somente de teste usa DeviceConfigurationOverride.FontScale e ComposeView com contexto configurado; mantém assertion da escala real, sem alterar preferências do aparelho ou produção para satisfazer o teste.

APKs/Lint aprovados em2m48; última recompilação de testes/Lint15s,zero erros/34 avisos. Versão instalada -r preservando dados; capturas sintéticas antes/depois e fonte ampliada revisadas. A última semana pode exigir rolagem, testada por gesto e confirmação da data correta. Evidências ignoradas `app/build/reports/calendar-layout/` (before-unclipped.txt, round2.txt, layout-final.txt e result-final.json) e `captures/2026-10-30-layout-calendario/`.

Sem rede/Auth/e-mail/SQL/reserva real, configuração remota, mudança de dependência, applicationId ou preferências de fonte do celular. Alterações anteriores preservadas. Pendência nativa de docs51 continua separada; não atribuir sua solução a este ajuste. Aceite visual da usuária e Git pendentes; não repetir testes por mero aceite ou iniciar F05/F15 automaticamente.
