#!/usr/bin/env node
// V2d PHASE 5 — BUNDLE VALIDATOR. Recomputes everything from disk; the manifest is never taken at its word.
// Runs identically in the repo and inside a clean-room extract (pass the extract dir as argv[2]).
import { readFileSync, existsSync, lstatSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve, sep } from 'node:path';
import { readArchive, pathViolation } from './build-source-archive.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const base = process.argv[2] ? resolve(process.argv[2]) : join(here, '..', '..');
const rel = (p) => join(base, p);
const gates = {};
const gate = (n, c, d) => { gates[n] ??= { status: 'PASS', diagnostics: [] }; if (!c) { gates[n].status = 'FAIL'; gates[n].diagnostics.push(d); } };
const readJson = (p) => { try { return JSON.parse(readFileSync(p, 'utf8')); } catch { return null; } };

const manifest = readJson(rel('BUNDLE_MANIFEST.json'));
const srcIndex = readJson(rel('PORTFOLIO_SOURCE_INDEX.json'));
const srcManifest = readJson(rel('PORTFOLIO_SOURCE_MANIFEST_V2.json'));

// ---------- BUNDLE_ROLE_GATE ----------
if (!manifest) gate('BUNDLE_ROLE_GATE', false, 'BUNDLE_MANIFEST.json missing');
else {
  const seen = new Map(), lower = new Map();
  for (const a of manifest.artifacts ?? []) {
    gate('BUNDLE_ROLE_GATE', !!a.role, `no role declared for "${a.path}"`);
    gate('BUNDLE_ROLE_GATE', !seen.has(a.path), `duplicate bundle entry: ${a.path}`);
    seen.set(a.path, a);
    const k = a.path.toLowerCase();
    gate('BUNDLE_ROLE_GATE', !lower.has(k) || lower.get(k) === a.path, `case-fold collision: ${a.path} vs ${lower.get(k)}`);
    lower.set(k, a.path);
    gate('BUNDLE_ROLE_GATE', !pathViolation(a.path.split('\\').join('/')), `unsafe bundle path: ${a.path}`);
  }
}

// ---------- DELIVERY_INTEGRITY_GATE — recomputed from disk, never from the manifest's claim ----------
if (manifest) {
  const sumsPath = rel('BUNDLE_SHA256SUMS.txt');
  const sumsText = existsSync(sumsPath) ? readFileSync(sumsPath, 'utf8') : '';
  const sumMap = new Map(sumsText.split('\n').filter((l) => /^[0-9a-f]{64}\s{2}/.test(l))
    .map((l) => { const [h, ...r] = l.split(/\s{2}/); return [r.join('  ').trim(), h]; }));
  // Exempt from SHA256SUMS MEMBERSHIP — a different question from manifest self-exclusion, and conflating the
  // two broke this gate once already:
  //   • BUNDLE_MANIFEST.json     is written after the sums, and hashes them; listing it here would be circular.
  //   • BUNDLE_SHA256SUMS.txt    cannot appear in its own list — that is the irreducible case.
  // Both are nonetheless HASHED by BUNDLE_MANIFEST.json (artifacts[]), so neither is unprotected. Only
  // BUNDLE_MANIFEST.json itself is covered by nothing but the out-of-band container digest.
  const SELF = new Set(['BUNDLE_MANIFEST.json', 'BUNDLE_SHA256SUMS.txt']);
  for (const a of manifest.artifacts ?? []) {
    const abs = rel(a.path);
    gate('DELIVERY_INTEGRITY_GATE', existsSync(abs), `missing: ${a.path}`);
    if (!existsSync(abs)) continue;
    const st = lstatSync(abs);
    gate('DELIVERY_INTEGRITY_GATE', st.isFile(), `not a regular file: ${a.path}`);
    gate('DELIVERY_INTEGRITY_GATE', !st.isSymbolicLink(), `unexpected symlink: ${a.path}`);
    gate('DELIVERY_INTEGRITY_GATE', st.size > 0, `zero-byte required file: ${a.path}`);
    const disk = createHash('sha256').update(readFileSync(abs)).digest('hex');
    gate('DELIVERY_INTEGRITY_GATE', disk === a.sha256, `${a.path}: on-disk ${disk.slice(0, 12)}… != manifest ${String(a.sha256).slice(0, 12)}…`);
    gate('DELIVERY_INTEGRITY_GATE', st.size === a.byteLength, `${a.path}: on-disk ${st.size}B != manifest ${a.byteLength}B`);
    if (!SELF.has(a.path)) gate('DELIVERY_INTEGRITY_GATE', sumMap.get(a.path) === disk, `${a.path}: absent from SHA256SUMS or hash mismatch`);
  }
  // SHA256SUMS and the manifest must describe the SAME frozen set (minus the documented self-excluded pair).
  const declared = new Set((manifest.artifacts ?? []).map((a) => a.path).filter((p) => !SELF.has(p)));
  for (const [p] of sumMap) gate('DELIVERY_INTEGRITY_GATE', declared.has(p), `SHA256SUMS lists "${p}" which the bundle manifest does not declare`);
  for (const p of declared) gate('DELIVERY_INTEGRITY_GATE', sumMap.has(p), `bundle manifest declares "${p}" which SHA256SUMS omits`);
}

// ---------- SOURCE_COMPLETENESS_GATE — the bytes must be here, not merely hashed ----------
if (!srcIndex) gate('SOURCE_COMPLETENESS_GATE', false, 'PORTFOLIO_SOURCE_INDEX.json missing');
else if (!existsSync(rel(srcIndex.archive.filename))) gate('SOURCE_COMPLETENESS_GATE', false, `source archive ${srcIndex.archive.filename} missing — the manifest hashes ${srcIndex.entries.length} paths whose bytes are not in this bundle (MB-PORTFOLIO-0017)`);
else {
  const buf = readFileSync(rel(srcIndex.archive.filename));
  const archiveSha = createHash('sha256').update(buf).digest('hex');
  gate('SOURCE_COMPLETENESS_GATE', archiveSha === srcIndex.archive.sha256, `source archive sha256 ${archiveSha.slice(0, 12)}… != index ${String(srcIndex.archive.sha256).slice(0, 12)}…`);
  let entries = null;
  try { entries = readArchive(buf); } catch (e) { gate('SOURCE_COMPLETENESS_GATE', false, `source archive unreadable: ${e.message}`); }
  if (entries) {
    gate('SOURCE_COMPLETENESS_GATE', entries.length === srcIndex.archive.entryCount, `archive holds ${entries.length} entries, index declares ${srcIndex.archive.entryCount}`);
    const inArchive = new Map(entries.map((e) => [e.path, e]));
    const inIndex = new Map(srcIndex.entries.map((e) => [e.path, e]));
    const manEntries = (srcManifest?.entries ?? srcManifest?.sources ?? []);
    // manifest entries == packaged entries == index entries == per-entry hash set
    for (const m of manEntries) {
      const a = inArchive.get(m.path);
      gate('SOURCE_COMPLETENESS_GATE', !!a, `manifest declares "${m.path}" but the archive does not carry its bytes`);
      if (a && m.sha256) gate('SOURCE_COMPLETENESS_GATE', a.sha256 === m.sha256, `${m.path}: archived ${a.sha256.slice(0, 12)}… != manifest ${String(m.sha256).slice(0, 12)}…`);
    }
    for (const [p, a] of inArchive) {
      gate('SOURCE_COMPLETENESS_GATE', inIndex.has(p), `archive carries "${p}" which the source index does not list (extra entry)`);
      const i = inIndex.get(p);
      if (i) {
        gate('SOURCE_COMPLETENESS_GATE', i.sha256 === a.sha256, `${p}: index ${String(i.sha256).slice(0, 12)}… != archive ${a.sha256.slice(0, 12)}…`);
        gate('SOURCE_COMPLETENESS_GATE', i.byteLength === a.byteLength, `${p}: index ${i.byteLength}B != archive ${a.byteLength}B`);
      }
    }
    for (const [p] of inIndex) gate('SOURCE_COMPLETENESS_GATE', inArchive.has(p), `source index lists "${p}" which the archive omits (missing entry)`);
    gate('SOURCE_COMPLETENESS_GATE', manEntries.length === entries.length, `manifest ${manEntries.length} entries != packaged ${entries.length}`);
    // HISTORICAL_EVIDENCE may be packaged but may never satisfy an ACTIVE evidence reference.
    const histPaths = new Set(manEntries.filter((e) => e.role === 'HISTORICAL_EVIDENCE').map((e) => e.path));
    const obs = readJson(rel('PORTFOLIO_CODE_OBSERVATIONS.json'));
    if (obs) for (const [id, o] of Object.entries(obs.perId ?? {})) for (const [ax, a] of Object.entries(o.axes ?? {})) {
      if (a?.evidencePath && histPaths.has(a.evidencePath)) gate('SOURCE_COMPLETENESS_GATE', false, `${id}.${ax} cites HISTORICAL_EVIDENCE "${a.evidencePath}" as active evidence`);
    }
  }
}

// ---------- CLEAN_ROOM_CONTAINMENT_GATE — no path may escape the extract ----------
if (srcIndex) {
  for (const p of srcIndex.pathIndex ?? []) {
    const v = pathViolation(p);
    gate('CLEAN_ROOM_CONTAINMENT_GATE', !v, `source index path "${p}": ${v}`);
    const abs = resolve(base, p);
    gate('CLEAN_ROOM_CONTAINMENT_GATE', abs === base || abs.startsWith(base + sep), `"${p}" resolves outside the extract root`);
  }
}

// ---------- RUNBOOK_GATE — no absolute paths, no username, no repo reference ----------
{
  const rb = rel('CLEAN_ROOM_RUNBOOK.md');
  gate('RUNBOOK_GATE', existsSync(rb), 'CLEAN_ROOM_RUNBOOK.md missing');
  if (existsSync(rb)) {
    const t = readFileSync(rb, 'utf8');
    gate('RUNBOOK_GATE', !/[A-Za-z]:\\Users\\|\/Users\/[a-z]+\/|\/home\/[a-z]+\//.test(t), 'runbook contains an absolute user path');
    gate('RUNBOOK_GATE', !/\bcurro\b/i.test(t), 'runbook leaks a username');
    gate('RUNBOOK_GATE', !/AndroidStudioProjects/.test(t), 'runbook references the original repository location');
  }
}

const ORDER = ['BUNDLE_ROLE_GATE', 'DELIVERY_INTEGRITY_GATE', 'SOURCE_COMPLETENESS_GATE', 'CLEAN_ROOM_CONTAINMENT_GATE', 'RUNBOOK_GATE'];
for (const g of ORDER) gates[g] ??= { status: 'PASS', diagnostics: [] };
const failed = ORDER.some((g) => gates[g].status === 'FAIL');
if (process.argv.includes('--json')) { console.log(JSON.stringify({ gates, failed }, null, 2)); process.exit(failed ? 1 : 0); }
console.log(`BUNDLE VALIDATION (base: ${base === join(here, '..', '..') ? 'repo' : base})`);
for (const g of ORDER) { console.log(`  ${g}: ${gates[g].status}`); for (const d of gates[g].diagnostics.slice(0, 5)) console.log(`      - ${d}`); if (gates[g].diagnostics.length > 5) console.log(`      … ${gates[g].diagnostics.length - 5} more`); }
console.log(failed ? 'BUNDLE GATES: FAIL' : 'BUNDLE GATES: PASS');
process.exit(failed ? 1 : 0);
