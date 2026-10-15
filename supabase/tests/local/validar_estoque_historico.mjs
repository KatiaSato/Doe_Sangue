// PostgreSQL em memória, sem rede/credenciais. PGlite é ferramenta externa de teste,
// não dependência do Android. Primeiro argumento: diretório temporário do pacote.
import { createRequire } from 'node:module';
import { readFile, readdir, mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { resolve, dirname } from 'node:path';
import assert from 'node:assert/strict';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const toolDir = process.argv[2];
if (!toolDir) throw new Error('Informe o diretório temporário onde PGlite foi instalado.');
const require = createRequire(resolve(toolDir, 'package.json'));
const { PGlite } = require('@electric-sql/pglite');
const db = new PGlite();
const results = [];
const runFile = async (relative) => {
    await db.exec(await readFile(resolve(root, relative), 'utf8'));
    results.push({ script: relative, passed: true });
};

try {
    // Simula só as interfaces Auth usadas pelo SQL; não simula login/JWT/API.
    await db.exec(`
        create role anon nologin;
        create role authenticated nologin;
        create schema auth;
        create table auth.users (id uuid primary key, raw_user_meta_data jsonb);
        create function auth.uid() returns uuid language sql stable as
        $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
        grant usage on schema public, auth to anon, authenticated;
        grant execute on function auth.uid() to anon, authenticated;
    `);
    const migrations = (await readdir(resolve(root, 'supabase/migrations'))).filter(n => n.endsWith('.sql')).sort();
    for (const name of migrations) await runFile(`supabase/migrations/${name}`);
    await runFile('supabase/seed.sql');
    await db.exec(`
        insert into auth.users (id, raw_user_meta_data) values
        ('00000000-0000-4000-8000-000000000001', '{"nome_exibicao":"Doador Demo A","tipo_sanguineo":"NAO_SEI"}'),
        ('00000000-0000-4000-8000-000000000003', '{"nome_exibicao":"Doador Demo C","tipo_sanguineo":"NAO_SEI"}');
        insert into public.horario_disponivel (unidade_id, inicio, capacidade, ativo)
        select id, timestamptz '2026-10-19 11:00:00+00', 10, true from public.unidade_coleta where nome = 'Unidade Demo A';
        insert into public.agendamento (id, doador_id, horario_id, chave_idempotencia, criado_em)
        overriding system value values (23, '00000000-0000-4000-8000-000000000001', 1,
        '00000000-0000-4000-8000-000000000023', timestamptz '2026-10-12 23:14:35.586383+00');
    `);
    const preserved = (await db.query(`select row_to_json(a)::text as row from public.agendamento a where id=23`)).rows;
    await runFile('supabase/seed_estoque_historico.sql');
    const first = (await db.query(`select id, referencia from public.doacao_historico order by id`)).rows;
    await runFile('supabase/seed_estoque_historico.sql');
    assert.deepEqual((await db.query(`select id, referencia from public.doacao_historico order by id`)).rows, first);
    assert.equal((await db.query('select count(*)::int as n from public.estoque_sangue')).rows[0].n, 16);
    assert.equal(first.length, 3);
    results.push({ check: 'Seed repetido preserva IDs e contagens 16/3', passed: true });
    for (const name of ['integridade', 'permissoes', 'leitura']) {
        await runFile(`supabase/tests/estoque_historico_${name}.sql`);
    }
    await runFile('supabase/tests/estoque_historico_conferencia.sql');
    assert.deepEqual((await db.query(`select row_to_json(a)::text as row from public.agendamento a where id=23`)).rows, preserved);
    assert.equal((await db.query('select count(*)::int as n from public.estoque_sangue')).rows[0].n, 16);
    assert.equal((await db.query('select count(*)::int as n from public.doacao_historico')).rows[0].n, 3);
    const config = (await db.query(`select proname, prosecdef, provolatile, proconfig from pg_proc
        where proname in ('consultar_estoques', 'consultar_minhas_doacoes') order by proname`)).rows;
    assert.equal(config[0].prosecdef, false);
    assert.equal(config[1].prosecdef, true);
    for (const fn of config) {
        assert.equal(fn.provolatile, 's');
        assert.deepEqual(fn.proconfig, ['search_path=""']);
    }
    const report = { engine: (await db.query('select version() as version')).rows[0].version,
        pglite: JSON.parse(await readFile(resolve(dirname(require.resolve('@electric-sql/pglite')), '../package.json'), 'utf8')).version,
        scope: 'Local isolado, Auth mínimo simulado; não é prova de Supabase/API/JWT.', results, config };
    const reportDir = resolve(root, 'app/build/reports/sql');
    await mkdir(reportDir, { recursive: true });
    await writeFile(resolve(reportDir, 'estoque-historico.json'), JSON.stringify(report, null, 2));
    console.log(JSON.stringify({ passed: true, checks: results.length, stock: 16, history: 3, migrations: migrations.length }));
} catch (error) {
    console.error(JSON.stringify({ passed: false, message: error.message, code: error.code, where: error.where }));
    process.exitCode = 1;
} finally {
    await db.close();
}
