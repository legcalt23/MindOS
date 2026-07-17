#!/usr/bin/env node
// V2d — THREE-ROOT CONTRACT (MB-V2D-R3-A §4).
//
// Why this module exists. The clean-room used ONE directory: the container was extracted into `dest`, then the
// source archive was extracted into `dest/sources`, and a probe script was written to `dest/_probe.mjs`. So the
// runbook polluted the very tree whose closed-set property it then tried to verify — Phase 8 could never reach
// 12/12, and every "fix" available at that point (allowlisting derived names, caching the closed-set claim
// before mutating, relaxing the contradiction) would have destroyed the invariant instead of satisfying it.
//
// The separation is physical, not conventional:
//
//   inputRoot   pristine extraction ONLY. Validated, fingerprinted, never written to again. Exactly the
//               manifest's file set — no sources/, no probes, no reports, no sidecars.
//   workRoot    everything mutable: extracted sources, regenerated model/view, scratch.
//   outputRoot  comparison reports and verdicts.
//
// Immutability of inputRoot is asserted by FINGERPRINT (before/after), not by a read-only attribute. The
// contract requires that the protection not rest on chmod alone: on Windows a read-only attribute is advisory
// (it does not stop a process running as the same user, and does not apply to directories at all). The
// attribute is set as defence-in-depth; the fingerprint is the proof.
import { mkdtempSync, mkdirSync, readdirSync, readFileSync, lstatSync, realpathSync, rmSync, chmodSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { tmpdir } from 'node:os';
import { join, sep, resolve } from 'node:path';

export const ROOT_KINDS = Object.freeze(['inputRoot', 'workRoot', 'outputRoot']);

export const THREE_ROOT_CONTRACT = Object.freeze({
  inputRoot: {
    receives: 'the pristine container extraction, and nothing else',
    mutableDuringRun: false,
    mayContainDerivedOutputs: false,
    proof: 'fingerprint(inputRoot) is identical before and after the whole run; a read-only attribute is set where the host allows, but is defence-in-depth, never the proof',
  },
  workRoot: {
    receives: 'extracted sources, regenerated model/view, scratch files',
    mutableDuringRun: true,
    constraint: 'must be physically disjoint from inputRoot — not equal, not an ancestor, not a descendant, and not reachable from it through a reparse point',
  },
  outputRoot: {
    receives: 'comparison reports and verdicts',
    mutableDuringRun: true,
    constraint: 'disjoint from both other roots; must start empty so a stale report can never be mistaken for this run',
  },
});

/** Physical disjointness, decided on REAL paths. A junction between two roots defeats any string comparison. */
export function disjointnessViolation(roots) {
  const real = {};
  for (const [k, p] of Object.entries(roots)) {
    try { real[k] = realpathSync(p); }
    catch (e) { return `${k} does not resolve: ${e.message}`; }
  }
  const ks = Object.keys(real);
  for (let i = 0; i < ks.length; i++) {
    for (let j = i + 1; j < ks.length; j++) {
      const a = real[ks[i]], b = real[ks[j]];
      if (a === b) return `${ks[i]} and ${ks[j]} resolve to the same directory (${a})`;
      if (a.startsWith(b + sep)) return `${ks[i]} (${a}) is inside ${ks[j]} (${b}) after link resolution`;
      if (b.startsWith(a + sep)) return `${ks[j]} (${b}) is inside ${ks[i]} (${a}) after link resolution`;
    }
  }
  return null;
}

/**
 * Content fingerprint of a tree: sorted relative path + sha256 of each file, hashed.
 * Records reparse points and hardlinks explicitly rather than following them — a pointer is not content, and
 * a fingerprint that silently followed one would report the target's bytes as if they lived here.
 */
export function fingerprint(dir) {
  const lines = [];
  const walk = (d, pre = '') => {
    for (const e of readdirSync(d, { withFileTypes: true }).sort((a, b) => (a.name < b.name ? -1 : 1))) {
      const r = pre ? `${pre}/${e.name}` : e.name;
      const abs = join(d, e.name);
      if (e.isSymbolicLink()) { lines.push(`${r}\tSYMLINK`); continue; }
      if (e.isDirectory()) { walk(abs, r); continue; }
      if (!e.isFile()) { lines.push(`${r}\tSPECIAL`); continue; }
      const st = lstatSync(abs);
      lines.push(`${r}\t${createHash('sha256').update(readFileSync(abs)).digest('hex')}\t${st.nlink > 1 ? 'NLINK>1' : 'ok'}`);
    }
  };
  walk(dir);
  const body = lines.join('\n');
  return { digest: createHash('sha256').update(body).digest('hex'), fileCount: lines.length, lines };
}

/** Create three empty, physically disjoint roots. Throws rather than return a compromised layout. */
export function createRoots(label = 'mb') {
  const base = mkdtempSync(join(tmpdir(), `${label}-roots-`));
  const roots = {
    inputRoot: join(base, 'input'),
    workRoot: join(base, 'work'),
    outputRoot: join(base, 'output'),
  };
  for (const p of Object.values(roots)) mkdirSync(p, { recursive: true });
  // Resolve AFTER creation: realpath needs the directory to exist, and a parent that is itself a junction
  // (os.tmpdir() is one on some hosts) must be resolved before any containment arithmetic is trusted.
  const v = disjointnessViolation(roots);
  if (v) { rmSync(base, { recursive: true, force: true }); throw new Error(`REFUSING compromised root layout: ${v}`); }
  return { base, ...roots, dispose: () => rmSync(base, { recursive: true, force: true }) };
}

/**
 * Seal inputRoot: take its fingerprint and set read-only where the host allows.
 * Returns a verifier that re-fingerprints and reports any drift. The verifier is the contract; the attribute
 * is a courtesy that a same-user process can defeat, which is precisely why it is not the proof.
 */
export function sealInput(inputRoot) {
  const before = fingerprint(inputRoot);
  let attributeApplied = 0, attributeFailed = 0;
  const walk = (d) => {
    for (const e of readdirSync(d, { withFileTypes: true })) {
      const abs = join(d, e.name);
      if (e.isDirectory()) { walk(abs); continue; }
      try { chmodSync(abs, 0o444); attributeApplied++; } catch { attributeFailed++; }
    }
  };
  try { walk(inputRoot); } catch { /* best-effort only — never the proof */ }
  return {
    before,
    attributeApplied, attributeFailed,
    note: 'read-only attribute is defence-in-depth. On Windows it is advisory for a same-user process and does not apply to directories, so immutability is PROVEN by the fingerprint comparison below, never by the attribute.',
    verify() {
      const after = fingerprint(inputRoot);
      const drifted = after.digest !== before.digest;
      const added = after.lines.filter((l) => !before.lines.includes(l));
      const removed = before.lines.filter((l) => !after.lines.includes(l));
      return {
        immutable: !drifted,
        before: before.digest, after: after.digest,
        fileCountBefore: before.fileCount, fileCountAfter: after.fileCount,
        added: added.slice(0, 10), removed: removed.slice(0, 10),
        addedCount: added.length, removedCount: removed.length,
      };
    },
  };
}

/** Refuse a write target that is not inside the root it claims. Used to prove outputs never land in input. */
export function assertWithin(root, target, what) {
  const realRoot = realpathSync(root);
  const abs = resolve(root, target);
  // Resolve the deepest existing ancestor: the leaf may not exist yet, but a junction above it would still
  // redirect the write.
  let probe = abs;
  for (;;) {
    try { probe = realpathSync(probe); break; }
    catch { const up = resolve(probe, '..'); if (up === probe) break; probe = up; }
  }
  if (probe !== realRoot && !probe.startsWith(realRoot + sep)) {
    throw new Error(`REFUSING to place ${what} outside its root: ${target} resolves under ${probe}, not ${realRoot}`);
  }
  return abs;
}
