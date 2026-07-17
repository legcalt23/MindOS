#!/usr/bin/env node
// R3-A3 §5 — snapshot-bound, atomically published reports.
//
// The defect this exists to kill: `writeFileSync(canonicalPath, ...)` at the end of a run. It looks safe because an
// *exception* is caught and still writes FREEZE_FAILED. It is not safe against a HARD kill (SIGKILL, OOM, power),
// which leaves the PREVIOUS green report fully intact and byte-perfect — indistinguishable from a report this run
// produced. Nothing on disk says "a newer run started and died". A reader then attributes a green to an execution
// that never finished.
//
// Three properties fix it, and all three are needed:
//   1. START MARKER  — written before the first check. Its presence with no matching completed report means the run
//                      died. Absence of evidence is made into evidence.
//   2. ATOMIC RENAME — the canonical path only ever contains a fully-written file. A kill mid-write leaves the temp,
//                      never a truncated canonical. rename() over an existing file is atomic on NTFS and POSIX.
//   3. SNAPSHOT BIND — the report names the bytes it describes. A green from a different snapshot is REJECTED by the
//                      consumer rather than silently accepted, so a report cannot outlive the tree it audited.
import { createHash, randomUUID } from 'node:crypto';
import { writeFileSync, readFileSync, existsSync, rmSync, renameSync, openSync, fsyncSync, closeSync } from 'node:fs';

export const REPORT_SCHEMA_VERSION = '3.0.0';
export const REPORT_STATUSES = Object.freeze(['started', 'completed', 'failed']);

const markerPath = (canonical) => `${canonical}.STARTED.json`;
const tempPath = (canonical, runId) => `${canonical}.${runId}.tmp`;

/** Read the frozen snapshot id. A run that cannot name its input has nothing to bind a report to. */
export function currentSnapshotId(repoRoot) {
  const p = `${repoRoot}/R3A_FINAL_SNAPSHOT.json`;
  if (!existsSync(p)) return null;
  try { return JSON.parse(readFileSync(p, 'utf8')).snapshotId ?? null; } catch { return null; }
}

/**
 * Open a run. Writes the start marker BEFORE any check executes, so an interrupted run is detectable.
 * Returns { runId, publish(payload, {verdict}), fail(err) }.
 */
export function beginRun(canonicalPath, { snapshotId, tool }) {
  const runId = randomUUID();
  const startedAt = new Date().toISOString();
  const marker = { schemaVersion: REPORT_SCHEMA_VERSION, status: 'started', runId, snapshotId, tool, startedAt };
  writeFileSync(markerPath(canonicalPath), JSON.stringify(marker, null, 2) + '\n', 'utf8');

  // Cleanup failures are recorded, never substituted for the primary result. A tree we could not tidy is a lesser
  // fact than what the run actually found; conflating them is how a real failure gets reported as a cleanup error.
  const cleanupErrors = [];

  const publish = (payload, { status }) => {
    if (!REPORT_STATUSES.includes(status)) throw new Error(`unknown report status "${status}"`);
    const full = {
      ...payload,
      schemaVersion: REPORT_SCHEMA_VERSION,
      status, runId, snapshotId, tool, startedAt,
      completedAt: new Date().toISOString(),
      cleanupErrors,
    };
    const body = JSON.stringify(full, null, 2) + '\n';
    const tmp = tempPath(canonicalPath, runId);
    // Write → fsync → rename. Without the fsync the rename can land while the data is still in the page cache; the
    // canonical name would then point at a file the OS has not durably written.
    writeFileSync(tmp, body, 'utf8');
    const fd = openSync(tmp, 'r+');
    try { fsyncSync(fd); } finally { closeSync(fd); }
    renameSync(tmp, canonicalPath);
    // The marker dies LAST. If we crash between rename and this unlink, the consumer sees a completed report whose
    // runId matches the marker — reconcilable. The reverse order would leave a window with no evidence at all.
    try { rmSync(markerPath(canonicalPath), { force: true }); } catch (e) { cleanupErrors.push(String(e?.message ?? e)); }
    return full;
  };

  return { runId, snapshotId, startedAt, cleanupErrors, publish, noteCleanupError: (e) => cleanupErrors.push(String(e?.message ?? e)) };
}

/**
 * Consumer side. A report is only evidence if it is complete, parseable, and describes the snapshot being asked
 * about. Everything else is INVALID with a reason — never a silent pass, never a fabricated failure.
 */
export function readReport(canonicalPath, { expectedSnapshotId } = {}) {
  if (existsSync(markerPath(canonicalPath))) {
    let m = null;
    try { m = JSON.parse(readFileSync(markerPath(canonicalPath), 'utf8')); } catch { /* unparseable marker is still a marker */ }
    return { valid: false, reason: 'RUN_INCOMPLETE', detail: `start marker present — a run began${m?.runId ? ` (${m.runId})` : ''} and did not publish`, report: null };
  }
  if (!existsSync(canonicalPath)) return { valid: false, reason: 'ABSENT', detail: 'no report at the canonical path', report: null };
  let r;
  try { r = JSON.parse(readFileSync(canonicalPath, 'utf8')); }
  catch (e) { return { valid: false, reason: 'UNPARSEABLE', detail: `truncated or corrupt JSON: ${e.message}`, report: null }; }
  if (r.status !== 'completed' && r.status !== 'failed') return { valid: false, reason: 'NOT_COMPLETED', detail: `status=${JSON.stringify(r.status)} — not a finished run`, report: r };
  if (!r.runId) return { valid: false, reason: 'NO_RUN_ID', detail: 'report does not identify its run', report: r };
  if (expectedSnapshotId !== undefined && r.snapshotId !== expectedSnapshotId) {
    return { valid: false, reason: 'SNAPSHOT_MISMATCH', detail: `report describes snapshot ${String(r.snapshotId).slice(0, 16)}…, asked about ${String(expectedSnapshotId).slice(0, 16)}…`, report: r };
  }
  return { valid: true, reason: null, detail: null, report: r };
}

export const _internal = { markerPath, tempPath, sha: (b) => createHash('sha256').update(b).digest('hex') };
