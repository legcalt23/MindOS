#!/usr/bin/env node
// R3-A3 §5 — the twelve mandatory stale-green fixtures.
//
// The single property under test, stated once: NO GREEN REPORT MAY EVER BE READ AS BELONGING TO A RUN THAT DID NOT
// PRODUCE IT. Every fixture below preseeds a convincing green report and then arranges a failure. A fixture passes
// only if the consumer refuses that green — not if the code merely "handled" the error.
import { mkdtempSync, writeFileSync, readFileSync, existsSync, rmSync, mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { beginRun, readReport, _internal } from './atomic-report.mjs';

const SNAP = 'a'.repeat(64);
const OTHER_SNAP = 'b'.repeat(64);
const results = [];
const t = (id, name, fn) => {
  try { const r = fn(); results.push({ id, name, pass: r.pass, detail: r.detail }); }
  catch (e) { results.push({ id, name, pass: false, detail: `THREW: ${e.message}` }); }
};

const ws = () => { const d = mkdtempSync(join(tmpdir(), 'arep-')); return { d, canonical: join(d, 'REPORT.json') }; };

/** A previous run's green report, fully valid, correct snapshot — the most dangerous kind. */
const preseedGreen = (canonical, snapshotId = SNAP) => writeFileSync(canonical, JSON.stringify({
  artifact: 'preseeded', schemaVersion: '3.0.0', status: 'completed', runId: 'OLD-RUN-0000',
  snapshotId, tool: 'phase8-freeze', verdict: 'FREEZE_VERIFIED', passed: 12, of: 12,
  startedAt: '2020-01-01T00:00:00.000Z', completedAt: '2020-01-01T00:01:00.000Z', cleanupErrors: [],
}, null, 2) + '\n', 'utf8');

// 1-3 — preseeded green + a failure at check 1 / 7 / 12. The run publishes `failed`; the old green must not survive.
for (const [id, at] of [['AR-01', 1], ['AR-02', 7], ['AR-03', 12]]) {
  t(id, `preseeded green + failure at check ${at} → green replaced by failed`, () => {
    const { canonical } = ws();
    preseedGreen(canonical);
    const run = beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' });
    run.publish({ verdict: 'FREEZE_FAILED', passed: at - 1, of: 12, failedAt: at }, { status: 'failed' });
    const r = readReport(canonical, { expectedSnapshotId: SNAP });
    const ok = r.valid && r.report.verdict === 'FREEZE_FAILED' && r.report.runId !== 'OLD-RUN-0000';
    return { pass: ok, detail: ok ? `verdict=${r.report.verdict}, runId rotated` : `LEAKED: ${JSON.stringify(r.report?.verdict)} runId=${r.report?.runId}` };
  });
}

// 4 — primary failure AND a cleanup failure. Both recorded; the cleanup must not become the headline.
t('AR-04', 'primary failure + cleanup failure → both recorded, primary preserved', () => {
  const { canonical } = ws();
  const run = beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' });
  run.noteCleanupError(new Error('seal.verify() exploded during dispose'));
  const pub = run.publish({ verdict: 'FREEZE_FAILED', passed: 3, of: 12, fatal: { message: 'primary: check 4 blew up' } }, { status: 'failed' });
  const ok = pub.fatal.message.includes('primary') && pub.cleanupErrors.length === 1 && pub.cleanupErrors[0].includes('seal.verify');
  return { pass: ok, detail: ok ? 'primary=fatal, cleanup listed separately' : JSON.stringify({ fatal: pub.fatal, cleanup: pub.cleanupErrors }) };
});

// 5 — ONLY cleanup failed. The run genuinely passed; a tidy-up error must not manufacture a failure.
t('AR-05', 'cleanup-only failure → run still completed, error still visible', () => {
  const { canonical } = ws();
  const run = beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' });
  run.noteCleanupError(new Error('temp dir busy'));
  run.publish({ verdict: 'FREEZE_VERIFIED', passed: 12, of: 12 }, { status: 'completed' });
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = r.valid && r.report.verdict === 'FREEZE_VERIFIED' && r.report.cleanupErrors.length === 1;
  return { pass: ok, detail: ok ? 'completed + cleanupErrors[1]' : JSON.stringify(r) };
});

// 6 — the report write itself fails. The canonical path must not be left holding the old green as if it were ours.
t('AR-06', 'report write fails → throws, marker survives, consumer sees RUN_INCOMPLETE', () => {
  const { d, canonical } = ws();
  preseedGreen(canonical);
  const run = beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' });
  // Make the temp path unwritable by turning it into a directory — writeFileSync then fails with EISDIR.
  mkdirSync(_internal.tempPath(canonical, run.runId));
  let threw = false;
  try { run.publish({ verdict: 'FREEZE_VERIFIED', passed: 12, of: 12 }, { status: 'completed' }); } catch { threw = true; }
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = threw && !r.valid && r.reason === 'RUN_INCOMPLETE';
  rmSync(d, { recursive: true, force: true });
  return { pass: ok, detail: ok ? 'write threw; marker left; old green rejected as RUN_INCOMPLETE' : `threw=${threw} reason=${r.reason}` };
});

// 7 — rename fails. Same requirement: the stale green must not be accepted.
t('AR-07', 'rename fails → old green rejected, not published', () => {
  const { d, canonical } = ws();
  preseedGreen(canonical);
  const run = beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' });
  // A directory at the canonical path makes rename() fail (EPERM/ENOTDIR/EISDIR) after a successful temp write.
  rmSync(canonical); mkdirSync(canonical); writeFileSync(join(canonical, 'blocker'), 'x');
  let threw = false;
  try { run.publish({ verdict: 'FREEZE_VERIFIED', passed: 12, of: 12 }, { status: 'completed' }); } catch { threw = true; }
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = threw && !r.valid;
  rmSync(d, { recursive: true, force: true });
  return { pass: ok, detail: ok ? `rename threw; consumer rejects (${r.reason})` : `threw=${threw} valid=${r.valid}` };
});

// 8 — THE decisive one. Process killed before the rename. Nothing catches this; only the marker survives to say so.
t('AR-08', 'killed before rename → preseeded green REJECTED via start marker', () => {
  const { canonical } = ws();
  preseedGreen(canonical);
  beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' }); // marker written; simulate SIGKILL — never publish
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = !r.valid && r.reason === 'RUN_INCOMPLETE';
  return { pass: ok, detail: ok ? 'a byte-perfect green with the RIGHT snapshot is still rejected — the marker outed it' : `LEAKED green: valid=${r.valid} reason=${r.reason}` };
});

// 9 — a green describing different bytes.
t('AR-09', 'report from a different snapshot → SNAPSHOT_MISMATCH', () => {
  const { canonical } = ws();
  preseedGreen(canonical, OTHER_SNAP);
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = !r.valid && r.reason === 'SNAPSHOT_MISMATCH';
  return { pass: ok, detail: ok ? 'rejected: report describes another tree' : `reason=${r.reason}` };
});

// 10 — truncated JSON is not "no report", it is a corrupt one.
t('AR-10', 'truncated JSON → UNPARSEABLE, never a silent pass', () => {
  const { canonical } = ws();
  preseedGreen(canonical);
  const body = readFileSync(canonical, 'utf8');
  writeFileSync(canonical, body.slice(0, Math.floor(body.length / 2)), 'utf8');
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = !r.valid && r.reason === 'UNPARSEABLE';
  return { pass: ok, detail: ok ? 'rejected as corrupt' : `reason=${r.reason}` };
});

// 11 — a report with no completion status.
t('AR-11', 'report without completed status → NOT_COMPLETED', () => {
  const { canonical } = ws();
  writeFileSync(canonical, JSON.stringify({ schemaVersion: '3.0.0', status: 'started', runId: 'X', snapshotId: SNAP, verdict: 'FREEZE_VERIFIED' }) + '\n');
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = !r.valid && r.reason === 'NOT_COMPLETED';
  return { pass: ok, detail: ok ? 'a green verdict inside an unfinished report is not evidence' : `reason=${r.reason}` };
});

// 12 — two consecutive runs. The second must fully supersede the first, marker cleared, ids distinct.
t('AR-12', 'two consecutive runs → second supersedes, no marker left, runIds distinct', () => {
  const { canonical } = ws();
  const r1 = beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' });
  r1.publish({ verdict: 'FREEZE_FAILED', passed: 5, of: 12 }, { status: 'failed' });
  const r2 = beginRun(canonical, { snapshotId: SNAP, tool: 'phase8-freeze' });
  r2.publish({ verdict: 'FREEZE_VERIFIED', passed: 12, of: 12 }, { status: 'completed' });
  const r = readReport(canonical, { expectedSnapshotId: SNAP });
  const ok = r.valid && r.report.runId === r2.runId && r1.runId !== r2.runId
    && !existsSync(_internal.markerPath(canonical)) && r.report.verdict === 'FREEZE_VERIFIED';
  return { pass: ok, detail: ok ? `run2 ${r2.runId.slice(0, 8)} superseded run1 ${r1.runId.slice(0, 8)}; marker cleared` : JSON.stringify({ got: r.report?.runId, want: r2.runId, marker: existsSync(_internal.markerPath(canonical)) }) };
});

const passed = results.filter((r) => r.pass).length;
for (const r of results) console.log(`  ${r.pass ? 'PASS' : 'FAIL'}  ${r.id}  ${r.name}\n         ${r.detail}`);
console.log(`\nATOMIC REPORT FIXTURES: ${passed}/${results.length}`);
process.exitCode = passed === results.length && results.length === 12 ? 0 : 1;
