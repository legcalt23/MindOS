#!/usr/bin/env node
// V2d — VALIDATION CONTEXT (MB-V2D-R03, R2 redesign).
//
// This module no longer DETECTS a mode. It VALIDATES a context that the caller must state. The distinction is
// the whole point: a detector lets the evaluated bytes choose their own policy, and every version of that idea
// has been broken within a day.
//
// History, so the next reader does not re-derive it:
//   v1  inferred the mode from the presence of gen-delivery.mjs / BUNDLE_MANIFEST.json — deletable bytes INSIDE
//       the tree being judged. Deleting two files downgraded a repo to NOT_APPLICABLE.
//   v2  replaced that with a "closed set" invariant and an optional --mode. Four bypasses were reproduced
//       independently within one round:
//         VM-01  `selfExcluded` is manifest-controlled, so a manifest listing every repo file there declared
//                itself closed. mode=bundle, gate=PASS. My own R03-05 test never probed this field.
//         VM-02  only `actual - listed` was computed, never `listed - actual`, so a manifest could enumerate
//                artifacts that do not exist and still be "closed".
//         VM-03  `if (modeArg && ...)` skipped validation for "" (falsy), and `modeArg ?? default` does not
//                fall back on "" (not nullish) — so mode="" sailed through with gate=PASS.
//         VM-04  no schema validation before spreading `artifacts`, so a manifest with an object there threw
//                TypeError instead of returning a diagnostic.
//
// The rule that survives all four: the mode is an ARGUMENT, required and closed-enum; the tree can only ever
// REFUTE it. Refutation never selects a laxer policy — it fails.
import { existsSync, readFileSync, readdirSync, lstatSync, realpathSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { join, sep } from 'node:path';

export const MODES = Object.freeze(['repo', 'bundle']);
export const MANIFEST_SCHEMA_VERSION = '2.0.0';

// selfExcluded is NOT an arbitrary manifest-controlled list (VM-01). The permitted set is fixed HERE, in code,
// and any element outside it is a schema violation. Only the manifest itself qualifies, for one reason that is
// arithmetic rather than convenience: a file cannot contain its own hash. Everything else — sources,
// validators, scripts, model, view, evidence — CAN be hashed, so nothing else may ever be excluded.
export const SELF_EXCLUDABLE = Object.freeze(['BUNDLE_MANIFEST.json']);

const D = (code, message) => ({ code, message });
const norm = (p) => String(p ?? '').replace(/\\/g, '/');
// Unicode normalisation collisions: "café" (NFC) and "café" (NFD) are different byte strings that many
// filesystems treat as one file. Compare on NFC + case-fold so neither can smuggle a second entry past.
const collideKey = (p) => norm(p).normalize('NFC').toLowerCase();

// ---------------------------------------------------------------- manifest schema (strict)
// Validated BEFORE any field is used. A manifest is an untrusted document until it passes this.
export function validateManifestSchema(bm) {
  const d = [];
  if (bm === null || typeof bm !== 'object' || Array.isArray(bm)) return [D('MANIFEST_NOT_OBJECT', 'manifest root must be a JSON object')];
  if (!Array.isArray(bm.artifacts)) d.push(D('ARTIFACTS_NOT_ARRAY', `artifacts must be an array, got ${Array.isArray(bm.artifacts) ? 'array' : typeof bm.artifacts}`));
  if ('selfExcluded' in bm && !Array.isArray(bm.selfExcluded)) d.push(D('SELF_EXCLUDED_NOT_ARRAY', `selfExcluded must be an array, got ${typeof bm.selfExcluded}`));
  if (d.length) return d;                                   // no point reading entries out of a wrong shape

  const seen = new Map();
  const checkEntry = (e, where, i) => {
    if (e === null || typeof e !== 'object' || Array.isArray(e)) { d.push(D('ENTRY_NOT_OBJECT', `${where}[${i}] must be an object`)); return; }
    if (typeof e.path !== 'string' || e.path.length === 0) { d.push(D('ENTRY_PATH_INVALID', `${where}[${i}].path must be a non-empty string`)); return; }
    // Validate the RAW string, NOT the normalised one. norm() rewrites backslashes to forward slashes, so
    // normalising first silently repaired the very thing the check exists to reject and made the backslash
    // rule dead code — caught by R03-04, which reported "backslash→ACCEPTED". A separator is ambiguous by
    // nature: "a\b.js" is one filename on Linux and two path segments on Windows, so the same manifest would
    // describe different trees on different hosts.
    const v = manifestPathViolation(e.path);
    const p = norm(e.path);
    if (v) { d.push(D('ENTRY_PATH_UNSAFE', `${where}[${i}].path "${e.path}": ${v}`)); return; }
    const k = collideKey(p);
    if (seen.has(k)) d.push(D('ENTRY_PATH_COLLISION', `${where}[${i}].path "${e.path}" collides with "${seen.get(k)}" after Unicode-NFC + case folding — two entries would resolve to one file`));
    else seen.set(k, e.path);
    if ('sha256' in e && e.sha256 !== null && !/^[0-9a-f]{64}$/.test(String(e.sha256))) d.push(D('ENTRY_HASH_MALFORMED', `${where}[${i}].sha256 "${e.sha256}" is not a 64-char lowercase hex digest`));
    if ('byteLength' in e && e.byteLength !== null && (!Number.isInteger(e.byteLength) || e.byteLength < 0)) d.push(D('ENTRY_SIZE_INVALID', `${where}[${i}].byteLength must be a non-negative integer`));
  };
  bm.artifacts.forEach((e, i) => checkEntry(e, 'artifacts', i));
  (bm.selfExcluded ?? []).forEach((e, i) => {
    checkEntry(e, 'selfExcluded', i);
    // VM-01: this is the barrier. selfExcluded may name ONLY what code permits.
    const p = norm(e?.path);
    if (p && !SELF_EXCLUDABLE.includes(p)) {
      d.push(D('SELF_EXCLUDED_FORBIDDEN', `selfExcluded[${i}] names "${e.path}", which is not in the code-defined SELF_EXCLUDABLE set [${SELF_EXCLUDABLE.join(', ')}]. selfExcluded is not a manifest-controlled escape hatch: a manifest that could exclude arbitrary files could declare any tree closed and thereby choose its own validation policy. Only a file that cannot contain its own hash may be excluded.`));
    }
  });
  return d;
}

export function manifestPathViolation(p) {
  if (!p) return 'empty path';
  if (p.includes('\0')) return 'NUL byte in path';
  if (p.includes('\\')) return 'backslash (manifest paths are always /-separated)';
  if (p.startsWith('/')) return 'absolute path';
  if (/^[A-Za-z]:/.test(p)) return 'drive-letter path';
  if (p.startsWith('//')) return 'UNC path';
  if (p.includes(':')) return 'colon (drive-letter or NTFS alternate-data-stream)';
  const segs = p.split('/');
  if (segs.some((s) => s === '..')) return 'parent traversal ("..")';
  if (segs.some((s) => s === '.')) return 'dot segment';
  if (segs.some((s) => s === '')) return 'empty path segment';
  return null;
}

// ---------------------------------------------------------------- exact set equality
// BOTH directions. VM-02 existed because only actual−manifest was computed. Full lists are retained; only the
// human-facing diagnostic is truncated.
export function snapshotEquality(inputRoot, bm) {
  const manifestFiles = new Set([...(bm.artifacts ?? []), ...(bm.selfExcluded ?? [])].map((e) => norm(e.path)));
  const actualFiles = new Set();
  const special = [];
  const walk = (dir, pre = '') => {
    for (const e of readdirSync(dir, { withFileTypes: true })) {
      const r = pre ? `${pre}/${e.name}` : e.name;
      const abs = join(dir, e.name);
      if (e.isSymbolicLink()) { special.push({ path: r, why: 'symlink/junction — a closed snapshot contains regular files only' }); continue; }
      if (e.isDirectory()) { walk(abs, r); continue; }
      if (!e.isFile()) { special.push({ path: r, why: 'not a regular file' }); continue; }
      const st = lstatSync(abs);
      if (st.nlink > 1) special.push({ path: r, why: `hardlink (nlink=${st.nlink}) — a second name for the same inode may live outside the snapshot` });
      actualFiles.add(r);
    }
  };
  walk(inputRoot);
  const unexpected = [...actualFiles].filter((p) => !manifestFiles.has(p));
  const missing = [...manifestFiles].filter((p) => !actualFiles.has(p));
  return { actualCount: actualFiles.size, manifestCount: manifestFiles.size, unexpected, missing, special };
}

// ---------------------------------------------------------------- hash verification
export function verifyHashes(inputRoot, bm) {
  const bad = [];
  for (const e of (bm.artifacts ?? [])) {
    if (!e.sha256) continue;
    const abs = join(inputRoot, norm(e.path));
    if (!existsSync(abs)) continue;                          // reported by snapshotEquality.missing
    const actual = createHash('sha256').update(readFileSync(abs)).digest('hex');
    if (actual !== e.sha256) bad.push({ path: norm(e.path), claimed: e.sha256, actual });
  }
  return bad;
}

/**
 * Validate an explicitly-stated context. There is no default and no autodetection.
 * @param {{mode:string, inputRoot:string, manifest?:object}} ctx
 * @returns {{gate:'PASS'|'FAIL', mode:string|null, diagnostics:Array, equality:object|null}}
 */
export function validateContext({ mode, inputRoot, manifest } = {}) {
  const diagnostics = [];
  const fail = (extra = {}) => ({ gate: 'FAIL', mode: MODES.includes(mode) ? mode : null, diagnostics, equality: null, ...extra });

  // VM-03: strict identity against the closed enum. Not truthiness — "" , false, 0, null, undefined and any
  // unknown string are all equally invalid, and none of them may fall back to a default.
  if (typeof mode !== 'string' || !MODES.includes(mode)) {
    diagnostics.push(D('MODE_INVALID', `mode must be exactly one of [${MODES.join(' | ')}]; received ${JSON.stringify(mode)} (${typeof mode}). There is no default: an unstated mode is a caller error, never an invitation to guess.`));
    return fail();
  }
  if (typeof inputRoot !== 'string' || !inputRoot || !existsSync(inputRoot)) {
    diagnostics.push(D('INPUT_ROOT_INVALID', `inputRoot must be an existing directory; received ${JSON.stringify(inputRoot)}`));
    return fail();
  }

  if (mode === 'repo') {
    // Repo is the STRICTER policy and can never be reached by degradation — only by being stated. A repo is an
    // open set by nature, so there is nothing about the tree to refute; the repo-side gates do the work.
    diagnostics.push(D('MODE_REPO', 'repo mode stated by the caller. The tree is an open set by nature (it holds sources no bundle manifest lists), so no closed-set claim is made or checked. Repo-side gates apply in full and cannot be skipped.'));
    return { gate: 'PASS', mode, diagnostics, equality: null };
  }

  // bundle mode — the claim must be PROVEN, and every step can only refute it.
  let bm = manifest;
  if (bm === undefined) {
    const p = join(inputRoot, 'BUNDLE_MANIFEST.json');
    if (!existsSync(p)) { diagnostics.push(D('MANIFEST_ABSENT', 'bundle mode requires BUNDLE_MANIFEST.json in inputRoot')); return fail(); }
    try { bm = JSON.parse(readFileSync(p, 'utf8')); }
    catch (e) { diagnostics.push(D('MANIFEST_UNPARSEABLE', `BUNDLE_MANIFEST.json is not valid JSON: ${e.message}`)); return fail(); }
  }

  // VM-04: schema BEFORE use. A malformed manifest returns a diagnostic, never a TypeError.
  const schema = validateManifestSchema(bm);
  if (schema.length) { diagnostics.push(...schema); return fail(); }
  if (bm.schemaVersion !== undefined && String(bm.schemaVersion) !== MANIFEST_SCHEMA_VERSION) {
    diagnostics.push(D('MANIFEST_SCHEMA_VERSION', `manifest schemaVersion "${bm.schemaVersion}" is not the supported ${MANIFEST_SCHEMA_VERSION}`));
    return fail();
  }

  const equality = snapshotEquality(inputRoot, bm);
  for (const s of equality.special) diagnostics.push(D('SNAPSHOT_SPECIAL_FILE', `${s.path}: ${s.why}`));
  if (equality.unexpected.length) diagnostics.push(D('SNAPSHOT_UNEXPECTED', `${equality.unexpected.length} file(s) present but not listed (e.g. ${equality.unexpected.slice(0, 3).join(', ')}) — a closed set contains nothing the manifest does not name. Full list in the machine-readable result.`));
  if (equality.missing.length) diagnostics.push(D('SNAPSHOT_MISSING', `${equality.missing.length} file(s) listed but absent (e.g. ${equality.missing.slice(0, 3).join(', ')}) — a manifest that names what is not there does not describe this tree. Full list in the machine-readable result.`));

  const badHashes = verifyHashes(inputRoot, bm);
  for (const b of badHashes.slice(0, 3)) diagnostics.push(D('HASH_MISMATCH', `${b.path}: manifest claims ${b.claimed.slice(0, 12)}…, disk is ${b.actual.slice(0, 12)}…`));
  if (badHashes.length > 3) diagnostics.push(D('HASH_MISMATCH', `…and ${badHashes.length - 3} more`));

  const ok = equality.unexpected.length === 0 && equality.missing.length === 0 && equality.special.length === 0 && badHashes.length === 0;
  if (ok) diagnostics.push(D('MODE_BUNDLE', `bundle mode stated by the caller and PROVEN: ${equality.actualCount} files on disk, ${equality.manifestCount} named by the manifest, exact set equality in both directions, all regular files, every declared hash re-verified from disk.`));
  return { gate: ok ? 'PASS' : 'FAIL', mode, diagnostics, equality: { ...equality, badHashes } };
}
