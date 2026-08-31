# Casos de uso

## Atores

- Doador: usa o aplicativo Android.
- Operação do hemocentro: mantém dados operacionais fora da primeira interface.
- Supabase: autenticação e persistência futura.

## UC-01 — Consultar unidades e necessidades

Pré-condição: dados publicados. O doador abre a consulta, seleciona uma unidade e lê a necessidade com origem e atualização. Alternativas: carregamento, vazio e falha. Resultado: nenhuma alteração operacional.

## UC-02 — Solicitar agendamento

Pré-condição: autenticação e horário disponível. O doador seleciona unidade/horário, revisa e confirma. Alternativas: conflito, indisponibilidade ou cancelamento. Resultado: agendamento criado; doação e estoque permanecem inalterados.

## UC-03 — Consultar histórico

O sistema retorna somente registros próprios e identifica a origem. Informação declarada não se torna registro confirmado.

## UC-04 — Consultar campanhas

Listar e abrir detalhes de campanhas publicadas, sem enviar mensagens reais.

## Pendente

Detalhar recorrência e fluxos administrativos após validar o enunciado.

