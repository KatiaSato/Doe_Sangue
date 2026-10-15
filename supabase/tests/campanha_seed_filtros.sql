-- Executar INTEIRO como postgres após o seed persistente.
-- Somente leitura de tabelas, sem fixtures, novas reservas ou contas Auth.
-- Simula papéis administrativos: não valida login, JWT nem API Android.
-- Em erro, executar ROLLBACK antes de investigar/repetir.
begin read only;

set local role anon;
do $$
declare
    v_fonte constant text := 'Demonstração acadêmica Doe Sangue — campanhas v1';
    v_geral bigint;
    v_dirigida bigint;
    v_todos bigint[];
    v_esperados bigint[];
    v_recebidos bigint[];
    v_tipo public.tipo_sanguineo;
begin
    select id into strict v_geral from public.campanha
    where fonte = v_fonte and titulo = 'Campanha Demo A — incentivo à doação';
    select id into strict v_dirigida from public.campanha
    where fonte = v_fonte and titulo = 'Campanha Demo A — tipos A+ e O+';
    select array_agg(id order by id) into v_todos
    from unnest(array[v_geral, v_dirigida]) as ids(id);

    select array_agg(id order by id) into v_recebidos
    from public.campanha where fonte = v_fonte;
    if v_recebidos is distinct from v_todos then
        raise exception '%: leitura direta expôs campanha oculta ou omitiu publicada.', current_user;
    end if;

    foreach v_tipo in array
        array[null, 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']::public.tipo_sanguineo[]
    loop
        v_esperados := case
            when v_tipo is null or v_tipo in ('A+', 'O+') then v_todos
            else array[v_geral] end;
        select array_agg(id order by id) into v_recebidos
        from public.consultar_campanhas(v_tipo) where fonte = v_fonte;
        if v_recebidos is distinct from v_esperados then
            raise exception '%: filtro % retornou IDs inesperados.', current_user, v_tipo;
        end if;
    end loop;

    if (select tipos_sanguineos from public.consultar_campanhas()
        where id = v_dirigida) is distinct from array['A+', 'O+']::text[] then
        raise exception '%: tipos da campanha direcionada incorretos.', current_user;
    end if;
    if (select tipos_sanguineos from public.consultar_campanhas()
        where id = v_geral) is distinct from array[]::text[] then
        raise exception '%: campanha geral tem vínculos inesperados.', current_user;
    end if;
end;
$$;

reset role;

set local role authenticated;
do $$
declare
    v_fonte constant text := 'Demonstração acadêmica Doe Sangue — campanhas v1';
    v_geral bigint;
    v_dirigida bigint;
    v_todos bigint[];
    v_esperados bigint[];
    v_recebidos bigint[];
    v_tipo public.tipo_sanguineo;
begin
    select id into strict v_geral from public.campanha
    where fonte = v_fonte and titulo = 'Campanha Demo A — incentivo à doação';
    select id into strict v_dirigida from public.campanha
    where fonte = v_fonte and titulo = 'Campanha Demo A — tipos A+ e O+';
    select array_agg(id order by id) into v_todos
    from unnest(array[v_geral, v_dirigida]) as ids(id);

    select array_agg(id order by id) into v_recebidos
    from public.campanha where fonte = v_fonte;
    if v_recebidos is distinct from v_todos then
        raise exception '%: leitura direta expôs campanha oculta ou omitiu publicada.', current_user;
    end if;

    foreach v_tipo in array
        array[null, 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']::public.tipo_sanguineo[]
    loop
        v_esperados := case
            when v_tipo is null or v_tipo in ('A+', 'O+') then v_todos
            else array[v_geral] end;
        select array_agg(id order by id) into v_recebidos
        from public.consultar_campanhas(v_tipo) where fonte = v_fonte;
        if v_recebidos is distinct from v_esperados then
            raise exception '%: filtro % retornou IDs inesperados.', current_user, v_tipo;
        end if;
    end loop;

    if (select tipos_sanguineos from public.consultar_campanhas()
        where id = v_dirigida) is distinct from array['A+', 'O+']::text[] then
        raise exception '%: tipos da campanha direcionada incorretos.', current_user;
    end if;
    if (select tipos_sanguineos from public.consultar_campanhas()
        where id = v_geral) is distinct from array[]::text[] then
        raise exception '%: campanha geral tem vínculos inesperados.', current_user;
    end if;
end;
$$;

reset role;

-- Esta saída só é alcançada se todas as asserções acima passarem.
select papel,
    'Todos e oito tipos conferidos' as filtro,
    'Rascunho e unidade oculta ausentes' as leitura,
    'Geral sem vínculos; direcionada A+ e O+' as formato
from (values ('anon'), ('authenticated')) as papeis(papel);
rollback;
