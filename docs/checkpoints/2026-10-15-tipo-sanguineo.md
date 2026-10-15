# Checkpoint — tipo sanguíneo, cadastro e filtro — 15/10/2026

## Retomada obrigatória

Ler AGENTS.md, este resumo e somente os arquivos do próximo passo. A implementação direta de cadastro/tipo/filtro foi uma exceção explícita, concluída neste recorte. **Voltar à tutoria: usuária escreve produção, um passo curto por resposta.** Antes de nova etapa, apresentar/salvar diagrama; não abrir outro chat automaticamente.

A usuária autorizou commit e push deste marco em 15/10/2026. Mensagem prevista: `feat: integra cadastro e tipo sanguíneo com filtro de campanhas`. Conferir o resultado em Git ao retomar; não pedir a autorização novamente. Base anterior: main/13e0ead. Preservar fora deste commit `supabase/seed_campanhas.sql`, incompleto e parcialmente staged (índice vazio, conteúdo de trabalho com uma campanha). Não sobrescrever, terminar nem aplicar automaticamente.

## Implementado e confirmado

- Cadastro em quatro passos: nome/e-mail, seleção explícita de oito tipos ou Não sei, revisão, senha/confirmação. Senha somente em memória da tela/requisição; resultado distingue sessão disponível de confirmação pendente.
- Migração incremental `20261015000200_tipo_sanguineo_cadastro_campanhas.sql` aplicada manualmente pela usuária e conferida. Enum, coluna privada, trigger Auth→perfil, associação campanha/tipo e RPC pública SECURITY INVOKER. **Não reaplicar** nem executar db reset/push.
- Perfil próprio mostra tipo e permite atualização. RLS/grants por coluna preservados; ID não pode ser alterado. Contas administrativas sem metadados mantêm recuperação Missing. Edição do perfil não sincroniza metadados iniciais do Auth.
- Campanhas: lista/filtro manual Todos/oito tipos, campanhas gerais incluídas, estados de consulta, detalhe por ID/fuso da unidade. Consulta cancelada não publica resposta tardia. Rotas/prévias antigas continuam protótipos. **Filtro real no Android ainda depende do seed persistente**; SQL e testes em memória já exercitados.
- Arquitetura MVVM/Repository/StateFlow/corrotinas/DI preservada. Tipo autodeclarado, sem compatibilidade médica, aptidão, coleta ou estoque.

## Dados remotos e evidências

Captura de `tipo_sanguineo_android_conferencia_final.sql`: **2 unidades, 3 horários, 2 perfis, 1 reserva, zero eventos/campanhas/vínculos; oito registros do domínio**. Sete tabelas/sete FKs de domínio, além da FK perfil→Auth. Contas Auth são separadas da contagem.

Preservar original **23**: perfil A, Demo A, 19/10/2026 08:00 America/Sao_Paulo, agendado, criado_em 2026-10-12 23:14:35.586383+00; conferência final=true.

Perfil **Doador Demo C/c**: NAO_SEI, criado_em 2026-10-15 16:24:09.945178+00, e-mail confirmado e dados conferidos=true. Usuária confirmou criação automática após cadastro/login, sem Criar perfil. Salvou O+ sintético, reabriu Perfil e confirmou persistência; restaurou Não sei, conferido no banco.

Primeira tentativa com doador.c@example.com foi recusada pelo Auth (400/endereço inválido). Usuária autorizou seu e-mail de acesso ao Supabase **somente no Auth**, mantendo perfil fictício. Não pedir novamente essa autorização nem solicitar/registrar endereço pessoal, senha, tokens ou UUID no chat/docs/código/logs. Não usar endereços de terceiros; não desativar validação/confirmação por causa da tentativa recusada.

## Validação

- Nove testes locais aprovados: cinco novos de mapper de campanha/perfil, três de agendamento e um exemplo.
- Suíte no Pixel_5/Android 12: **68 aprovados, zero falhas/erros e um remoto ignorado**, conferidos nos 12 XML; console exibiu 70, soma XML=69 incluindo skipped. Cadastros, filtros, perfil e regressões usam repositories/sessões fictícios, sem rede.
- `assembleDebug` e `lintDebug` finais aprovados; Lint zero erros/31 avisos. Sem alteração de produção após essa validação; documentação e roteiros de conferência atualizados depois.
- Catálogos/RLS/grants/funções/trigger/policies conferidos; final de CASCADE parcialmente cortado na imagem. `campanha_filtro_tipo.sql` e `perfil_tipo_sanguineo_isolamento.sql` passaram por captura, com fixtures em ROLLBACK/papéis simulados. Conferência de sete registros antes do cadastro e de oito depois; não reutilizar a expectativa antiga após C.
- Cadastro automático e edição/releitura/restauração reais de C validados por relato/capturas/consulta administrativa. Não houve auditoria de JWT/tráfego nem teste real de perda de resposta, falhas concorrentes ou renovação de sessão neste recorte.
- Intercorrências resolvidas: helper de rolagem; emulador travado/force-stop externo; Lint exigiu NavBackStackEntry na chave de remember do cadastro compartilhado. Suíte final aprovada com emulador livre.

Evidências detalhadas: [validação](../12-validacao.md). Decisões/diagrama: [etapa 24](../24-tipo-sanguineo-cadastro-filtro.md). Nenhum SQL remoto foi executado pelo agente.

## Arquivos principais

Domínio: BloodType, DonorProfile, RegistrationResult, Campaign e contratos Auth/Perfil/Campaign. Dados: DataSources/repositories/DTOs/mappers Supabase. Apresentação: RegistrationViewModel/estado, ProfileViewModel/estado e presentation/campaigns. UI: BloodTypePicker, RegistrationScreens, ProfileContent/ProfileScreens, CampaignReadScreens; DI/navegação/factories adaptados. Testes de navegação/fakes adaptados; novos RegistrationFlowTest, CampaignFilterFlowTest, FeatureTestHost, FakeCampaignRepository e CampaignAndProfileMapperTest; dois cenários novos no ProfileFlowTest. Migração e seis roteiros SQL desta etapa em supabase (conferir lista no Git).

## Pendências e próximo passo

1. Concluir commit/push autorizado e verificar remoto. Preservar seed SQL incompleto e seu estado no índice fora do commit.
2. Retomar seed mínimo de campanhas em tutoria: ler arquivo de trabalho e docs/23, atualizar diagrama para vínculos de tipos antes de código. Plano de três campanhas: publicada/A, rascunho/A e publicada/B oculta. Decidir/acrescentar vínculos sintéticos, conferir repetição e testar filtro real/lista/detalhe no app. Não aplicar seed antes de completo/revisado.
3. Melhorias futuras registradas: mensagens específicas para erros conhecidos com próxima ação (invalid email/email_not_confirmed/credenciais/limites/conexão), fallback só quando necessário; Perfil mostra apenas tipo escolhido e abre opções ao editar. **Ainda não implementadas**; manter tutoria/diagrama antes da etapa.
4. Política de senha/configuração do Auth, reenvio/deep link de confirmação, demais cenários de autenticação, recorrência, matriz final, conta do professor e seed de 200 registros continuam pendentes. Prazo acadêmico 25/11/2026. A integração não encerra o projeto inteiro.
