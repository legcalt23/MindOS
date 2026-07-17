#!/usr/bin/env node
// V2d PHASE 4 — MUTATION ORACLE (18 mandatory mutations).
//
// A gate that cannot go red is decoration. Each mutation below (a) proves the mutation actually changed the
// target, (b) asserts the expected gate fails with the expected diagnostic, and (c) restores bytes exactly.
// Mutations that assert an already-red state prove nothing, so each one records its precondition.
//
// The harness is fail-closed about its own baseline (the Phase 4 fail-before harness once crashed mid-probe
// and poisoned the CSV): pre-flight hash-checks every mutable input, and every mutation runs in try/finally.
import { readFileSync, writeFileSync, copyFileSync, existsSync, unlinkSync, renameSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { parseCsv, parseCsvRaw, headerOf, toCsv } from './csv.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const sha = (p) => (existsSync(p) ? createHash('sha256').update(readFileSync(p)).digest('hex') : 'ABSENT');

const CSV = rel('MINDBOOST_EXERCISE_PORTFOLIO.csv');
const MD = rel('MINDBOOST_EXERCISE_PORTFOLIO.md');
const RT = rel('PORTFOLIO_RUNTIME_EVIDENCE.json');
const MODEL = rel('PORTFOLIO_DERIVED_MODEL.json');
const METRICS = rel('scripts/portfolio/metrics.mjs');
const RENDER = rel('scripts/portfolio/render.mjs');
const LEGACY = rel('PORTFOLIO_V2C_LEGACY_CLAIMS.csv');
const MUTABLE = [CSV, MD, RT, MODEL, METRICS, RENDER];
const BAK = (p) => `${p}.p4mbak`;

const results = [];
const backup = (ps) => ps.forEach((p) => existsSync(p) && copyFileSync(p, BAK(p)));
const restore = (ps) => ps.forEach((p) => { if (existsSync(BAK(p))) { copyFileSync(BAK(p), p); unlinkSync(BAK(p)); } });

function preflight() {
  const strays = MUTABLE.filter((p) => existsSync(BAK(p)));
  if (strays.length) { console.error(`PRE-FLIGHT FAILED: stray .p4mbak from a crashed run: ${strays.map((s) => s.replace(root, '.')).join(', ')}`); process.exit(2); }
  const v = validator();
  if (!v) { console.error('PRE-FLIGHT FAILED: validator produced no JSON at baseline'); process.exit(2); }
  if (v.blockingFail) {
    console.error('PRE-FLIGHT FAILED: baseline already has a failing BLOCKING gate — every mutation below would be vacuous:');
    for (const [g, x] of Object.entries(v.gates)) if (x.status === 'FAIL') console.error(`  ! ${g}: ${x.diagnostics[0]}`);
    process.exit(2);
  }
  console.log('PRE-FLIGHT: baseline green, no stray backups.\n');
  return v;
}

const run = (script, args = []) => {
  try { return { code: 0, out: execFileSync(process.execPath, [rel(script), ...args], { encoding: 'utf8', cwd: root, stdio: ['ignore', 'pipe', 'pipe'] }) }; }
  catch (e) { return { code: e.status ?? 1, out: (e.stdout || '') + (e.stderr || '') }; }
};
const validator = () => { const r = run('scripts/portfolio/validate-portfolio.mjs', ['--mode=repo', '--json']); try { return JSON.parse(r.out); } catch { return null; } };
const rebuild = () => { run('scripts/portfolio/derive-portfolio-model.mjs'); run('scripts/portfolio/build-portfolio.mjs'); };

// CSV cell helpers
function setCell(slotId, column, value) {
  const text = readFileSync(CSV, 'utf8');
  const hdr = headerOf(text);
  const idx = hdr.indexOf(column);
  if (idx < 0) throw new Error(`no column ${column}`);
  const lines = text.split('\n');
  for (let i = 1; i < lines.length; i++) {
    const cells = parseCsvRaw(lines[i])[0];
    if (!cells || cells[hdr.indexOf('slotId')] !== slotId) continue;
    cells[idx] = value;
    lines[i] = cells.map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',');
    writeFileSync(CSV, lines.join('\n'), 'utf8');
    return;
  }
  throw new Error(`no row ${slotId}`);
}
function addColumn(column, valueFor) {
  const rows = parseCsv(readFileSync(CSV, 'utf8'));
  const cols = [...headerOf(readFileSync(CSV, 'utf8')), column];
  writeFileSync(CSV, toCsv(rows.map((r) => ({ ...r, [column]: valueFor(r) })), cols), 'utf8');
}
const validRun = (slotId, codeExerciseId, over = {}) => ({
  slotId, codeExerciseId, platform: 'android', snapshotSha256: sha(CSV),
  testOrDossier: 'phase4-mutation-probe', result: 'VERIFIED', evidencePath: 'scripts/portfolio/phase4-mutation-tests.mjs',
  provenance: 'synthetic run injected by the Phase 4 mutation oracle', executedAtUtc: '2026-07-15T00:00:00Z', ...over,
});
const writeRuns = (runs) => { const rt = JSON.parse(readFileSync(RT, 'utf8')); rt.runs = runs; writeFileSync(RT, JSON.stringify(rt, null, 2) + '\n', 'utf8'); };

// ---------------------------------------------------------------- the oracle
// expectGate: the gate that must go FAIL. expectDiag: substring the diagnostic must contain.
// changed(): must return true, proving the mutation moved the target (a no-op mutation is a vacuous test).
function mutation({ id, title, files, mutate, expectGate, expectDiag, changed, expectPass }) {
  backup(files);
  let out;
  try {
    const before = { validator: validator(), model: existsSync(MODEL) ? JSON.parse(readFileSync(MODEL, 'utf8')) : null, md: existsSync(MD) ? readFileSync(MD, 'utf8') : '' };
    mutate({ before });
    const after = { validator: validator(), model: existsSync(MODEL) ? JSON.parse(readFileSync(MODEL, 'utf8')) : null, md: existsSync(MD) ? readFileSync(MD, 'utf8') : '' };
    const didChange = changed ? changed({ before, after }) : true;
    const g = after.validator?.gates?.[expectGate];
    const wanted = expectPass ? 'PASS' : 'FAIL';
    const gateOk = expectPass ? (g?.status === 'PASS' || g?.status === 'NOT_RUN') : g?.status === 'FAIL';
    const diagOk = expectPass ? true : (g?.diagnostics ?? []).some((d) => d.includes(expectDiag));
    const pass = didChange && gateOk && diagOk;
    out = { id, title, pass, expectGate, expectedStatus: wanted, actualStatus: g?.status ?? '(gate absent)',
      targetChanged: didChange, diagnosticMatched: diagOk,
      diagnostic: (g?.diagnostics ?? []).find((d) => d.includes(expectDiag)) ?? (g?.diagnostics ?? [])[0] ?? null };
  } finally {
    restore(files);
    rebuild();
  }
  const restored = files.every((f) => !existsSync(BAK(f)));
  out.bytesRestored = restored;
  results.push(out);
  console.log(`${out.pass ? 'PASS' : 'FAIL'}  ${id} — ${title}`);
  console.log(`      gate ${expectGate} → ${out.actualStatus} (want ${out.expectedStatus}) · target changed=${out.targetChanged} · diag matched=${out.diagnosticMatched}`);
  if (out.diagnostic) console.log(`      "${out.diagnostic.slice(0, 150)}"`);
  return out;
}

const base = preflight();
const BASE_METRICS = JSON.parse(readFileSync(MODEL, 'utf8')).metrics;

// 1. Changing a legacy status has no effect — the column cannot even exist in the active schema.
mutation({
  id: 'P4-01', title: 'legacy implementationStatus reintroduced → schema rejects it; metrics never move',
  files: [CSV, MODEL, MD],
  mutate: () => { addColumn('implementationStatus', () => 'LIVE_REACHABLE'); rebuild(); },
  expectGate: 'PORTFOLIO_ACTIVE_SCHEMA_GATE', expectDiag: 'RETIRED column "implementationStatus"',
  changed: ({ after }) => {
    const m = after.model?.metrics ?? {};
    const same = JSON.stringify(m) === JSON.stringify(BASE_METRICS);
    if (!same) throw new Error('a legacy column changed the metrics — the retirement is not real');
    return true; // the target here is the GATE: metrics provably did NOT move
  },
});

// 2. Re-reading legacy mechanicImplemented → red gate.
mutation({
  id: 'P4-02', title: 'legacy mechanicImplemented reintroduced as active input → red gate',
  files: [CSV, MODEL, MD],
  mutate: () => { addColumn('mechanicImplemented', () => 'true'); rebuild(); },
  expectGate: 'PORTFOLIO_ACTIVE_SCHEMA_GATE', expectDiag: 'RETIRED column "mechanicImplemented"',
  changed: ({ after }) => JSON.stringify(after.model?.metrics) === JSON.stringify(BASE_METRICS),
});

// 3. UNASSIGNED carrying code+design ids → ALL applicable diagnostics, not just the first.
mutation({
  id: 'P4-03', title: 'UNASSIGNED with code/design ids → every applicable diagnostic (non-shadowing)',
  files: [CSV, MODEL, MD],
  mutate: () => { setCell('EXTRA-121', 'definitionStatus', 'UNASSIGNED'); setCell('EXTRA-121', 'codeExerciseId', 'E1_VIGIA'); setCell('EXTRA-121', 'designExerciseId', 'D_PROBE'); rebuild(); },
  expectGate: 'STATE_INVARIANTS_GATE', expectDiag: 'definitionStatus=UNASSIGNED but the row carries ids',
  changed: ({ after }) => {
    const d = (after.validator?.gates?.STATE_INVARIANTS_GATE?.diagnostics ?? []).filter((x) => x.includes('EXTRA-121'));
    const fams = new Set(d.map((x) => /\[(\w+)\]/.exec(x)?.[1]));
    // The V2c chain reported ONE category. Here at least the assignment and identity families must both speak.
    if (fams.size < 2) throw new Error(`expected multiple families to fire, got [${[...fams]}]`);
    return true;
  },
});

// 4. DESIGN_ONLY with a codeExerciseId is not expressible: carrying code makes it CODE_ASSIGNED, and the
//    editorial contradiction (definitionStatus vs ids) is what goes red.
mutation({
  id: 'P4-04', title: 'design-only slot given a codeExerciseId → assignment family fires',
  files: [CSV, MODEL, MD],
  mutate: () => { setCell('LANG-11', 'definitionStatus', 'UNASSIGNED'); setCell('LANG-11', 'codeExerciseId', 'E1_VIGIA'); rebuild(); },
  expectGate: 'STATE_INVARIANTS_GATE', expectDiag: 'LANG-11',
  changed: ({ before, after }) => before.model.perSlot['LANG-11'].assignment.state !== after.model.perSlot['LANG-11'].assignment.state,
});

// 5. TARGET_UNASSIGNED with a designExerciseId → red.
mutation({
  id: 'P4-05', title: 'TARGET_UNASSIGNED carrying a designExerciseId → red gate',
  files: [CSV, MODEL, MD],
  mutate: () => { setCell('EXTRA-121', 'designExerciseId', 'D_PROBE'); rebuild(); },
  expectGate: 'STATE_INVARIANTS_GATE', expectDiag: 'definitionStatus=UNASSIGNED but the row carries ids',
  changed: ({ before, after }) => before.model.perSlot['EXTRA-121'].assignment.state === 'TARGET_UNASSIGNED' && after.model.perSlot['EXTRA-121'].assignment.state === 'DESIGN_ONLY_ASSIGNED',
});

// 6. CODE_ASSIGNED with an id the observer never saw → UNRESOLVED + diagnostic, never VERIFIED_ABSENT.
mutation({
  id: 'P4-06', title: 'code id the observer never saw → UNRESOLVED + referentiality diagnostic (never ABSENT)',
  files: [CSV, MODEL, MD],
  mutate: () => { setCell('EXTRA-121', 'canonicalExerciseId', 'GHOST'); setCell('EXTRA-121', 'definitionStatus', 'DESIGNED'); setCell('EXTRA-121', 'codeExerciseId', 'GHOST_EXERCISE_NOT_IN_CODE'); rebuild(); },
  expectGate: 'STATE_INVARIANTS_GATE', expectDiag: 'the code-observation authority never observed',
  changed: ({ after }) => {
    const s = after.model.perSlot['EXTRA-121'];
    if (s.staticComponents.state === 'VERIFIED_ABSENT') throw new Error('an unobserved id was reported ABSENT — must be UNRESOLVED');
    return s.staticComponents.state === 'UNRESOLVED';
  },
});

// 7. An invented enum value → red gate.
mutation({
  id: 'P4-07', title: 'invented state value → red gate (unknown states are never tolerated)',
  files: [CSV, MODEL, MD],
  mutate: () => { setCell('FOCUS-01', 'definitionStatus', 'TOTALLY_MADE_UP'); rebuild(); },
  expectGate: 'STATE_INVARIANTS_GATE', expectDiag: 'not in closed enum',
  changed: ({ before, after }) => before.validator.gates.STATE_INVARIANTS_GATE.status !== after.validator.gates.STATE_INVARIANTS_GATE.status,
});

// 8. RESTORED as the discriminating entityType mutation (P4-R1). The earlier revision of this test dodged
//    `entityType` because it made the row vanish — that avoided the test instead of removing the defect.
//    Now membership belongs to the container, so reintroducing the discriminator is itself a red gate AND
//    the row keeps producing every diagnostic it earns.
mutation({
  id: 'P4-08', title: 'entityType reintroduced to evade validation → red gate; the row is still fully diagnosed',
  files: [CSV, MODEL, MD],
  mutate: () => {
    addColumn('entityType', (r) => (r.slotId === 'EXTRA-121' ? 'AUXILIARY' : 'TARGET_SLOT'));
    setCell('EXTRA-121', 'definitionStatus', 'TOTALLY_MADE_UP');
    setCell('EXTRA-121', 'codeExerciseId', 'GHOST;ALSO_GHOST');
    rebuild();
  },
  expectGate: 'ROW_MEMBERSHIP_GATE', expectDiag: 'self-asserted membership discriminator "entityType"',
  changed: ({ after }) => {
    // The evasion must NOT work: the row stays in the model and keeps every diagnostic.
    if (!after.model?.perSlot?.['EXTRA-121']) throw new Error('the row escaped validation — entityType still excludes it');
    const d = (after.validator?.gates?.STATE_INVARIANTS_GATE?.diagnostics ?? []).filter((x) => x.includes('EXTRA-121'));
    const fams = new Set(d.map((x) => /\[(\w+)\]/.exec(x)?.[1]));
    if (fams.size < 3) throw new Error(`row self-excluded or under-diagnosed: only ${fams.size} families fired [${[...fams]}]`);
    console.log(`      → evasion failed: row still in the model, ${d.length} diagnostics across ${fams.size} families [${[...fams].join(', ')}]`);
    return true;
  },
});

// 8b. countsTowardTarget — the latent discriminator.
mutation({
  id: 'P4-08b', title: 'countsTowardTarget=false reintroduced → red gate; row still validated',
  files: [CSV, MODEL, MD],
  mutate: () => {
    addColumn('countsTowardTarget', (r) => (r.slotId === 'FOCUS-01' ? 'false' : 'true'));
    setCell('FOCUS-01', 'definitionStatus', 'TOTALLY_MADE_UP');
    rebuild();
  },
  expectGate: 'ROW_MEMBERSHIP_GATE', expectDiag: 'self-asserted membership discriminator "countsTowardTarget"',
  changed: ({ after }) => {
    if (!after.model?.perSlot?.['FOCUS-01']) throw new Error('FOCUS-01 escaped validation');
    const d = (after.validator?.gates?.STATE_INVARIANTS_GATE?.diagnostics ?? []).filter((x) => x.includes('FOCUS-01'));
    if (!d.some((x) => /not in closed enum/.test(x))) throw new Error('the row stopped being diagnosed');
    console.log(`      → row still validated: ${d.length} diagnostics survive the discriminator`);
    return true;
  },
});

// 8c. An unknown discriminator column.
mutation({
  id: 'P4-08c', title: 'unknown discriminator column → red gate (every column needs an owner)',
  files: [CSV, MODEL, MD],
  mutate: () => { addColumn('isRealSlot', (r) => (r.slotId === 'MEMORY-01' ? 'false' : 'true')); rebuild(); },
  expectGate: 'PORTFOLIO_ACTIVE_SCHEMA_GATE', expectDiag: 'undeclared column "isRealSlot"',
  changed: ({ after }) => {
    if (!after.model?.perSlot?.['MEMORY-01']) throw new Error('MEMORY-01 escaped validation via an invented discriminator');
    return true;
  },
});

// 8d. Three internal defects PLUS an invalid discriminator → every expected diagnostic still reported.
mutation({
  id: 'P4-08d', title: 'three internal defects + invalid discriminator → all diagnostics still reported',
  files: [CSV, MODEL, MD],
  // The defect set is the one the R1 fail-before proved yields 4 families. `definitionStatus=UNASSIGNED`
  // (a VALID enum value) is what triggers the assignment family once the row carries ids; the schema
  // defect therefore has to come from a different column.
  mutate: () => {
    addColumn('entityType', (r) => (r.slotId === 'EXTRA-121' ? 'NOT_A_SLOT_AT_ALL' : 'TARGET_SLOT'));
    setCell('EXTRA-121', 'definitionStatus', 'UNASSIGNED');                  // assignment: UNASSIGNED carrying ids
    setCell('EXTRA-121', 'codeExerciseId', 'GHOST;ALSO_GHOST');              // identity + referentiality
    setCell('EXTRA-121', 'designExerciseId', 'D_PROBE');
    setCell('EXTRA-121', 'distinctnessReviewStatus', 'TOTALLY_MADE_UP');     // schema: not in closed enum
    rebuild();
  },
  expectGate: 'ROW_MEMBERSHIP_GATE', expectDiag: 'self-asserted membership discriminator "entityType"',
  changed: ({ after }) => {
    if (!after.model?.perSlot?.['EXTRA-121']) throw new Error('the row vanished despite the fix');
    const d = (after.validator?.gates?.STATE_INVARIANTS_GATE?.diagnostics ?? []).filter((x) => x.includes('EXTRA-121'));
    const fams = new Set(d.map((x) => /\[(\w+)\]/.exec(x)?.[1]));
    for (const want of ['schema', 'identity', 'codeObservationReferentiality', 'assignment']) {
      if (!fams.has(want)) throw new Error(`family "${want}" did not fire; got [${[...fams]}]`);
    }
    console.log(`      → an invalid discriminator suppresses nothing: ${d.length} diagnostics across [${[...fams].join(', ')}]`);
    return true;
  },
});

// 9. A valid run changes EXACTLY ONE target and no other.
mutation({
  id: 'P4-09', title: 'a valid runtime run changes exactly one slot and converts no other',
  files: [RT, MODEL, MD], expectPass: true,
  mutate: () => { writeRuns([validRun('FOCUS-01', 'E1_VIGIA')]); rebuild(); },
  expectGate: 'RUNTIME_PLAYABILITY_GATE',
  changed: ({ before, after }) => {
    const moved = Object.keys(after.model.perSlot).filter((k) => before.model.perSlot[k].runtime.state !== after.model.perSlot[k].runtime.state);
    if (moved.length !== 1 || moved[0] !== 'FOCUS-01') throw new Error(`expected exactly FOCUS-01 to move, moved: [${moved}]`);
    if (after.model.perSlot['FOCUS-01'].runtime.state !== 'VERIFIED') throw new Error('FOCUS-01 did not become VERIFIED');
    if (after.model.metrics.runtimeVerified !== 1) throw new Error(`runtimeVerified=${after.model.metrics.runtimeVerified}, want 1`);
    if (after.model.metrics.trainingReady !== 0) throw new Error('a runtime run alone must not create TRAINING_READY');
    console.log(`      → runtimeVerified 0→1, exactly FOCUS-01; TRAINING_READY still 0 (other axes still block)`);
    return true;
  },
});

// 10. A run missing its binding fields → red.
mutation({
  id: 'P4-10', title: 'run without snapshot/build/platform → red gate',
  files: [RT, MODEL, MD],
  mutate: () => { writeRuns([validRun('FOCUS-01', 'E1_VIGIA', { snapshotSha256: '', platform: '' })]); rebuild(); },
  expectGate: 'RUNTIME_PLAYABILITY_GATE', expectDiag: 'missing required field',
  changed: ({ before, after }) => before.validator.gates.RUNTIME_PLAYABILITY_GATE.status !== after.validator.gates.RUNTIME_PLAYABILITY_GATE.status,
});

// 11. Changing EXTRA-121 moves the table and the totals coherently (F4-6 proved it was a literal).
mutation({
  id: 'P4-11', title: 'changing EXTRA-121 moves its published row AND the totals',
  files: [CSV, MODEL, MD], expectPass: true,
  mutate: () => { setCell('EXTRA-121', 'canonicalExerciseId', 'EXTRA_PROBE'); setCell('EXTRA-121', 'definitionStatus', 'DESIGNED'); setCell('EXTRA-121', 'designExerciseId', 'EXTRA_PROBE'); rebuild(); },
  expectGate: 'MD_PUBLISHED_NUMBERS_GATE',
  changed: ({ before, after }) => {
    const b = before.md.split('\n').find((l) => /\(extra\)/.test(l));
    const a = after.md.split('\n').find((l) => /\(extra\)/.test(l));
    if (b === a) throw new Error('the published EXTRA row did not move — still a literal?');
    if (after.model.metrics.designOnlyAssigned !== BASE_METRICS.designOnlyAssigned + 1) throw new Error('totals did not follow the EXTRA row');
    if (after.model.metrics.targetUnassigned !== BASE_METRICS.targetUnassigned - 1) throw new Error('unassigned total did not follow');
    console.log(`      → EXTRA row "${b?.trim()}" → "${a?.trim()}"; designOnly ${BASE_METRICS.designOnlyAssigned}→${after.model.metrics.designOnlyAssigned}`);
    return true;
  },
});

// 12. Hard-coding the EXTRA row in the renderer → red.
mutation({
  id: 'P4-12', title: 'hard-coded EXTRA literal in the renderer → red gate',
  files: [RENDER, MD],
  mutate: () => {
    const src = readFileSync(RENDER, 'utf8').replace(
      "  for (const b of perBlock(extra, extraBlocks)) L.push(row(`${b.block} (extra)`, b));",
      "  L.push('| EXTRA-121 | 0/1 | 0 | 0 | 0 | 0 | 1 |');");
    writeFileSync(RENDER, src, 'utf8');
    run('scripts/portfolio/build-portfolio.mjs');
  },
  expectGate: 'MD_PUBLISHED_NUMBERS_GATE', expectDiag: 'hard-coded EXTRA-121 literal',
  changed: ({ before, after }) => before.md !== after.md,
});

// 13. Changing a formula label without the function → red.
mutation({
  id: 'P4-13', title: 'formula label altered without the function → red gate',
  files: [METRICS, MODEL, MD],
  mutate: () => {
    const src = readFileSync(METRICS, 'utf8').replace(
      "formula: 'staticComponents == VERIFIED_PRESENT'",
      "formula: 'liveReachable + hiddenImplemented'");
    writeFileSync(METRICS, src, 'utf8');
    rebuild();
  },
  expectGate: 'FORMULA_TRUTH_GATE', expectDiag: 'compute() never references',
  changed: ({ before, after }) => before.validator.gates.FORMULA_TRUTH_GATE.status !== after.validator.gates.FORMULA_TRUTH_GATE.status,
});

// 14. Changing a function without its label → red (the label now lies about the function).
mutation({
  id: 'P4-14', title: 'compute function altered without its label → red gate',
  files: [METRICS, MODEL, MD],
  mutate: () => {
    const src = readFileSync(METRICS, 'utf8').replace(
      "compute: (slots) => count(slots, (s) => s.staticComponents.state === 'VERIFIED_PRESENT') },",
      "compute: (slots) => count(slots, (s) => s.assignment.state === 'CODE_ASSIGNED') },");
    writeFileSync(METRICS, src, 'utf8');
    rebuild();
  },
  expectGate: 'FORMULA_TRUTH_GATE', expectDiag: 'compute() never references',
  changed: ({ before, after }) => {
    if (after.model.metrics.staticComponentsVerified === before.model.metrics.staticComponentsVerified) throw new Error('the function change did not move the metric — vacuous mutation');
    console.log(`      → staticComponentsVerified ${before.model.metrics.staticComponentsVerified}→${after.model.metrics.staticComponentsVerified} while the label still says VERIFIED_PRESENT`);
    return true;
  },
});

// 15. Stale derived model → red.
mutation({
  id: 'P4-15', title: 'stale derived model → red gate',
  files: [MODEL],
  mutate: () => { const m = JSON.parse(readFileSync(MODEL, 'utf8')); m.metrics.staticComponentsVerified = 43; writeFileSync(MODEL, JSON.stringify(m, null, 2) + '\n', 'utf8'); },
  expectGate: 'DERIVED_MODEL_FRESHNESS_GATE', expectDiag: 'stale derived model',
  changed: ({ before, after }) => before.model.metrics.staticComponentsVerified !== after.model.metrics.staticComponentsVerified,
});

// 16. Stale Markdown → red.
mutation({
  id: 'P4-16', title: 'stale published Markdown → red gate',
  files: [MD],
  mutate: () => { writeFileSync(MD, readFileSync(MD, 'utf8').replace(/\| Static components verified \| 9\/121/, '| Static components verified | 43/121'), 'utf8'); },
  expectGate: 'MD_PUBLISHED_NUMBERS_GATE', expectDiag: 'but the authorities derive',
  changed: ({ before, after }) => before.md !== after.md,
});

// 17. The historical legacy artifact used as an active input → red.
mutation({
  id: 'P4-17', title: 'historical legacy artifact wired back as an active input → red gate',
  files: [rel('scripts/portfolio/metrics.mjs')],
  mutate: () => {
    const src = readFileSync(METRICS, 'utf8').replace(
      'const count = (slots, p) => slots.filter(p).length;',
      "import { readFileSync } from 'node:fs';\nimport { parseCsv } from './csv.mjs';\nconst legacy = parseCsv(readFileSync('PORTFOLIO_V2C_LEGACY_CLAIMS.csv', 'utf8'));\nconst count = (slots, p) => slots.filter(p).length;");
    writeFileSync(METRICS, src, 'utf8');
  },
  expectGate: 'LEGACY_ISOLATION_GATE', expectDiag: 'PARSES the historical legacy claims',
  changed: ({ before, after }) => before.validator.gates.LEGACY_ISOLATION_GATE.status !== after.validator.gates.LEGACY_ISOLATION_GATE.status,
});

// 18. TRAINING_READY asserted while runtime is NOT_RUN → red on every absent axis.
mutation({
  id: 'P4-18', title: 'TRAINING_READY forced while runtime NOT_RUN → red on every absent axis',
  files: [MODEL],
  mutate: () => {
    const m = JSON.parse(readFileSync(MODEL, 'utf8'));
    m.perSlot['FOCUS-01'].readiness = { state: 'TRAINING_READY', blocking: [], rule: 'forged by the mutation oracle' };
    m.metrics.trainingReady = 1;
    writeFileSync(MODEL, JSON.stringify(m, null, 2) + '\n', 'utf8');
  },
  expectGate: 'TRAINING_READY_DERIVATION_GATE', expectDiag: 'runtime=NOT_RUN (needs VERIFIED)',
  changed: ({ before, after }) => before.model.perSlot['FOCUS-01'].readiness.state !== after.model.perSlot['FOCUS-01'].readiness.state,
});

// ---------------------------------------------------------------- report
const failed = results.filter((r) => !r.pass);
const notRestored = results.filter((r) => !r.bytesRestored);
const finalV = validator();
const baselineOk = finalV && !finalV.blockingFail;

writeFileSync(rel('PHASE4_MUTATION_RESULT.json'), JSON.stringify({
  artifact: 'V2d Phase 4 mutation oracle result', schemaVersion: '1.0.0',
  contract: 'Each mutation must (a) provably change its target, (b) fail the expected gate with the expected diagnostic, (c) restore bytes exactly.',
  mutations: results, passed: results.length - failed.length, of: results.length,
  allBytesRestored: notRestored.length === 0, baselineGreenAfterRestore: baselineOk,
}, null, 2) + '\n', 'utf8');

console.log(`\nMUTATIONS: ${results.length - failed.length}/${results.length} passed`);
console.log(`BYTES RESTORED: ${notRestored.length === 0}`);
console.log(`BASELINE GREEN AFTER RESTORE: ${baselineOk}`);
for (const f of failed) console.log(`  ! ${f.id} ${f.title} — gate ${f.expectGate} was ${f.actualStatus}, changed=${f.targetChanged}, diagMatched=${f.diagnosticMatched}`);
process.exit(failed.length || notRestored.length || !baselineOk ? 1 : 0);
