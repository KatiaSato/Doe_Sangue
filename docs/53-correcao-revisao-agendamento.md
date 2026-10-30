# Confirmação — textos longos no cartão, 30/10/2026

Usuária anexou confirmação com nome longo da unidade invadindo o espaço do rótulo, que quebra letra por letra. ReviewRow atribuía weight apenas ao rótulo: o valor era medido primeiro e podia consumir quase toda a largura.

Plano antes da produção: **cartão mantém duas colunas → rótulo e valor recebem partes definidas da largura com intervalo → nome longo quebra em palavras na própria coluna → rótulos permanecem legíveis**. Preservar cores, dados, estado, confirmação e navegação; sem encurtar nome ou diminuir a fonte.

| Arquivo | Responsabilidade |
| --- | --- |
| `ui/screens/scheduling/SchedulingScreens.kt`, ReviewRow | Repartir largura entre ambos os textos e alinhar valor à direita, com espaço entre colunas |
| `ui/navigation/ConfirmationLayoutTest.kt` em androidTest, novo | Reproduzir antes da correção e conferir integridade das palavras dos rótulos, nomes longos, fontes1/1,15/1,5, rotina e remarcação |

## Reprodução antes da correção

Teste novo no Moto g04/Android14/fonte1,15 falhou em3,538s: a palavra Unidade começava na linha0 e terminava na linha6, reproduzindo as sete linhas da imagem. Teste também confere que escala real chega à tela, sem alterar preferências do aparelho.

Aplicadas duas colunas ponderadas (35%/65%) com intervalo12dp e valor alinhado à direita, mantendo os textos completos. Testes de interface usam dados locais fictícios, inclusive callback de confirmação; nenhuma reserva real ou operação remota.

## Validação concluída

21 testes Android distintos aprovados no Moto g04/Android14 em103,691s:

| Cobertura | Casos |
| --- | ---: |
| Novo ConfirmationLayoutTest: nome da imagem, nome maior/fonte1,5, rotina e remarcação; fontes reais1/1,15/1,5 conferidas na tela | 4 |
| AppointmentCreationFlowTest: confirmação, resposta perdida, falha, bloqueios e seleção | 6 |
| RescheduleRoutineFlowTest: remarcação e preferências de rotina com fontes/respostas fictícias | 11 |

Compilação dos APKs e Lint aprovados; conferência incremental final em19s, zero erros/34 avisos. Instalados ambos os APKs com -r, preservando dados. Nenhuma nova suíte JVM necessária para esta mudança de disposição visual. Capturas sintéticas revisadas: nome original em duas linhas, rótulo inteiro; nome ainda maior com fonte1,5 também preservado, sem sobreposição. Conteúdo pode rolar quando necessário; fontes do aparelho não foram reduzidas nem alteradas.

Evidências locais ignoradas: `app/build/reports/confirmation-layout/before.txt` (falha antes), `after.txt` (21 aprovados), `result-final.json` e `captures/2026-10-30-revisao-agendamento/` (nome-longo.png/fonte-ampliada.png).

Sem rede/Auth/e-mails/SQL/reservas reais ou alteração de regras. Os IDs em fixtures são apenas valores locais, sem consultar/tocar a reserva23. Aprovação técnica não equivale ao aceite visual da usuária, ainda pendente. Pendência nativa anterior de docs51 continua separada, sem atribuir sua resolução a este ajuste. Preservar demais alterações locais; nenhum Git autorizado e não repetir testes por mero aceite.
