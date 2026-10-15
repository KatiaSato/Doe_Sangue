-- Migração incremental. Aplicar somente após revisão, uma vez no desenvolvimento.
-- Não modifica reservas. Perfis existentes recebem Não sei, sem inferir tipagem.
begin;

create type public.tipo_sanguineo as enum (
    'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-', 'NAO_SEI'
);
grant usage on type public.tipo_sanguineo to anon, authenticated;

alter table public.perfil_doador
add column tipo_sanguineo public.tipo_sanguineo not null default 'NAO_SEI';

grant insert (tipo_sanguineo), update (tipo_sanguineo)
on table public.perfil_doador to authenticated;

-- Apenas o fluxo que fornece os metadados do cadastro cria o perfil automático.
-- Contas administrativas antigas/sem esses metadados mantêm a recuperação existente.
create function public.criar_perfil_no_cadastro()
returns trigger language plpgsql security definer set search_path = ''
as $$
declare
    v_nome text;
    v_tipo public.tipo_sanguineo;
begin
    if not coalesce(new.raw_user_meta_data ? 'nome_exibicao', false) then
        return new;
    end if;
    v_nome := btrim(new.raw_user_meta_data->>'nome_exibicao');
    if v_nome is null or v_nome = '' then
        raise exception 'Informe o nome de exibição.' using errcode = '22023';
    end if;
    v_tipo := (new.raw_user_meta_data->>'tipo_sanguineo')::public.tipo_sanguineo;
    if v_tipo is null then
        raise exception 'Escolha o tipo sanguíneo ou Não sei.' using errcode = '22023';
    end if;
    insert into public.perfil_doador (id, nome_exibicao, tipo_sanguineo)
    values (new.id, v_nome, v_tipo);
    return new;
end;
$$;
revoke all on function public.criar_perfil_no_cadastro() from public, anon, authenticated;
create trigger criar_perfil_apos_cadastro
after insert on auth.users
for each row execute function public.criar_perfil_no_cadastro();

create table public.campanha_tipo_sanguineo (
    campanha_id bigint not null references public.campanha(id) on delete cascade,
    tipo_sanguineo public.tipo_sanguineo not null
        check (tipo_sanguineo <> 'NAO_SEI'),
    primary key (campanha_id, tipo_sanguineo)
);
alter table public.campanha_tipo_sanguineo enable row level security;
revoke all on table public.campanha_tipo_sanguineo from public, anon, authenticated;
grant select on table public.campanha_tipo_sanguineo to anon, authenticated;
create policy "Consultar tipos de campanhas publicadas"
on public.campanha_tipo_sanguineo for select to anon, authenticated
using (exists (
    select 1 from public.campanha as campanha
    where campanha.id = campanha_tipo_sanguineo.campanha_id
      and campanha.publicada
));

create function public.consultar_campanhas(p_tipo_sanguineo public.tipo_sanguineo default null)
returns table (
    id bigint, unidade_id bigint, titulo text, descricao text,
    inicio timestamptz, fim timestamptz, fonte text,
    unidade_nome text, fuso_horario text, tipos_sanguineos text[]
)
language plpgsql stable security invoker set search_path = ''
as $$
begin
    if p_tipo_sanguineo = 'NAO_SEI' then
        raise exception 'Use todos os tipos quando não souber seu tipo.' using errcode = '22023';
    end if;
    return query
    select c.id, c.unidade_id, c.titulo, c.descricao, c.inicio, c.fim, c.fonte,
        u.nome, u.fuso_horario,
        coalesce((select array_agg(t.tipo_sanguineo::text order by t.tipo_sanguineo)
            from public.campanha_tipo_sanguineo as t where t.campanha_id = c.id), array[]::text[])
    from public.campanha as c
    join public.unidade_coleta as u on u.id = c.unidade_id
    where c.publicada and u.publicada
      and (p_tipo_sanguineo is null
        or not exists (select 1 from public.campanha_tipo_sanguineo as t where t.campanha_id = c.id)
        or exists (select 1 from public.campanha_tipo_sanguineo as t
            where t.campanha_id = c.id and t.tipo_sanguineo = p_tipo_sanguineo))
    order by c.inicio, c.id;
end;
$$;
revoke all on function public.consultar_campanhas(public.tipo_sanguineo) from public, anon, authenticated;
grant execute on function public.consultar_campanhas(public.tipo_sanguineo) to anon, authenticated;

commit;
