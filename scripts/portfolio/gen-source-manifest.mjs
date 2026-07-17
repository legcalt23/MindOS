#!/usr/bin/env node
// UNIFIED SOURCE MANIFEST (V2d Phase 3). Emits PORTFOLIO_SOURCE_MANIFEST_V2.json.
//
// V2c manifested 14 hand-listed files and therefore omitted the very sources that carried the real
// dispatch (FocusGraph.kt, per-exercise screens, state holders) — the omission was itself the evidence
// that V2c could not sustain its conclusions (MB-PORTFOLIO-0013). This does NOT hand-list: it walks a
// DECLARED, DETERMINISTIC SCOPE and hashes every file in it from disk.
//
// SCOPE (exact — this is NOT the whole repository):
//   1. every *.kt under the eight MindBoost block subtrees
//   2. the MindBoost hub/presentation subtree (*.kt)
//   3. the editorial authority CSVs
//   4. the runtime/QA authority
//   5. the documentary authority (authority map)
//   6. the tools that generate/validate the model
//   7. the retired seed — role HISTORICAL_EVIDENCE only, never an input to a current state
//
// NOTHING is trusted from a `status: PRESENT` field. Every property is computed from disk: existence,
// regular-file, symlink, byteLength, sha256. The validator re-reads the bytes and recomputes all of it.
import { readdirSync, statSync, lstatSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join, relative, sep } from 'node:path';
import { ADAPTERS, AXES, P, HUB_FILE } from './block-adapters.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

export const ROLES = {
  CODE_SOURCE_ACTIVE: 'a Kotlin source inside the declared MindBoost scope; may support a current state',
  EDITORIAL_AUTHORITY: 'target-slot authority (the 121 slots and their assignments)',
  RUNTIME_AUTHORITY: 'the only source of runtime facts',
  DOC_AUTHORITY: 'documentary authority (authority map)',
  TOOL_ACTIVE: 'generates or validates the model',
  HISTORICAL_EVIDENCE: 'retained as history ONLY; must never feed a current state',
};

export const SCOPE = {
  description: 'The eight MindBoost block subtrees plus the MindBoost hub/presentation subtree, the editorial CSVs, the runtime authority, the authority map, and the model tooling. This is NOT the whole repository.',
  kotlinSubtrees: [
    `${P}/focus`, `${P}/speed`, `${P}/logic`, `${P}/language`,
    `${P}/memory`, `${P}/decisiones`, `${P}/emotions`, `${P}/creativity`,
    `${P}/presentation`,
  ],
  kotlinExtensions: ['.kt'],
  documents: ['MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_AUXILIARIES.csv', 'PORTFOLIO_RUNTIME_EVIDENCE.json', 'PORTFOLIO_AUTHORITY_MAP.md'],
  tools: [
    'scripts/portfolio/kotlin-code-scan.mjs', 'scripts/portfolio/block-adapters.mjs',
    'scripts/portfolio/observe-code.mjs', 'scripts/portfolio/derive-portfolio-model.mjs',
    'scripts/portfolio/gen-source-manifest.mjs', 'scripts/portfolio/validate-evidence.mjs',
    'scripts/portfolio/authority-tests.mjs',
    // V2d Phase 4 — the chain that turns authorities into the published view
    'scripts/portfolio/portfolio-state-model.mjs', 'scripts/portfolio/metrics.mjs',
    'scripts/portfolio/migrate-legacy-columns.mjs', 'scripts/portfolio/render.mjs',
    'scripts/portfolio/build-portfolio.mjs', 'scripts/portfolio/validate-portfolio.mjs',
    'scripts/portfolio/phase4-fail-before.mjs', 'scripts/portfolio/phase4-mutation-tests.mjs',
    'scripts/portfolio/phase4-r1-fail-before.mjs',
    // V2d Phase 5 — the packaging chain must hash its own machinery, or the bundle attests tools it does not carry
    'scripts/portfolio/build-source-archive.mjs', 'scripts/portfolio/deterministic-zip.mjs',
    'scripts/portfolio/build-bundle.mjs', 'scripts/portfolio/validate-bundle.mjs',
    'scripts/portfolio/clean-room-test.mjs', 'scripts/portfolio/phase5-mutation-tests.mjs',
  ],
  // HISTORICAL_EVIDENCE: preserved and hashed so it cannot be altered silently; never an active input.
  historical: [
    'scripts/portfolio/seed-portfolio.mjs.RETIRED',
    'PORTFOLIO_V2C_LEGACY_CLAIMS.csv', 'PORTFOLIO_V2C_LEGACY_CLAIMS.meta.json',
  ],
  notInScope: 'androidApp/, functions/, mindos/, iosApp/, other shared/ features, build outputs, and every non-MindBoost Kotlin source.',
};

const walk = (rootDir, rel, out = []) => {
  let entries;
  try { entries = readdirSync(join(rootDir, rel), { withFileTypes: true }); } catch { return out; }
  for (const e of entries.sort((a, b) => (a.name < b.name ? -1 : 1))) {
    const r = `${rel}/${e.name}`;
    if (e.isDirectory()) walk(rootDir, r, out);
    else if (SCOPE.kotlinExtensions.some((x) => e.name.endsWith(x))) out.push(r);
  }
  return out;
};

export function fileFacts(rootDir, rel) {
  const abs = join(rootDir, rel);
  if (!existsSync(abs)) return { path: rel, exists: false, isRegularFile: false, isSymlink: false, byteLength: null, sha256: null };
  const l = lstatSync(abs);
  const isSymlink = l.isSymbolicLink();
  const s = statSync(abs);
  const buf = s.isFile() ? readFileSync(abs) : null;
  return {
    path: rel, exists: true, isRegularFile: s.isFile(), isSymlink,
    byteLength: buf ? buf.length : null,
    sha256: buf ? createHash('sha256').update(buf).digest('hex') : null,
  };
}

// Which adapter/axis consumes each source — so "manifested but nobody uses it" is detectable.
function consumerMap() {
  const m = {};
  const add = (f, c) => { (m[f] ??= []).push(c); };
  for (const [k, a] of Object.entries(ADAPTERS)) for (const ax of AXES) for (const f of (a[ax].files || [])) add(f, `${k}.${ax}`);
  add(HUB_FILE, 'ALL.blockReachability');
  return m;
}

export function buildManifest(rootDir = root) {
  const consumers = consumerMap();
  const entries = [];
  const push = (rel, role) => {
    const f = fileFacts(rootDir, rel);
    entries.push({ ...f, role, consumers: consumers[rel] ?? [] });
  };
  const kt = new Set();
  for (const sub of SCOPE.kotlinSubtrees) for (const f of walk(rootDir, sub)) kt.add(f);
  for (const f of [...kt].sort()) push(f, 'CODE_SOURCE_ACTIVE');
  for (const d of SCOPE.documents) push(d, d.endsWith('.csv') ? 'EDITORIAL_AUTHORITY' : d.includes('RUNTIME') ? 'RUNTIME_AUTHORITY' : 'DOC_AUTHORITY');
  for (const t of SCOPE.tools) push(t, 'TOOL_ACTIVE');
  for (const h of SCOPE.historical) push(h, 'HISTORICAL_EVIDENCE');

  return {
    artifact: 'MindBoost portfolio UNIFIED SOURCE MANIFEST', schemaVersion: '2.0.0',
    note: 'Every property is computed from disk. No entry is trusted from a declared status field. Contains no wall clock: operational time belongs outside the hashed artifact.',
    scope: SCOPE, roles: ROLES,
    counts: {
      total: entries.length,
      byRole: entries.reduce((a, e) => ({ ...a, [e.role]: (a[e.role] || 0) + 1 }), {}),
      missing: entries.filter((e) => !e.exists).length,
      zeroByte: entries.filter((e) => e.exists && e.byteLength === 0).length,
      symlinks: entries.filter((e) => e.isSymlink).length,
    },
    entries: entries.sort((a, b) => (a.path < b.path ? -1 : 1)),
  };
}

if (process.argv[1]?.endsWith('gen-source-manifest.mjs')) {
  const m = buildManifest();
  writeFileSync(join(root, 'PORTFOLIO_SOURCE_MANIFEST_V2.json'), JSON.stringify(m, null, 2) + '\n', 'utf8');
  console.log(`SOURCE MANIFEST: ${m.counts.total} entries`);
  for (const [r, n] of Object.entries(m.counts.byRole)) console.log(`  ${r.padEnd(22)} ${n}`);
  console.log(`  missing=${m.counts.missing} zeroByte=${m.counts.zeroByte} symlinks=${m.counts.symlinks}`);
  console.log(`  scope: NOT the whole repo — ${SCOPE.notInScope}`);
}
