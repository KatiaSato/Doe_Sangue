// Banco PostgreSQL descartável em memória. Nenhum acesso remoto ou conta real.
import {createRequire} from 'node:module';
import {readFile, readdir, mkdir, writeFile} from 'node:fs/promises';
import {resolve, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const toolDir = process.argv[2];
if (!toolDir) throw new Error('Informe o diretório da ferramenta de testes PGlite.');
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
    const migrations = (await readdir(resolve(root, 'supabase/migrations'))).filter(n => n.endsWith('.sql')).sort();
    for (const name of migrations) await db.exec(await readFile(resolve(root, 'supabase/migrations', name), 'utf8'));
    await db.exec(`insert into auth.users values
        ('00000000-0000-4000-8000-000000000001', '{"nome_exibicao":"Doador sintético A","tipo_sanguineo":"NAO_SEI"}'),
        ('00000000-0000-4000-8000-000000000002', '{"nome_exibicao":"Doador sintético B","tipo_sanguineo":"NAO_SEI"}');`);
    await db.exec(await readFile(resolve(root, 'supabase/seed.sql'), 'utf8'));
    await db.exec(`insert into public.horario_disponivel(unidade_id, inicio, capacidade, ativo)
        select id, timestamptz '2026-10-19 11:00:00+00', 10, true from public.unidade_coleta where nome = 'Unidade Demo A';
        insert into public.agendamento(id, doador_id, horario_id, chave_idempotencia)
        overriding system value select 23, '00000000-0000-4000-8000-000000000001', id,
        '00000000-0000-4000-8000-000000000023' from public.horario_disponivel limit 1;`);
    assert.deepEqual((await db.query('select telefone, data_nascimento from public.perfil_doador')).rows,
        [{telefone: null, data_nascimento: null}, {telefone: null, data_nascimento: null}]);
    const before = (await db.query('select to_jsonb(p)::text as row from public.perfil_doador p order by id')).rows;
    const reservation = (await db.query('select to_jsonb(a)::text as row from public.agendamento a where id=23')).rows;
    assert.equal(reservation.length, 1);
    await db.exec(await readFile(resolve(root, 'supabase/tests/perfil_edicao_integridade_isolamento.sql'), 'utf8'));
    assert.deepEqual((await db.query('select to_jsonb(p)::text as row from public.perfil_doador p order by id')).rows, before);
    assert.deepEqual((await db.query('select to_jsonb(a)::text as row from public.agendamento a where id=23')).rows, reservation);
    await db.exec(await readFile(resolve(root, 'supabase/tests/perfil_edicao_catalogo.sql'), 'utf8'));
    await mkdir(resolve(root, 'app/build/reports/sql'), {recursive:true});
    await writeFile(resolve(root, 'app/build/reports/sql/edicao-perfil.json'), JSON.stringify({
        migrations: migrations.length, passed: true, checks: ['novos campos nulos', 'edição própria',
            'calendário', 'telefone', 'limpeza', 'privilégios', 'RLS', 'anon', 'rollback preserva perfis'],
        limits: 'Auth mínimo simulado; sem JWT, HTTP, Supabase remoto ou contas reais.',
    }, null, 2));
    console.log('SQL de edição do perfil aprovado; dados preservados pelo ROLLBACK.');
} finally { await db.close(); }
