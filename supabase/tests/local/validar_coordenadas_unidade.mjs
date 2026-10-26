// PostgreSQL descartável em memória; sem acesso ao Supabase ou credenciais reais.
import {createRequire} from 'node:module';
import {readFile, readdir, mkdir, writeFile} from 'node:fs/promises';
import {resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const toolDir = process.argv[2];
if (!toolDir) throw new Error('Informe o diretório da ferramenta PGlite.');
const {PGlite} = createRequire(resolve(toolDir, 'package.json'))('@electric-sql/pglite');
const db = new PGlite();
try {
    await db.exec(`create role anon nologin; create role authenticated nologin;
        create schema auth;
        create table auth.users(id uuid primary key, raw_user_meta_data jsonb);
        create function auth.uid() returns uuid language sql stable as
        $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
        grant usage on schema public, auth to anon, authenticated;
        grant execute on function auth.uid() to anon, authenticated;`);
    const migrations = (await readdir(resolve(root, 'supabase/migrations')))
        .filter(n => n.endsWith('.sql')).sort();
    const target = '20261024000100_adicionar_coordenadas_unidade.sql';
    assert.ok(migrations.includes(target));
    for (const name of migrations.filter(n => n < target)) {
        await db.exec(await readFile(resolve(root, 'supabase/migrations', name), 'utf8'));
    }
    await db.exec(await readFile(resolve(root, 'supabase/seed.sql'), 'utf8'));
    await db.exec(`insert into auth.users values
        ('00000000-0000-4000-8000-000000000001','{"nome_exibicao":"Doador Sintético","tipo_sanguineo":"NAO_SEI"}');
        insert into public.horario_disponivel(unidade_id,inicio,capacidade,ativo)
        select id, timestamptz '2026-11-20 11:00:00+00',10,true from public.unidade_coleta
        where nome='Unidade Demo A';
        insert into public.agendamento(id,doador_id,horario_id,chave_idempotencia)
        overriding system value select 23,'00000000-0000-4000-8000-000000000001',id,
        '00000000-0000-4000-8000-000000000023' from public.horario_disponivel limit 1;`);
    const units = (await db.query('select to_jsonb(u)::text as row from public.unidade_coleta u order by id')).rows;
    const reservation = (await db.query('select to_jsonb(a)::text as row from public.agendamento a where id=23')).rows;
    assert.equal(reservation.length,1);
    await db.exec(await readFile(resolve(root,'supabase/migrations',target),'utf8'));
    assert.deepEqual((await db.query(`select (to_jsonb(u)-'latitude'-'longitude')::text as row
        from public.unidade_coleta u order by id`)).rows,units);
    assert.equal((await db.query(`select count(*)::int as n from public.unidade_coleta
        where latitude is not null or longitude is not null`)).rows[0].n,0);
    await db.exec(await readFile(resolve(root,'supabase/tests/unidade_coordenadas_integridade_permissoes.sql'),'utf8'));
    assert.deepEqual((await db.query('select to_jsonb(a)::text as row from public.agendamento a where id=23')).rows,reservation);
    assert.deepEqual((await db.query(`select (to_jsonb(u)-'latitude'-'longitude')::text as row
        from public.unidade_coleta u order by id`)).rows,units);
    assert.equal((await db.query(`select count(*)::int as n from public.unidade_coleta
        where latitude is not null or longitude is not null`)).rows[0].n,0);
    // Seed: somente as duas correspondências exatas; não tocar em nomes semelhantes.
    await db.exec(`insert into public.unidade_coleta(nome,cidade,endereco,fuso_horario,publicada) values
        ('Unidade Demo A','Outra Cidade Sintética','Endereço fictício A','America/Sao_Paulo',false),
        ('Unidade Demo A','Cidade fictícia','Outro Endereço Sintético','America/Sao_Paulo',true),
        ('Outra Unidade Sintética','Cidade fictícia','Endereço fictício A','America/Sao_Paulo',true);`);
    const seedBase = (await db.query(`select (to_jsonb(u)-'latitude'-'longitude')::text as row
        from public.unidade_coleta u order by id`)).rows;
    const profiles = (await db.query('select to_jsonb(p)::text as row from public.perfil_doador p order by id')).rows;
    const seedSql = await readFile(resolve(root,'supabase/seed_coordenadas_unidades.sql'),'utf8');
    const first = (await db.exec(seedSql)).flatMap(result=>result.rows);
    assert.equal(first.length,2);
    const demoA = first.find(row=>row.nome==='Unidade Demo A');
    const demoB = first.find(row=>row.nome==='Unidade Demo B');
    assert.ok(demoA && demoB);
    assert.equal(demoA.latitude,-22.9); assert.equal(demoA.longitude,-47.05);
    assert.equal(demoB.latitude,-22.91); assert.equal(demoB.longitude,-47.06);
    assert.equal(demoA.publicada,true); assert.equal(demoB.publicada,false);
    const firstState = (await db.query('select to_jsonb(u)::text as row from public.unidade_coleta u order by id')).rows;
    assert.deepEqual((await db.exec(seedSql)).flatMap(result=>result.rows),[]);
    assert.deepEqual((await db.query('select to_jsonb(u)::text as row from public.unidade_coleta u order by id')).rows,firstState);
    // Posição já cadastrada permanece; a unidade sem posição ainda é preenchida.
    await db.query('update public.unidade_coleta set latitude=-20,longitude=-40 where id=$1',[demoA.id]);
    await db.query('update public.unidade_coleta set latitude=null,longitude=null where id=$1',[demoB.id]);
    const refill = (await db.exec(seedSql)).flatMap(result=>result.rows);
    assert.equal(refill.length,1); assert.equal(refill[0].id,demoB.id);
    assert.deepEqual((await db.query('select latitude,longitude from public.unidade_coleta where id=$1',[demoA.id])).rows,
        [{latitude:-20,longitude:-40}]);
    assert.deepEqual((await db.query(`select (to_jsonb(u)-'latitude'-'longitude')::text as row
        from public.unidade_coleta u order by id`)).rows,seedBase);
    assert.equal((await db.query(`select count(*)::int as n from public.unidade_coleta
        where latitude is not null or longitude is not null`)).rows[0].n,2);
    for (const role of ['anon','authenticated']) {
        await db.exec(`set role ${role}`);
        try {
            const visible = (await db.query(`select id,latitude,longitude from public.unidade_coleta
                where id in ($1,$2) order by id`,[demoA.id,demoB.id])).rows;
            assert.deepEqual(visible,[{id:demoA.id,latitude:-20,longitude:-40}]);
        } finally { await db.exec('reset role'); }
    }
    assert.deepEqual((await db.query('select to_jsonb(p)::text as row from public.perfil_doador p order by id')).rows,profiles);
    assert.deepEqual((await db.query('select to_jsonb(a)::text as row from public.agendamento a where id=23')).rows,reservation);
    await mkdir(resolve(root,'app/build/reports/sql'),{recursive:true});
    await writeFile(resolve(root,'app/build/reports/sql/coordenadas-unidade.json'),JSON.stringify({
        passed:true, migrations:migrations.filter(n=>n<=target).length,
        checks:['migração sobre unidades existentes','par nulo/completo','zero/limites inclusivos',
            '12 pares inválidos e constraint específica','RLS/publicação','leitura anon/authenticated',
            'INSERT/UPDATE/DELETE/TRUNCATE bloqueados','ROLLBACK preserva unidades/reserva 23',
            'seed preenche somente correspondências exatas','seed repetido não altera registros',
            'seed preserva posição existente e completa somente ausente','publicação/leitura de coordenadas preservadas',
            'seed preserva demais campos, perfis e reserva 23'],
        limits:'PostgreSQL em memória/Auth mínimo simulado; não comprova JWT, HTTP ou Supabase remoto.',
    },null,2));
    console.log('Coordenadas: migração/integridade/RLS/permissões e seed aprovados; repetição e preservação conferidas.');
} finally { await db.close(); }
