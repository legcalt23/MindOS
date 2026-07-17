#!/usr/bin/env node
// CODE-OBSERVATION AUTHORITY (V2d Phase 2 + Phase 3 corrections). Emits PORTFOLIO_CODE_OBSERVATIONS.json.
//
// STATIC OBSERVATIONS ONLY. It never emits implementation status, launch readiness, runtime verdicts or
// scientific judgement — those belong to other authorities (PORTFOLIO_AUTHORITY_MAP.md).
//
// Per exercise id and per axis it records: state, evidencePath, evidenceSha256, symbol, construct,
// matchRange, axisSupported, strategyId, strategyVersion, exhaustive, exhaustivenessEvidenceId,
// evidenceDependency, reason, reasonCode, limitations.
//
// Determinism: no timestamp, no HEAD, no wall clock. Pure function of source bytes + adapters.
import { readFileSync, existsSync, writeFileSync, readdirSync, statSync, lstatSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { ADAPTERS, AXES, CONTEXT_AXES, ADAPTERS_VERSION, HUB_FILE, S } from './block-adapters.mjs';
import * as scan from './kotlin-code-scan.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');

export function observe(rootDir = root) {
  const rd = (rel) => existsSync(join(rootDir, rel)) ? readFileSync(join(rootDir, rel), 'utf8') : null;
  const sha = (rel) => existsSync(join(rootDir, rel)) ? createHash('sha256').update(readFileSync(join(rootDir, rel))).digest('hex') : null;
  const ctx = {
    listDir: (rel) => { try { return readdirSync(join(rootDir, rel)); } catch { return []; } },
    isDir: (rel) => { try { return statSync(join(rootDir, rel)).isDirectory(); } catch { return false; } },
  };

  const sources = {}, missingSources = [];
  const need = (f) => { if (!(f in sources)) { const t = rd(f); sources[f] = t; if (t === null) missingSources.push(f); } return sources[f]; };
  need(HUB_FILE);

  const out = {
    artifact: 'MindBoost portfolio CODE OBSERVATIONS (static, semantic-binding evidence per axis)',
    schemaVersion: '3.0.0', adaptersVersion: ADAPTERS_VERSION, scannerVersion: scan.STRATEGY_VERSION,
    authority: 'code-observation',
    emits: 'static observations only — never implementation status, runtime, readiness or scientific judgement',
    contract: {
      VERIFIED_PRESENT: 'a semantic binding acceptable for the axis was observed; a bare code token is NOT sufficient (CODE_TOKEN_ONLY_NO_SEMANTIC_BINDING → UNRESOLVED)',
      VERIFIED_ABSENT: 'requires exhaustive:true AND an exhaustivenessEvidenceId naming a closed-world source set; enforced below',
      UNRESOLVED: 'the strategy cannot resolve this architecture, or a declared source is missing',
      blockReachability: 'CONTEXT ONLY — never substitutes for exerciseRoute',
      evidenceDependency: 'SHARED when two axes rest on the same path+symbol+match; they are then NOT independent corroboration',
    },
    verifiedAbsentAllowedStrategies: [], contextAxes: CONTEXT_AXES,
    blocks: {}, perId: {}, sourceManifest: {}, missingSources: [], invariantViolations: [],
  };

  for (const [key, a] of Object.entries(ADAPTERS)) {
    for (const ax of AXES) for (const f of (a[ax].files || [])) need(f);
    const texts = sources;
    const declFiles = a.declaration.files;
    const declResolvable = declFiles.every((f) => texts[f] != null);
    const declaredIds = declResolvable ? a.declaration.enumerate(texts, scan) : [];

    out.blocks[key] = {
      label: a.label, strategyVersion: a.strategyVersion, limitations: a.limitations,
      strategies: Object.fromEntries(AXES.map((ax) => [ax, a[ax].strategyId])),
      declaredIds, declarationResolvable: declResolvable,
      missingSourceFiles: [...new Set(AXES.flatMap((ax) => a[ax].files || []))].filter((f) => texts[f] == null),
    };

    for (const id of declaredIds) {
      const rec = { block: key, axes: {} };
      const fingerprints = new Map();
      for (const ax of AXES) {
        const spec = a[ax];
        let r;
        try { r = spec.probe(id, texts, ctx); } catch (e) { r = { state: S.UNRESOLVED, reason: `probe threw: ${e.message}` }; }

        // C6 INVARIANT: VERIFIED_ABSENT requires exhaustive:true AND a declared exhaustivenessEvidenceId.
        // No strategy currently carries one, so this downgrades every attempt.
        if (r.state === S.ABSENT && !(spec.exhaustive && spec.exhaustivenessEvidenceId)) {
          out.invariantViolations.push(`${id}.${ax}: strategy ${spec.strategyId} returned VERIFIED_ABSENT without exhaustive+exhaustivenessEvidenceId → downgraded`);
          r = { state: S.UNRESOLVED, reasonCode: 'ABSENCE_NOT_EXHAUSTIVELY_EVIDENCED',
            reason: `INVARIANT: ${spec.strategyId} may not assert VERIFIED_ABSENT without exhaustive:true AND an exhaustivenessEvidenceId naming a closed-world source set (original reason: ${r.reason})` };
        }
        const v = {
          state: r.state, evidencePath: r.evidencePath ?? null,
          evidenceSha256: r.evidencePath ? sha(r.evidencePath) : null,
          symbol: r.symbol ?? null, construct: r.construct ?? null, matchRange: r.matchRange ?? null,
          axisSupported: ax, strategyId: spec.strategyId, strategyVersion: a.strategyVersion,
          exhaustive: !!spec.exhaustive, exhaustivenessEvidenceId: spec.exhaustivenessEvidenceId ?? null,
          evidenceDependency: 'INDEPENDENT', reasonCode: r.reasonCode ?? null, reason: r.reason,
          limitations: a.limitations,
        };
        // C4: two axes resting on the same path+symbol+match are ONE observation wearing two labels.
        if (v.evidencePath) {
          const fp = `${v.evidencePath}::${v.symbol}::${JSON.stringify(v.matchRange)}`;
          if (fingerprints.has(fp)) {
            v.evidenceDependency = 'SHARED';
            v.sharedWith = fingerprints.get(fp);
            rec.axes[fingerprints.get(fp)].evidenceDependency = 'SHARED';
            (rec.axes[fingerprints.get(fp)].sharedWith ??= []);
            rec.axes[fingerprints.get(fp)].sharedWith = [].concat(rec.axes[fingerprints.get(fp)].sharedWith || [], ax);
          } else fingerprints.set(fp, ax);
        }
        rec.axes[ax] = v;
      }
      out.perId[id] = rec;
    }
  }
  for (const [f, t] of Object.entries(sources)) if (t !== null) out.sourceManifest[f] = sha(f);
  out.missingSources = [...new Set(missingSources)].sort();
  return out;
}

if (process.argv[1]?.endsWith('observe-code.mjs')) {
  const o = observe();
  writeFileSync(join(root, 'PORTFOLIO_CODE_OBSERVATIONS.json'), JSON.stringify(o, null, 2) + '\n', 'utf8');
  const ids = Object.keys(o.perId);
  console.log(`CODE OBSERVATIONS: ${ids.length} ids / ${Object.keys(o.blocks).length} blocks / ${Object.keys(o.sourceManifest).length} hashed sources / ${o.missingSources.length} missing`);
  const tally = {};
  for (const r of Object.values(o.perId)) for (const [ax, v] of Object.entries(r.axes)) { tally[ax] ??= {}; tally[ax][v.state] = (tally[ax][v.state] || 0) + 1; }
  for (const [ax, t] of Object.entries(tally)) console.log(`  ${ax.padEnd(18)} ${JSON.stringify(t)}`);
  const shared = Object.values(o.perId).flatMap((r) => Object.values(r.axes)).filter((v) => v.evidenceDependency === 'SHARED').length;
  console.log(`  evidence records marked SHARED (not independent): ${shared}`);
  console.log(`  invariant downgrades: ${o.invariantViolations.length} | VERIFIED_ABSENT allowed strategies: ${o.verifiedAbsentAllowedStrategies.length}`);
}
