-- Desenvolvimento acadêmico. Aplicar manualmente uma única vez após revisão.
begin;

create table public.estoque_sangue (
    unidade_id bigint not null references public.unidade_coleta(id) on delete restrict,
    tipo_sanguineo public.tipo_sanguineo not null check (tipo_sanguineo <> 'NAO_SEI'),
    nivel text not null check (nivel in ('estavel', 'alerta', 'critico')),
    fonte text not null check (char_length(btrim(fonte)) > 0),
    atualizado_em timestamptz not null default now(),
    sintetico boolean not null default true check (sintetico),
    primary key (unidade_id, tipo_sanguineo)
);
alter table public.estoque_sangue enable row level security;
revoke all on table public.estoque_sangue from public, anon, authenticated;
grant select on table public.estoque_sangue to anon, authenticated;
create policy "Consultar estoque de unidades publicadas"
on public.estoque_sangue for select to anon, authenticated
using (exists (select 1 from public.unidade_coleta as u
    where u.id = estoque_sangue.unidade_id and u.publicada));

create function public.consultar_estoques()
returns table (
    unidade_id bigint, unidade_nome text, tipo_sanguineo public.tipo_sanguineo,
    nivel text, fonte text, atualizado_em timestamptz, fuso_horario text, sintetico boolean
)
language sql stable security invoker set search_path = ''
as $$
    select e.unidade_id, u.nome, e.tipo_sanguineo, e.nivel, e.fonte,
        e.atualizado_em, u.fuso_horario, e.sintetico
    from public.estoque_sangue as e
    join public.unidade_coleta as u on u.id = e.unidade_id
    where u.publicada
    order by u.nome, u.id, e.tipo_sanguineo;
$$;
revoke all on function public.consultar_estoques() from public, anon, authenticated;
grant execute on function public.consultar_estoques() to anon, authenticated;

-- Não é derivado de agendamento; publicação administrativa de demonstrações.
create table public.doacao_historico (
    id bigint generated always as identity primary key,
    doador_id uuid not null references public.perfil_doador(id) on delete restrict,
    unidade_id bigint not null references public.unidade_coleta(id) on delete restrict,
    ocorrido_em timestamptz not null,
    fonte text not null check (char_length(btrim(fonte)) > 0),
    referencia text not null unique check (char_length(btrim(referencia)) > 0),
    sintetico boolean not null default true check (sintetico),
    criado_em timestamptz not null default now()
);
create index doacao_historico_doador_data on public.doacao_historico (doador_id, ocorrido_em desc, id desc);
alter table public.doacao_historico enable row level security;
revoke all on table public.doacao_historico from public, anon, authenticated;
revoke all on sequence public.doacao_historico_id_seq from public, anon, authenticated;
-- Defesa adicional: leitura própria caso um grant seja acrescentado no futuro.
create policy "Consultar somente historico proprio"
on public.doacao_historico for select to authenticated
using (doador_id = (select auth.uid()));

-- Definer permite devolver o nome/fuso da unidade histórica mesmo se ocultada.
-- Não recebe doador por parâmetro; não devolve UUID, referência interna ou criado_em.
create function public.consultar_minhas_doacoes()
returns table (
    id bigint, unidade_nome text, ocorrido_em timestamptz,
    fuso_horario text, fonte text, sintetico boolean
)
language plpgsql stable security definer set search_path = ''
as $$
declare
    v_doador uuid := auth.uid();
begin
    if v_doador is null then
        raise exception 'É necessário entrar na conta.' using errcode = '42501';
    end if;
    return query
    select h.id, u.nome, h.ocorrido_em, u.fuso_horario, h.fonte, h.sintetico
    from public.doacao_historico as h
    join public.unidade_coleta as u on u.id = h.unidade_id
    where h.doador_id = v_doador
    order by h.ocorrido_em desc, h.id desc;
end;
$$;
revoke all on function public.consultar_minhas_doacoes() from public, anon, authenticated;
grant execute on function public.consultar_minhas_doacoes() to authenticated;
commit;
