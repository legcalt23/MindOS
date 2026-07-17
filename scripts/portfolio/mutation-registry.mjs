#!/usr/bin/env node
// V2d — MUTATION REGISTRY (MB-V2D-R2 §7). The single authority for what was mutated and what it proved.
//
// Why this exists: the previous report published "62 KILLED" while the per-suite breakdown summed to 60
// (21 + 29 + 10). The two extra entries were `(suite)` pseudo-records for phase2 and phase3. That is not an
// arithmetic slip — it is two distinct honesty defects:
//
//   1. phase2 (~16 mutations) and phase3 (~18) report ONLY a process exit code. The oracle turned each whole
//      suite into ONE record and scored it KILLED because exit===0. An exit code is a generic pass; R02 is
//      explicit that a generic failure does not demonstrate a requirement was protected, and the converse
//      holds too — a generic success does not demonstrate ~34 individual mutations were killed.
//   2. Summing per-mutation records and suite-level records into one total silently mixes two different units.
//
// So the registry counts ONLY per-mutation records with an expected gate and an expected diagnostic. Suites
// that cannot produce those are recorded as OPAQUE and are excluded from the denominator — visibly, with their
// internal mutation count marked UNVERIFIED_INDIVIDUALLY, rather than being quietly folded in as 1 each.
//
// Every published total is generated from here. Hand-written counts are the defect this file removes.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);

// A record counts toward the denominator only if it carries per-mutation evidence.
const COUNTABLE = ['KILLED', 'SURVIVED', 'NOT_TESTED', 'NO_OP', 'WRONG_GATE', 'WRONG_DIAGNOSTIC', 'CRASH', 'TIMEOUT', 'INFRA_ERROR', 'RESTORATION_FAILURE'];
const KILLING = ['KILLED'];   // ONLY this. Everything else is a failure of the oracle or of the code.

const SOURCES = [
  { suite: 'phase2', result: 'PORTFOLIO_AUTHORITY_TESTS_RESULT.json', granularity: 'OPAQUE',
    note: 'authority-tests.mjs asserts internally and exposes only a process exit code. Its ~16 mutations (F1-F6 + M1-M10) are NOT individually verifiable from its output, so they are excluded from the denominator rather than counted as one KILLED. Closing this needs the suite to emit per-mutation records.' },
  { suite: 'phase3', result: 'PORTFOLIO_EVIDENCE_MUTATIONS_RESULT.json', granularity: 'OPAQUE',
    note: 'evidence-mutation-tests.mjs — same shape, ~18 mutations (P0 + P3-01..16 + P3-R), same exclusion, same fix.' },
  { suite: 'phase4', result: 'PHASE4_MUTATION_RESULT.json', granularity: 'PER_MUTATION' },
  { suite: 'phase5', result: 'PHASE5_MUTATION_RESULT.json', granularity: 'PER_MUTATION' },
  { suite: 'r03', result: 'R03_MODE_MUTATION_RESULT.json', granularity: 'PER_MUTATION' },
];

export function buildRegistry() {
  const records = [];
  const opaque = [];
  const problems = [];

  for (const s of SOURCES) {
    const p = rel(s.result);
    if (!existsSync(p)) { problems.push(`${s.suite}: result artifact ${s.result} is absent — the suite did not run, so its mutations are UNKNOWN, never assumed`); continue; }
    const raw = readFileSync(p);
    const snapshotHash = createHash('sha256').update(raw).digest('hex');
    let j; try { j = JSON.parse(raw.toString('utf8')); } catch (e) { problems.push(`${s.suite}: ${s.result} is unparseable: ${e.message}`); continue; }

    if (s.granularity === 'OPAQUE') {
      opaque.push({ suite: s.suite, result: s.result, snapshotHash, granularity: 'OPAQUE',
        individualMutations: 'UNVERIFIED_INDIVIDUALLY', excludedFromDenominator: true, note: s.note });
      continue;
    }
    const list = j.mutations ?? j.results ?? [];
    if (!Array.isArray(list) || list.length === 0) { problems.push(`${s.suite}: no per-mutation records in ${s.result}`); continue; }
    for (const m of list) {
      const classification = m.classification ?? (m.pass === true ? 'KILLED' : m.pass === false ? 'SURVIVED' : 'UNKNOWN');
      records.push({
        id: m.id, suite: s.suite,
        requirement: m.requirement ?? m.title ?? null,
        mutationOperator: m.mutationOperator ?? null,
        expectedGate: m.expectedGate ?? null,
        expectedDiagnostic: m.expectedDiagnostic ?? null,
        classification,
        evidence: m.evidence ?? m.detail ?? null,
        evidencePath: s.result,
        snapshotHash,
      });
    }
  }

  // ---- invariants. Any violation is a registry FAIL: an unreconciled denominator is not publishable.
  const inv = [];
  const ids = records.map((r) => r.id);
  const dupes = ids.filter((id, i) => ids.indexOf(id) !== i);
  if (dupes.length) inv.push(`duplicate mutation ids: ${[...new Set(dupes)].join(', ')}`);
  const unknown = records.filter((r) => !COUNTABLE.includes(r.classification));
  if (unknown.length) inv.push(`${unknown.length} record(s) with an unknown classification: ${unknown.map((r) => `${r.id}=${r.classification}`).slice(0, 5).join(', ')}`);
  const noGate = records.filter((r) => !r.expectedGate);
  // Not fatal, but it must be visible: a mutation without a declared expected gate cannot prove WHICH
  // requirement it protects, only that something changed.
  const bySuite = {};
  for (const r of records) { bySuite[r.suite] ??= { total: 0, killed: 0 }; bySuite[r.suite].total++; if (KILLING.includes(r.classification)) bySuite[r.suite].killed++; }
  const total = records.length;
  const killed = records.filter((r) => KILLING.includes(r.classification)).length;
  const sumOfSuites = Object.values(bySuite).reduce((a, b) => a + b.total, 0);
  if (sumOfSuites !== total) inv.push(`denominator does not reconcile: per-suite sum ${sumOfSuites} != total ${total}`);
  if (problems.length) inv.push(...problems);

  const byClassification = {};
  for (const r of records) byClassification[r.classification] = (byClassification[r.classification] ?? 0) + 1;

  return {
    artifact: 'V2d mutation registry — the single authority for mutation counts', schemaVersion: '1.0.0',
    generatedFrom: SOURCES.map((s) => s.result),
    denominatorRule: 'ONLY per-mutation records with a declared expected gate + diagnostic are counted. ONLY the KILLED classification counts as a kill; CRASH, TIMEOUT, INFRA_ERROR, NOT_TESTED, SURVIVED, NO_OP, WRONG_GATE, WRONG_DIAGNOSTIC and RESTORATION_FAILURE do not. Suites that emit only an exit code are OPAQUE and are excluded from the denominator — an exit code is not per-mutation evidence.',
    total, killed, notKilled: total - killed,
    byClassification, bySuite,
    mutationsWithoutDeclaredGate: noGate.length,
    opaqueSuites: opaque,
    invariantViolations: inv,
    reconciled: inv.length === 0 && sumOfSuites === total,
    records,
  };
}

if (process.argv[1]?.endsWith('mutation-registry.mjs')) {
  const r = buildRegistry();
  writeFileSync(rel('MUTATION_REGISTRY.json'), JSON.stringify(r, null, 2) + '\n', 'utf8');
  console.log('V2d MUTATION REGISTRY\n');
  for (const [s, c] of Object.entries(r.bySuite)) console.log(`  ${s.padEnd(8)} ${String(c.killed).padStart(3)}/${String(c.total).padEnd(3)} KILLED`);
  console.log(`  ${'TOTAL'.padEnd(8)} ${String(r.killed).padStart(3)}/${String(r.total).padEnd(3)} KILLED   (per-suite sum reconciles: ${r.bySuite && Object.values(r.bySuite).reduce((a, b) => a + b.total, 0) === r.total})`);
  console.log(`\n  classifications: ${JSON.stringify(r.byClassification)}`);
  if (r.mutationsWithoutDeclaredGate) console.log(`  ! ${r.mutationsWithoutDeclaredGate} mutation(s) carry no declared expected gate — they prove a change, not which requirement it protects`);
  for (const o of r.opaqueSuites) console.log(`  ! ${o.suite}: OPAQUE, EXCLUDED from the denominator — ${o.note}`);
  for (const v of r.invariantViolations) console.log(`  ! INVARIANT: ${v}`);
  console.log(`\nREGISTRY: ${r.reconciled ? 'RECONCILED' : 'NOT RECONCILED'}`);
  process.exit(r.reconciled && r.killed === r.total ? 0 : 1);
}
