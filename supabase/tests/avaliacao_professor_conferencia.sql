-- Injetar SET LOCAL doesangue.professor_id após BEGIN no mesmo lote.
-- Leitura administrativa e papéis simulados; nenhuma linha é alterada.
begin;
set transaction read only;
do $$
declare p uuid:=nullif(current_setting('doesangue.professor_id',true),'')::uuid;
begin
    if p is null or not exists(select 1 from auth.users where id=p and email_confirmed_at is not null)
        or not exists(select 1 from public.perfil_doador where id=p and nome_exibicao='Professor Demo') then
        raise exception 'Conta de avaliação não está pronta.';
    end if;
    if exists(select 1 from public.agendamento_cancelamento e join public.agendamento a on a.id=e.agendamento_id
        where e.cancelado_por=p and (a.doador_id<>p or a.estado<>'cancelado' or e.cancelado_em<a.criado_em)) then
        raise exception 'Evento de avaliação inconsistente.';
    end if;
    if exists(select 1 from public.agendamento a join public.horario_disponivel h on h.id=a.horario_id
        where a.doador_id=p and a.estado='agendado' and
        (select count(*) from public.agendamento ocupada where ocupada.horario_id=h.id and ocupada.estado='agendado')>h.capacidade) then
        raise exception 'Capacidade ultrapassada.';
    end if;
    if exists(select 1 from pg_class where relnamespace='public'::regnamespace and relname in
        ('unidade_coleta','estoque_sangue','horario_disponivel','campanha','campanha_tipo_sanguineo',
        'perfil_doador','agendamento','agendamento_cancelamento','doacao_historico','agendamento_remarcacao',
        'rotina_lembrete','agendamento_rotina_pedido') and not relrowsecurity) then
        raise exception 'RLS não está ativo.';
    end if;
end $$;
set local role anon;
do $$
begin
    if (select count(*) from public.estoque_sangue where fonte='Demonstração acadêmica Doe Sangue — avaliação v1')<>80
        or (select count(*) from public.campanha where fonte='Demonstração acadêmica Doe Sangue — avaliação v1')<>19 then
        raise exception 'Publicação de avaliação incorreta.';
    end if;
end $$;
reset role;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('doesangue.professor_id'),true);
do $$
begin
    if (select count(*) from public.consultar_minhas_doacoes())<>6
        or (select count(*) from public.consultar_meus_agendamentos_v2())<>7
        or (select count(*) from public.consultar_minha_rotina())<>1 then
        raise exception 'Consultas privadas de avaliação incorretas.';
    end if;
end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000099',true);
do $$
begin
    if exists(select 1 from public.consultar_minhas_doacoes())
        or exists(select 1 from public.consultar_meus_agendamentos_v2()) then
        raise exception 'Dados privados retornados para outra identidade.';
    end if;
end $$;
reset role;
select 'Total de registros do domínio' item,(select sum(n) from (
    select count(*) n from public.unidade_coleta union all select count(*) from public.estoque_sangue
    union all select count(*) from public.horario_disponivel union all select count(*) from public.campanha
    union all select count(*) from public.campanha_tipo_sanguineo union all select count(*) from public.perfil_doador
    union all select count(*) from public.agendamento union all select count(*) from public.agendamento_cancelamento
    union all select count(*) from public.doacao_historico union all select count(*) from public.agendamento_remarcacao
    union all select count(*) from public.rotina_lembrete union all select count(*) from public.agendamento_rotina_pedido) x)::text resultado
union all select 'Novos registros nesta avaliação', (
    (select count(*) from public.unidade_coleta where endereco like '% — avaliacao-professor-v1') +
    (select count(*) from public.estoque_sangue where fonte='Demonstração acadêmica Doe Sangue — avaliação v1') +
    (select count(*) from public.horario_disponivel h join public.unidade_coleta u on u.id=h.unidade_id where u.endereco like '% — avaliacao-professor-v1') +
    (select count(*) from public.campanha where fonte='Demonstração acadêmica Doe Sangue — avaliação v1') +
    (select count(*) from public.campanha_tipo_sanguineo ct join public.campanha c on c.id=ct.campanha_id where c.fonte='Demonstração acadêmica Doe Sangue — avaliação v1') +
    (select count(*) from public.perfil_doador where id=current_setting('doesangue.professor_id')::uuid) +
    (select count(*) from public.agendamento where doador_id=current_setting('doesangue.professor_id')::uuid) +
    (select count(*) from public.agendamento_cancelamento where cancelado_por=current_setting('doesangue.professor_id')::uuid) +
    (select count(*) from public.doacao_historico where doador_id=current_setting('doesangue.professor_id')::uuid and fonte='Demonstração acadêmica Doe Sangue — avaliação v1') +
    (select count(*) from public.agendamento_remarcacao where doador_id=current_setting('doesangue.professor_id')::uuid) +
    (select count(*) from public.rotina_lembrete where doador_id=current_setting('doesangue.professor_id')::uuid) +
    (select count(*) from public.agendamento_rotina_pedido where doador_id=current_setting('doesangue.professor_id')::uuid))::text
union all select 'Professor: doações fictícias',count(*)::text from public.doacao_historico where doador_id=current_setting('doesangue.professor_id')::uuid
union all select 'Professor: agendamentos',count(*)::text from public.agendamento where doador_id=current_setting('doesangue.professor_id')::uuid
union all select 'Professor: eventos de cancelamento',count(*)::text from public.agendamento_cancelamento where cancelado_por=current_setting('doesangue.professor_id')::uuid
union all select 'Professor: remarcações',count(*)::text from public.agendamento_remarcacao where doador_id=current_setting('doesangue.professor_id')::uuid
union all select 'Professor: rotina ativa',count(*)::text from public.rotina_lembrete where doador_id=current_setting('doesangue.professor_id')::uuid and ativa
union all select 'Integridade / RLS / isolamento','Conferidos; dados anteriores preservados pelos seeds';
rollback;
