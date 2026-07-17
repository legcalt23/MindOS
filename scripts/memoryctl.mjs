#!/usr/bin/env node
// memoryctl — local, dependency-free validator for the MindOS memory system (ADR-MEM-001).
// Enforces the boundaries the architecture depends on. Grows as the canon lands.
// Usage: node scripts/memoryctl.mjs [status|ledger|graph-stale|graph-scope|context-pack-dup]
import { readFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const R = (p) => join(root, p);
const read = (p) => (existsSync(R(p)) ? readFileSync(R(p), 'utf8') : null);
const git = (args) => { try { return execFileSync('git', args, { cwd: root }).toString().trim(); } catch { return null; } };

const hard = []; // failures (exit 1)
const soft = []; // warnings (exit 0)

// 1) LEDGER — delegate to the dedicated validator (don't duplicate its logic).
function checkLedger() {
  try {
    const out = execFileSync('node', [R('scripts/validate-audit-ledger.mjs')], { cwd: root }).toString().trim();
    return `ledger: OK (${out.replace(/^LEDGER OK — /, '')})`;
  } catch (e) {
    hard.push('ledger: INVALID — run `node scripts/validate-audit-ledger.mjs`');
    return 'ledger: INVALID';
  }
}

// 2) GRAPH-STALE — GRAPH_REPORT "Built from commit" vs HEAD (soft; refresh is manual).
function checkGraphStale() {
  const rep = read('graphify-out/GRAPH_REPORT.md');
  if (!rep) return 'graph: absent (no GRAPH_REPORT.md)';
  const built = (rep.match(/Built from commit:\s*`?([0-9a-f]{6,40})`?/) || [])[1];
  const head = git(['rev-parse', 'HEAD']);
  if (!built || !head) return 'graph: freshness unknown';
  if (!head.startsWith(built) && !built.startsWith(head.slice(0, built.length))) {
    soft.push(`graph: STALE — built ${built}, HEAD ${head.slice(0, 8)} (run \`graphify update .\` on a clean tree)`);
    return `graph: STALE (built ${built} ≠ HEAD ${head.slice(0, 8)})`;
  }
  return `graph: fresh (${built})`;
}

// 3) GRAPH-SCOPE — .graphifyignore MUST exclude the prose authorities (contamination guard).
function checkGraphScope() {
  const ig = read('.graphifyignore') || '';
  const must = ['CLAUDE.md', 'AGENTS.md', 'docs/context_pack/'];
  const missing = must.filter((m) => !ig.split('\n').some((l) => l.trim() === m));
  if (missing.length) {
    soft.push(`graph-scope: .graphifyignore does not exclude prose authorities: ${missing.join(', ')} (MB-MEMORY-0002)`);
    return `graph-scope: CONTAMINATED (missing ${missing.join(', ')})`;
  }
  return 'graph-scope: clean (prose authorities excluded)';
}

// 4) CONTEXT-PACK — docs/context_pack (underscore, v2.0.2) is the SINGLE live canon. The hyphen
//    path docs/context-pack is SUPERSEDED and must never be chosen (MB-ENG-0006 / MB-MEMORY-0004).
function checkContextPackDup() {
  const canon = existsSync(R('docs/context_pack'));
  const superseded = existsSync(R('docs/context-pack'));
  if (superseded) {
    soft.push('context-pack: SUPERSEDED path docs/context-pack (hyphen) present — canon is docs/context_pack (underscore, v2.0.2); do not use the hyphen copy (MB-ENG-0006)');
  }
  if (!canon) {
    soft.push('context-pack: canon docs/context_pack (underscore) MISSING');
    return 'context-pack: canon MISSING (expected docs/context_pack)';
  }
  return `context-pack: canon = docs/context_pack (underscore)${superseded ? ' [+ stale hyphen present]' : ''}`;
}

const checks = {
  ledger: checkLedger,
  'graph-stale': checkGraphStale,
  'graph-scope': checkGraphScope,
  'context-pack-dup': checkContextPackDup,
};

const cmd = process.argv[2] || 'status';
if (cmd !== 'status' && !checks[cmd]) {
  console.error(`unknown command: ${cmd}\nusage: node scripts/memoryctl.mjs [status|${Object.keys(checks).join('|')}]`);
  process.exit(2);
}

const toRun = cmd === 'status' ? Object.values(checks) : [checks[cmd]];
console.log('memoryctl —', cmd);
for (const fn of toRun) console.log('  •', fn());
if (soft.length) console.log('\nWARNINGS:\n' + soft.map((s) => '  ⚠ ' + s).join('\n'));
if (hard.length) { console.error('\nFAILURES:\n' + hard.map((s) => '  ✗ ' + s).join('\n')); process.exit(1); }
console.log(soft.length ? '\nmemoryctl: OK with warnings' : '\nmemoryctl: OK');
