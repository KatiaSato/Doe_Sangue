import {createRequire} from 'node:module';
import {readFile,readdir,mkdir,writeFile} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../../..');
const {PGlite}=createRequire(resolve(process.argv[2],'package.json'))('@electric-sql/pglite');
const db=new PGlite();
const professor='00000000-0000-4000-8000-000000000002';
const previous='00000000-0000-4000-8000-000000000001';
const tables=['unidade_coleta','estoque_sangue','horario_disponivel','campanha','campanha_tipo_sanguineo',
 'perfil_doador','agendamento','agendamento_cancelamento','doacao_historico','agendamento_remarcacao','rotina_lembrete','agendamento_rotina_pedido'];
const snapshot=async()=>Object.fromEntries(await Promise.all(tables.map(async t=>[t,(await db.query(`select to_jsonb(x)::text registro from public.${t} x order by to_jsonb(x)::text`)).rows])));
try {
 await db.exec(`create role anon nologin;create role authenticated nologin;create schema auth;
 create table auth.users(id uuid primary key,raw_user_meta_data jsonb);
 create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
 grant usage on schema public,auth to anon,authenticated;`);
 const migrations=(await readdir(resolve(root,'supabase/migrations'))).filter(x=>x.endsWith('.sql')).sort();
 for(const name of migrations) await db.exec(await readFile(resolve(root,'supabase/migrations',name),'utf8'));
 await db.exec(await readFile(resolve(root,'supabase/seed.sql'),'utf8'));
 await db.exec(`insert into auth.users values('${previous}','{"nome_exibicao":"Perfil sintético preservado","tipo_sanguineo":"NAO_SEI"}'),('${professor}','{}');
 insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo) select id,timestamptz '2026-11-28 12:00:00+00',2,true from public.unidade_coleta where nome='Unidade Demo A';
 insert into public.agendamento(id,doador_id,horario_id,chave_idempotencia) overriding system value select 23,'${previous}',id,'00000000-0000-4000-8000-000000000023' from public.horario_disponivel limit 1;`);
 const before=await snapshot();
 const metadata=async()=>(await db.query(`select p.oid,p.prosrc,p.proacl::text,p.prosecdef,p.proconfig from pg_proc p where p.pronamespace='public'::regnamespace order by p.oid`)).rows;
 const functions=await metadata();
 const pub=await readFile(resolve(root,'supabase/seed_avaliacao_publica.sql'),'utf8');
 const priv=await readFile(resolve(root,'supabase/seed_avaliacao_professor.sql'),'utf8');
 const forUser=id=>priv.replace('begin;',`begin;select set_config('doesangue.professor_id','${id}',true);`);
 await db.exec(pub);const publicOnce=await snapshot();await db.exec(pub);assert.deepEqual(await snapshot(),publicOnce);
 assert.equal(tables.reduce((n,t)=>n+publicOnce[t].length-before[t].length,0),252);
 try {await db.exec(forUser('00000000-0000-4000-8000-000000000099'));assert.fail('conta inexistente aceita');}catch(e){assert.match(e.message,/Auth válida/);await db.exec('rollback');}
 assert.deepEqual(await snapshot(),publicOnce);
 await db.exec(forUser(professor));const once=await snapshot();await db.exec(forUser(professor));assert.deepEqual(await snapshot(),once);
 for(const t of tables) for(const row of before[t]) assert(once[t].some(x=>x.registro===row.registro),`${t}: dado antigo alterado`);
 assert.deepEqual(await metadata(),functions);
 assert.equal((await db.query(`select count(*)::int n from public.agendamento where doador_id='${professor}'`)).rows[0].n,7);
 assert.equal((await db.query(`select count(*)::int n from public.agendamento_cancelamento where cancelado_por='${professor}'`)).rows[0].n,4);
 await db.exec(`begin;set local role anon;`);
 assert.equal((await db.query(`select count(*)::int n from public.estoque_sangue where fonte='Demonstração acadêmica Doe Sangue — avaliação v1'`)).rows[0].n,80);
 assert.equal((await db.query(`select count(*)::int n from public.campanha where fonte='Demonstração acadêmica Doe Sangue — avaliação v1'`)).rows[0].n,19);
 await db.exec('rollback');
 for(const [id,donations] of [[professor,6],[previous,0]]) {
  await db.exec(`begin;set local role authenticated;select set_config('request.jwt.claim.sub','${id}',true);`);
  assert.equal((await db.query('select count(*)::int n from public.consultar_minhas_doacoes()')).rows[0].n,donations);
  assert.equal((await db.query('select count(*)::int n from public.consultar_minha_rotina()')).rows[0].n,id===professor?1:0);
  await db.exec('rollback');
 }
 await db.exec('alter table auth.users add column email_confirmed_at timestamptz default now();');
 const conference=await readFile(resolve(root,'supabase/tests/avaliacao_professor_conferencia.sql'),'utf8');
 await db.exec(conference.replace('begin;',`begin;set local doesangue.professor_id='${professor}';`));
 assert.deepEqual(await snapshot(),once);
 await db.exec(`delete from public.rotina_lembrete where doador_id='${professor}';`);
 const withoutRoutine=await snapshot();await db.exec(forUser(professor));assert.deepEqual(await snapshot(),withoutRoutine);
 const counts=Object.fromEntries(tables.map(t=>[t,once[t].length]));
 const report={passed:true,migrations:migrations.length,addedPublic:252,addedPrivate:tables.reduce((n,t)=>n+once[t].length-publicOnce[t].length,0),counts,
 checks:['repetição de ambos os seeds','preservação de todas as linhas anteriores','funções e ACL preservadas','conta ausente bloqueada sem inserção','RLS de publicados','histórico e rotina privados','replay não recria rotina excluída'],
 limits:['PGlite sequencial; papéis simulados, sem JWT ou login real; não prova concorrência']};
 const out=resolve(root,'app/build/reports/massa-avaliacao');await mkdir(out,{recursive:true});
 await writeFile(resolve(out,'local.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
} catch(e){console.error(JSON.stringify({message:e.message,code:e.code,detail:e.detail,where:e.where}));process.exitCode=1;}
finally{await db.close();}
