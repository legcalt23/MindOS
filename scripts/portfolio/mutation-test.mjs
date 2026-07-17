#!/usr/bin/env node
// Mutation ORACLE (C). Each mutation changes exactly ONE field/artifact in an isolated temp fixture and
// asserts the EXPECTED gate turns FAIL for the EXPECTED reason (diagnostic substring). A crash, a wrong
// gate, or a different diagnostic = TEST FAILURE. Uses the shared RFC-4180 parser/serializer. Never
// touches the real ledger/lock. Run: node scripts/portfolio/mutation-test.mjs
import { readFileSync, writeFileSync, mkdtempSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { parseCsv, parseCsvRaw, headerOf, toCsv } from './csv.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..', '..');
const validator = join(here, 'validate-portfolio.mjs');
const rel = (p) => join(root, p);
const CSV = readFileSync(rel('MINDBOOST_EXERCISE_PORTFOLIO.csv'), 'utf8');
const COLS = headerOf(CSV);
const ROWS = parseCsv(CSV);
const AUX = readFileSync(rel('MINDBOOST_EXERCISE_AUXILIARIES.csv'), 'utf8');
const MD = readFileSync(rel('MINDBOOST_EXERCISE_PORTFOLIO.md'), 'utf8');
const INV = readFileSync(rel('PORTFOLIO_CODE_INVENTORY.json'), 'utf8');
const MAN = readFileSync(rel('PORTFOLIO_SOURCE_MANIFEST.json'), 'utf8');
const DEL = readFileSync(rel('DELIVERY_MANIFEST.json'), 'utf8');
const SUMS = readFileSync(rel('SHA256SUMS.txt'), 'utf8');
const LOCKRES = readFileSync(rel('LOCK_SAFETY_RESULT.json'), 'utf8');

let tmpN = 0;
const wtmp = (name, content) => { const d = mkdtempSync(join(tmpdir(), `mut${tmpN++}-`)); const p = join(d, name); writeFileSync(p, content, 'utf8'); return p; };

// Build a mutated CSV changing exactly one (rowIdx, col). Verifies validity + single-field-change.
function mutateCsv(pred, col, val) {
  const objs = ROWS.map((r) => ({ ...r }));
  const idx = objs.findIndex(pred);
  if (idx < 0) throw new Error('mutation target row not found');
  objs[idx][col] = val;
  const text = toCsv(objs, COLS);
  // integrity checks: parses to 121 rows; exactly one cell differs vs original
  const re = parseCsv(text);
  if (re.length !== ROWS.length) throw new Error('mutation broke row count');
  let diffs = 0;
  for (let i = 0; i < ROWS.length; i++) for (const c of COLS) if ((re[i][c] || '') !== (ROWS[i][c] || '')) diffs++;
  if (diffs !== 1) throw new Error(`mutation changed ${diffs} cells, expected 1`);
  return text;
}
function runValidator(env) {
  try { const out = execFileSync('node', [validator, '--mode=repo', '--json'], { env: { ...process.env, ...env }, stdio: ['ignore', 'pipe', 'pipe'] }); return { crash: false, json: JSON.parse(out.toString()) }; }
  catch (e) { const so = (e.stdout || '').toString(); try { return { crash: false, json: JSON.parse(so) }; } catch { return { crash: true, stderr: (e.stderr || '').toString().slice(0, 200) }; } }
}

// A mutation spec: {id, desc, env(), expectedGate, expectedDiag}
const M = [];
const csvEnv = (text) => ({ PORTFOLIO_CSV: wtmp('m.csv', text) });
M.push({ id: 1, desc: 'fake target codeExerciseId (E1_VIGIA→TOTALLY_FAKE_CODE_ID)', gate: 'STATIC_CODE_ID_RECONCILIATION_GATE', diag: 'invented/unknown target codeExerciseId', env: () => csvEnv(mutateCsv((r) => r.codeExerciseId === 'E1_VIGIA', 'codeExerciseId', 'TOTALLY_FAKE_CODE_ID')) });
M.push({ id: 2, desc: 'inventory gains an unaccounted real id (GHOST_ID)', gate: 'STATIC_CODE_ID_RECONCILIATION_GATE', diag: 'not accounted', env: () => { const j = JSON.parse(INV); j.observedCodeIds.push('GHOST_ID'); return { PORTFOLIO_INVENTORY: wtmp('inv.json', JSON.stringify(j)) }; } });
M.push({ id: 3, desc: 'source route omitted (block rootExists=false)', gate: 'ROUTE_REACHABILITY_STATIC_GATE', diag: 'reachability not satisfied', env: () => { const j = JSON.parse(INV); j.blocks.FOCUS.rootExists = false; return { PORTFOLIO_INVENTORY: wtmp('inv.json', JSON.stringify(j)) }; } });
M.push({ id: 4, desc: 'generated MD wrong metric (Assigned candidates 999/121)', gate: 'GENERATED_VIEW_GATE', diag: 'differs from CSV', env: () => { const m = MD.replace(/Assigned candidates \| \d+\/121/, 'Assigned candidates | 999/121'); if (m === MD) throw new Error('MD metric anchor not found'); return { PORTFOLIO_MD: wtmp('m.md', m) }; } });
M.push({ id: 5, desc: 'SHA256SUMS empty', gate: 'IN_REPO_PACKAGE_GATE', diag: 'SHA256SUMS.txt is empty', env: () => ({ PORTFOLIO_SHA256SUMS: wtmp('sums.txt', '') }) });
M.push({ id: 6, desc: 'SHA256SUMS wrong hash for an artifact', gate: 'IN_REPO_PACKAGE_GATE', diag: 'hash missing/mismatch', env: () => ({ PORTFOLIO_SHA256SUMS: wtmp('sums.txt', SUMS.replace(/^[0-9a-f]{64}/m, '0'.repeat(64))) }) });
M.push({ id: 7, desc: 'delivery references a missing required artifact', gate: 'IN_REPO_PACKAGE_GATE', diag: 'required artifact missing', env: () => { const j = JSON.parse(DEL); j.artifacts.push({ path: 'DOES_NOT_EXIST.json', role: 'hashed', sha256: '0'.repeat(64) }); return { PORTFOLIO_DELIVERY: wtmp('del.json', JSON.stringify(j)) }; } });
M.push({ id: 8, desc: 'manifest source set incomplete (a source MISSING)', gate: 'IN_REPO_PACKAGE_GATE', diag: 'manifest source not present', env: () => { const j = JSON.parse(MAN); j.sources[0] = { ...j.sources[0], status: 'MISSING' }; return { PORTFOLIO_MANIFEST: wtmp('man.json', JSON.stringify(j)) }; } });
M.push({ id: 9, desc: 'design-only marked TRAINING_READY', gate: 'STATUS_CROSS_INVARIANTS_GATE', diag: 'TRAINING_READY but implementationStatus', env: () => csvEnv(mutateCsv((r) => r.implementationStatus === 'NOT_FOUND' && r.definitionStatus === 'DESIGNED', 'launchReadiness', 'TRAINING_READY')) });
M.push({ id: 10, desc: 'hidden marked TRAINING_READY', gate: 'STATUS_CROSS_INVARIANTS_GATE', diag: 'HIDDEN cannot be TRAINING_READY', env: () => csvEnv(mutateCsv((r) => r.implementationStatus === 'HIDDEN', 'launchReadiness', 'TRAINING_READY')) });
M.push({ id: 11, desc: 'fake current-verification (real path check)', gate: 'CLAIM_EVIDENCE_GATE', diag: 'evidencePaths not a real file', env: () => csvEnv(mutateCsv((r) => r.codeExerciseId === 'E2_FILTRO', 'evidencePaths', 'not-a-real-path')) });
M.push({ id: 12, desc: 'invalid status string PASS', gate: 'STATUS_CROSS_INVARIANTS_GATE', diag: 'not in closed enum', env: () => csvEnv(mutateCsv((r) => r.codeExerciseId === 'E3_GATILLO', 'tutorialStatus', 'PASS_TOTALLY_FAKE')) });
M.push({ id: 13, desc: 'design-only iosStatus=UNKNOWN', gate: 'STATUS_CROSS_INVARIANTS_GATE', diag: 'DESIGN_ONLY platform not NOT_IMPLEMENTED', env: () => csvEnv(mutateCsv((r) => r.implementationStatus === 'NOT_FOUND' && r.definitionStatus === 'DESIGNED', 'iosStatus', 'UNKNOWN')) });
M.push({ id: 14, desc: 'live androidStatus=NOT_APPLICABLE', gate: 'STATUS_CROSS_INVARIANTS_GATE', diag: 'LIVE androidStatus', env: () => csvEnv(mutateCsv((r) => r.implementationStatus === 'LIVE_REACHABLE', 'androidStatus', 'NOT_APPLICABLE')) });
M.push({ id: 15, desc: 'live generatorOrBankStatus=NOT_APPLICABLE', gate: 'STATUS_CROSS_INVARIANTS_GATE', diag: 'LIVE generator', env: () => csvEnv(mutateCsv((r) => r.implementationStatus === 'LIVE_REACHABLE', 'generatorOrBankStatus', 'NOT_APPLICABLE')) });
M.push({ id: 16, desc: 'UNKNOWN platform in an UNASSIGNED slot', gate: 'STATUS_CROSS_INVARIANTS_GATE', diag: 'UNASSIGNED but', env: () => csvEnv(mutateCsv((r) => r.definitionStatus === 'UNASSIGNED', 'iosStatus', 'UNKNOWN')) });
M.push({ id: 17, desc: 'auxiliary CheckIn occupies a target slot', gate: 'TARGET_SLOT_SEMANTICS_GATE', diag: 'occupies a target slot', env: () => csvEnv(mutateCsv((r) => r.codeExerciseId === 'E1_VIGIA', 'codeExerciseId', 'CheckIn')) });
M.push({ id: 18, desc: 'lock safety result = fail', gate: 'LEDGER_LOCK_SAFETY_GATE', diag: 'not passing', env: () => ({ PORTFOLIO_LOCKRESULT: wtmp('lr.json', JSON.stringify({ pass: false, fails: 3 })) }) });

const results = [];
for (const m of M) {
  let env; try { env = m.env(); } catch (e) { results.push({ ...m, ok: false, why: 'mutation-setup-error: ' + e.message }); continue; }
  const r = runValidator(env);
  if (r.crash) { results.push({ ...m, ok: false, why: 'validator CRASHED: ' + (r.stderr || '') }); continue; }
  const g = r.json.gates[m.gate];
  const failed = g && g.status === 'FAIL';
  const diagOk = failed && g.diagnostics.some((d) => d.includes(m.diag));
  results.push({ ...m, ok: failed && diagOk, actual: g ? g.status : 'ABSENT', why: !failed ? `expected ${m.gate}=FAIL, got ${g ? g.status : 'ABSENT'}` : (!diagOk ? `gate FAILed but diagnostic missing "${m.diag}"` : '') });
}

let pass = true;
console.log('MUTATION ORACLE (expected gate + diagnostic per mutation):');
for (const r of results) { console.log(`  #${String(r.id).padStart(2)} ${r.ok ? 'OK ' : 'BAD'}  ${r.gate} ⇐ ${r.desc}${r.ok ? '' : '  [' + r.why + ']'}`); if (!r.ok) pass = false; }
console.log(pass ? `MUTATION SUITE: PASS (${results.length}/${results.length} caught by the expected gate+diagnostic)` : 'MUTATION SUITE: FAIL');
process.exit(pass ? 0 : 1);
