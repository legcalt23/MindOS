# Legacy-key migration — `mindboost_sessions` + `mindboost_session_runs`

> SYSTEM-1 slice 6-R3, CEO closure contract §18 C1 (2026-07-11). Owner: `data-sync-dev`. Reviewed by:
> `privacy-security` (pending). Companion to
> [`SYSTEM1_SLICE_6R3_REOPEN.md`](./SYSTEM1_SLICE_6R3_REOPEN.md) §12.1/§16/§17/§18.

## 1. Decision (CEO, 2026-07-11 — closed, not re-opened here)

Both `SecureSessionRecordStore` (`mindboost_sessions`) and `SecureCognitiveSessionRunStore`
(`mindboost_session_runs`) shipped their FIRST implementation with physical keys that carried **no subject at
all** (one global row per `sessionId`/`runId`, shared by every locally-authenticated account on the device — the
root cause of 6R3-P0C-01 and its §14 sibling). Both stores were later fixed to require a mandatory `subject`
constructor parameter and subject-scoped keys (`session_record:<subject>:<sessionId>`,
`crun:<subject>:<runId>`, plus subject-scoped indices). The FIX closes the isolation gap for every ROW WRITTEN
FROM NOW ON. It does **not**, by itself, remove any row a pre-fix build may already have written under the old,
subject-less key shape.

The CEO ruled: any surviving pre-fix row is **not unambiguously attributable to any one subject** (that is
precisely the defect being fixed), so it must never be silently kept, never be silently attributed to
"whichever account happens to read it next," and never require an interactive migration step. The correct,
closed decision is a **local, versioned, idempotent, fail-closed migration that DELETES** every row matching the
legacy (subject-less) key shape, in **both** namespaces, unconditionally — there is no "recover the data for its
rightful owner" path, because there is no way to determine who that owner is from the key alone.

## 2. Evidence considered (repo-verified, informs "how conservative to be" — not the "whether")

- `SecureCognitiveSessionRunStore.kt` was `??` (untracked) in the working tree at the time of this fix — it had
  **never been committed** to the repository history.
- The repository has **zero git tags** (`git tag` returns nothing) — no evidence of a tagged release build.
- `docs/release/RELEASE_EVIDENCE.json` records `releaseStatus: "NO_GO"` with every release checklist item
  `pending`, dated 2026-06-19 — the most recent recorded release-readiness snapshot shows the app was **not**
  cleared for external distribution.
- The local Android `versionCode` is `1` (the initial value; no evidence of an incremented, shipped build).

**Conclusion:** there is no repo evidence that `mindboost_session_runs` ever reached a real end user's device
through an official release channel. This is **not** a 100%-certain absence, however — development-signed
installs on a physical device or emulator (sideloaded APKs, internal test builds) are not excludable from the
repository's evidence alone, and `mindboost_sessions` (the OTHER namespace) predates this specific evidence
question and was already live across all 8 modules before its own fix (§16). Per the CEO's instruction ("si no
es demostrable, mismo principio"), the migration purges **both** namespaces with the identical mechanism and
the identical conservative posture, rather than special-casing the runs store as "definitely never shipped."

## 3. What gets purged, and why these exact shapes

A legacy key is recognized **structurally, by its literal shape**, never by attempting to decode its payload
(decoding is unnecessary and would only add a failure mode — a legacy row may even predate the current DTO
schema). The delimiter is the same `:` used to separate the subject in the current format; a legacy key always
has exactly the colon count of today's format **minus** the inserted subject segment and its delimiter:

| Namespace | Legacy shape (purged) | Current shape (preserved) |
|---|---|---|
| `mindboost_sessions` | `session_record:<sessionId>` (one colon) | `session_record:<subject>:<sessionId>` (two colons) |
| `mindboost_session_runs` | `crun:<runId>` (one colon) | `crun:<subject>:<runId>` (two colons) |
| `mindboost_session_runs` | `crun_idx_recent` (**no** colon — a single global key) | `crun_idx_recent:<subject>` (one colon) |
| `mindboost_session_runs` | `crun_idx_block:<blockId>` (one colon) | `crun_idx_block:<subject>:<blockId>` (two colons) |

Each shape is matched with an **anchored** regular expression (`^session_record:[^:]+$`, `^crun:[^:]+$`,
`^crun_idx_recent$`, `^crun_idx_block:[^:]+$`) over the candidate set returned by
`SecureKeyValueStore.keys(literalPrefix)` — the literal prefix is always a superset of what the anchored regex
can match, so the regex is what actually decides "legacy or not," never the prefix scan alone. A subject-scoped
key can **never** satisfy an anchored legacy regex, because it always carries one extra colon the anchor
forbids — this is a structural guarantee, not a best-effort heuristic, and it was verified line-by-line against
the CURRENT key-builder functions in both stores (`SecureSessionRecordStore.prefix()`/`keyFor()`,
`SecureCognitiveSessionRunStore.primaryKey()`/`recentIndexKey()`/`blockIndexKey()`).

Only the schema/index rows above are in scope. Nothing subject-scoped is ever touched, in any namespace, by
this mechanism.

## 4. Mechanism (implementation)

- `com.mindos.app.features.mindboost.storage.LegacyNamespacePurge` (`commonMain`) — a small, per-instance,
  reusable class parameterized by a `LegacyPurgeSpec` (the shapes to sweep + a reserved marker key/version).
  `MindBoostLegacyPurgeSpecs.Sessions` / `.SessionRuns` are the two canonical specs for this migration.
- **Mechanical invocation, not convention:** `SecureSessionRecordStore` and `SecureCognitiveSessionRunStore`
  each own one `LegacyNamespacePurge` instance and call `ensurePurged(store)` as the FIRST statement of every
  **read/list/delete/reconcile** method (`find`/`all`/`delete`/`findEvidence`/`deleteAll` for the sessions
  store; `get`/`listRecent`/`listByBlock`/`deleteAll`/`reconcileIndices` for the runs store). This means the
  purge runs before `SessionRecoveryScanner.recover()` can ever see a legacy row (it only calls `store.all()`),
  and before `reconcileIndices()` can ever fold one into a rebuilt index — by construction, not by a caller
  remembering to invoke a migration step first.
- **Write methods (`upsert`/`save`) are deliberately NOT gated.** They only ever compute and touch one EXACT,
  subject-scoped key (never a prefix scan), so they cannot observe or destroy a legacy row either way; gating
  them would add pure overhead with no isolation benefit.
- **Marker, fail-closed:** a reserved key (`legacy_purge_marker:v1`, identical literal in both namespaces — safe,
  because each spec is only ever checked against its own distinct physical store) records completion. Its
  persisted VALUE is just the plain decimal marker version (e.g. `"1"`) — no JSON, so it can never leak into a
  consumer's or a test's raw-blob substring check. The marker is written **only after** every matched key in
  every pattern has been removed. If the process dies mid-purge (or the backend genuinely faults for any other
  reason), the marker stays absent and the purge is swallowed internally — it never changes the throwing
  contract of whichever store method happened to trigger it first. The **next** call, on this instance or a
  freshly-constructed one over the same namespace, resumes from a clean scan; removing an already-removed key
  is a no-op, so a partial retry can never corrupt or double-purge anything.
- **Cheap after the first success:** an in-memory flag short-circuits every later call on the SAME instance for
  free. A brand-new instance over an ALREADY-purged namespace (an account switch, or the next app launch) still
  costs only one key read (the marker), never a re-scan — repeated sign-in/sign-out never re-triggers the sweep.
- **Quarantine posture (verified, not assumed):** `SecureKeyValueStore.keys()` never returns an
  already-quarantined key — both `InMemorySecureKeyValueStore.quarantine()` and
  `IndexedSecureKeyValueStore.quarantine()` remove the key from **both** the backend and the enumerable index
  the instant `quarantine()` runs. A legacy row that some earlier failed read already quarantined is therefore
  already gone before this scan ever runs; there is nothing left for the purge to do about it.
- **Conservation:** the purge never touches a subject-scoped key, in either namespace, under any circumstance.
  It is never invoked from sign-in or sign-out (only from the store's own read/list/delete/reconcile methods).
  A subject's own `deleteAll()` (the account-deletion primitive) never removes the marker, because the marker's
  key does not match any subject-scoped prefix — verified by a dedicated regression test in both stores' legacy
  suites, and end-to-end across an account switch and both accounts' erasure in
  `CrossAccountLegacyPurgeE2ETest`.

## 5. Upgrade behavior

The migration requires **no explicit user action, no app-startup hook, and no new lifecycle wiring.** It runs
automatically the first time any existing production code path (recovery scan on track entry, a profile/history
read, an account deletion) invokes a gated method on a store constructed over the real, persisted
`mindboost_sessions` / `mindboost_session_runs` namespace after this change ships. Because every production
construction site already threads the authenticated `userId` as `subject` (traced in §16/§17 of the reopen
doc), no additional wiring was needed to bind the purge to real installs.

## 6. Rollback — stated honestly

The purge is **irreversible by design.** A legacy row's whole reason for being removed is that it cannot be
attributed to a subject; there is no "undo" that could restore it to the correct owner, because no such
attribution exists to restore. There is therefore no code-level rollback of the deletion itself.

What CAN be "rolled back" is the **marker's own semantics**: if a legacy row were somehow still needed for
forensic purposes (e.g., a pre-purge encrypted backup of the raw namespace file taken out-of-band before this
change ships), restoring that backup and re-installing/downgrading the app to a pre-6R3 build would put the
legacy rows back — at which point the marker is simply not yet present, and the very next launch of a fixed
build purges them again (idempotent by construction, not a one-time destructive action that could misfire on a
retry). There is no in-app "undo this migration" affordance, and none is warranted: the data being removed was
never safely attributable in the first place.

## 7. Idempotency / crash-safety — proof, not assertion

Covered by automated tests (`LegacyNamespacePurgeTest`, `SecureSessionRecordStoreLegacyPurgeTest`,
`SecureCognitiveSessionRunStoreLegacyPurgeTest`, `CrossAccountLegacyPurgeE2ETest`, all in `desktopTest`):

- A purge that fails partway through (a `remove()` throws on the first attempt) leaves the marker **absent**;
  a subsequent call (simulating the next process/app launch) finds the same un-removed legacy keys and
  finishes removing them, then writes the marker.
- A re-run over an already-purged namespace — whether on the same instance (free, zero I/O) or a brand-new
  instance (one cheap marker read, no re-scan) — never re-triggers the sweep and never mutates the namespace
  further.
- Concurrent callers on the same instance are serialized by a `Mutex`; exactly one of them performs the actual
  scan-and-purge, proven with 20 concurrently-launched coroutines against a call-counting fake store.
  Repeated sign-in/sign-out (a fresh store instance per session, same physical backing store) is a no-op after
  the first successful purge.
- A corrupted/garbled marker value is treated as "not yet purged" (never trusted as a false success) and safely
  re-sweeps — idempotent, since there is nothing left to remove and the marker is simply rewritten cleanly.

## 8. Implementation files

- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/storage/LegacyNamespacePurge.kt` (new) — the
  generic mechanism + `MindBoostLegacyPurgeSpecs.{Sessions,SessionRuns}`.
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/SecureSessionRecordStore.kt`
  (edited) — gates `find`/`all`/`delete`/`findEvidence`/`deleteAll`.
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/SecureCognitiveSessionRunStore.kt`
  (edited) — gates `get`/`listRecent`/`listByBlock`/`deleteAll`/`reconcileIndices`.
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/storage/LegacyNamespacePurgeTest.kt` (new).
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/SecureSessionRecordStoreLegacyPurgeTest.kt`
  (new).
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/SecureCognitiveSessionRunStoreLegacyPurgeTest.kt`
  (new).
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/CrossAccountLegacyPurgeE2ETest.kt`
  (new — the §18 C5 single-process, real-factory, physical-key-level isolation test).
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/CognitiveSessionRunStoreTest.kt`
  (edited — one pre-existing assertion updated to account for the marker surviving `deleteAll`, see §4).
