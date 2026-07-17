#!/usr/bin/env node
// V2d PHASE 4-R1 — MANDATORY FAIL-BEFORE: ROW MEMBERSHIP EVASION.
//
// The defect: target membership is decided by a field the ROW ITSELF carries. `entityType` is filtered
// BEFORE any invariant family runs, so a corrupt row can switch off all of its own invariants by editing
// one cell. My Phase 4 report noticed this ("setting entityType removes the row from the target set
// entirely, so no family sees it") and then MOVED THE TEST to defects that stayed inside the model.
// That avoided the test; it did not remove the defect. This file reproduces it properly.
//
// Fail-closed about its own baseline (MB-PORTFOLIO-0018): hash-pin, then try/finally.
import { readFileSync, writeFileSync, copyFileSync, existsSync, unlinkSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { parseCsvRaw, headerOf } from './csv.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const sha = (p) => (existsSync(p) ? createHash('sha256').update(readFileSync(p)).digest('hex') : 'ABSENT');
const CSV = rel('MINDBOOST_EXERCISE_PORTFOLIO.csv');
const MODEL = rel('PORTFOLIO_DERIVED_MODEL.json');
const MD = rel('MINDBOOST_EXERCISE_PORTFOLIO.md');
const BAK = (p) => `${p}.r1bak`;

const results = [];
const rec = (id, title, proven, detail) => { results.push({ id, title, proven, detail }); console.log(`${proven ? 'REPRODUCED' : 'NOT REPRODUCED'}  ${id} — ${title}\n    ${detail}\n`); };
const backup = (ps) => ps.forEach((p) => existsSync(p) && copyFileSync(p, BAK(p)));
const restore = (ps) => ps.forEach((p) => { if (existsSync(BAK(p))) { copyFileSync(BAK(p), p); unlinkSync(BAK(p)); } });
const probe = (files, fn) => { backup(files); try { return fn(); } finally { restore(files); rebuild(); } };

const run = (s, a = []) => { try { return { code: 0, out: execFileSync(process.execPath, [rel(s), ...a], { encoding: 'utf8', cwd: root, stdio: ['ignore', 'pipe', 'pipe'] }) }; } catch (e) { return { code: e.status ?? 1, out: (e.stdout || '') + (e.stderr || '') }; } };
const validator = () => { const r = run('scripts/portfolio/validate-portfolio.mjs', ['--mode=repo', '--json']); try { return JSON.parse(r.out); } catch { return null; } };
const rebuild = () => { run('scripts/portfolio/derive-portfolio-model.mjs'); run('scripts/portfolio/build-portfolio.mjs'); };

function setCells(slotId, kv) {
  const text = readFileSync(CSV, 'utf8');
  const hdr = headerOf(text);
  const lines = text.split('\n');
  for (let i = 1; i < lines.length; i++) {
    const cells = parseCsvRaw(lines[i])[0];
    if (!cells || cells[hdr.indexOf('slotId')] !== slotId) continue;
    for (const [k, v] of Object.entries(kv)) { const idx = hdr.indexOf(k); if (idx < 0) throw new Error(`no column ${k}`); cells[idx] = v; }
    lines[i] = cells.map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',');
    writeFileSync(CSV, lines.join('\n'), 'utf8');
    return;
  }
  throw new Error(`no row ${slotId}`);
}
const diagsFor = (v, slot) => (v?.gates?.STATE_INVARIANTS_GATE?.diagnostics ?? []).filter((d) => d.includes(slot));
const famsOf = (ds) => [...new Set(ds.map((d) => /\[(\w+)\]/.exec(d)?.[1]).filter(Boolean))];

// PRE-FLIGHT
{
  const sums = readFileSync(rel('SHA256SUMS.txt'), 'utf8');
  const pin = (p) => (new RegExp(`^([0-9a-f]{64})\\s{2}${p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`, 'm').exec(sums) || [])[1];
  const bad = [];
  for (const p of ['MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_PORTFOLIO.md']) {
    const w = pin(p), g = sha(rel(p));
    if (w !== g) bad.push(`${p}: disk ${g.slice(0, 12)}… != pin ${String(w).slice(0, 12)}…`);
  }
  if ([CSV, MODEL, MD].some((p) => existsSync(BAK(p)))) bad.push('stray .r1bak from a crashed run');
  if (bad.length) { console.error('PRE-FLIGHT FAILED:'); bad.forEach((b) => console.error(`  ! ${b}`)); process.exit(2); }
  console.log('PRE-FLIGHT: mutable inputs match their SHA256SUMS pins.\n');
}
const SNAP = { csv: sha(CSV), md: sha(MD), model: sha(MODEL) };
console.log('PHASE 4 SNAPSHOT');
for (const [k, v] of Object.entries(SNAP)) console.log(`  ${k.padEnd(6)} ${v}`);
console.log('');

const SLOT = 'FOCUS-01';
// The three internal defects, all expressible in the ACTIVE editorial schema.
// NOTE: "TRAINING_READY without runtime" is NOT expressible here — `launchReadiness` was retired from the
// active CSV in Phase 4, so readiness can only be forged in the derived model (covered by P4-18). Reporting
// that honestly rather than inventing a CSV field to satisfy the script.
const DEFECTS = {
  definitionStatus: 'UNASSIGNED',              // assignment family: UNASSIGNED while carrying ids
  codeExerciseId: 'GHOST;ALSO_GHOST',          // identity family: multi-valued + referentiality: never observed
  designExerciseId: 'D_PROBE',
  distinctnessReviewStatus: 'TOTALLY_MADE_UP', // schema family: not in closed enum
};

// ---------------------------------------------------------------- R1-A: the baseline (row IS validated)
let baselineDiags = [], baselineFams = [];
probe([CSV, MODEL, MD], () => {
  setCells(SLOT, DEFECTS);
  rebuild();
  const v = validator();
  baselineDiags = diagsFor(v, SLOT);
  baselineFams = famsOf(baselineDiags);
  rec('R1-A', 'control: with entityType intact the row IS validated', baselineDiags.length >= 3 && baselineFams.length >= 3,
    `${SLOT} given 3 internal defects (definitionStatus=UNASSIGNED while carrying ids; codeExerciseId multi-valued + never observed; ` +
    `distinctnessReviewStatus not in the closed enum), entityType left = TARGET_SLOT. ` +
    `Result: ${baselineDiags.length} diagnostics across ${baselineFams.length} families [${baselineFams.join(', ')}]. ` +
    `This is the evidence the row SHOULD produce.`);
});

// ---------------------------------------------------------------- R1-B: entityType evasion
probe([CSV, MODEL, MD], () => {
  setCells(SLOT, { ...DEFECTS, entityType: 'AUXILIARY' });
  rebuild();
  const v = validator();
  const d = diagsFor(v, SLOT);
  const f = famsOf(d);
  const sem = v?.gates?.TARGET_SLOT_SEMANTICS_GATE;
  const countDiag = (sem?.diagnostics ?? []).find((x) => /expected 121 target rows/.test(x));
  const missingDiag = (sem?.diagnostics ?? []).find((x) => x.includes(SLOT));
  const model = JSON.parse(readFileSync(MODEL, 'utf8'));
  const inModel = !!model.perSlot[SLOT];
  rec('R1-B', 'entityType evasion: one cell switches off every invariant on the row',
    baselineDiags.length >= 3 && d.length === 0 && !!countDiag,
    `Same 3 defects, plus entityType=AUXILIARY. Diagnostics for ${SLOT}: ${baselineDiags.length} → ${d.length}. ` +
    `Families that fired: [${baselineFams.join(', ')}] → [${f.join(', ') || 'NONE'}]. Row present in the derived model: ${inModel}. ` +
    `The global count notices something is off ("${countDiag}") and even names the slot — but it says "${missingDiag}". ` +
    `That is WORSE than silence: the row is physically present in the file, carrying ${baselineDiags.length} defects, and the validator ` +
    `sends you looking for a MISSING row that is sitting right there. Not one of its ${baselineDiags.length} real defects is reported. ` +
    `A row decides, via its own field, whether it is validated.`);
});

// ---------------------------------------------------------------- R1-C: does countsTowardTarget control membership?
probe([CSV, MODEL, MD], () => {
  setCells(SLOT, { ...DEFECTS, countsTowardTarget: 'false' });
  rebuild();
  const v = validator();
  const d = diagsFor(v, SLOT);
  const f = famsOf(d);
  const model = JSON.parse(readFileSync(MODEL, 'utf8'));
  const inModel = !!model.perSlot[SLOT];
  const flagged = d.some((x) => /countsTowardTarget/.test(x));
  // Honest outcome: this field is VALIDATED, never used to filter. So it must NOT evade.
  rec('R1-C', 'countsTowardTarget does NOT control membership (validated, never filtered)',
    inModel && d.length > baselineDiags.length && flagged,
    `Same 3 defects, plus countsTowardTarget=false. Row still in the model: ${inModel}. Diagnostics ${baselineDiags.length} → ${d.length} ` +
    `across [${f.join(', ')}], including its own schema diagnostic (countsTowardTarget flagged=${flagged}). ` +
    `FINDING NARROWED, honestly: the CEO brief asked to repeat the evasion with countsTowardTarget "if that field controls membership". ` +
    `It does NOT — grep shows it only at portfolio-state-model.mjs:118 as a validated value, never in a .filter(). ` +
    `Only entityType is a live evasion vector today. countsTowardTarget is a LATENT one: it is exactly the kind of ` +
    `self-asserted discriminator that a future filter would reach for, which is why the fix removes BOTH from the active schema.`);
});

// ---------------------------------------------------------------- R1-D: an auxiliary smuggled into the target CSV
probe([CSV, MODEL, MD], () => {
  setCells(SLOT, { entityType: 'AUXILIARY', codeExerciseId: 'EX16_CALIBRACION_CONFIANZA' });
  rebuild();
  const v = validator();
  const d = diagsFor(v, SLOT);
  const auxDiag = [...(v?.gates?.TARGET_SLOT_SEMANTICS_GATE?.diagnostics ?? []), ...(v?.gates?.COUNT_RECONCILIATION_GATE?.diagnostics ?? [])]
    .find((x) => /auxiliary/i.test(x));
  rec('R1-D', 'an auxiliary id smuggled into the target CSV is IGNORED, not rejected',
    d.length === 0 && !auxDiag,
    `${SLOT} relabelled entityType=AUXILIARY carrying the registered auxiliary id EX16_CALIBRACION_CONFIANZA. ` +
    `Diagnostics for the row: ${d.length}. "auxiliary occupies a target slot" fired: ${!!auxDiag}. ` +
    `The rule "auxiliaries come only from the auxiliary file" is unenforceable while a row can declare itself an auxiliary: ` +
    `the check that would catch it (${'`'}auxiliary occupies a target slot${'`'}) never runs, because the row was filtered out first.`);
});

// ---------------------------------------------------------------- integrity
const POST = { csv: sha(CSV), md: sha(MD), model: sha(MODEL) };
const restored = POST.csv === SNAP.csv && POST.md === SNAP.md && POST.model === SNAP.model;
console.log(`RESTORATION: byte-identical = ${restored}`);
for (const [k, v] of Object.entries(POST)) console.log(`  ${k.padEnd(6)} ${v}`);

const failed = results.filter((r) => !r.proven);
writeFileSync(rel('PHASE4_R1_FAIL_BEFORE_RESULT.json'), JSON.stringify({
  artifact: 'V2d Phase 4-R1 fail-before — row membership evasion', schemaVersion: '1.0.0',
  snapshot: SNAP, postSnapshot: POST, restoredByteIdentical: restored,
  baselineDiagnostics: baselineDiags, baselineFamilies: baselineFams,
  probes: results, reproduced: results.length - failed.length, of: results.length,
}, null, 2) + '\n', 'utf8');
console.log(`\nFAIL-BEFORE R1: ${results.length - failed.length}/${results.length} reproduced`);
process.exit(failed.length || !restored ? 1 : 0);
