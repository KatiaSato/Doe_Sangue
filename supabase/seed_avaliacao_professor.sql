-- Executar no mesmo lote: BEGIN seguido de SET LOCAL doesangue.professor_id='ID_AUTH';
-- O aplicador injeta o SET LOCAL logo após o BEGIN abaixo. Não versionar e-mail/senha.
begin;
set local lock_timeout='5s';
set local statement_timeout='30s';
lock table public.perfil_doador,public.horario_disponivel,public.agendamento,public.agendamento_cancelamento,
    public.doacao_historico,public.agendamento_remarcacao,public.rotina_lembrete,public.agendamento_rotina_pedido
    in share row exclusive mode;
create temp table avaliacao_privada_antes(tabela text,registro jsonb) on commit drop;
create temp table avaliacao_reservas(ordem integer primary key,reserva bigint,horario bigint) on commit drop;
do $$
declare
    v_professor uuid:=nullif(current_setting('doesangue.professor_id',true),'')::uuid;
    v_fonte constant text:='Demonstração acadêmica Doe Sangue — avaliação v1';
    v_agora timestamptz:=clock_timestamp();
    i integer; unidade bigint; horario bigint; reserva bigint; rotina bigint; origem bigint; destino bigint;
    d date; instante timestamptz; chave uuid; n bigint; t text;
    hist date[]:=array[date '2025-03-15',date '2025-11-12',date '2026-06-18'];
    doacoes date[]:=array[date '2023-07-10',date '2024-02-15',date '2024-08-20',date '2025-04-12',date '2025-10-18',date '2026-06-20'];
begin
    if v_professor is null or not exists(select 1 from auth.users where id=v_professor) then
        raise exception 'Informe o ID de uma conta Auth válida já criada; nenhum dado inserido.';
    end if;
    foreach t in array array['unidade_coleta','estoque_sangue','horario_disponivel','campanha',
        'campanha_tipo_sanguineo','perfil_doador','agendamento','agendamento_cancelamento',
        'doacao_historico','agendamento_remarcacao','rotina_lembrete','agendamento_rotina_pedido'] loop
        execute format('insert into avaliacao_privada_antes select %L,to_jsonb(x) from public.%I x',t,t);
    end loop;
    if exists(select 1 from public.perfil_doador where id=v_professor and nome_exibicao<>'Professor Demo') then
        raise exception 'Conta já possui outro perfil; não sobrescrever.';
    end if;
    insert into public.perfil_doador(id,nome_exibicao,tipo_sanguineo)
    values(v_professor,'Professor Demo','NAO_SEI') on conflict(id) do nothing;
    for i in 1..7 loop
        select id into strict unidade from public.unidade_coleta
        where endereco='Endereço fictício '||(case when i<=3 then i when i=4 then 1 when i=5 then 2 when i=6 then 3 else 4 end)||' — avaliacao-professor-v1';
        d:=case when i<=3 then hist[i] when i=4 then date '2026-12-02' when i=6 then date '2026-11-25' else date '2026-12-09' end;
        instante:=(d::timestamp+make_interval(hours=>case when i<=3 then 9 when i=4 then 10 when i=6 then 8 else 12 end)) at time zone 'America/Sao_Paulo';
        if i<=3 then
            insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
            values(unidade,instante,3,false) on conflict(unidade_id,inicio) do nothing;
        end if;
        select id into strict horario from public.horario_disponivel where unidade_id=unidade and inicio=instante;
        chave:=md5('avaliacao-professor-v1:'||v_professor||':reserva:'||i)::uuid;
        insert into public.agendamento(doador_id,horario_id,estado,chave_idempotencia,criado_em)
        values(v_professor,horario,case when i<=3 or i=6 then 'cancelado' else 'agendado' end,chave,
            least(v_agora,instante-interval '30 days')) on conflict(doador_id,chave_idempotencia) do nothing;
        select id into strict reserva from public.agendamento where doador_id=v_professor and chave_idempotencia=chave;
        insert into avaliacao_reservas values(i,reserva,horario);
        if i<=3 or i=6 then
            insert into public.agendamento_cancelamento(agendamento_id,cancelado_por,cancelado_em)
            values(reserva,v_professor,least(v_agora,instante-interval '3 days')) on conflict(agendamento_id) do nothing;
        end if;
    end loop;
    select a.reserva into strict origem from avaliacao_reservas a where ordem=6;
    select a.reserva,a.horario into strict destino,horario from avaliacao_reservas a where ordem=7;
    insert into public.agendamento_remarcacao(agendamento_original_id,novo_agendamento_id,doador_id,horario_destino_id,chave_idempotencia,remarcado_em)
    values(origem,destino,v_professor,horario,md5('avaliacao-professor-v1:'||v_professor||':reserva:7')::uuid,v_agora)
    on conflict(agendamento_original_id) do nothing;
    for i in 1..6 loop
        select id into strict unidade from public.unidade_coleta where endereco='Endereço fictício '||(1+(i%3))||' — avaliacao-professor-v1';
        insert into public.doacao_historico(doador_id,unidade_id,ocorrido_em,fonte,referencia)
        values(v_professor,unidade,(doacoes[i]::timestamp+interval '10 hours') at time zone 'America/Sao_Paulo',v_fonte,
            'avaliacao-professor-v1:'||v_professor||':doacao:'||i) on conflict(referencia) do nothing;
    end loop;
    select a.reserva,a.horario into strict reserva,horario from avaliacao_reservas a where ordem=4;
    -- O fingerprint histórico já existente impede recriar uma rotina excluída pelo usuário.
    if not exists(select 1 from public.agendamento_rotina_pedido where agendamento_id=reserva) then
        if exists(select 1 from public.rotina_lembrete where doador_id=v_professor) then
            raise exception 'Conta já tem rotina própria fora desta massa; não sobrescrever.';
        end if;
        insert into public.rotina_lembrete(doador_id,inicio,intervalo_dias,fuso_horario,agendamento_origem_id)
        values(v_professor,date '2026-12-01',30,'America/Sao_Paulo',reserva) returning id into rotina;
        insert into public.agendamento_rotina_pedido(doador_id,chave_idempotencia,agendamento_id,horario_id,inicio,intervalo_dias,fuso_horario,versao_esperada,rotina_id)
        values(v_professor,md5('avaliacao-professor-v1:'||v_professor||':reserva:4')::uuid,reserva,horario,date '2026-12-01',30,'America/Sao_Paulo',0,rotina);
    end if;
    if (select count(*) from public.doacao_historico where doador_id=v_professor and fonte=v_fonte)<>6
        or (select count(*) from avaliacao_reservas)<>7 then raise exception 'Massa privada incompleta.'; end if;
    foreach t in array array['unidade_coleta','estoque_sangue','horario_disponivel','campanha',
        'campanha_tipo_sanguineo','perfil_doador','agendamento','agendamento_cancelamento',
        'doacao_historico','agendamento_remarcacao','rotina_lembrete','agendamento_rotina_pedido'] loop
        execute format('select count(*) from avaliacao_privada_antes b where tabela=%L and not exists
            (select 1 from public.%I x where to_jsonb(x)=b.registro)',t,t) into n;
        if n<>0 then raise exception 'Registro anterior alterado em %; carga revertida.',t; end if;
    end loop;
end $$;
commit;
