# ADR-002 — Arquitetura e fontes de dados

- **Status:** proposta; implementação futura.
- **Data:** 30/08/2026.

## Decisão

Adotar MVVM com Repository, StateFlow e corrotinas. Repositories terão implementações separadas para demonstração local e Supabase, para que simulação nunca seja apresentada como integração real.

## Consequências

A UI não conhece PostgREST diretamente; estados assíncronos ficam explícitos e a troca de fonte é testável. Evitar camadas adicionais sem necessidade concreta.

