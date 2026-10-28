-- Somente dados fictícios. Seed aditivo da avaliação, versão1, datas fixas.
begin;
set local lock_timeout='5s';
set local statement_timeout='30s';
lock table public.unidade_coleta,public.estoque_sangue,public.horario_disponivel,
    public.campanha,public.campanha_tipo_sanguineo in share row exclusive mode;
create temp table avaliacao_preservados(tabela text,registro jsonb) on commit drop;
do $$
declare t text;
begin
    foreach t in array array['unidade_coleta','estoque_sangue','horario_disponivel','campanha',
        'campanha_tipo_sanguineo','perfil_doador','agendamento','agendamento_cancelamento',
        'doacao_historico','agendamento_remarcacao','rotina_lembrete','agendamento_rotina_pedido'] loop
        execute format('insert into avaliacao_preservados select %L,to_jsonb(x) from public.%I x',t,t);
    end loop;
end $$;
do $$
declare
    i integer; j integer; u bigint; c bigint; n bigint; t text;
    v_fonte constant text := 'Demonstração acadêmica Doe Sangue — avaliação v1';
    cidades text[] := array['Vila Aurora','Vale Sereno','Lago Azul','Serra Clara','Jardim do Sol','Campo Alegre',
        'Porto das Flores','Vila das Águas','Colina Verde','Bosque Sereno','Vila Horizonte','Vale das Nuvens'];
    tipos text[] := array['A+','A-','B+','B-','AB+','AB-','O+','O-'];
    dias integer[] := array[-3,1,3,10,17,24,31,38];
    inicio_slot timestamptz;
begin
    for i in 1..12 loop
        insert into public.unidade_coleta(nome,cidade,endereco,horario_atendimento,telefone,publicada,fuso_horario,latitude,longitude)
        select 'Unidade Escola Demo '||lpad(i::text,2,'0')||' — '||cidades[i],
            cidades[i]||' (fictícia)','Endereço fictício '||i||' — avaliacao-professor-v1',
            'Demonstração: segunda a sexta, 08h às 16h',null,i<=10,'America/Sao_Paulo',
            -22.90+i*0.01,-47.05-i*0.01
        where not exists(select 1 from public.unidade_coleta where endereco='Endereço fictício '||i||' — avaliacao-professor-v1');
        select id into strict u from public.unidade_coleta where endereco='Endereço fictício '||i||' — avaliacao-professor-v1';
        for j in 1..8 loop
            insert into public.estoque_sangue(unidade_id,tipo_sanguineo,nivel,fonte,atualizado_em)
            values(u,tipos[j]::public.tipo_sanguineo,
                (array['estavel','alerta','critico'])[1+((i+j)%3)],v_fonte,timestamptz '2026-10-28 12:00:00+00')
            on conflict(unidade_id,tipo_sanguineo) do nothing;
            inicio_slot:=((date '2026-11-01'+dias[j])::timestamp+make_interval(hours=>8+(j%3)*2)) at time zone 'America/Sao_Paulo';
            insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
            values(u,inicio_slot,2+(i+j)%4,true) on conflict(unidade_id,inicio) do nothing;
        end loop;
        for j in 1..2 loop
            insert into public.campanha(unidade_id,titulo,descricao,inicio,fim,fonte,publicada)
            select u,'Campanha Escola Demo '||lpad(i::text,2,'0')||case when j=1 then ' — geral' else ' — tipos divulgados' end,
                'Campanha fictícia para avaliação acadêmica. Unidade e estoque demonstrativos; não representa atendimento ou necessidade real.',
                timestamptz '2026-10-28 03:00:00+00',timestamptz '2026-12-16 03:00:00+00',v_fonte,not(i=10 and j=2)
            where not exists(select 1 from public.campanha where unidade_id=u and fonte=v_fonte
                and titulo='Campanha Escola Demo '||lpad(i::text,2,'0')||case when j=1 then ' — geral' else ' — tipos divulgados' end);
            select id into strict c from public.campanha where unidade_id=u
                and titulo='Campanha Escola Demo '||lpad(i::text,2,'0')||case when j=1 then ' — geral' else ' — tipos divulgados' end;
            if j=2 then
                insert into public.campanha_tipo_sanguineo(campanha_id,tipo_sanguineo)
                values(c,tipos[1+((i-1)%8)]::public.tipo_sanguineo),(c,tipos[1+(i%8)]::public.tipo_sanguineo)
                on conflict(campanha_id,tipo_sanguineo) do nothing;
            end if;
        end loop;
    end loop;
    if (select count(*) from public.unidade_coleta where endereco like '% — avaliacao-professor-v1')<>12
        or (select count(*) from public.estoque_sangue e join public.unidade_coleta x on x.id=e.unidade_id where x.endereco like '% — avaliacao-professor-v1')<>96
        or (select count(*) from public.horario_disponivel h join public.unidade_coleta x on x.id=h.unidade_id where x.endereco like '% — avaliacao-professor-v1')<>96
        or (select count(*) from public.campanha where fonte='Demonstração acadêmica Doe Sangue — avaliação v1')<>24
        or (select count(*) from public.campanha_tipo_sanguineo ct join public.campanha ca on ca.id=ct.campanha_id where ca.fonte='Demonstração acadêmica Doe Sangue — avaliação v1')<>24 then
        raise exception 'Massa pública incompleta ou marca reutilizada; revisar sem sobrescrever.';
    end if;
    foreach t in array array['unidade_coleta','estoque_sangue','horario_disponivel','campanha',
        'campanha_tipo_sanguineo','perfil_doador','agendamento','agendamento_cancelamento',
        'doacao_historico','agendamento_remarcacao','rotina_lembrete','agendamento_rotina_pedido'] loop
        execute format('select count(*) from avaliacao_preservados b where tabela=%L and not exists
            (select 1 from public.%I x where to_jsonb(x)=b.registro)',t,t) into n;
        if n<>0 then raise exception 'Registro anterior alterado em %; carga revertida.',t; end if;
    end loop;
end $$;
commit;
