// PostgreSQL descartável; nenhum acesso remoto/credencial/conta real.
import {createRequire} from 'node:module';
import {readFile,readdir,mkdir,writeFile} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../../..');
const toolDir=process.argv[2];
if(!toolDir) throw new Error('Informe o diretório PGlite externo ao app.');
const {PGlite}=createRequire(resolve(toolDir,'package.json'))('@electric-sql/pglite');
const db=new PGlite();
const target='20261026000200_identificar_erros_agendamento.sql';
const normalize=body=>body.replace(/, detail = 'doesangue\.[a-z_]+\.v1'/g,'')
    .replace(/ using detail = 'doesangue\.[a-z_]+\.v1'/g,'').replace(/\s+/g,' ').trim();
try {
    await db.exec(`create role anon nologin; create role authenticated nologin;
        create schema auth; create table auth.users(id uuid primary key,raw_user_meta_data jsonb);
        create function auth.uid() returns uuid language sql stable as
        $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
        grant usage on schema public,auth to anon,authenticated;`);
    const migrations=(await readdir(resolve(root,'supabase/migrations'))).filter(x=>x.endsWith('.sql')&&x<target).sort();
    for(const name of migrations) await db.exec(await readFile(resolve(root,'supabase/migrations',name),'utf8'));
    await db.exec(await readFile(resolve(root,'supabase/seed.sql'),'utf8'));
    await db.exec(`insert into auth.users values
        ('00000000-0000-4000-8000-000000000001','{"nome_exibicao":"Doador Sintético A","tipo_sanguineo":"NAO_SEI"}'),
        ('00000000-0000-4000-8000-000000000002','{"nome_exibicao":"Doador Sintético B","tipo_sanguineo":"NAO_SEI"}');
        insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        select id,statement_timestamp()+interval '1 month',2,true from public.unidade_coleta where nome='Unidade Demo A';
        insert into public.agendamento(id,doador_id,horario_id,chave_idempotencia) overriding system value
        select 23,'00000000-0000-4000-8000-000000000001',id,'00000000-0000-4000-8000-000000000023'
        from public.horario_disponivel limit 1;`);
    const definitions=()=>db.query(`select proname,prosrc,proacl::text,prosecdef,provolatile,proconfig,prorettype::text,proargtypes::text
        from pg_proc where oid in ('public.criar_agendamento(bigint,uuid)'::regprocedure,'public.cancelar_agendamento(bigint)'::regprocedure)
        order by proname`);
    const tables=()=>db.query(`select relname,relacl::text,relrowsecurity from pg_class where relnamespace='public'::regnamespace order by relname`);
    const snapshot=()=>db.query(`select 'unidades' as tipo,to_jsonb(t)::text as row from public.unidade_coleta t
        union all select 'horarios',to_jsonb(t)::text from public.horario_disponivel t
        union all select 'perfis',to_jsonb(t)::text from public.perfil_doador t
        union all select 'reservas',to_jsonb(t)::text from public.agendamento t
        union all select 'eventos',to_jsonb(t)::text from public.agendamento_cancelamento t order by tipo,row`);
    const before=(await definitions()).rows, data=(await snapshot()).rows, acl=(await tables()).rows;
    await db.exec(await readFile(resolve(root,'supabase/migrations',target),'utf8'));
    const after=(await definitions()).rows;
    for(let i=0;i<before.length;i++) {
        assert.deepEqual({...after[i],prosrc:normalize(after[i].prosrc)},{...before[i],prosrc:normalize(before[i].prosrc)});
        assert.ok(after[i].prosrc.includes('doesangue.'));
    }
    await db.exec(await readFile(resolve(root,'supabase/tests/erros_agendamento_identificadores.sql'),'utf8'));
    assert.deepEqual((await snapshot()).rows,data);
    assert.deepEqual((await tables()).rows,acl);
    for(const role of ['anon','authenticated']) for(const fn of ['criar_agendamento(bigint,uuid)','cancelar_agendamento(bigint)']) {
        const allowed=(await db.query('select has_function_privilege($1,$2,\'EXECUTE\') as allowed',[role,'public.'+fn])).rows[0].allowed;
        assert.equal(allowed,role==='authenticated');
    }
    await mkdir(resolve(root,'app/build/reports/sql'),{recursive:true});
    await writeFile(resolve(root,'app/build/reports/sql/erros-agendamento.json'),JSON.stringify({passed:true,migrations:migrations.length+1,
        checks:['somente DETAIL nos corpos','SQLSTATE e mensagens preservados','assinaturas/grants/RLS preservados',
        'causas de criação/cancelamento','inexistente e alheio indistinguíveis','idempotência e evento único',
        'isolamentos rejeitados','dados/perfis/reserva23 preservados']},null,2));
    console.log('F19: identificadores, regras e permissões preservados; testes descartáveis aprovados.');
} catch(error) {
    console.error(JSON.stringify({message:error.message,code:error.code,where:error.where,detail:error.detail}));
    process.exitCode=1;
} finally { await db.close(); }
