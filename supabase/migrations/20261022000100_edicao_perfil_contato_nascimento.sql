-- Incremental: perfis existentes preservam telefone/nascimento nulos.
begin;

alter table public.perfil_doador
    add column telefone text,
    add column data_nascimento date,
    add constraint perfil_doador_telefone_check
        check (telefone is null or telefone ~ '^[0-9]{10,11}$'),
    add constraint perfil_doador_nascimento_check
        check (data_nascimento is null or data_nascimento <= current_date);

-- Policies existentes continuam restringindo leitura/edição ao próprio ID.
-- Não conceder UPDATE do ID ou de criado_em, nem novos acessos a anon.
grant update (telefone, data_nascimento)
    on public.perfil_doador to authenticated;

commit;
