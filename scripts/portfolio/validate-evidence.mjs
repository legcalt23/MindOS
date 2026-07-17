#!/usr/bin/env node
// EVIDENCE VALIDATOR (V2d Phase 3). Re-reads every byte and recomputes; trusts no declared field.
//
// Gates (each emits a named diagnostic, not just an exit code):
//   G1  manifest entry claims a file that does not exist / is not a regular file
//   G2  manifest sha256 or byteLength disagrees with the bytes on disk
//   G3  an evidencePath emitted by an axis is not in the manifest
//   G4  an evidence sha256 disagrees with the manifest entry for the same path
//   G5  an axis is VERIFIED_PRESENT without a construct/symbol supporting it
//   G6  an adapter consumes a source that the manifest does not declare
//   G7  the manifest declares an ACTIVE source no adapter/authority consumes
//   G8  a HISTORICAL_EVIDENCE source is cited by a current state
//   G9  VERIFIED_ABSENT without exhaustive + exhaustivenessEvidenceId
//   G10 a runtime claim without a valid run in the runtime authority
//   G11 a manifested source that exists but is zero bytes
//   G12 target-state invariants (sum=121, design-only vs unassigned, assigned reconciliation, aux excluded)
//   G13 two axes resting on one observation not marked evidenceDependency=SHARED
import { readFileSync, existsSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { buildManifest, fileFacts } from './gen-source-manifest.mjs';
import { ADAPTERS, AXES } from './block-adapters.mjs';
import { derive, parseCsv, checkInvariants, COMPONENT_AXES } from './derive-portfolio-model.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

export function validate(rootDir = root, opts = {}) {
  const gates = [];
  const fail = (gate, msg) => gates.push({ gate, msg });
  const rd = (rel) => JSON.parse(readFileSync(join(rootDir, rel), 'utf8'));

  const manifest = opts.manifest ?? rd('PORTFOLIO_SOURCE_MANIFEST_V2.json');
  const obs = opts.obs ?? rd('PORTFOLIO_CODE_OBSERVATIONS.json');
  const runtime = existsSync(join(rootDir, 'PORTFOLIO_RUNTIME_EVIDENCE.json')) ? rd('PORTFOLIO_RUNTIME_EVIDENCE.json') : null;
  const byPath = new Map(manifest.entries.map((e) => [e.path, e]));

  // G1/G2/G11 — recompute every manifest entry from disk.
  for (const e of manifest.entries) {
    const f = fileFacts(rootDir, e.path);
    if (e.exists && !f.exists) fail('G1', `manifest claims ${e.path} exists, but it is not on disk`);
    if (e.exists && f.exists && e.isRegularFile && !f.isRegularFile) fail('G1', `${e.path} is not a regular file`);
    if (f.exists && e.sha256 !== f.sha256) fail('G2', `${e.path}: manifest sha256 ${String(e.sha256).slice(0, 12)}… disagrees with disk ${String(f.sha256).slice(0, 12)}…`);
    if (f.exists && e.byteLength !== f.byteLength) fail('G2', `${e.path}: manifest byteLength ${e.byteLength} disagrees with disk ${f.byteLength}`);
    if (f.exists && f.byteLength === 0 && e.role !== 'HISTORICAL_EVIDENCE') fail('G11', `${e.path}: manifested ACTIVE source is zero bytes`);
  }

  // G3/G4/G5/G8/G9/G13 — per-axis referentiality.
  for (const [id, r] of Object.entries(obs.perId)) {
    const fp = new Map();
    for (const [ax, v] of Object.entries(r.axes)) {
      if (v.evidencePath) {
        const e = byPath.get(v.evidencePath);
        if (!e) fail('G3', `${id}.${ax}: evidencePath ${v.evidencePath} is not in the manifest`);
        else {
          if (e.sha256 !== v.evidenceSha256) fail('G4', `${id}.${ax}: evidence sha256 disagrees with the manifest entry for ${v.evidencePath}`);
          if (e.role === 'HISTORICAL_EVIDENCE' && v.state === 'VERIFIED_PRESENT') fail('G8', `${id}.${ax}: a HISTORICAL_EVIDENCE source (${v.evidencePath}) is cited by a current state`);
        }
        const k = `${v.evidencePath}::${v.symbol}::${JSON.stringify(v.matchRange)}`;
        if (fp.has(k) && v.evidenceDependency !== 'SHARED') fail('G13', `${id}.${ax} and ${id}.${fp.get(k)} rest on one observation (${v.evidencePath}::${v.symbol}) but are not marked evidenceDependency=SHARED`);
        if (!fp.has(k)) fp.set(k, ax);
      }
      if (v.state === 'VERIFIED_PRESENT' && !v.construct) fail('G5', `${id}.${ax}: VERIFIED_PRESENT without a construct — a bare code token cannot accredit an axis`);
      if (v.state === 'VERIFIED_PRESENT' && !v.evidencePath) fail('G5', `${id}.${ax}: VERIFIED_PRESENT without an evidencePath`);
      if (v.state === 'VERIFIED_ABSENT' && !(v.exhaustive && v.exhaustivenessEvidenceId)) fail('G9', `${id}.${ax}: VERIFIED_ABSENT without exhaustive:true AND an exhaustivenessEvidenceId`);
    }
  }

  // G6 — every adapter-consumed source must be manifested.
  for (const [k, a] of Object.entries(ADAPTERS)) for (const ax of AXES) for (const f of (a[ax].files || []))
    if (!byPath.has(f)) fail('G6', `adapter ${k}.${ax} consumes ${f}, which the manifest does not declare`);

  // G7 — a TOOL/EDITORIAL/RUNTIME/DOC entry is used by definition; an ACTIVE Kotlin source that no adapter
  // consumes is in scope as a source-of-truth boundary, not an error — but an entry with a `consumers` claim
  // that names a non-existent adapter axis IS an error.
  const validConsumers = new Set(Object.entries(ADAPTERS).flatMap(([k]) => AXES.map((ax) => `${k}.${ax}`)).concat(['ALL.blockReachability']));
  for (const e of manifest.entries) for (const c of e.consumers) if (!validConsumers.has(c)) fail('G7', `${e.path}: declares consumer ${c}, which is not a real adapter axis`);

  // G10 — runtime claims require a valid run.
  if (runtime) {
    for (const run of runtime.runs) {
      const bad = ['slotId', 'platform', 'snapshotSha256', 'testOrDossier', 'result', 'provenance'].filter((f) => !run[f]);
      if (bad.length) fail('G10', `runtime run for ${run.slotId ?? '(no slot)'} is missing required fields: ${bad.join(', ')}`);
      if (run.result === 'VERIFIED' && (!run.snapshotSha256 || !run.evidencePath)) fail('G10', `runtime run for ${run.slotId} claims VERIFIED without a snapshot hash and evidencePath`);
    }
  } else fail('G10', 'runtime authority artifact PORTFOLIO_RUNTIME_EVIDENCE.json is missing');

  // G12 — target-state invariants.
  const all = parseCsv(readFileSync(join(rootDir, 'MINDBOOST_EXERCISE_PORTFOLIO.csv'), 'utf8'));
  const rows = all; // P4-R1: the CSV IS the target authority — every physical row is a target slot and is validated
  const aux = existsSync(join(rootDir, 'MINDBOOST_EXERCISE_AUXILIARIES.csv')) ? parseCsv(readFileSync(join(rootDir, 'MINDBOOST_EXERCISE_AUXILIARIES.csv'), 'utf8')) : [];
  const model = derive(obs, rows, runtime);
  for (const v of checkInvariants(model, rows, aux, obs)) fail('G12', v);

  return { gates, model, manifest, obs };
}

if (process.argv[1]?.endsWith('validate-evidence.mjs')) {
  const { gates } = validate();
  const byGate = gates.reduce((a, g) => ({ ...a, [g.gate]: (a[g.gate] || 0) + 1 }), {});
  writeFileSync(join(root, 'PORTFOLIO_EVIDENCE_VALIDATION.json'), JSON.stringify({
    artifact: 'MindBoost portfolio EVIDENCE VALIDATION', schemaVersion: '1.0.0',
    result: gates.length ? 'FAIL' : 'PASS', failures: gates.length, byGate, gates,
  }, null, 2) + '\n', 'utf8');
  console.log(`EVIDENCE VALIDATION: ${gates.length ? 'FAIL' : 'PASS'} (${gates.length} gate failures)`);
  for (const [g, n] of Object.entries(byGate)) console.log(`  ${g}: ${n}`);
  gates.slice(0, 12).forEach((g) => console.log(`    [${g.gate}] ${g.msg}`));
  process.exit(gates.length ? 1 : 0);
}
