#!/usr/bin/env node
// validate-authority-entrypoints.mjs — Focus V5 authority-index discoverability gate.
//
// Enforces that the CURRENT authority index (docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md) is
// DISCOVERABLE from the mandatory entrypoints, and that legacy bundle docs do NOT masquerade as the
// current authority. Read-only. No dependencies. Node ESM.
//
//   node scripts/validate-authority-entrypoints.mjs             # scan the repo (exit 0 = PASS, 1 = FAIL)
//   node scripts/validate-authority-entrypoints.mjs --self-test # run the embedded oracle (fixtures)
//
// This gate is NOT wired to Gradle/CI in Paso 7 (that is a separate authorized follow-up); it is a
// standalone runnable gate whose self-test proves it can FAIL.
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');

// The machine markers (exact-string match — prose that merely names a file never trips these).
const MARKER = 'FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md';
const CURRENT_ROLE = 'AUTHORITY_INDEX_ROLE: CURRENT';
const INDEX_BASENAME = 'CURRENT_AUTHORITY_INDEX.md';
const ENTRYPOINTS = ['CLAUDE.md', 'docs/engines/focus_v5/ADR_REGISTER_V5.md'];
const LEGACY_MUST_NOT_CLAIM = ['FOCUS_ENGINE_MASTER_PLAN_V5.md', 'GATE_CATALOG_V5.md'];

/**
 * Pure core. Both inputs are Maps of name -> file content.
 * @param entrypoints Map<entrypointPath, content>
 * @param focusMd     Map<basename, content> for every *.md in docs/engines/focus_v5
 * @returns {ok: boolean, errors: string[]}
 */
export function scanContent(entrypoints, focusMd) {
  const errors = [];

  // (1) every mandatory entrypoint references the index via the exact marker.
  for (const ep of ENTRYPOINTS) {
    const content = entrypoints.get(ep);
    if (content == null) { errors.push(`entrypoint missing: ${ep}`); continue; }
    if (!content.includes(MARKER)) errors.push(`entrypoint does not reference the authority index (marker absent): ${ep}`);
  }

  // (2) exactly ONE focus_v5 md DECLARES the CURRENT role, and it is the index file.
  // A DECLARATION is a line that, trimmed, equals the marker exactly — a prose MENTION (e.g. the marker
  // inside backticks or a sentence) does NOT count. This is the "machine marker exact" classifier that
  // separates a real authority declaration from historical citation (learning 2026-06-28).
  const declaresRole = (c) => c.split(/\r?\n/).some((l) => l.trim() === CURRENT_ROLE);
  const claimers = [...focusMd.entries()].filter(([, c]) => declaresRole(c)).map(([n]) => n);
  if (claimers.length === 0) errors.push('no file declares AUTHORITY_INDEX_ROLE: CURRENT');
  else if (claimers.length > 1) errors.push(`AUTHORITY_INDEX_ROLE: CURRENT declared by ${claimers.length} files: ${claimers.join(', ')}`);
  else if (claimers[0] !== INDEX_BASENAME) errors.push(`AUTHORITY_INDEX_ROLE: CURRENT declared by the wrong file: ${claimers[0]} (expected ${INDEX_BASENAME})`);

  // (3) legacy bundle docs must not carry the marker nor the CURRENT role.
  for (const legacy of LEGACY_MUST_NOT_CLAIM) {
    const c = focusMd.get(legacy);
    if (c == null) continue; // absence is not this gate's concern
    if (c.includes(MARKER)) errors.push(`legacy doc carries the index marker: ${legacy}`);
    if (c.includes(CURRENT_ROLE)) errors.push(`legacy doc claims AUTHORITY_INDEX_ROLE: CURRENT: ${legacy}`);
  }

  // (4) QUARANTINE (reaudit 2026-07-22, MB-FOCUS-0055): every PRESENT legacy/frozen doc must be
  // dispositioned by name inside the CURRENT index. If the index drops a legacy doc's disposition,
  // that frozen doc (e.g. GATE_CATALOG_V5.md, whose L9 frontmatter is stale and which self-declares
  // sole gate authority) could silently be read as current again. The index is tracked/sealed and
  // cannot be edited, so the enforceable invariant lives here: index MUST name each present legacy doc.
  const indexContent = focusMd.get(INDEX_BASENAME);
  if (indexContent != null) {
    for (const legacy of LEGACY_MUST_NOT_CLAIM) {
      if (focusMd.get(legacy) != null && !indexContent.includes(legacy)) {
        errors.push(`quarantine gap: CURRENT index does not disposition present legacy doc: ${legacy}`);
      }
    }
  }

  return { ok: errors.length === 0, errors };
}

function scanRepo() {
  const entrypoints = new Map();
  for (const ep of ENTRYPOINTS) {
    try { entrypoints.set(ep, readFileSync(join(ROOT, ep), 'utf8')); } catch { /* reported as missing */ }
  }
  const focusDir = join(ROOT, 'docs/engines/focus_v5');
  const focusMd = new Map();
  for (const name of readdirSync(focusDir)) {
    if (name.endsWith('.md')) focusMd.set(name, readFileSync(join(focusDir, name), 'utf8'));
  }
  return scanContent(entrypoints, focusMd);
}

// --- embedded oracle: proves the gate can FAIL, not just PASS ---
function selfTest() {
  const ep = (mark = true) => new Map([
    ['CLAUDE.md', mark ? `x\n${MARKER}\ny` : 'x\ny'],
    ['docs/engines/focus_v5/ADR_REGISTER_V5.md', `h\n${MARKER}\n`],
  ]);
  const idx = (c = `${CURRENT_ROLE}\n`) => c;
  const cases = [
    { name: 'PASS: markers present, one index, clean legacy, legacy dispositioned in index',
      r: scanContent(ep(true), new Map([[INDEX_BASENAME, `${idx()}disposition: GATE_CATALOG_V5.md + FOCUS_ENGINE_MASTER_PLAN_V5.md\n`], ['GATE_CATALOG_V5.md', 'gates'], ['FOCUS_ENGINE_MASTER_PLAN_V5.md', 'plan']])),
      expectOk: true },
    { name: 'FAIL: quarantine gap — present legacy doc not dispositioned in index',
      r: scanContent(ep(true), new Map([[INDEX_BASENAME, idx()], ['GATE_CATALOG_V5.md', 'gates']])),
      expectOk: false },
    { name: 'FAIL: an entrypoint lacks the marker',
      r: scanContent(ep(false), new Map([[INDEX_BASENAME, idx()]])),
      expectOk: false },
    { name: 'FAIL: duplicate CURRENT role',
      r: scanContent(ep(true), new Map([[INDEX_BASENAME, idx()], ['OTHER.md', idx()]])),
      expectOk: false },
    { name: 'FAIL: legacy doc carries the marker',
      r: scanContent(ep(true), new Map([[INDEX_BASENAME, idx()], ['GATE_CATALOG_V5.md', `gates\n${MARKER}\n`]])),
      expectOk: false },
    { name: 'FAIL: no file declares the CURRENT role',
      r: scanContent(ep(true), new Map([['SOMETHING.md', 'nothing']])),
      expectOk: false },
  ];
  let bad = 0;
  for (const c of cases) {
    const pass = c.r.ok === c.expectOk;
    if (!pass) bad++;
    console.log(`${pass ? 'OK  ' : 'BAD '} ${c.name}${pass ? '' : ` (got ok=${c.r.ok}; errors=${c.r.errors.join('; ')})`}`);
  }
  return bad === 0;
}

const isSelfTest = process.argv.includes('--self-test');
const result = isSelfTest ? { ok: selfTest(), errors: [] } : scanRepo();
if (isSelfTest) {
  console.log(result.ok ? 'SELF-TEST OK — all oracle cases behaved as expected' : 'SELF-TEST FAILED');
  process.exit(result.ok ? 0 : 1);
} else if (result.ok) {
  console.log('AUTHORITY ENTRYPOINTS OK — index discoverable from all mandatory entrypoints; no legacy masquerade');
  process.exit(0);
} else {
  console.error('AUTHORITY ENTRYPOINTS FAILED:');
  for (const e of result.errors) console.error(`  - ${e}`);
  process.exit(1);
}
