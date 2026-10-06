# Supabase — unidades sintéticas

## Implementado em 06/10/2026

- `migrations/20261002000100_criar_unidade_coleta.sql`: cria a tabela, ativa RLS, revoga privilégios anteriores de `anon`/`authenticated` e permite somente SELECT das linhas com `publicada = true`.
- `seed.sql`: insere Demo A publicada e Demo B não publicada, sem dados pessoais ou credenciais.
- O aplicativo consulta a API com chave publicável. A inserção do seed é administrativa; não deve ser feita com a chave do Android.

## Ordem e aplicação

1. Identificar o ambiente acadêmico de desenvolvimento e obter autorização antes de qualquer aplicação remota.
2. Em um ambiente novo, aplicar a migração uma única vez. Ela usa uma transação e não é idempotente: tabela/política existentes causam erro. Não apagar a tabela para reaplicá-la.
3. Executar o seed após a migração. Reexecuções sequenciais não duplicam unidades com o mesmo nome, cidade e endereço; não há garantia de unicidade concorrente nem atualização de linhas já existentes.

A usuária já aplicou ambos os arquivos pelo SQL Editor do painel. Essa execução manual não configura o histórico de migrações da CLI. Não executar `db push`, reset ou reaplicar a migração no banco existente sem antes reconciliar o histórico e obter autorização. Nenhuma operação remota foi executada durante a preparação deste commit.

## Verificação manual já relatada

- Papel administrativo: duas unidades.
- Papéis `anon` e `authenticated`: somente Demo A.
- SELECT permitido; INSERT, UPDATE, DELETE e TRUNCATE ausentes para esses papéis.
- Consulta no Android: Demo A exibida, conforme confirmação da usuária.

O relato de privilégios não substitui testes completos de RLS. Auth, tabelas de horários/agendamentos e suas políticas ainda não existem nesta implementação. Ver [banco](../docs/09-banco-de-dados.md) e [integração](../docs/14-integracao-unidades.md).
