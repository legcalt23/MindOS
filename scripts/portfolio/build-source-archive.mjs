#!/usr/bin/env node
// V2d PHASE 5 — DETERMINISTIC SOURCE ARCHIVE.
//
// MB-PORTFOLIO-0017: the Phase 3 bundle hashed 490 paths and shipped 0 of their bytes, so a clean-room
// could read the manifest but never recompute a single hash. This builds the bytes into a single
// deterministic container.
//
// Why a hand-rolled format rather than tar/zip: both embed mtime, uid/gid, and (for zip) tool-version and
// compression metadata, none of which are inputs to the evidence. Reproducing them byte-for-byte across
// machines means fighting the tool. `MBSRC1` contains ONLY what is being attested — the sorted path and the
// exact bytes — so byte-determinism is a property of the format, not of a flag we remembered to pass.
//
// FORMAT (MBSRC1), all integers little-endian uint32, all text UTF-8:
//   magic   "MBSRC1\0\0"                       (8 bytes)
//   count   uint32                              number of entries
//   entries sorted by path, BYTE order (not locale collation):
//             pathLen uint32 · path bytes · dataLen uint32 · data bytes
// No mtime. No permissions. No uid/gid. No compression. No symlinks — they are resolved or rejected.
//
// SAFETY (extraction is the dangerous half):
//   * paths are validated on WRITE and again on READ: relative, `/`-separated, no `..`, no leading `/`,
//     no drive letters, no backslashes, no NUL, no empty segments;
//   * case-fold collisions are rejected at build time (a Windows/macOS clean-room would silently overwrite);
//   * extraction refuses any path that escapes the destination after resolution.
import { readFileSync, writeFileSync, existsSync, lstatSync, mkdirSync, realpathSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve, sep } from 'node:path';

export const MAGIC = Buffer.from('MBSRC1\0\0', 'utf8');
export const ARCHIVE_FORMAT = {
  name: 'MBSRC1',
  spec: 'magic("MBSRC1\\0\\0") · count:uint32le · entries sorted by path byte-order [ pathLen:uint32le · path:utf8 · dataLen:uint32le · data ]',
  carries: ['relative path', 'exact file bytes'],
  deliberatelyOmits: ['mtime', 'permissions', 'uid/gid', 'compression', 'symlinks', 'directory entries'],
  rationale: 'the omitted fields are not evidence; including them would make byte-determinism depend on tool flags and host state',
};

// A path is only safe if it is relative, forward-slashed, and cannot escape the extraction root.
export function pathViolation(p) {
  if (!p) return 'empty path';
  if (p.includes('\0')) return 'NUL byte in path';
  if (p.includes('\\')) return 'backslash in path (archive paths are always /-separated)';
  if (p.startsWith('/')) return 'absolute path';
  if (/^[A-Za-z]:/.test(p)) return 'drive-letter path';
  // A colon ANYWHERE — not just at index 1 — is refused. On NTFS, "file.kt:hidden" names an Alternate Data
  // Stream: writeFileSync creates a 0-byte visible "file.kt" and hides the real bytes in the stream, so one
  // declared path performs two writes and the content is invisible to any hash-by-path check. Anchoring the
  // colon test at the string start caught only the drive-letter case and let the ADS case straight through.
  if (p.includes(':')) return 'colon in path (drive-letter or NTFS alternate-data-stream)';
  const segs = p.split('/');
  if (segs.some((s) => s === '..')) return 'parent traversal ("..")';
  if (segs.some((s) => s === '' )) return 'empty path segment';
  if (segs.some((s) => s === '.')) return 'dot segment';
  return null;
}

export function buildArchive(rootDir, relPaths) {
  const seen = new Map();
  const entries = [];
  for (const p of [...relPaths].sort()) {                 // byte-order sort, locale-independent
    const v = pathViolation(p);
    if (v) throw new Error(`REFUSING unsafe archive path "${p}": ${v}`);
    const lower = p.toLowerCase();
    if (seen.has(lower)) throw new Error(`REFUSING case-fold collision: "${p}" vs "${seen.get(lower)}" — a case-insensitive clean-room would silently overwrite one with the other`);
    seen.set(lower, p);
    const abs = join(rootDir, p);
    if (!existsSync(abs)) throw new Error(`REFUSING: declared source is missing from disk: ${p}`);
    const st = lstatSync(abs);
    // lstat inspects only the TERMINAL component. An earlier version stopped here, and P5-08 passed because
    // it tested a LINK AT THE LEAF — the one shape this catches. With `linked/dir/file.kt`, where `linked` is
    // a junction, the leaf is an ordinary regular file, the guard never fires, and bytes from OUTSIDE rootDir
    // are archived as evidence. realpath resolves every ancestor, so the whole path is checked, not its tail.
    if (st.isSymbolicLink()) throw new Error(`REFUSING symlink: ${p} — a symlink is a pointer, not evidence`);
    if (!st.isFile()) throw new Error(`REFUSING non-regular file: ${p}`);
    const real = realpathSync(abs);
    const realRoot = realpathSync(rootDir);
    if (real !== join(realRoot, ...p.split('/'))) {
      throw new Error(`REFUSING path that resolves elsewhere: ${p} → ${real} — a link somewhere in the path (leaf or ancestor) points outside its declared location; the archive would carry bytes that are not where it says they are`);
    }
    // A hardlink is indistinguishable from an original by lstat: same inode, isFile() true, isSymbolicLink()
    // false. nlink is the only signal. Refuse rather than archive a file with a second name that may live
    // anywhere on the volume.
    if (st.nlink > 1) throw new Error(`REFUSING hardlinked file: ${p} (nlink=${st.nlink}) — a second name for the same inode may live outside the tree, so the path does not identify the content`);
    const data = readFileSync(abs);
    entries.push({ path: p, data, sha256: createHash('sha256').update(data).digest('hex'), byteLength: data.length });
  }
  const chunks = [MAGIC];
  const u32 = (n) => { const b = Buffer.alloc(4); b.writeUInt32LE(n); return b; };
  chunks.push(u32(entries.length));
  for (const e of entries) {
    const pb = Buffer.from(e.path, 'utf8');
    chunks.push(u32(pb.length), pb, u32(e.data.length), e.data);
  }
  const buf = Buffer.concat(chunks);
  return { buffer: buf, entries: entries.map(({ path, sha256, byteLength }) => ({ path, sha256, byteLength })) };
}

export function readArchive(buf) {
  if (!buf.subarray(0, 8).equals(MAGIC)) throw new Error('not an MBSRC1 archive (bad magic)');
  let o = 8;
  const count = buf.readUInt32LE(o); o += 4;
  const out = [];
  // Cross-entry collisions must be caught HERE, not only in buildArchive. buildArchive protects a WELL-FORMED
  // archive we produced; readArchive is what faces a FORGED one. This module already states the principle for
  // P5-06 — "a build-time-only check would be theatre" — and then failed to apply it to collisions: two
  // entries at the same path (or paths equal under case-fold) extracted silently, last-write-wins, so a
  // hostile archive could overwrite a verified source with different bytes carrying the same manifest path.
  const seen = new Map();
  for (let i = 0; i < count; i++) {
    const pl = buf.readUInt32LE(o); o += 4;
    const path = buf.subarray(o, o + pl).toString('utf8'); o += pl;
    const dl = buf.readUInt32LE(o); o += 4;
    const data = buf.subarray(o, o + dl); o += dl;
    const v = pathViolation(path);
    if (v) throw new Error(`REFUSING unsafe path on extraction "${path}": ${v}`);
    const lower = path.toLowerCase();
    if (seen.has(lower)) {
      throw new Error(seen.get(lower) === path
        ? `REFUSING duplicate entry "${path}" — two entries share one path; extraction would silently keep whichever came last`
        : `REFUSING case-fold collision on extraction: "${path}" vs "${seen.get(lower)}" — on a case-insensitive clean-room one would silently overwrite the other`);
    }
    seen.set(lower, path);
    out.push({ path, data, sha256: createHash('sha256').update(data).digest('hex'), byteLength: data.length });
  }
  if (o !== buf.length) throw new Error(`trailing bytes: consumed ${o} of ${buf.length}`);
  return out;
}

// Extraction is the security boundary: readArchive validates the whole set before a single byte is written,
// and every write is checked against the REAL destination, not the declared one.
//
// The previous comment here claimed to "re-check containment AFTER resolution". It did not. `resolve()` is
// pure string arithmetic — it never touches the filesystem, so it cannot see that a directory inside destDir
// is a junction pointing elsewhere. With `destDir/sub` pre-planted as a reparse point, `writeFileSync` follows
// it and lands outside destDir while every string check passes. The archive needs no hostile path or content;
// only the destination has to be pre-compromised. This is the write-side twin of the ancestor-link hole, and
// the fix is the same: ask the filesystem where the path REALLY goes.
export function extractArchive(buf, destDir) {
  const dest = resolve(destDir);
  const entries = readArchive(buf);           // validates every entry (paths + collisions) before any write
  mkdirSync(dest, { recursive: true });
  const realDest = realpathSync(dest);        // destDir itself may legitimately sit under a symlinked tmpdir
  const written = [];
  const contained = (real) => real === realDest || real.startsWith(realDest + sep);
  for (const e of entries) {
    const abs = resolve(dest, e.path);
    if (abs !== dest && !abs.startsWith(dest + sep)) throw new Error(`REFUSING extraction outside the destination: "${e.path}" resolves to ${abs}`);
    const parent = dirname(abs);
    mkdirSync(parent, { recursive: true });
    // Resolve the parent AFTER creating it: this is what a pre-planted junction cannot survive.
    const realParent = realpathSync(parent);
    if (!contained(realParent)) {
      throw new Error(`REFUSING extraction through a link: "${e.path}" would be written to ${join(realParent, dirname(e.path) === '.' ? '' : '')} — a directory inside the destination resolves to ${realParent}, which is outside ${realDest}. String containment passed; the filesystem disagreed.`);
    }
    // Refuse to write THROUGH an existing link at the leaf, too: writeFileSync follows symlinks by default,
    // so an existing pointer at the target name would redirect the bytes even with a contained parent.
    if (existsSync(abs) && lstatSync(abs).isSymbolicLink()) {
      throw new Error(`REFUSING to write through an existing link at "${e.path}" — writeFileSync would follow it out of the destination`);
    }
    writeFileSync(abs, e.data);
    written.push(e.path);
  }
  return written;
}

if (process.argv[1]?.endsWith('build-source-archive.mjs')) {
  const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
  const manifest = JSON.parse(readFileSync(join(root, 'PORTFOLIO_SOURCE_MANIFEST_V2.json'), 'utf8'));
  const all = manifest.entries ?? manifest.sources ?? [];
  // HISTORICAL_EVIDENCE is packaged too (so the provenance travels) but is tagged, and can never satisfy an
  // ACTIVE evidence reference.
  const active = all.filter((e) => e.role !== 'HISTORICAL_EVIDENCE');
  const historical = all.filter((e) => e.role === 'HISTORICAL_EVIDENCE');

  const { buffer, entries } = buildArchive(root, all.map((e) => e.path));
  writeFileSync(join(root, 'PORTFOLIO_SOURCE_ARCHIVE.mbsrc'), buffer);

  const byPath = new Map(entries.map((e) => [e.path, e]));
  const drift = [];
  for (const m of all) {
    const a = byPath.get(m.path);
    if (!a) { drift.push(`${m.path}: declared in the manifest but absent from the archive`); continue; }
    if (m.sha256 && m.sha256 !== a.sha256) drift.push(`${m.path}: manifest sha256 ${String(m.sha256).slice(0, 12)}… != archived ${a.sha256.slice(0, 12)}…`);
    if (m.byteLength != null && m.byteLength !== a.byteLength) drift.push(`${m.path}: manifest byteLength ${m.byteLength} != archived ${a.byteLength}`);
  }
  const archiveSha = createHash('sha256').update(buffer).digest('hex');
  const index = {
    artifact: 'MindBoost portfolio SOURCE ARCHIVE index', schemaVersion: '1.0.0',
    purpose: 'The bytes the source manifest hashes. A clean-room extracts this and recomputes every hash WITHOUT the original repository (MB-PORTFOLIO-0017).',
    archive: { filename: 'PORTFOLIO_SOURCE_ARCHIVE.mbsrc', sha256: archiveSha, byteLength: buffer.length, entryCount: entries.length },
    format: ARCHIVE_FORMAT,
    construction: 'buildArchive(root, manifestPaths): paths sorted by BYTE order; each entry [pathLen·path·dataLen·data]; no mtime/permissions/compression; symlinks rejected; case-fold collisions rejected.',
    extraction: 'extractArchive(buf, dest): re-validates every path (relative, /-separated, no "..", no drive letter, no NUL) and re-checks containment AFTER resolve() — refusing any path that escapes dest.',
    traversalPrevention: 'pathViolation() on write AND read, plus a post-resolve containment check on extraction',
    symlinkPolicy: 'REJECTED at build time — a symlink is a pointer, not evidence',
    caseCollisionGate: 'REJECTED at build time — a case-insensitive clean-room would silently overwrite',
    counts: { manifestEntries: all.length, activeEntries: active.length, historicalEntries: historical.length, archivedEntries: entries.length },
    manifestDrift: drift,
    pathIndex: entries.map((e) => e.path),
    entries,
  };
  writeFileSync(join(root, 'PORTFOLIO_SOURCE_INDEX.json'), JSON.stringify(index, null, 2) + '\n', 'utf8');

  console.log(`SOURCE ARCHIVE: ${entries.length} entries, ${buffer.length} bytes, sha256 ${archiveSha}`);
  console.log(`  manifest entries ${all.length} (active ${active.length} · historical ${historical.length})`);
  console.log(`  manifest↔archive drift: ${drift.length}`);
  drift.slice(0, 5).forEach((d) => console.log(`    ! ${d}`));
  process.exit(drift.length ? 1 : 0);
}
