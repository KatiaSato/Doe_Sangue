// PostgreSQL descartável: não usa rede, credenciais nem contas reais.
import {createRequire} from 'node:module';
import {readFile,readdir,mkdir,writeFile} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../../..');
const {PGlite}=createRequire(resolve(process.argv[2],'package.json'))('@electric-sql/pglite');
const db=new PGlite();
try {
    await db.exec(`create role anon nologin; create role authenticated nologin;
      create schema auth; create table auth.users(id uuid primary key,raw_user_meta_data jsonb);
      create function auth.uid() returns uuid language sql stable as
      $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
      grant usage on schema public,auth to anon,authenticated;`);
    const migrations=(await readdir(resolve(root,'supabase/migrations'))).filter(x=>x.endsWith('.sql')).sort();
    const old=migrations.filter(x=>x<'20261026000300');
    for(const name of old) await db.exec(await readFile(resolve(root,'supabase/migrations',name),'utf8'));
    await db.exec(await readFile(resolve(root,'supabase/seed.sql'),'utf8'));
    await db.exec(`insert into auth.users values
      ('00000000-0000-4000-8000-000000000001','{"nome_exibicao":"Doador Sintético A","tipo_sanguineo":"NAO_SEI"}'),
      ('00000000-0000-4000-8000-000000000002','{"nome_exibicao":"Doador Sintético B","tipo_sanguineo":"NAO_SEI"}');
      insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
      select id,clock_timestamp()+interval '30 days',2,true from public.unidade_coleta where nome='Unidade Demo A';
      insert into public.agendamento(id,doador_id,horario_id,chave_idempotencia) overriding system value
      select 23,'00000000-0000-4000-8000-000000000001',id,'00000000-0000-4000-8000-000000000023' from public.horario_disponivel limit 1;`);
    const definitions=()=>db.query(`select proname,prosrc,proacl::text,prosecdef,provolatile,proconfig,prorettype::text,proargtypes::text from pg_proc
      where oid in ('public.criar_agendamento(bigint,uuid)'::regprocedure,'public.cancelar_agendamento(bigint)'::regprocedure,
      'public.consultar_meus_agendamentos()'::regprocedure) order by proname`);
    const snapshot=async()=>{
        const names=['unidade_coleta','horario_disponivel','perfil_doador','agendamento','agendamento_cancelamento'];
        return Promise.all(names.map(async n=>(await db.query(`select to_jsonb(t)::text from public.${n} t order by id`)).rows));
    };
    const before=(await definitions()).rows,data=await snapshot();
    const acl=(await db.query(`select oid,relname,relacl::text,relrowsecurity from pg_class where relnamespace='public'::regnamespace order by oid`)).rows;
    for(const name of migrations.filter(x=>!old.includes(x))) await db.exec(await readFile(resolve(root,'supabase/migrations',name),'utf8'));
    assert.deepEqual((await definitions()).rows,before);
    assert.deepEqual(await snapshot(),data);
    for(const row of acl) assert.deepEqual((await db.query('select oid,relname,relacl::text,relrowsecurity from pg_class where oid=$1',[row.oid])).rows[0],row);
    await db.exec(await readFile(resolve(root,'supabase/tests/remarcacao_rotina_lembretes.sql'),'utf8'));
    assert.deepEqual(await snapshot(),data);
    for(const name of ['agendamento_remarcacao','rotina_lembrete','agendamento_rotina_pedido']) {
        assert.equal((await db.query('select relrowsecurity from pg_class where oid=$1::regclass',['public.'+name])).rows[0].relrowsecurity,true);
        for(const role of ['anon','authenticated']) for(const privilege of ['SELECT','INSERT','UPDATE','DELETE','TRUNCATE'])
            assert.equal((await db.query('select has_table_privilege($1,$2,$3) as ok',[role,'public.'+name,privilege])).rows[0].ok,false);
    }
    // Isolamentos incompatíveis: mutações recusadas, sem alterar dados.
    for(const level of ['repeatable read','serializable']) {
        await db.exec(`begin; set transaction isolation level ${level}; set local role authenticated;
          select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000001',true);`);
        try { await db.query("select public.remarcar_agendamento(23,1,'00000000-0000-4000-8000-000000000099')"); assert.fail('isolamento aceito'); }
        catch(e) { assert.equal(e.code,'25000'); }
        await db.exec('rollback');
    }
    const out=resolve(root,'app/build/reports/sql'); await mkdir(out,{recursive:true});
    await writeFile(resolve(out,'remarcacao-rotina.json'),JSON.stringify({passed:true,migrations:migrations.length,
      checks:['originais/ACL/corpos preservados','remarcação+evento+replay','falha atômica','vínculos privados','rotina isolada e versionada',
      'excluir/recriar sem ABA','reserva+rotina atômicas','replay não recria preferência','RLS/ACL/auxiliares','isolamentos','dados preservados após ROLLBACK'],
      limits:['PGlite sequencial; não prova JWT nem concorrência entre conexões']},null,2));
    console.log('F13/F14: migrações e roteiro ROLLBACK aprovados; dados, funções e ACL anteriores preservados.');
} catch(e) { console.error(JSON.stringify({message:e.message,code:e.code,detail:e.detail,where:e.where})); process.exitCode=1; }
finally {await db.close();}
