# Orientações para agentes — Doe Sangue

## Objetivo e estado

Projeto acadêmico Android do aplicativo Doe Sangue. A base Compose, 34 telas visuais e a navegação local existem; telas não consomem dados funcionais e integrações ainda não existem. Explique decisões em português e mantenha a solução compreensível para apresentação acadêmica.

## Stack e estrutura

- Kotlin, Android nativo, Jetpack Compose.
- Arquitetura planejada: MVVM com Repository, StateFlow e corrotinas.
- Backend planejado: Supabase Auth, PostgreSQL e PostgREST.
- Tema acadêmico aceito pelo professor: Clínica/Hospital — atendimento humano. Supabase foi sugerido por ele; integração ainda não existe.
- Autenticação planejada: e-mail/senha e Google. Para e-mail/senha, mínimo de 8 caracteres com maiúscula, minúscula, número e símbolo, aplicado no Auth e explicado na UI.
- `domain/`: modelos, contratos e casos de uso sem dependência da UI.
- `data/`: fontes de dados e implementações dos repositories; demo e Supabase devem permanecer separados.
- `presentation/`: ViewModels e estados imutáveis, sem componentes Compose.
- `ui/`: componentes e telas Compose; `di/`: composição manual inicial.
- `docs/`: documentação; `supabase/`: reservado para SQL versionado.

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
- Pendentes: recorrência, cardinalidades finais, matriz de permissões, ambiente Supabase de desenvolvimento, distribuição do seed, conta do professor e confirmação do `applicationId`.

## Atualização de navegação — 23/09/2026

- Navigation Compose 2.9.8 e rotas tipadas estão implementados; `AppNavHost` conecta Auth/Main/Scheduling. O roteador próprio foi substituído.
- A seta das raízes usa `navigateBackOrHome`; decisões de navegação permanecem em `ui/navigation`, sem NavController nos ViewModels.
- Testes instrumentados: `.\gradlew.bat connectedDebugAndroidTest`, com emulador/dispositivo conectado. Ver `docs/12-validacao.md` e ADR-003.
- Trocar de aba ainda remove o percurso anterior. Rascunho, argumentos de registros, deep links e autenticação real continuam pendentes.
