#!/usr/bin/env node
// Lightweight validator for MINDBOOST_AUDIT_LEDGER.md.
// Checks: unique IDs, valid states, CLOSED⇒evidence+gate, SUPERSEDED⇒destination,
// self-excluding SHA-256 present & matching, no obvious secrets, a checkpoint exists.
// No dependencies. Run: node scripts/validate-audit-ledger.mjs
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const path = join(root, 'MINDBOOST_AUDIT_LEDGER.md');
const text = readFileSync(path, 'utf8');
const errs = [];
const ok = (c, m) => { if (!c) errs.push(m); };

const STATES = ['OPEN', 'IN_PROGRESS', 'MITIGATED', 'BLOCKED', 'CLOSED', 'REOPENED', 'SUPERSEDED'];

// --- self-excluding hash ---
const hashLine = text.split('\n').find((l) => l.startsWith('Canonical SHA-256'));
ok(hashLine, 'missing "Canonical SHA-256" line in §0');
const stamped = hashLine && (hashLine.match(/[0-9a-f]{64}/) || [])[0];
ok(stamped, 'no 64-hex hash stamped in §0');
const body = text.split('\n').filter((l) => !l.startsWith('Canonical SHA-256')).join('\n');
const actual = createHash('sha256').update(body, 'utf8').digest('hex');
ok(stamped === actual, `hash mismatch: stamped=${stamped} actual=${actual}`);

// --- findings register rows (§2): first cell = ID in backticks ---
const rows = text.split('\n').filter((l) => /^\|\s*`[^`]+`\s*\|/.test(l));
const ids = rows.map((l) => l.match(/^\|\s*`([^`]+)`/)[1]);
const regRows = rows.filter((l) => l.split('|').length >= 12); // register rows have 12+ cells
const seen = new Set();
const regState = new Map();
for (const l of regRows) {
  const cells = l.split('|').map((c) => c.trim());
  const id = cells[1].replace(/`/g, '');
  ok(!seen.has(id), `duplicate finding ID: ${id}`);
  seen.add(id);
  const state = cells[4];
  regState.set(id, state);
  ok(STATES.includes(state), `invalid state "${state}" for ${id}`);
  if (state === 'CLOSED') {
    ok(cells[6] && cells[6] !== '—', `CLOSED ${id} has no closure evidence`);
    ok(cells[8] && cells[8] !== '—', `CLOSED ${id} has no closure gate`);
  }
  if (state === 'SUPERSEDED') {
    ok(/->|→|→|see §6/.test(l), `SUPERSEDED ${id} has no destination`);
  }
}
ok(seen.size >= 1, 'no findings in register');

// --- register ↔ event contradiction check ---
// The last event that assigns a CLEAN single-state token to a finding must agree with its register state
// (catches: a finding CLOSED in the register but REOPENED by a later event, and vice-versa).
const eventsSection = (text.split(/^## 3\./m)[1] || '').split(/^## 4\./m)[0] || '';
const lastEventState = new Map();
for (const l of eventsSection.split('\n')) {
  if (!/^\|\s*\d{4}-\d{2}-\d{2}/.test(l)) continue; // event rows start with a timestamp
  const cells = l.split('|').map((c) => c.trim());
  const newState = cells[5]; // Timestamp|Task|ID|Prev|New|...
  if (!STATES.includes(newState)) continue; // skip multi-id "seeded (...)" / "—" cells
  const idCell = cells[3] || '';
  for (const m of idCell.matchAll(/`([^`]+)`/g)) lastEventState.set(m[1], newState);
}
for (const [id, evState] of lastEventState) {
  if (!regState.has(id)) continue;
  ok(regState.get(id) === evState, `register/event contradiction: ${id} is "${regState.get(id)}" in the register but the last materialized event set "${evState}"`);
}

// --- SUPERSEDED rows in §6 map must name a destination ---
// (structural: every "superseded" alias-map row should have a non-empty canonical ID cell)

// --- a checkpoint exists (§4) ---
ok(/TASK-\d{4}-\d{2}-\d{2}/.test(text), 'no task checkpoint (TASK-YYYY-MM-DD-...) found');

// --- no obvious secrets ---
ok(!/eyJ[A-Za-z0-9_-]{10,}\./.test(text), 'possible JWT token in ledger');
ok(!/-----BEGIN [A-Z ]+PRIVATE KEY-----/.test(text), 'private key block in ledger');

if (errs.length) {
  console.error('LEDGER INVALID:\n' + errs.map((e) => '  - ' + e).join('\n'));
  process.exit(1);
}
console.log(`LEDGER OK — ${seen.size} findings, hash matches (${actual.slice(0, 12)}…)`);
