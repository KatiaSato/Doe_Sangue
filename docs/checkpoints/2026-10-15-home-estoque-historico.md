# Checkpoint — Home, estoque, histórico, FAQ e privacidade — 15/10/2026

## Encerramento autorizado

Usuária delegou F01/F11/F12/F18 e FAQ, aprovou o redesenho da Home no Figma e confirmou o estoque após correção no Android. Pediu um commit com o conjunto acumulado e encerrar por hoje. Commit autorizado; push novo não solicitado. Base anterior: ff21bf0. Conferir o commit deste marco pelo Git ao retomar, sem recriar alterações.

## Implementado

- Home com resultados independentes de perfil, próxima reserva futura ativa, estoque e campanhas; acessos por rotas/IDs existentes. Redesenho aprovado: uma ação principal, cartões clicáveis, indicadores por unidade e barra com ícones. Diagrama/referência/assets em docs/30 e docs/figma/inicio.
- Estoque fictício publicado manualmente por unidade/tipo, com fonte/atualização; consulta pública só de unidade publicada. Histórico fictício próprio, independente de agendamentos, lista e total calculado dos registros recebidos. Camadas domain/data/presentation/ui e composição manual existentes preservadas.
- FAQ informativo com fontes e privacidade/termos acessíveis por Sobre; versão real do aplicativo. Inventário das demais pendências em docs/26.

## Supabase

- Usuária aplicou manualmente `20261015000300_estoque_historico_sinteticos.sql` e `seed_estoque_historico.sql`, repetido sem divergência. **Não reaplicar a migração, nem usar reset/db push.** Histórico da CLI ainda não conciliado.
- Capturas confirmam integridade/defaults/NULL/PK/FKs/CHECKs, escrita e sequência bloqueadas para anon/authenticated, leitura simulada isolada A/C, visitante/identidade ausente bloqueados, RLS nas duas tabelas, funções STABLE/search_path vazio (estoque invoker; histórico definer).
- Conferência independente: estoque=16, histórico=3, A=2/C=1, reserva original 23 agendada e dados=true. Preservar 23 e perfis A/C; nenhuma nova conta Auth foi criada por estes scripts.
- Nove tabelas/dez FKs no esquema versionado do domínio, além de perfil→Auth. Total global previsto=33 (14 anteriores+19 novos), ainda sem consulta global independente. Meta acadêmica de 200 e distribuição continuam pendentes; contas Auth não entram nessa massa.

## Correção e testes

- Primeiro percurso real do estoque falhou como A/C. HTTP público retornava 200/oito registros; o Android rejeitava o timestamp com `+00:00` em Instant.parse. Causa reproduzida no mapper e na chamada SDK real. Corrigidos os dois mappers para OffsetDateTime.parse(...).toInstant(), conforme padrão de campanhas/agendamentos. Sem mudança no banco.
- Após correção: seis testes JVM aprovados no XML; dois Android por instrumentação direta (`OK (2 tests)`), cobrindo offsets/frações em ambos os mappers e leitura real de oito estoques de Demo A pelo SDK. Teste remoto somente leitura e opt-in (`enableSupabaseReadTests=true`); não efetua login explícito nem escreve dados. Não comprova isolamento privado de histórico pela API.
- APKs/Lint aprovados: zero erros/32 avisos. Validações anteriores do recorte: 27 locais, 32 instrumentados, SQL PostgreSQL/PGlite isolado; redesenho com 32 aprovados e repetição dos 12 de Home. Não somar execuções repetidas como testes distintos.
- Ocorrências e limites em docs/12. Capturas sintéticas locais em captures/2026-10-15-home-design, ignoradas pelo Git. APK corrigido instalado no emulator-5556 preservando dados; confirmação final de funcionamento veio por relato, sem nova captura.

## Retomada

1. **Tutoria em pequenos passos**, sem implementar automaticamente o restante do backlog.
2. Atualização de 16/10: usuária confirmou pelo aplicativo os históricos com sessões reais C (um registro/total 1, sem registros de A) e depois A (dois registros/total 2, incluindo o próprio da Demo B), além do estoque de A após correção (oito tipos somente de Demo A, com nível/fonte/atualização). Recorte manual concluído por relatos, sem novas capturas ou inspeção de tráfego/JWT; não é auditoria completa da API. Código/SQL não alterados por essas confirmações.
3. Depois escolher a próxima funcionalidade em docs/26; F02/F03 (unidades/busca) aproveitam o repository existente. Mostrar diagrama antes de nova etapa.
4. Manter pendentes massa de 200, DER/dicionário/matriz finais, conciliação de migrações, apresentação/conta do professor e demais IDs escolhidos. Não inserir dados sem propósito para atingir contagem.

Nenhuma credencial, token ou dado pessoal deve ser registrado em código, documentação ou logs. Agendamento não comprova coleta/aptidão e não modifica estoque ou histórico.
