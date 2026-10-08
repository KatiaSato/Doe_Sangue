# Orientações para agentes — Doe Sangue

## Objetivo e estado

Projeto acadêmico Android do aplicativo Doe Sangue. A base Compose e a navegação existem; unidades e horários sintéticos do agendamento são consultados no Supabase. Unidade, data e horário são mantidos no rascunho em StateFlow e exibidos na revisão. Recorrência, sucesso e autenticação continuam visuais; confirmar ainda não grava agendamento. Explique decisões em português e mantenha a solução compreensível para apresentação acadêmica. A usuária implementa acompanhando pequenos passos; não entregar funcionalidades inteiras prontas sem solicitação. Ver `docs/14-integracao-unidades.md` e `docs/15-disponibilidade-agendamento.md`.

## Stack e estrutura

- Kotlin, Android nativo, Jetpack Compose.
- Arquitetura planejada: MVVM com Repository, StateFlow e corrotinas.
- Backend: PostgreSQL/PostgREST integrado à consulta de unidades; Supabase Auth planejado.
- Tema acadêmico aceito pelo professor: Clínica/Hospital — atendimento humano. Supabase foi sugerido por ele; ambiente de desenvolvimento acadêmico confirmado pela usuária, somente dados sintéticos e sem reservas em hemocentros reais.
- Autenticação planejada: e-mail/senha e Google. Para e-mail/senha, mínimo de 8 caracteres com maiúscula, minúscula, número e símbolo, aplicado no Auth e explicado na UI.
- `domain/`: modelos, contratos e casos de uso sem dependência da UI.
- `data/`: fontes de dados e implementações dos repositories; demo e Supabase devem permanecer separados.
- `presentation/`: ViewModels e estados imutáveis, sem componentes Compose.
- `ui/`: componentes e telas Compose; `di/`: composição manual inicial.
- `docs/`: documentação; `supabase/`: migração de `unidade_coleta` e seed sintético versionados.

## Comandos reais

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

## Regras de trabalho e segurança

- Leia este arquivo e os documentos relacionados antes de escrever.
- Preserve a separação entre Confirmado, Proposto, Implementado e Pendente.
- Não invente regras médicas, critérios de aptidão ou intervalos de doação.
- Agendamento não comprova comparecimento, coleta, aptidão ou alteração de estoque.
- Use somente dados sintéticos; não registre credenciais, tokens ou dados pessoais em logs.
- Entrega acadêmica final: pelo menos 6 tabelas relacionadas, 5 FKs e 200 registros sintéticos distribuídos; distinguir seed do domínio de contas aptas a fazer login. Ver `docs/09-banco-de-dados.md`.
- Supabase Auth guarda credenciais; nunca criar coluna de senha no domínio.
- Nunca incluir `service_role`, senha do banco ou segredo no Android.
- RLS deve impedir acesso privado de terceiros, elevação de privilégios, alteração de estoque e confirmação de coleta pelo doador.
- Não aplicar migrações remotas, publicar, fazer deploy ou alterar produção sem autorização explícita.
- Não modificar o Figma sem solicitação explícita.

## Fontes e pendências

- Entrega confirmada pela usuária para a última quarta-feira de novembro de 2026: 25/11/2026. Apresentação focada em banco de dados, na disciplina de persistência. Priorizar modelagem, integridade, permissões, operações persistidas e requisitos acadêmicos; continuar o ensino em pequenos passos.
- Briefing local fornecido em 30/08/2026.
- Figma: `Doe Sangue — Android App — Education`, nó `9:8`; requer inventário antes das telas.
- Enunciado acadêmico recebido: `Aula 07 - LBD - M - Projeto Integrador Especificação e Critérios - 16.09.pdf` (fornecido pela usuária, fora do repositório).
- Pendentes: recorrência, cardinalidades finais, matriz de permissões completa, distribuição do seed, conta do professor e confirmação do `applicationId`.

## Atualização de navegação — 23/09/2026

- Navigation Compose 2.9.8 e rotas tipadas estão implementados; `AppNavHost` conecta Auth/Main/Scheduling. O roteador próprio foi substituído.
- A seta das raízes usa `navigateBackOrHome`; decisões de navegação permanecem em `ui/navigation`, sem NavController nos ViewModels.
- Testes instrumentados: `.\gradlew.bat connectedDebugAndroidTest`, com emulador/dispositivo conectado. Ver `docs/12-validacao.md` e ADR-003.
- Trocar de aba ainda remove o percurso anterior. Rascunho completo, argumentos de registros, deep links e autenticação real continuam pendentes.

## Integração de unidades — 06/10/2026

- `SchedulingViewModel` depende de `DonationCenterRepository`; `AppContainer` injeta a implementação Supabase por padrão e aceita substituição explícita nos testes.
- DTO e mapper ficam em `data/supabase`. O repository preserva `CancellationException` e retorna outras exceções em `Result.failure`.
- `local.properties` fornece URL/chave publicável ao `BuildConfig`; não exibir valores nem versionar esse arquivo. Chave publicável pode estar no APK; nenhum segredo administrativo pode estar nele.
- RLS/grants da tabela permitem aos papéis `anon` e `authenticated` somente leitura das unidades publicadas. SQL foi aplicado manualmente pela usuária; histórico da CLI ainda não foi conciliado. Não reaplicar automaticamente a migração.
- Em 06/10/2026: compilação, teste local, Lint e 12 testes instrumentados passaram; Lint tem 31 avisos e zero erros. A consulta real no celular foi confirmada pela usuária em 03/10/2026.
- Data e horário já preenchem o rascunho; a revisão passou a exibir as escolhas em 07/10/2026 e exige os três campos para habilitar Confirmar. Consultar resultados atuais em `docs/12-validacao.md`. Próximos passos: modelar disponibilidade sintética, depois autenticação e persistência. Agendamento ainda não é gravado.

## Disponibilidade por unidade — decisão de 06/10/2026

- Permitir agendamento somente nas datas/horários disponibilizados para a unidade escolhida. Não bloquear sábados, domingos ou feriados universalmente; representar aberturas especiais e fechamentos na programação.
- `horario_atendimento` é texto descritivo, não fonte estruturada de disponibilidade. Modelar horários ligados à unidade antes de implementar a regra no calendário.
- A futura gravação deve revalidar disponibilidade/capacidade no servidor, com controle de concorrência; o estado da tela não garante reserva.
- Em 07/10/2026, a usuária escolheu um registro por horário oferecido, com capacidade configurável para vários agendamentos. Capacidade total não equivale a vagas restantes; o servidor deverá calcular e revalidar a disponibilidade.
- Em 07/10/2026, a usuária aplicou manualmente a migração de `fuso_horario` e mostrou Demo A/B com `America/Sao_Paulo` e publicação preservada. DTO, mapper e domínio já transportam `timeZoneId`; aplicativo e APK de testes compilaram, e a usuária confirmou a consulta real sem erro. O caso de uso já converte o dia para instantes no fuso da unidade, e a tela converte os instantes recebidos para horários locais. Não reaplicar a migração. Ver `supabase/README.md` para ordem e evidências.
- Programação de segunda a sexta, sábado pela manhã e domingo excepcional é apenas exemplo sintético proposto. Esquema, permissões de leitura e seed mínimo de horários já existem; ampliação da programação, limites operacionais da capacidade e antecedência permanecem pendentes.
- A tabela `horario_disponivel` foi criada manualmente pela usuária em 07/10/2026; captura de `pg_tables` confirma RLS ativo. O seed de horários foi aplicado: três ofertas sintéticas. Consultas com JOIN sob `anon` e `authenticated` retornaram somente Demo A às 08:00; `has_table_privilege` confirmou SELECT e ausência de INSERT/UPDATE/DELETE para ambos. Ver limites da evidência em `docs/12-validacao.md`. Não reaplicar a migração. Nenhuma tabela de reservas foi criada. Ler `docs/15-disponibilidade-agendamento.md` antes de continuar.

## Consulta de horários — evolução de 07/10/2026

- DTO, mapper, DataSource, repository e caso de uso de horários estão ligados ao SchedulingViewModel via AppContainer. Ao mudar a data, consulta o intervalo no fuso da unidade; ao mudar a unidade, cancela e limpa o rascunho. A tela mostra carregamento, erro com nova tentativa, vazio ou horários retornados; o ViewModel valida a escolha contra unidade/data/lista.
- Supabase Kotlin 3.6.0: agrupar gte/lt da coluna inicio dentro de and no DataSource. O envio de parâmetros dessa versão usa somente o primeiro valor por chave quando as condições ficam diretamente no filter; isso descartava o limite superior. Não remover o agrupamento sem um teste do pedido HTTP ou de integração que cubra dias adjacentes.
- AppNavigationTest e DonationRestrictionsTest injetam repositories fictícios de unidades e horários. SupabaseTimeSlotsReadTest é uma exceção explícita, somente leitura, ativada por enableSupabaseReadTests=true contra o seed de desenvolvimento. Resultados em docs/12-validacao.md.
- Calendário ainda permite escolher qualquer data para consultar; não há bloqueio antecipado de dias vazios. Capacidade restante, persistência de reservas e autenticação continuam pendentes.

## Identidade do horário no rascunho — 08/10/2026

- A tela comunica o ID do horário ao ViewModel por `onSelectTimeSlot`. `selectTimeSlot` valida o registro na lista da unidade/data atuais e guarda `selectedTimeSlotId` junto de `selectedTime` para exibição.
- Mudar de unidade/data ou recarregar horários limpa o ID e a hora. Continuar e Confirmar exigem o ID preenchido. A usuária confirmou o percurso manual de seleção, troca de data, retorno sem seleção e revisão; resultados automatizados em `docs/12-validacao.md`.
- O ID identifica a oferta para a futura persistência; não constitui reserva nem garante capacidade disponível. Confirmar ainda é navegação visual.

## Revisão editorial — 06/10/2026

- Orientações de idade, procedimentos e medicamentos revisadas após a Portaria GM/MS nº 11.685/2026, com fontes e data visíveis. Ver `docs/16-revisao-regras-doacao.md` para referências e limites.
- Não reintroduzir a cadência trimestral nem datas futuras como recomendação universal. Recorrência ainda não está implementada; preservar a distinção entre agendamento e aptidão.
- Inventários locais do Figma são históricos; os textos atuais estão em `RestrictionContent.kt`. Figma remoto não foi alterado.
