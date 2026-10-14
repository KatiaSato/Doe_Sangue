# Checkpoint — cancelamento no Supabase — 14/10/2026

## Retomada obrigatória

Projeto: `C:\Users\katia\Documents\Apps\DoeSangue`. Ler AGENTS.md, este checkpoint e somente os arquivos relacionados ao próximo passo.

**A usuária escreve produção; o agente orienta e revisa um único passo curto por resposta.** Não criar/preencher/editar Kotlin ou migrações automaticamente. “Vamos”, “feito” e retomada em outra conversa não autorizam implementação automática. Testes/documentação e operações Git autorizadas podem ser executados pelo agente. Antes da integração Android, apresentar o diagrama com SQL implementado e Android proposto, relacionando arquivos/camadas. Preservar arquivos de outras conversas e a reserva 23. A usuária preferiu continuar nesta conversa; este checkpoint não cria nem exige um chat novo.

## Concluído neste recorte do banco

- Regra aprovada: doador cancela reserva própria agendada antes do início, sem antecedência adicional. Relógio do servidor decide o prazo após obter os bloqueios.
- A usuária escreveu e aplicou manualmente a oitava migração `supabase/migrations/20261014000100_criar_cancelamento_agendamento.sql`. Configuração, permissões, estrutura e comportamento exercitados por consultas/roteiros manuais, com capturas. **Não reaplicar** esta ou outras migrações; histórico da CLI ainda não conciliado.
- Nova tabela `agendamento_cancelamento`: ID gerado, FK única para reserva, FK para perfil que cancelou e instante do servidor. RLS ativo, SELECT/INSERT/UPDATE/DELETE/TRUNCATE negados a anon/authenticated; sequência sem USAGE/SELECT/UPDATE para esses papéis. Não há policy de leitura dos eventos pelo aplicativo.
- `cancelar_agendamento(bigint)` retorna `setof public.agendamento`; VOLATILE, SECURITY DEFINER, search_path vazio, EXECUTE somente authenticated. Identidade de auth.uid(); parâmetro, perfil e propriedade conferidos. Exige READ COMMITTED. Ordem de bloqueios: perfil → horário → reserva, compatível com criação. Relê ID/dono/horário sob bloqueio e confere o prazo após as esperas.
- Reserva própria já cancelada retorna seu estado atual antes da validação de uma nova transição, inclusive depois do início. Não cria outro evento nem altera o instante original. Reserva oculta/inativa pode ser cancelada dentro do prazo. Não há reativação; nova reserva continua exigindo nova intenção/chave.
- UPDATE da reserva e INSERT do evento na mesma transação, com retorno após ambos. Falha específica de unicidade do evento testada: reserva fica agendada, evento anterior intacto; retirar o conflito artificial permite nova tentativa. Não é auditoria genérica de todos os estados nem de operações administrativas externas.

## Evidências e limites

Todas as execuções remotas foram manuais pela usuária. O agente preparou testes e revisou código/documentação; não conectou diretamente ao banco nem inspecionou JWT, credenciais ou tráfego.

- Quatro consultas de catálogo: configuração da função, tabela/RLS/grants, sequência e constraints (PK, UNIQUE e duas FKs ON DELETE RESTRICT), conforme capturas.
- `cancelar_agendamento_criacao_repeticao.sql`: cancelamento temporário, mesmo registro/evento completo na repetição, autor/instante, apenas um evento e reserva 23 preservada.
- `cancelar_agendamento_prazo_oferta.sql`: primeiro cancelamento passado bloqueado; oferta inativa/unidade não publicada canceladas pelo dono; ofertas seguem ocultas na leitura pública; repetição depois do início preserva evento e reserva 23.
- `cancelar_agendamento_validacao_acesso.sql`: IDs inválidos (22023), inexistente/identidade ausente/sem perfil/terceiros (42501), A→B/B→A com ambos possuindo perfil, acessos diretos e anon bloqueados, dados preservados. UUID sem perfil é identidade administrativa simulada, não conta aceita pelo Auth.
- `cancelar_agendamento_atomicidade.sql`: 23505 especificamente na unicidade do evento, ausência de mudança parcial e recuperação após remover somente o conflito artificial. Estado/evento/23 conferidos.
- `cancelar_agendamento_isolamento.sql`: REPEATABLE READ/SERIALIZABLE rejeitados por 25000, com mensagem específica. Não altera registros.
- Concorrência em `supabase/tests/concorrencia_cancelamento/`: horário temporário **23**, início **2026-10-21 21:31:31.436406+00**, capacidade 1; reserva de A **37**. A cancelou e observou B aguardando seu bloqueio=true antes de COMMIT. B criou **38** depois do cancelamento: somente B ativo, A cancelado/um evento, reserva original 23 preservada. B termina em ROLLBACK. É um cenário controlado de duas sessões/uma vaga, sem prova de todos os cenários concorrentes.
- Limpeza específica com COMMIT confirmada por captura: oferta_removida=true, reservas_removidas=true, eventos_removidos=true. **Não repetir preparação/sessões desse cenário histórico.** Horário 23 e reserva original 23 pertencem a tabelas diferentes; o horário temporário já foi removido.
- Conferência final por captura: **2 unidades, 3 horários, 1 perfil, 1 agendamento, 0 eventos**; horário 23/reservas 37/38/eventos relacionados ausentes. Reserva original **23 agendada**, com dono/unidade/início/fuso/criado_em conferidos=true.
- Identidades/papéis simulados administrativamente não são login nem teste de JWT pela API. Compilação/Lint/testes Android não foram repetidos nesta etapa somente SQL. `git diff --check` passou. Detalhes em `docs/12-validacao.md`.

## Estado preservado e requisitos acadêmicos

- **5 tabelas e 5 FKs do domínio**, além da FK perfil → auth.users. Massa persistente final: **7 registros**; duas contas Auth sintéticas são separadas da contagem do domínio. Eventos do teste não integram o seed.
- Preservar reserva original 23: conta sintética A, Unidade Demo A, 19/10/2026 às 08:00 em America/Sao_Paulo, estado agendado, criado_em 2026-10-12 23:14:35.586383+00. Nenhuma credencial deve ser versionada.
- Entrega 25/11/2026: ainda falta pelo menos uma tabela do domínio e massa de pelo menos 200 registros coerentes/distribuídos. Escolha de entidade/cardinalidades/distribuição precisa ser concluída; não criar tabelas automaticamente para atingir números.
- Cadastro real, confirmação de e-mail, recuperação de senha, Google, gestão completa da sessão, recorrência, alterações de reserva, conta do professor e demais pendências permanecem. Agendamento/cancelamento não comprova coleta, aptidão, comparecimento nem altera estoque.

## Git na criação do checkpoint

- Branch `main`; base `62c10d024f60b79e0e821061352b24fbde3ba3bc` (`feat: permite mostrar e ocultar senha no login`). HEAD e origin/main estavam iguais (0/0).
- Na criação do checkpoint, migração, roteiros e documentação estavam locais e ainda não commitados/enviados; a migração apareceu parcialmente preparada no índice (AM). Na sequência, a usuária autorizou commit/push em 14/10. Incluir a versão completa atual, não somente o rascunho do índice. Não incluir local.properties ou credenciais. Conferir no Git o commit resultante e sua sincronização ao retomar.
- Conferir status/log/sincronização ao retomar; esta seção registra o instante do checkpoint, não substitui o Git atual. Não fazer commit/push desta etapa sem autorização aplicável da usuária.

## Próximo passo: integração Android em tutoria

O aplicativo cria, lista e abre detalhes por ID; o detalhe integrado **ainda não cancela**. Telas antigas de cancelamento permanecem visuais. Nenhum Kotlin de produção foi alterado nesta etapa do banco.

1. Retomar o diagrama de `docs/20-diagrama-sequencia-cancelamento.md`, distinguindo servidor implementado de Android proposto.
2. Orientar um pequeno passo no contrato `domain/repository/AppointmentRepository.kt` para futura operação `cancelAppointment(appointmentId): Result<Appointment>`; implementação pela usuária. Reutilizar DTO/mapper existentes de Appointment: o formato da reserva retornada não mudou.
3. Depois orientar DataSource RPC e repository, preservando CancellationException e sessão compartilhada. Atualizar repositories fictícios somente conforme o contrato evoluir, sem rede nos testes normais.
4. Planejar estados/coordenação no detalhe, confirmação da intenção e recuperação de falha/repetição do mesmo ID. Navegação continua em ui/navigation, sem NavController no ViewModel. Lista deverá recarregar/refletir o estado persistido ao voltar; definir a ligação na etapa de UI.
5. Validar testes focados e integração real usando **outra reserva sintética**, preservando 23. Não interpretar a leitura de 23 no app ou os testes SQL administrativos como cancelamento já integrado pela API.

Avançar somente um passo por resposta, aguardando implementação e revisão. Este roteiro não autoriza o agente a entregar a funcionalidade Android pronta.
