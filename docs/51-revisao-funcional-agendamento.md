# Revisão funcional solicitada — 30/10/2026

A usuária relata que nenhuma unidade permite escolher data/horário e confirmou que o calendário permanece em **Consultando dias disponíveis…**. Pediu revisão das funcionalidades, com testes antes de novas alterações. O ajuste anterior do rodapé resolve a localização de Continuar, mas não comprova este relato de carregamento. Não atribuir a causa a ausência de ofertas sem reproduzir o fluxo.

## Plano de diagnóstico antes de produção

Fluxo: **catálogo público → escolha da unidade → ViewModel → SDK → RPC de dias → calendário → RPC de horários → escolha → revisão**. Reservas/perfis/Auth são fictícios nos testes de interface. Consultas remotas deste diagnóstico são somente as leituras públicas que o aplicativo já usa; nenhuma migração, seed, sessão da usuária, e-mail ou reserva real.

| Parte | Verificação |
| --- | --- |
| Domínio/validação | Executar suíte JVM atual |
| SDK, ViewModel e calendário | Executar testes existentes antes de alterar produção |
| Integração real de disponibilidade | Ler unidades/dias/horários públicos; exercitar uma oferta futura da massa no SDK e na interface, sem confirmar reserva |
| Demais funcionalidades implementadas | Revisar fluxos e executar regressão relevante com fontes fictícias; Google/notificações continuam pendentes |
| Defeito reproduzido | Criar teste de regressão, registrar a falha, só depois corrigir e revalidar |

## Arquivos inicialmente envolvidos

| Arquivo | Papel |
| --- | --- |
| `SupabaseTimeSlotsReadTest.kt` em androidTest/ui/navigation | Consulta pública real pelo SDK, incluindo oferta positiva vigente |
| `SupabaseAvailabilityFlowTest.kt` no mesmo pacote | Percurso de calendário/horário com dados públicos reais e identidade/reservas fictícias |
| `AvailabilityCalendarTest.kt`, `AvailabilityViewModelTest.kt`, `SupabaseAvailabilityTest.kt` | Isolar apresentação, estado e contrato do SDK |
| Código de produção | Sem nova alteração nesta fase; localizar a falha pelos resultados |

## Evidências iniciais

- 96 testes JVM aprovados, zero falhas/erros, em33s.
- Diagnóstico HTTP público:11 unidades retornadas; Demo A sem dias em outubro/novembro; Escola Demo01 com cinco dias em novembro (02,04,11,18,25), cada um com horário e vagas positivas. HTTP200 em todas as seis consultas; horário do servidor30/10/2026 conferido. Isso não comprova o funcionamento do calendário no Android nem da sessão real.
- Evidências ignoradas em `app/build/reports/function-review/`. O script diagnóstico mantém a chave publicável somente em memória; não lê sessão, perfis ou reservas.
- Testes Android ainda em execução/preparação. Celular com conexão instável; usar emulador temporário read-only/no-snapshot e encerrá-lo ao concluir.

## Defeito reproduzido antes da correção

96 JVM e17 Android existentes aprovados. Acrescentados dois testes de integração de oferta vigente: SDK positivo e grafo/calendário/horário real, ambos aprovados no emulador (92,570s), sem Auth/reserva real. No Moto g04, leitura da tela e navegação acompanhada confirmaram Demo A sem dias e Escola Demo01/novembro com dias disponíveis; escolhido02/11 somente no formulário, que carregou12h/5 vagas. Não selecionado horário nem avançado para criar reserva. Nenhum carregamento permanente observado nessa sessão; causa do relato original não confirmada.

Novo teste `PersistentDraftFlowTest.androidBackFromPreparedDraftExitsWithoutExposingUneditableCalendar` falhou no Moto g04 em6,930s **antes de alterar produção**: após retomar um pedido fictício preparado, Voltar do Android abriu ScheduleRecurrence em vez de Home. A seta existente já sai do fluxo preservando o arquivo/chave. Esse caminho permite chegar a DATE/CENTER com edição bloqueada; loadAvailableDates ignora a solicitação de um pedido protegido, e o calendário sem mês consultado mostra Consultando indefinidamente. Não prova que a usuária estava nesse mesmo estado.

Fluxo da correção antes do código: **pedido preparado → Voltar do Android → mesmo callback da seta → Início, mantendo arquivo/chave → nova entrada oferece Retomar → confirmação do mesmo pedido**. Carregamento e resultado de sucesso continuam bloqueando Voltar até o encaminhamento apropriado.

| Arquivo adicional existente | Responsabilidade |
| --- | --- |
| `ui/screens/scheduling/SchedulingScreens.kt` | Fazer BackHandler de Confirmar respeitar o callback existente quando requestLocked, sem permitir editar pedido incerto |
| `ui/navigation/PersistentDraftFlowTest.kt` em androidTest | Regredir retorno Android com arquivo preparado e verificar mesma chave/nenhum envio |

Os dois primeiros testes desse caso no emulador não comprovaram o defeito: falharam por falta de foco/timeout de abertura e havia ANR de com.android.systemui (captura em evidências). Emulador encerrado sem salvar; resultado válido de regressão vermelha é o do Moto g04. Não alterar regras por falhas de infraestrutura.

## Correção e revisão concluídas tecnicamente

BackHandler de Confirmar agora chama o callback existente da seta quando requestLocked e a navegação é permitida; requisição em andamento e sucesso ainda aguardam resposta/encaminhamento. O teste vermelho passou após a mudança, mantendo o mesmo arquivo/chave e nenhum envio. Não alterados consultas, reservas, idempotência, permissões, migrações ou seeds.

| Execução | Resultado |
| --- | --- |
| JVM antes de produção | 96 aprovados; zero falhas/erros;33s |
| SDK/VM/calendário e leitura pública, antes da correção | 17 aprovados;266,517s; Pixel_5/Android12/API32 |
| Nova integração positiva vigente: SDK + calendário até horário | 2 aprovados;92,570s; mesmo emulador; sem criar reserva |
| Agendamento, rascunho, origem, remarcação, rotina e calendário após correção | 38 aprovados;133,537s; Moto g04/Android14 |
| Início, unidades, campanhas e edição do perfil | 35 aprovados;144,782s; Moto g04 |
| Lista/detalhe de agendamentos, filtros e navegação | 33 aprovados na rodada final;100,496s; Moto g04 |
| Login, cadastro, saída, perfil, solicitação de recuperação, alteração de senha e monitor de sessão | 50 aprovados;177,814s; Moto g04; somente fontes fictícias/sem e-mails |
| APKs e Lint finais | Aprovados em4m24; zero erros/34 avisos |

**170 Android distintos aprovados ao final das rodadas**:19 anteriores à correção e156 posteriores, com5 casos de calendário repetidos. Não somar repetições, teste vermelho ou tentativas interrompidas. A regressão de Voltar é um caso dos38. Não se repetiu toda a bateria histórica de PKCE, intenções de mapas, concorrência SQL ou testes remotos de integridade; esta revisão por fluxos não é prova exaustiva de todas as combinações.

Leitura pública adicional de todas as11 unidades visíveis:10 retornam dias com vagas em novembro; Demo A é a única sem dias nesse mês. No aparelho original, o calendário carregou esses dias e o formulário retornou02/11/2026 às12h com5 vagas para Escola Demo01. Escolha local de unidade/data atualizada durante a conferência; não selecionado horário nem solicitada reserva na sessão real. Não se reproduziu o carregamento permanente original nessa sessão; a falha de Voltar é um caminho compatível reproduzido com fixture separada, não uma atribuição comprovada à tentativa original da usuária.

### Pendência nativa observada

Uma rodada de33 foi interrompida no primeiro caso (`AppointmentListFlowTest.leavingListCancelsPendingQueryAndReturningLoadsFreshResult`), sem caso concluído. Exit-info e crash buffer do PID12183 registraram **SIGSEGV/RenderThread**, null pointer em `libhwui.so/BaseRenderNodeAnimator::pushStaging`. Caso isolado passou em8,621s; rodada completa33 passou depois. Causa segue **não conclusiva**, sem atribuir a memória, ação da usuária ou garantir ausência de falhas em uso real. Registro permanece como pendência técnica; nenhuma alteração de aceleração/animações ou enfraquecimento de teste foi feita. As falhas de foco/ANR do emulador são evidências separadas.

### Registro e limites

- Versão corrigida instalada `-r` no Moto g04; dados, sessão persistida, arquivos ignorados e applicationId preservados. Testes usam Application/repositories fictícios e diretórios separados. Consultas reais foram somente públicas; sem nova leitura de perfis/reservas, e-mail ou escrita remota. Não declarar nova auditoria da reserva23 ou nova contagem318.
- Com fontScale1,15 no Moto, o calendário apresentou espaçamento apertado na primeira linha. A seleção de02/11 funcionou; observação de refinamento visual separada do carregamento. **Correção posterior em30/10 após capturas da usuária:** reprodução geométrica, rolagem do conteúdo e18 testes de layout/regressão aprovados; detalhes em [docs52](52-correcao-layout-calendario.md).
- Emulador temporário read-only/no-snapshot encerrado sem salvar base. Arquivos temporários de leitura de tela removidos do celular; evidências locais ignoradas mantidas em `app/build/reports/function-review/`: logs, capturas públicas do calendário/horário, `all-public-dates.json` e `result-final.json`.
- Google e notificações continuam pendentes em tutoria. Não foram implementados nesta revisão. Conferência da correção pela usuária ainda pendente; nenhum commit/push autorizado.
