// PostgreSQL descartável; não acessa Supabase nem credenciais reais.
import {createRequire} from 'node:module';
import {readFile,readdir,mkdir,writeFile} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../../..');
const toolDir=process.argv[2];
if(!toolDir) throw new Error('Informe o diretório da ferramenta PGlite.');
const {PGlite}=createRequire(resolve(toolDir,'package.json'))('@electric-sql/pglite');
const db=new PGlite();
try {
    await db.exec(`create role anon nologin; create role authenticated nologin;
        create schema auth; create table auth.users(id uuid primary key,raw_user_meta_data jsonb);
        create function auth.uid() returns uuid language sql stable as
        $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
        grant usage on schema public,auth to anon,authenticated;
        grant execute on function auth.uid() to anon,authenticated;`);
    const target='20261026000100_consultar_disponibilidade_agendamento.sql';
    const migrations=(await readdir(resolve(root,'supabase/migrations'))).filter(x=>x.endsWith('.sql')&&x<target).sort();
    for(const migration of migrations) await db.exec(await readFile(resolve(root,'supabase/migrations',migration),'utf8'));
    await db.exec(await readFile(resolve(root,'supabase/seed.sql'),'utf8'));
    await db.exec(`insert into auth.users values('00000000-0000-4000-8000-000000000001',
        '{"nome_exibicao":"Doador Sintético Disponibilidade","tipo_sanguineo":"NAO_SEI"}');
        insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        select id,statement_timestamp()+interval '1 month',2,true from public.unidade_coleta where nome='Unidade Demo A';
        insert into public.agendamento(id,doador_id,horario_id,chave_idempotencia) overriding system value
        select 23,'00000000-0000-4000-8000-000000000001',id,'00000000-0000-4000-8000-000000000023'
        from public.horario_disponivel limit 1;`);
    const snapshot=()=>db.query(`select 'unidades' as tipo,to_jsonb(t)::text as row from public.unidade_coleta t
        union all select 'horarios',to_jsonb(t)::text from public.horario_disponivel t
        union all select 'perfis',to_jsonb(t)::text from public.perfil_doador t
        union all select 'reservas',to_jsonb(t)::text from public.agendamento t order by tipo,row`);
    const before=(await snapshot()).rows;
    const authority=(await db.query(`select pg_get_functiondef('public.criar_agendamento(bigint,uuid)'::regprocedure) as definition`)).rows;
    const grants=(await db.query(`select relname,relacl::text,relrowsecurity from pg_class
        where oid in ('public.agendamento'::regclass,'public.horario_disponivel'::regclass) order by relname`)).rows;
    await db.exec(await readFile(resolve(root,'supabase/migrations',target),'utf8'));
    await db.exec(await readFile(resolve(root,'supabase/tests/disponibilidade_calendario_horarios.sql'),'utf8'));
    assert.deepEqual((await snapshot()).rows,before);
    assert.deepEqual((await db.query(`select pg_get_functiondef('public.criar_agendamento(bigint,uuid)'::regprocedure) as definition`)).rows,authority);
    assert.deepEqual((await db.query(`select relname,relacl::text,relrowsecurity from pg_class
        where oid in ('public.agendamento'::regclass,'public.horario_disponivel'::regclass) order by relname`)).rows,grants);
    for(const role of ['anon','authenticated']) {
        const result=await db.query(`select has_function_privilege($1,'public.consultar_horarios_disponiveis(bigint,timestamptz,timestamptz)','EXECUTE') as slots,
            has_function_privilege($1,'public.consultar_dias_disponiveis(bigint,date)','EXECUTE') as dates`,[role]);
        assert.deepEqual(result.rows,[{slots:true,dates:true}]);
    }
    const seed=await readFile(resolve(root,'supabase/seed_disponibilidade_futura.sql'),'utf8');
    const expected=await db.query(`select count(*)::int as n from public.unidade_coleta u cross join
        (values(timestamp '2026-10-28 08:00:00'),(timestamp '2026-10-28 10:00:00'),(timestamp '2026-10-29 08:00:00')) d(inicio)
        where u.nome='Unidade Demo A' and u.cidade='Cidade fictícia' and u.endereco='Endereço fictício A'
        and u.publicada and d.inicio at time zone u.fuso_horario>statement_timestamp()`);
    const seeded=(await db.exec(seed)).flatMap(x=>x.rows);
    assert.equal(seeded.length,expected.rows[0].n);
    const seededState=(await snapshot()).rows;
    assert.deepEqual((await db.exec(seed)).flatMap(x=>x.rows),[]);
    assert.deepEqual((await snapshot()).rows,seededState);
    if(seeded.length) {
        await db.query('update public.horario_disponivel set capacidade=99,ativo=false where id=$1',[seeded[0].id]);
        assert.deepEqual((await db.exec(seed)).flatMap(x=>x.rows),[]);
        assert.deepEqual((await db.query('select capacidade,ativo from public.horario_disponivel where id=$1',[seeded[0].id])).rows,
            [{capacidade:99,ativo:false}]);
    }
    const afterSeed=(await snapshot()).rows;
    for(const row of before) assert.ok(afterSeed.some(item=>item.tipo===row.tipo&&item.row===row.row));
    await mkdir(resolve(root,'app/build/reports/sql'),{recursive:true});
    await writeFile(resolve(root,'app/build/reports/sql/disponibilidade.json'),JSON.stringify({passed:true,
        migrations:migrations.length+1,checks:['futuro/ativo/publicado','vagas de todos os doadores/cancelados',
        'ordenação/bordas/fuso','dias agregados','validação de parâmetros','dois papéis/RLS preservada',
        'cancelamento libera vaga','reserva original/função de criação/grants preservados',
        'seed futuro somente ausentes/repetição sem duplicação/sem sobrescrita']},null,2));
    console.log('Disponibilidade: migração, horários/dias/vagas, dois papéis, parâmetros e preservação aprovados.');
} catch (error) {
    console.error(JSON.stringify({message:error.message,code:error.code,where:error.where,detail:error.detail}));
    process.exitCode=1;
} finally { await db.close(); }
