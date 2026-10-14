# Concorrência: cancelamento de A e criação por B

Preparação executada pela usuária em 14/10/2026: captura mostra **horário 23**, início **2026-10-21 21:31:31.436406+00**, capacidade **1**, **reserva temporária 37**, agendada. Horário 23 e reserva original 23 são IDs de tabelas diferentes; a reserva original não participa do teste. Desenvolvimento acadêmico sintético, com identidades/papéis simulados; sem login/JWT pela API. Preservar reserva 23 e não reutilizar o cenário histórico da pasta `concorrencia/`.

## Cenário

Uma oferta temporária de capacidade 1 começa ocupada por A. A cancela, mantendo a transação aberta; B tenta reservar a mesma oferta. A deverá observar B esperando seu bloqueio antes de confirmar. Após COMMIT de A, B deverá criar a reserva com leitura atualizada, vendo apenas sua própria linha pelo acesso normal. A conferência administrativa deverá encontrar A cancelado, um evento de A e somente B ativo. B termina em ROLLBACK; a limpeza remove o cancelamento/reserva/oferta temporários de A.

## Etapas

1. Preparação já executada, usando COMMIT para compartilhar oferta/reserva. Não repetir `preparar.sql`.
2. `sessao_a.sql`, `sessao_b.sql` e `limpar.sql` configurados com os dados exatos da captura. Antes de iniciar o cenário, abrir duas abas do navegador no SQL Editor e carregar A/B inteiros em consultas distintas, ambas inicialmente como postgres.
3. Executar A e, após cerca de dois segundos, executar B enquanto A ainda estiver rodando. Não executar B após A terminar. A só confirma após detectar B esperando seu bloqueio em outro backend, com limite de 35 segundos. A retorna reserva_id=37 e b_aguardou_bloqueio_de_a=true. B deverá retornar nova reserva, A cancelado/um evento/somente B ativo e reserva original 23 preservada.
4. Conferir as duas respostas. B termina em ROLLBACK, inclusive perfil preparado somente se ausente. Depois de ambas as sessões encerrarem, executar `limpar.sql` configurado e inteiro como postgres; usa COMMIT. Confirmar oferta/reservas/eventos removidos e fazer a conferência final da massa/23.

Se o cenário falhar, esperar ambas as sessões encerrarem e resolver eventual transação abortada com ROLLBACK antes de inspecionar o estado. Não repetir preparação nem apagar registros sem conferir os identificadores. A limpeza rejeita qualquer reserva que não seja a original de A; não exclui unidade, perfil ou conta. Sequências podem avançar.

## Evidência

Preparação confirmada por captura. Capturas de A/B confirmam cancelamento de 37 e B aguardando o bloqueio de A=true; B criou reserva 38 depois do cancelamento, com somente B ativo, A cancelado/um evento e reserva original 23 preservada. A termina em COMMIT; B termina em ROLLBACK. Captura da limpeza confirma oferta_removida=true, reservas_removidas=true e eventos_removidos=true. Horário 23/reserva 37/evento removidos; reserva 38 desfeita por ROLLBACK. Conferência global por captura em `../cancelar_agendamento_conferencia_final.sql` confirma massa original e ausência dos registros temporários. Não repetir preparação ou sessões deste cenário histórico.

Este é um cenário controlado de duas sessões/uma vaga, com identidades simuladas; não prova todas as combinações de concorrência nem login/JWT pela API. A limpeza só aceita a reserva original temporária de A (37) e seu evento; se B 38 não tiver sido desfeito ou houver outro registro no horário, interrompe sem excluir.
