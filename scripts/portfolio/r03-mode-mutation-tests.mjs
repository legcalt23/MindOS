#!/usr/bin/env node
// MB-V2D-R03 (R2) — trust-root mutations against the PRODUCTION validateContext.
//
// The v2 suite passed 10/10 and was still wrong: it only ever planted an `artifacts`-based manifest, so it
// never touched `selfExcluded` — the field that actually granted the bypass (VM-01). A mutation suite that
// does not probe the field carrying the authority proves nothing about it. VM-01..VM-04 are therefore the
// first four cases here, each stated as the exact fixture that was reproduced against the old engine.
//
// Every case runs the shipped function over a disposable tree. The canonical repo is never mutated.
import { mkdtempSync, mkdirSync, writeFileSync, rmSync, symlinkSync, linkSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { validateContext, validateManifestSchema, SELF_EXCLUDABLE } from './validation-mode.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const results = [];
const rec = (id, requirement, expectedGate, expectedCode, pass, evidence) => {
  results.push({ id, suite: 'r03', requirement, mutationOperator: 'trust-root fixture', expectedGate, expectedDiagnostic: expectedCode,
    pass, classification: pass ? 'KILLED' : 'SURVIVED', detail: evidence, evidence });
  console.log(`${pass ? 'KILLED  ' : 'SURVIVED'} ${id} — ${requirement}\n         ${evidence}`);
};
const codes = (r) => r.diagnostics.map((d) => d.code);
const has = (r, c) => codes(r).includes(c);

// A tree that is a genuine closed bundle: exactly what the manifest names, hashes correct.
function closedBundle() {
  const d = mkdtempSync(join(tmpdir(), 'r03-closed-'));
  const body = '{"a":1}';
  writeFileSync(join(d, 'A.json'), body);
  const bm = { schemaVersion: '2.0.0',
    artifacts: [{ path: 'A.json', sha256: createHash('sha256').update(body).digest('hex'), byteLength: body.length }],
    selfExcluded: [{ path: 'BUNDLE_MANIFEST.json' }] };
  writeFileSync(join(d, 'BUNDLE_MANIFEST.json'), JSON.stringify(bm));
  return { d, bm };
}
function repoTree() {
  const d = mkdtempSync(join(tmpdir(), 'r03-repo-'));
  mkdirSync(join(d, 'shared/src'), { recursive: true });
  writeFileSync(join(d, 'shared/src/Real.kt'), 'val x = 1\n');
  return d;
}

export function run() {
  results.length = 0;

  // ---- baseline: the honest closed bundle MUST pass, or every "FAIL" below is meaningless.
  { const { d } = closedBundle();
    const r = validateContext({ mode: 'bundle', inputRoot: d });
    rec('R03-00', 'BASELINE: a genuine closed bundle passes', 'PASS', 'MODE_BUNDLE', r.gate === 'PASS' && has(r, 'MODE_BUNDLE'),
      `gate=${r.gate} codes=[${codes(r)}]. Without a passing baseline, a suite of failures proves only that the validator can say no.`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- VM-01 (reproduced against the old engine: closed=true, mode=bundle, gate=PASS)
  { const d = repoTree();
    writeFileSync(join(d, 'repo-source.js'), 'secret');
    writeFileSync(join(d, 'BUNDLE_MANIFEST.json'), JSON.stringify({ schemaVersion: '2.0.0',
      artifacts: [], selfExcluded: [{ path: 'repo-source.js' }, { path: 'BUNDLE_MANIFEST.json' }] }));
    const r = validateContext({ mode: 'bundle', inputRoot: d });
    rec('R03-VM01', 'arbitrary selfExcluded cannot declare a tree closed', 'FAIL', 'SELF_EXCLUDED_FORBIDDEN',
      r.gate === 'FAIL' && has(r, 'SELF_EXCLUDED_FORBIDDEN'),
      `gate=${r.gate} codes=[${codes(r)}]. Old engine: closed=true, mode=bundle, gate=PASS — a manifest excluded a real repo source and thereby chose its own policy. SELF_EXCLUDABLE is now fixed in code to [${SELF_EXCLUDABLE.join(', ')}].`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- VM-02 (reproduced: closed=true with a listed file that does not exist)
  { const d = mkdtempSync(join(tmpdir(), 'r03-vm02-'));
    writeFileSync(join(d, 'BUNDLE_MANIFEST.json'), JSON.stringify({ schemaVersion: '2.0.0',
      artifacts: [{ path: 'missing.js' }], selfExcluded: [{ path: 'BUNDLE_MANIFEST.json' }] }));
    const r = validateContext({ mode: 'bundle', inputRoot: d });
    rec('R03-VM02', 'a listed artifact that is absent → FAIL', 'FAIL', 'SNAPSHOT_MISSING',
      r.gate === 'FAIL' && has(r, 'SNAPSHOT_MISSING'),
      `gate=${r.gate} codes=[${codes(r)}] missing=${JSON.stringify(r.equality?.missing)}. Old engine computed only actual−listed, never listed−actual, so a manifest could name files that do not exist and still be "closed".`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- VM-03 (reproduced: mode="" → gate=PASS)
  { const { d } = closedBundle();
    const cases = [['null', null], ['undefined', undefined], ['empty string', ''], ['false', false], ['zero', 0], ['unknown', 'bundle2'], ['array', ['bundle']]];
    const out = cases.map(([label, v]) => { const r = validateContext({ mode: v, inputRoot: d }); return { label, gate: r.gate, code: codes(r)[0], mode: r.mode }; });
    const allFail = out.every((o) => o.gate === 'FAIL' && o.code === 'MODE_INVALID' && o.mode === null);
    rec('R03-VM03', 'every non-enum mode → FAIL, never a default', 'FAIL', 'MODE_INVALID', allFail,
      out.map((o) => `${o.label}→${o.gate}`).join(' · ') + `. Old engine: mode="" passed with gate=PASS — "" is falsy so \`if (modeArg && ...)\` skipped validation, and \`??\` does not fall back on "" because it is not nullish. Strict enum identity now, no truthiness, no default.`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- VM-04 (reproduced: TypeError)
  { const d = mkdtempSync(join(tmpdir(), 'r03-vm04-'));
    const shapes = [['artifacts object', { artifacts: { path: 'x' } }], ['artifacts string', { artifacts: 'x' }],
      ['selfExcluded object', { artifacts: [], selfExcluded: { path: 'x' } }], ['root array', []], ['root null', null]];
    const out = shapes.map(([label, bm]) => { let r; try { r = validateContext({ mode: 'bundle', inputRoot: d, manifest: bm }); } catch (e) { return { label, gate: `THREW ${e.constructor.name}` }; } return { label, gate: r.gate, code: codes(r)[0] }; });
    const allFail = out.every((o) => o.gate === 'FAIL');
    rec('R03-VM04', 'a malformed manifest → structured FAIL, never a crash', 'FAIL', 'ARTIFACTS_NOT_ARRAY', allFail,
      out.map((o) => `${o.label}→${o.gate}${o.code ? '/' + o.code : ''}`).join(' · ') + `. Old engine threw TypeError: (bm.artifacts || []) is not iterable — an expected-invalid input must return a diagnostic, not an exception.`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- set equality: an unlisted sidecar
  { const { d } = closedBundle(); writeFileSync(join(d, 'SNEAKED_IN.json'), '{}');
    const r = validateContext({ mode: 'bundle', inputRoot: d });
    rec('R03-01', 'a file present but unlisted → FAIL', 'FAIL', 'SNAPSHOT_UNEXPECTED', r.gate === 'FAIL' && has(r, 'SNAPSHOT_UNEXPECTED'),
      `gate=${r.gate} unexpected=${JSON.stringify(r.equality?.unexpected)}. Hashes prove the integrity of what IS listed; only set equality proves nothing else was added.`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- hash mismatch
  { const { d } = closedBundle(); writeFileSync(join(d, 'A.json'), '{"a":2}');
    const r = validateContext({ mode: 'bundle', inputRoot: d });
    rec('R03-02', 'a hash that does not match disk → FAIL', 'FAIL', 'HASH_MISMATCH', r.gate === 'FAIL' && has(r, 'HASH_MISMATCH'),
      `gate=${r.gate} codes=[${codes(r)}]. The manifest is believed only after its claims are recomputed from disk.`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- manifest from another snapshot
  { const { d } = closedBundle();
    writeFileSync(join(d, 'BUNDLE_MANIFEST.json'), JSON.stringify({ schemaVersion: '2.0.0',
      artifacts: [{ path: 'SOMETHING_ELSE.json' }], selfExcluded: [{ path: 'BUNDLE_MANIFEST.json' }] }));
    const r = validateContext({ mode: 'bundle', inputRoot: d });
    rec('R03-03', 'a manifest from another snapshot → FAIL', 'FAIL', 'SNAPSHOT_MISSING',
      r.gate === 'FAIL' && has(r, 'SNAPSHOT_MISSING') && has(r, 'SNAPSHOT_UNEXPECTED'),
      `gate=${r.gate} missing=${JSON.stringify(r.equality?.missing)} unexpected=${JSON.stringify(r.equality?.unexpected)}. Both directions fire: it names what is absent and omits what is present.`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- unsafe paths in the manifest
  { const bads = [['traversal', '../evil.js'], ['absolute', '/etc/passwd'], ['drive letter', 'C:/evil.js'],
      ['UNC', '//host/share/x.js'], ['backslash', 'a\\b.js'], ['NTFS ADS', 'a.js:hidden'], ['dot segment', 'a/./b.js']];
    const out = bads.map(([label, p]) => ({ label, d: validateManifestSchema({ artifacts: [{ path: p }] }).map((x) => x.code) }));
    const allCaught = out.every((o) => o.d.includes('ENTRY_PATH_UNSAFE'));
    rec('R03-04', 'unsafe manifest paths → schema FAIL', 'FAIL', 'ENTRY_PATH_UNSAFE', allCaught,
      out.map((o) => `${o.label}→${o.d[0] ?? 'ACCEPTED'}`).join(' · '));
  }

  // ---- duplicate / case-fold / Unicode-normalisation collisions
  { const dup = validateManifestSchema({ artifacts: [{ path: 'a.js' }, { path: 'a.js' }] }).map((x) => x.code);
    const cf = validateManifestSchema({ artifacts: [{ path: 'a.js' }, { path: 'A.js' }] }).map((x) => x.code);
    const uni = validateManifestSchema({ artifacts: [{ path: 'café.js' }, { path: 'café.js' }] }).map((x) => x.code);
    rec('R03-05', 'duplicate / case-fold / Unicode-NFC collisions → schema FAIL', 'FAIL', 'ENTRY_PATH_COLLISION',
      dup.includes('ENTRY_PATH_COLLISION') && cf.includes('ENTRY_PATH_COLLISION') && uni.includes('ENTRY_PATH_COLLISION'),
      `duplicate→${dup[0]} · case-fold→${cf[0]} · NFD-vs-NFC "café"→${uni[0]}. The Unicode case matters because two different byte strings name one file on this filesystem.`);
  }

  // ---- malformed entries
  { const noPath = validateManifestSchema({ artifacts: [{ sha256: 'a'.repeat(64) }] }).map((x) => x.code);
    const badHash = validateManifestSchema({ artifacts: [{ path: 'a.js', sha256: 'nope' }] }).map((x) => x.code);
    const badSize = validateManifestSchema({ artifacts: [{ path: 'a.js', byteLength: -1 }] }).map((x) => x.code);
    const notObj = validateManifestSchema({ artifacts: ['a.js'] }).map((x) => x.code);
    rec('R03-06', 'entry without path / malformed hash / bad size / non-object → schema FAIL', 'FAIL', 'ENTRY_PATH_INVALID',
      noPath.includes('ENTRY_PATH_INVALID') && badHash.includes('ENTRY_HASH_MALFORMED') && badSize.includes('ENTRY_SIZE_INVALID') && notObj.includes('ENTRY_NOT_OBJECT'),
      `no path→${noPath[0]} · bad hash→${badHash[0]} · negative size→${badSize[0]} · string entry→${notObj[0]}`);
  }

  // ---- symlink / junction anywhere in the snapshot
  { const { d } = closedBundle();
    const outside = mkdtempSync(join(tmpdir(), 'r03-outside-'));
    let made = false;
    try { symlinkSync(outside, join(d, 'linked'), 'junction'); made = true; } catch { /* no reparse point */ }
    if (!made) rec('R03-07', 'a symlink/junction inside the snapshot → FAIL', 'FAIL', 'SNAPSHOT_SPECIAL_FILE', false,
      'BLOCKED/NOT_TESTED: could not create a junction. Scored as a failure, never a pass.');
    else { const r = validateContext({ mode: 'bundle', inputRoot: d });
      rec('R03-07', 'a symlink/junction inside the snapshot → FAIL', 'FAIL', 'SNAPSHOT_SPECIAL_FILE',
        r.gate === 'FAIL' && has(r, 'SNAPSHOT_SPECIAL_FILE'),
        `gate=${r.gate} codes=[${codes(r)}]. A closed snapshot contains regular files only — a pointer is not evidence, and the walk refuses to traverse it.`); }
    rmSync(outside, { recursive: true, force: true }); rmSync(d, { recursive: true, force: true }); }

  // ---- hardlink inside the snapshot
  { const { d } = closedBundle();
    const outside = mkdtempSync(join(tmpdir(), 'r03-hl-'));
    writeFileSync(join(outside, 'origin.bin'), 'x');
    let made = false;
    try { linkSync(join(outside, 'origin.bin'), join(d, 'hard.bin')); made = true; } catch { /* cross-volume */ }
    if (!made) rec('R03-08', 'a hardlink inside the snapshot → FAIL', 'FAIL', 'SNAPSHOT_SPECIAL_FILE', false,
      'BLOCKED/NOT_TESTED: could not create a hardlink. Scored as a failure, never a pass.');
    else { const r = validateContext({ mode: 'bundle', inputRoot: d });
      rec('R03-08', 'a hardlink inside the snapshot → FAIL', 'FAIL', 'SNAPSHOT_SPECIAL_FILE',
        r.gate === 'FAIL' && has(r, 'SNAPSHOT_SPECIAL_FILE'),
        `gate=${r.gate} codes=[${codes(r)}]. lstat cannot tell a hardlink from an original — only nlink>1 betrays it.`); }
    rmSync(outside, { recursive: true, force: true }); rmSync(d, { recursive: true, force: true }); }

  // ---- bundle mode against a repo
  { const d = repoTree();
    const r = validateContext({ mode: 'bundle', inputRoot: d });
    rec('R03-09', 'bundle mode stated over a repo → FAIL, never a downgrade', 'FAIL', 'MANIFEST_ABSENT',
      r.gate === 'FAIL', `gate=${r.gate} codes=[${codes(r)}]. Stating a mode is not proving it.`);
    rmSync(d, { recursive: true, force: true }); }

  // ---- repo mode against a bundle: keeps the STRICTER policy, never degrades
  { const { d } = closedBundle();
    const r = validateContext({ mode: 'repo', inputRoot: d });
    rec('R03-10', 'repo mode over a bundle tree → stays repo (strictest), never degrades', 'PASS', 'MODE_REPO',
      r.gate === 'PASS' && r.mode === 'repo' && has(r, 'MODE_REPO'),
      `gate=${r.gate} mode=${r.mode}. Repo is the stricter policy; the tree cannot talk the validator down into bundle mode. Repo-side gates then apply in full and fail on the artifacts a bundle does not carry.`);
    rmSync(d, { recursive: true, force: true }); }

  const killed = results.filter((r) => r.classification === 'KILLED').length;
  console.log(`\nR03 TRUST-ROOT MUTATIONS: ${killed}/${results.length} KILLED`);
  return { suite: 'MB-V2D-R03', total: results.length, killed, mutations: results };
}

if (process.argv[1] && process.argv[1].endsWith('r03-mode-mutation-tests.mjs')) {
  const r = run();
  writeFileSync(join(root, 'R03_MODE_MUTATION_RESULT.json'), JSON.stringify(r, null, 2) + '\n', 'utf8');
  process.exit(r.killed === r.total ? 0 : 1);
}
