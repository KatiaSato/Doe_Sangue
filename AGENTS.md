# Orientações para agentes — Doe Sangue

## Objetivo e estado

Projeto acadêmico Android do aplicativo Doe Sangue. A base Compose e a navegação existem; a escolha de unidade no agendamento consulta o Supabase. Unidade, data e horário são mantidos no rascunho em StateFlow e exibidos na revisão; os horários ainda são opções fixas de demonstração. Recorrência, sucesso e autenticação continuam visuais; confirmar ainda não grava agendamento. Explique decisões em português e mantenha a solução compreensível para apresentação acadêmica. A usuária implementa acompanhando pequenos passos; não entregar funcionalidades inteiras prontas sem solicitação. Ver `docs/14-integracao-unidades.md` e `docs/15-disponibilidade-agendamento.md`.

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
- Programação de segunda a sexta, sábado pela manhã e domingo excepcional é apenas exemplo sintético proposto. Esquema, capacidade, fuso, antecedência, permissões e seed ainda serão definidos.
- Ler `docs/15-disponibilidade-agendamento.md` antes de continuar esta etapa. Nenhuma migração remota foi aplicada por esta decisão.

## Revisão editorial — 06/10/2026

- Orientações de idade, procedimentos e medicamentos revisadas após a Portaria GM/MS nº 11.685/2026, com fontes e data visíveis. Ver `docs/16-revisao-regras-doacao.md` para referências e limites.
- Não reintroduzir a cadência trimestral nem datas futuras como recomendação universal. Recorrência ainda não está implementada; preservar a distinção entre agendamento e aptidão.
- Inventários locais do Figma são históricos; os textos atuais estão em `RestrictionContent.kt`. Figma remoto não foi alterado.
