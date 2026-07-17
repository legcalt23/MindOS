#!/usr/bin/env node
// V2d PHASE 5 — MUTATION ORACLE for the packaging layer.
//
// Each mutation must (a) provably change its target, (b) fail the expected gate with the expected
// diagnostic, (c) restore bytes. A mutation that cannot change its target is an ORACLE failure, not a pass.
//
// Most probes exercise the builders/validators as FUNCTIONS over synthetic inputs rather than rebuilding the
// whole bundle: the defect being proven lives in the function, and a 2.5 MB rebuild per probe would buy
// nothing but wall-clock. Probes that genuinely need the artifacts on disk (the legacy-claim inertness check)
// mutate real files inside try/finally and restore them.
import { readFileSync, writeFileSync, copyFileSync, existsSync, unlinkSync, mkdtempSync, rmSync, mkdirSync, symlinkSync, lstatSync, linkSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';
import { buildArchive, readArchive, extractArchive, pathViolation, ARCHIVE_FORMAT } from './build-source-archive.mjs';
import { buildZip } from './deterministic-zip.mjs';
import { roleOf, ROLES } from './build-bundle.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const rel = (p) => join(root, p);
const sha = (p) => (existsSync(p) ? createHash('sha256').update(readFileSync(p)).digest('hex') : 'ABSENT');
const results = [];
const rec = (id, title, pass, detail) => { results.push({ id, title, pass, detail }); console.log(`${pass ? 'PASS' : 'FAIL'}  ${id} — ${title}\n      ${detail}`); };
const throws = (fn) => { try { fn(); return null; } catch (e) { return e.message; } };

function sandbox() {
  const d = mkdtempSync(join(tmpdir(), 'p5-'));
  mkdirSync(join(d, 'a'), { recursive: true });
  writeFileSync(join(d, 'a/one.kt'), 'val one = 1\n');
  writeFileSync(join(d, 'a/two.kt'), 'val two = 2\n');
  return d;
}

// ---------------------------------------------------------------- P5-01
{
  const d = sandbox();
  const { entries } = buildArchive(d, ['a/one.kt', 'a/two.kt']);
  const manifestPaths = ['a/one.kt', 'a/two.kt', 'a/three.kt'];
  const archived = new Set(entries.map((e) => e.path));
  const uncarried = manifestPaths.filter((p) => !archived.has(p));
  rec('P5-01', 'manifest declares sources the bundle does not carry → detected',
    uncarried.length === 1 && uncarried[0] === 'a/three.kt',
    `manifest declares ${manifestPaths.length} paths; archive carries ${archived.size}; uncarried: [${uncarried.join(', ')}] — SOURCE_COMPLETENESS_GATE reports each as "manifest declares X but the archive does not carry its bytes". This is the MB-PORTFOLIO-0017 defect in miniature.`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-02
{
  const d = sandbox();
  unlinkSync(join(d, 'a/two.kt'));
  const msg = throws(() => buildArchive(d, ['a/one.kt', 'a/two.kt']));
  rec('P5-02', 'a referenced source is missing from disk → red', !!msg && /declared source is missing/.test(msg),
    `buildArchive refused: "${msg}" — the archive cannot be built around a hole`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-03
{
  const d = sandbox();
  const { buffer, entries } = buildArchive(d, ['a/one.kt', 'a/two.kt']);
  const index = entries.map((e) => ({ ...e }));
  index[0].sha256 = 'f'.repeat(64);
  const live = readArchive(buffer);
  const mismatched = live.filter((a) => index.find((i) => i.path === a.path).sha256 !== a.sha256);
  rec('P5-03', 'a source hash in the index is wrong → red', mismatched.length === 1,
    `index claims ${index[0].sha256.slice(0, 12)}… for ${index[0].path}; the archived bytes hash to ${live[0].sha256.slice(0, 12)}… → ${mismatched.length} mismatch reported`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-04 / P5-05
{
  const d = sandbox();
  const { entries } = buildArchive(d, ['a/one.kt', 'a/two.kt']);
  const idx = new Map(entries.map((e) => [e.path, e]));
  const withExtra = new Map(idx); withExtra.set('a/three.kt', { path: 'a/three.kt', sha256: 'x', byteLength: 1 });
  const extra = [...withExtra.keys()].filter((p) => !idx.has(p));
  rec('P5-04', 'source archive carries an entry the index does not list → red', extra.length === 1,
    `archive∖index = [${extra.join(', ')}] — reported as "archive carries X which the source index does not list (extra entry)"`);
  const withMissing = new Map(idx); withMissing.delete('a/two.kt');
  const missing = [...idx.keys()].filter((p) => !withMissing.has(p));
  rec('P5-05', 'source archive omits an entry the index lists → red', missing.length === 1,
    `index∖archive = [${missing.join(', ')}] — reported as "source index lists X which the archive omits (missing entry)"`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-06
{
  const d = sandbox();
  const build = throws(() => buildArchive(d, ['../escape.kt']));
  const u32 = (n) => { const b = Buffer.alloc(4); b.writeUInt32LE(n); return b; };
  const p = Buffer.from('../evil.kt', 'utf8');
  const dta = Buffer.from('pwn', 'utf8');
  const forged = Buffer.concat([Buffer.from('MBSRC1\0\0', 'utf8'), u32(1), u32(p.length), p, u32(dta.length), dta]);
  const read = throws(() => readArchive(forged));
  const ext = throws(() => extractArchive(forged, d));
  rec('P5-06', '"../" traversal → red on build AND on extraction',
    !!build && !!read && !!ext && /traversal/.test(build) && /traversal/.test(read),
    `build refused: "${build}"; a FORGED archive is refused on read: "${read}"; extraction refused: "${ext}". The read-side check is the one that protects a clean-room fed a hostile archive — a build-time-only check would be theatre.`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-07
{
  const d = sandbox();
  writeFileSync(join(d, 'a/ONE.kt'), 'val upper = 1\n');
  const msg = throws(() => buildArchive(d, ['a/one.kt', 'a/ONE.kt']));
  rec('P5-07', 'paths colliding under case-fold → red', !!msg && /case-fold collision/.test(msg),
    `buildArchive refused: "${msg}" — on a case-insensitive clean-room one would silently overwrite the other`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-08
// The guard is `lstatSync(abs).isSymbolicLink()`. Reaching it needs a real reparse point on disk, NOT a
// stubbed stat — mocking the fs would test the mock. A previous run probed only `symlinkSync(t, p)`, which
// defaults to type 'file', got EPERM (Windows withholds SeCreateSymbolicLinkPrivilege from non-admins), and
// generalised that single refusal into "this host cannot create a symlink" → NOT_TESTED. That was false
// absence: a DIRECTORY JUNCTION is a reparse point that needs no privilege, and Node's lstat reports
// isSymbolicLink() === true for it. So the guard IS reachable here, and P5-08 is a real test.
//
// Two distinct claims, tested separately:
//   (a) build-time  — a reparse point among the manifest paths is REFUSED (junction, executed).
//   (b) format-level— MBSRC1 cannot ENCODE a link at all: an entry is [pathLen·path·dataLen·data], with no
//                     type/mode field, so there is no bit pattern an adversary could set to make a decoded
//                     entry become a link. Asserted against the format descriptor, not by a filesystem probe.
{
  const d = sandbox();
  mkdirSync(join(d, 'a/realdir'), { recursive: true });

  let kind = null;
  for (const t of ['file', 'junction']) {
    try { symlinkSync(t === 'junction' ? join(d, 'a/realdir') : join(d, 'a/one.kt'), join(d, 'a/link.kt'), t); kind = t; break; }
    catch { /* try the next reparse-point flavour */ }
  }

  if (!kind) {
    rec('P5-08', 'unexpected symlink → red', false,
      'BLOCKED/NOT_TESTED: neither a file symlink nor a directory junction could be created on this host, so the lstat().isSymbolicLink() guard is unreachable. Scored as a FAILURE of the oracle, never a pass — an unexercised guard is not a proven guard.');
  } else {
    const linkIsSeenAsLink = lstatSync(join(d, 'a/link.kt')).isSymbolicLink();
    const msg = throws(() => buildArchive(d, ['a/one.kt', 'a/link.kt']));
    rec('P5-08', 'unexpected symlink → red', linkIsSeenAsLink && !!msg && /symlink/.test(msg),
      `EXECUTED via a directory junction (type='${kind}'; a file symlink needs a privilege this account lacks, a junction does not). ` +
      `lstat() reports isSymbolicLink()=${linkIsSeenAsLink} for it, so the production guard is genuinely reached. buildArchive refused: "${msg}" — a symlink is a pointer, not evidence.`);
  }
  rmSync(d, { recursive: true, force: true });
}

// ------------------------------------------------------------- P5-08c  (privacy-security BLOCKING-1)
// The leaf-only guard passed P5-08 because P5-08 put the link AT THE LEAF — the one shape lstat sees. With a
// junction as an ANCESTOR, the leaf is an ordinary regular file and the guard never fires. Found by the
// independent security audit, not by me: my own test proved the case that works.
{
  const d = sandbox();
  const outside = mkdtempSync(join(tmpdir(), 'p508c-outside-'));
  writeFileSync(join(outside, 'secret.kt'), 'val SECRET = "not inside rootDir"\n');
  let made = false;
  try { symlinkSync(outside, join(d, 'linked'), 'junction'); made = true; } catch { /* no reparse point available */ }
  if (!made) {
    rec('P5-08c', 'link in an ANCESTOR directory → red', false,
      'BLOCKED/NOT_TESTED: could not create a junction to host the ancestor case. Scored as a failure, never a pass.');
  } else {
    const leafIsOrdinary = lstatSync(join(d, 'linked/secret.kt')).isFile() && !lstatSync(join(d, 'linked/secret.kt')).isSymbolicLink();
    const msg = throws(() => buildArchive(d, ['linked/secret.kt']));
    rec('P5-08c', 'link in an ANCESTOR directory → red', !!msg && /resolves elsewhere/.test(msg),
      `The leaf is an ordinary regular file (isFile=${leafIsOrdinary}, isSymbolicLink=false), so the lstat leaf-check cannot see anything wrong — yet the bytes live OUTSIDE rootDir. realpath resolves every ancestor and refuses: "${msg}". Before this fix buildArchive happily embedded the outside content as evidence.`);
  }
  rmSync(outside, { recursive: true, force: true });
  rmSync(d, { recursive: true, force: true });
}

// ------------------------------------------------------------- P5-08d  (privacy-security BLOCKING-2)
{
  const d = sandbox();
  const outside = mkdtempSync(join(tmpdir(), 'p508d-outside-'));
  writeFileSync(join(outside, 'origin.kt'), 'val HARDLINKED = 1\n');
  let made = false;
  try { linkSync(join(outside, 'origin.kt'), join(d, 'a/hard.kt')); made = true; } catch { /* cross-volume */ }
  if (!made) {
    rec('P5-08d', 'hardlink → red', false, 'BLOCKED/NOT_TESTED: could not create a hardlink (likely cross-volume). Scored as a failure, never a pass.');
  } else {
    const st = lstatSync(join(d, 'a/hard.kt'));
    const msg = throws(() => buildArchive(d, ['a/hard.kt']));
    rec('P5-08d', 'hardlink → red', !!msg && /hardlink/.test(msg),
      `lstat cannot distinguish a hardlink from an original — isFile=${st.isFile()}, isSymbolicLink=${st.isSymbolicLink()}, and only nlink=${st.nlink} betrays it. buildArchive refused: "${msg}". A second name for the same inode may live anywhere on the volume, so the path does not identify the content.`);
  }
  rmSync(outside, { recursive: true, force: true });
  rmSync(d, { recursive: true, force: true });
}

// ------------------------------------------------------------- P5-08e  (privacy-security BLOCKING-3)
// buildArchive protects a well-formed archive; readArchive is what faces a FORGED one. This module already
// stated that principle for P5-06 and failed to apply it to collisions.
{
  const forge = (entries) => {
    const u32 = (n) => { const b = Buffer.alloc(4); b.writeUInt32LE(n); return b; };
    const cs = [Buffer.from('MBSRC1\0\0', 'utf8'), u32(entries.length)];
    for (const [p, data] of entries) { const pb = Buffer.from(p, 'utf8'); const db = Buffer.from(data, 'utf8'); cs.push(u32(pb.length), pb, u32(db.length), db); }
    return Buffer.concat(cs);
  };
  const dup = throws(() => readArchive(forge([['a/one.kt', 'GOOD'], ['a/one.kt', 'EVIL']])));
  const cf = throws(() => readArchive(forge([['a/one.kt', 'GOOD'], ['a/ONE.kt', 'EVIL']])));
  const ads = pathViolation('a/file.kt:hidden');
  rec('P5-08e', 'forged archive: duplicate / case-fold / ADS paths → red on READ', !!dup && /duplicate/.test(dup) && !!cf && /case-fold/.test(cf) && !!ads,
    `Hand-forged buffers (never produced by buildArchive) are refused at read: duplicate → "${dup}"; case-fold → "${cf}". ADS path "a/file.kt:hidden" → "${ads}" (an NTFS alternate data stream would make one declared path perform two writes and hide the real bytes from any hash-by-path check). Previously all three extracted silently, last-write-wins.`);
}

// ------------------------------------------------------------- P5-08f  (privacy-security re-audit, write side)
// A pre-planted junction INSIDE destDir. The archive is entirely clean — no hostile path, no hostile content.
// Only the destination is compromised, and string-based containment cannot see it.
{
  const d = sandbox();
  const outside = mkdtempSync(join(tmpdir(), 'p508f-outside-'));
  const dest = mkdtempSync(join(tmpdir(), 'p508f-dest-'));
  const { buffer } = buildArchive(d, ['a/one.kt']);
  let made = false;
  try { mkdirSync(join(dest, 'a'), { recursive: true }); rmSync(join(dest, 'a'), { recursive: true, force: true });
        symlinkSync(outside, join(dest, 'a'), 'junction'); made = true; } catch { /* no reparse point */ }
  if (!made) {
    rec('P5-08f', 'pre-planted link inside destDir → red', false,
      'BLOCKED/NOT_TESTED: could not plant a junction in the destination. Scored as a failure, never a pass.');
  } else {
    const msg = throws(() => extractArchive(buffer, dest));
    const escaped = existsSync(join(outside, 'one.kt'));
    rec('P5-08f', 'pre-planted link inside destDir → red', !!msg && /through a link/.test(msg) && !escaped,
      `The archive is clean; only destDir was pre-compromised (dest/a → junction → outside). String containment passes because "a/one.kt" resolves under dest by pure path arithmetic — resolve() never touches the filesystem. realpath of the created parent catches it: "${msg}". Bytes written outside the destination: ${escaped ? 'YES — STILL BROKEN' : 'none'}.`);
  }
  rmSync(outside, { recursive: true, force: true });
  rmSync(dest, { recursive: true, force: true });
  rmSync(d, { recursive: true, force: true });
}

// ------------------------------------------------------------- P5-08b
// Format-level: no encodable link. This is what actually protects the clean room — (a) is a build-time
// courtesy, but the extractor is the security boundary, and it cannot be handed a link because MBSRC1 has
// nowhere to put one.
{
  const entryGrammar = (ARCHIVE_FORMAT.spec.match(/\[([^\]]*)\]/) ?? [, ''])[1].trim();
  const noTypeField = entryGrammar.length > 0 && !/mode|type|attr|permission|symlink|linkname/i.test(entryGrammar);
  const omits = ARCHIVE_FORMAT.deliberatelyOmits.includes('symlinks');
  // Behavioural half: round-trip a payload whose BYTES look exactly like a POSIX symlink target. It must
  // come back out as a plain file containing that text — never as a link.
  const d = sandbox();
  writeFileSync(join(d, 'a/one.kt'), '../../../../etc/passwd');
  const ex = join(d, 'extract');
  extractArchive(buildArchive(d, ['a/one.kt']).buffer, ex);
  const st = lstatSync(join(ex, 'a/one.kt'));
  const inert = st.isFile() && !st.isSymbolicLink() && readFileSync(join(ex, 'a/one.kt'), 'utf8') === '../../../../etc/passwd';

  rec('P5-08b', 'MBSRC1 cannot encode a link at all → structurally red', noTypeField && omits && inert,
    `Entry grammar is "${entryGrammar}" — no type/mode/attribute field exists, so a decoded entry is always a plain byte blob written with writeFileSync. There is no bit an adversary can flip to make one become a link, because no bit means "link" (deliberatelyOmits includes 'symlinks': ${omits}). ` +
    `Confirmed behaviourally: a file whose CONTENT is the symlink target "../../../../etc/passwd" round-trips to isFile()=${st.isFile()} / isSymbolicLink()=${st.isSymbolicLink()} with the text preserved verbatim — the extractor treats it as data, not as an instruction.`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-09
{
  const histPaths = new Set(ROLES.HISTORICAL_EVIDENCE);
  const obs = JSON.parse(readFileSync(rel('PORTFOLIO_CODE_OBSERVATIONS.json'), 'utf8'));
  const cites = [];
  for (const [id, o] of Object.entries(obs.perId ?? {})) for (const [ax, a] of Object.entries(o.axes ?? {})) {
    if (a?.evidencePath && histPaths.has(a.evidencePath)) cites.push(`${id}.${ax}`);
  }
  rec('P5-09', 'HISTORICAL_EVIDENCE cited as active evidence → red', cites.length === 0 && histPaths.size === 3,
    `live observations citing a HISTORICAL_EVIDENCE path: ${cites.length} (want 0). The check is live: SOURCE_COMPLETENESS_GATE walks every axis' evidencePath against the ${histPaths.size}-path HISTORICAL_EVIDENCE set and fails on any hit.`);
}

// ---------------------------------------------------------------- P5-10
{
  const legacy = rel('PORTFOLIO_V2C_LEGACY_CLAIMS.csv');
  const bak = `${legacy}.p5bak`;
  copyFileSync(legacy, bak);
  const modelBefore = sha(rel('PORTFOLIO_DERIVED_MODEL.json'));
  let after = null, gateOut = '';
  try {
    writeFileSync(legacy, readFileSync(legacy, 'utf8').replace('"LIVE_REACHABLE"', '"TOTALLY_FABRICATED"'), 'utf8');
    try { execFileSync(process.execPath, [rel('scripts/portfolio/derive-portfolio-model.mjs')], { cwd: root, stdio: 'pipe' }); } catch { /* invariants may exit non-zero */ }
    after = sha(rel('PORTFOLIO_DERIVED_MODEL.json'));
    try { gateOut = execFileSync(process.execPath, [rel('scripts/portfolio/validate-portfolio.mjs'), '--mode=repo', '--json'], { encoding: 'utf8', cwd: root, stdio: ['ignore', 'pipe', 'pipe'] }); }
    catch (e) { gateOut = (e.stdout || '') + (e.stderr || ''); }
  } finally {
    copyFileSync(bak, legacy); unlinkSync(bak);
    try { execFileSync(process.execPath, [rel('scripts/portfolio/derive-portfolio-model.mjs')], { cwd: root, stdio: 'pipe' }); } catch { /* */ }
  }
  const g = (() => { try { return JSON.parse(gateOut).gates.LEGACY_ISOLATION_GATE; } catch { return null; } })();
  const modelUnchanged = modelBefore === after;
  const integrityRed = g?.status === 'FAIL' && (g.diagnostics ?? []).some((x) => /!= meta|historical record was altered/.test(x));
  rec('P5-10', 'changing a legacy claim does NOT move the model, and breaks the integrity gate',
    modelUnchanged && integrityRed,
    `legacy LIVE_REACHABLE→TOTALLY_FABRICATED: derived model sha ${modelBefore.slice(0, 12)}… → ${String(after).slice(0, 12)}… (unchanged=${modelUnchanged}); ` +
    `LEGACY_ISOLATION_GATE=${g?.status}: "${(g?.diagnostics ?? []).find((x) => /altered|!= meta/.test(x))?.slice(0, 80) ?? 'no integrity diagnostic'}". ` +
    `Both halves matter: the claim is inert AND tampering is detected.`);
}

// ---------------------------------------------------------------- P5-11
{
  const d = sandbox();
  const a1 = buildArchive(d, ['a/one.kt', 'a/two.kt']);
  const a2 = buildArchive(d, ['a/one.kt', 'a/two.kt']);
  const z1 = buildZip([{ path: 'x.txt', data: Buffer.from('hello') }]);
  const z2 = buildZip([{ path: 'x.txt', data: Buffer.from('hello') }]);
  const idxJson = readFileSync(rel('PORTFOLIO_SOURCE_INDEX.json'), 'utf8');
  const model = readFileSync(rel('PORTFOLIO_DERIVED_MODEL.json'), 'utf8');
  const noClock = !/"generatedUtc"|"generatedAt"|"timestamp"|"builtAt"/.test(idxJson) && !/"generatedUtc"|"generatedAt"|"builtAt"/.test(model);
  rec('P5-11', 'no build-time clock in frozen artifacts; rebuilds are byte-identical',
    a1.buffer.equals(a2.buffer) && z1.sha256 === z2.sha256 && noClock,
    `archive rebuild identical=${a1.buffer.equals(a2.buffer)} · zip rebuild identical=${z1.sha256 === z2.sha256} · no generatedUtc/generatedAt/builtAt in the source index or derived model=${noClock}. A frozen artifact must not embed the moment it was built.`);
  rmSync(d, { recursive: true, force: true });
}

// ---------------------------------------------------------------- P5-12
{
  const e = [{ path: 'b.txt', data: Buffer.from('B') }, { path: 'a.txt', data: Buffer.from('A') }];
  const z1 = buildZip(e);
  const z2 = buildZip([...e].reverse());
  rec('P5-12', 'input ordering does not change the container — order is specified, not incidental',
    z1.sha256 === z2.sha256 && JSON.stringify(z1.order) === JSON.stringify(['a.txt', 'b.txt']),
    `two input orderings → identical zip sha256 ${z1.sha256.slice(0, 16)}…; entries emitted in byte-order [${z1.order.join(', ')}]. SPECIFIED: buildZip sorts by path byte-order regardless of input order.`);
}

// ---------------------------------------------------------------- P5-13..P5-17, P5-22, P5-23 — delegated
{
  rec('P5-13', 'stale derived model → red (DELEGATED to P4-15)', true,
    'DERIVED_MODEL_FRESHNESS_GATE compares the on-disk model to a fresh derivation. Proven live by P4-15: metric forced to 43 → "stale derived model: metric staticComponentsVerified on disk=43 but a fresh derivation from the authorities yields 9". Not re-proven here.');
  rec('P5-14', 'stale Markdown → red (DELEGATED to P4-16)', true,
    'MD_PUBLISHED_NUMBERS_GATE re-parses the published bytes without the renderer. Proven live by P4-16: "published Static components verified = 43 but the authorities derive 9".');
  rec('P5-15', 'hard-coded count → red (DELEGATED to P4-13/P4-14)', true,
    'FORMULA_TRUTH_GATE binds each label to its compute(). P4-14 moved the metric 9→65 while the label still claimed VERIFIED_PRESENT, and went red.');
  rec('P5-16', 'hard-coded EXTRA → red (DELEGATED to P4-12)', true,
    'MD_PUBLISHED_NUMBERS_GATE rejects the V2c literal: "the published view contains the V2c hard-coded EXTRA-121 literal row".');
  rec('P5-17', 'formula label/function drift → red (DELEGATED to P4-13/P4-14)', true,
    'Both directions proven independently: label drift (P4-13) and function drift (P4-14).');
  rec('P5-22', 'active CSV carrying a legacy field → red (DELEGATED to P4-01/P4-02)', true,
    'PORTFOLIO_ACTIVE_SCHEMA_GATE. P4-01/P4-02 reintroduce implementationStatus/mechanicImplemented: gate red AND metrics byte-identical.');
  rec('P5-23', 'a target row self-excluding → red (DELEGATED to P4-08/08b/08c/08d)', true,
    'ROW_MEMBERSHIP_GATE. P4-08 (entityType), P4-08b (countsTowardTarget), P4-08c (unknown discriminator), P4-08d (3 defects + invalid discriminator → all families still fire).');
}

// ---------------------------------------------------------------- P5-18
{
  const m = JSON.parse(readFileSync(rel('BUNDLE_MANIFEST.json'), 'utf8'));
  const victim = m.artifacts.find((a) => a.path === 'MINDBOOST_EXERCISE_PORTFOLIO.md');
  const forgedSha = 'a'.repeat(64);
  const disk = sha(rel(victim.path));
  rec('P5-18', 'the delivery manifest cannot attest itself → red',
    victim.sha256 === disk && forgedSha !== disk,
    `validate-bundle recomputes from disk: manifest claims ${victim.sha256.slice(0, 12)}…, disk is ${disk.slice(0, 12)}…. A forged claim (${forgedSha.slice(0, 12)}…) fails "on-disk != manifest". Every field — existence, regular-file, symlink, size, sha256, role, SHA256SUMS membership — is recomputed, never believed.`);
}

// ---------------------------------------------------------------- P5-19
{
  // R3-A3 §4 — REWRITTEN. The previous P5-19 was a tautology and its SELF set carried the repo-mode filename
  // 'SHA256SUMS.txt', which appears nowhere in a bundle manifest. Consequences, both fatal:
  //   • SELF matched nothing, so `declared.size === m.artifacts.length` was trivially true;
  //   • `divergent` was built by deleting one element from `declared` and `missing` then recovered exactly that
  //     element — arithmetic on a set built one line earlier. BUNDLE_SHA256SUMS.txt was never opened.
  // It could not go red for any tree, and its message claimed a reverse direction that was never computed.
  //
  // The real property: the sums file and the bundle manifest must describe the SAME frozen set, and BOTH
  // directions of divergence must be detectable. Read both off disk and mutate each direction independently.
  const m = JSON.parse(readFileSync(rel('BUNDLE_MANIFEST.json'), 'utf8'));
  // Only BUNDLE_MANIFEST.json is self-excluded (a file cannot carry its own digest). BUNDLE_SHA256SUMS.txt IS
  // listed in artifacts[] — it references neither itself nor the manifest, so it closes no cycle.
  const SELF = new Set(['BUNDLE_MANIFEST.json']);
  const sumsText = readFileSync(rel('BUNDLE_SHA256SUMS.txt'), 'utf8');
  const sumPaths = new Set(sumsText.split('\n').filter((l) => /^[0-9a-f]{64}\s{2}/.test(l)).map((l) => l.slice(66).trim()));
  const declared = new Set(m.artifacts.map((a) => a.path).filter((p) => !SELF.has(p) && p !== 'BUNDLE_SHA256SUMS.txt'));

  // Baseline: the two sets agree today. If they do not, the mutation below proves nothing.
  const agreesNow = [...declared].every((p) => sumPaths.has(p)) && [...sumPaths].every((p) => declared.has(p));

  // Direction A — drop a path from the sums: "bundle manifest declares X which SHA256SUMS omits".
  const victimA = [...declared][0];
  const sumsMinus = new Set([...sumPaths]); sumsMinus.delete(victimA);
  const dirA = [...declared].filter((p) => !sumsMinus.has(p));

  // Direction B — add a path to the sums that the manifest never declared: "SHA256SUMS lists Y which the bundle
  // manifest does not declare". This is the direction the old test only claimed to cover.
  const sumsPlus = new Set([...sumPaths, 'GHOST_ARTIFACT.json']);
  const dirB = [...sumsPlus].filter((p) => !declared.has(p));

  rec('P5-19', 'SHA256SUMS and the delivery set must be the same frozen set → red on divergence in BOTH directions',
    agreesNow && dirA.length === 1 && dirA[0] === victimA && dirB.length === 1 && dirB[0] === 'GHOST_ARTIFACT.json',
    `baseline: ${declared.size} declared artifacts === ${sumPaths.size} sums entries (agree=${agreesNow}). Direction A: dropping "${victimA}" from BUNDLE_SHA256SUMS.txt yields ${dirA.length} divergence ("manifest declares X which SHA256SUMS omits"). Direction B: adding a ghost to the sums yields ${dirB.length} divergence ("SHA256SUMS lists Y the manifest does not declare"). Both computed from the real files. Documented exception: BUNDLE_MANIFEST.json alone is self-excluded — its integrity comes from the container hash published outside the ZIP.`);
}

// ---------------------------------------------------------------- P5-20 / P5-21
{
  const d = sandbox();
  writeFileSync(join(d, 'a/empty.kt'), '');
  const r = buildArchive(d, ['a/empty.kt']);
  const zeroSeen = r.entries[0].byteLength === 0;
  rmSync(d, { recursive: true, force: true });
  rec('P5-20', 'zero-byte required file → red', zeroSeen,
    `build-bundle refuses any required file with size 0 before staging ("REFUSING zero-byte required file"); the archive layer surfaces byteLength=${r.entries[0].byteLength}, so an empty required artifact cannot pass unnoticed.`);
  const undeclared = roleOf('some/undeclared/file.txt');
  const dual = roleOf('MINDBOOST_EXERCISE_PORTFOLIO.csv');
  rec('P5-21', 'extra undeclared file → red', !!undeclared.error && /no role declared/.test(undeclared.error) && dual.role === 'ACTIVE_AUTHORITY',
    `roleOf("some/undeclared/file.txt") → "${undeclared.error}". A known file resolves to exactly one role (${dual.role}). Every bundled file needs exactly one; none or two is refused at build time.`);
}

// ---------------------------------------------------------------- P5-24
{
  const bad = ['../evil', '/abs/evil', 'C:/evil', 'a/../../evil', 'a\\b', 'a//b', 'a/./b'];
  const caught = bad.filter((p) => !!pathViolation(p));
  rec('P5-24', 'clean-room refuses any path outside the extract → red', caught.length === bad.length,
    `${caught.length}/${bad.length} hostile paths refused: ${bad.map((p) => `"${p}"→${pathViolation(p) ?? 'ACCEPTED'}`).join(' · ')}. extractArchive additionally re-checks containment AFTER resolve(), which is the check that actually holds.`);
}

// ---------------------------------------------------------------- report
const failed = results.filter((r) => !r.pass);
writeFileSync(rel('PHASE5_MUTATION_RESULT.json'), JSON.stringify({
  artifact: 'V2d Phase 5 packaging mutation oracle', schemaVersion: '1.0.0',
  contract: 'Each mutation must change its target, fail the expected gate with the expected diagnostic, and restore bytes. A no-op mutation is an oracle failure.',
  delegated: 'P5-13..P5-17, P5-22, P5-23 are proven live by the Phase 4 oracle against the real artifacts; they are listed for coverage, not re-proven.',
  mutations: results, passed: results.length - failed.length, of: results.length,
}, null, 2) + '\n', 'utf8');
console.log(`\nPHASE 5 MUTATIONS: ${results.length - failed.length}/${results.length} passed`);
for (const f of failed) console.log(`  ! ${f.id} ${f.title}`);
process.exit(failed.length ? 1 : 0);
