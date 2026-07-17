package com.mindos.app.features.mindboost.storage

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One legacy-key SHAPE within a namespace: [scanPrefix] bounds the [SecureKeyValueStore.keys] scan (always a
 * literal superset of anything [regex] can match); [regex] is the anchored, EXACT shape of a legacy (subject-
 * less, pre-6R3) key. A subject-scoped key always has one MORE colon than its legacy counterpart, so it can
 * never satisfy an anchored legacy [regex] — the scan can never sweep a real, subject-scoped row.
 */
class LegacyKeyPattern(val scanPrefix: String, val regex: Regex)

/**
 * A namespace's full legacy-purge contract: every [patterns] shape to sweep, plus the reserved [markerKey] /
 * [markerVersion] that record completion so an already-purged namespace is never re-scanned.
 */
class LegacyPurgeSpec(val markerKey: String, val markerVersion: Int, val patterns: List<LegacyKeyPattern>)

/**
 * SYSTEM-1 slice 6-R3 (CEO closure contract, §18 C1) — the two canonical legacy-purge specs, one per namespace
 * that shipped keys WITHOUT subject-scoping before later gaining it. Both share one reserved marker key literal
 * because each spec is only ever checked against its OWN [SecureKeyValueStore] instance — a distinct physical
 * namespace (`mindboost_sessions` vs `mindboost_session_runs`) — so there is no cross-namespace collision.
 *
 * A legacy key always has EXACTLY the colon count of the CURRENT format minus one (the subject segment plus its
 * delimiter, inserted later): the fixed literal prefix already shipped is unchanged; only the subject segment is
 * new. Verified against the current key builders:
 * [com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore] (`session_record:<subject>:<id>`)
 * and [com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore]
 * (`crun:<subject>:<runId>`, `crun_idx_recent:<subject>`, `crun_idx_block:<subject>:<blockId>`).
 */
object MindBoostLegacyPurgeSpecs {
    /** Reserved for BOTH specs below — safe: each is only ever read against its own distinct physical store. */
    private const val MARKER_KEY = "legacy_purge_marker:v1"
    private const val MARKER_VERSION = 1

    /** `mindboost_sessions`: the legacy shape was `session_record:<sessionId>` (one colon; current has two). */
    val Sessions = LegacyPurgeSpec(
        markerKey = MARKER_KEY,
        markerVersion = MARKER_VERSION,
        patterns = listOf(
            LegacyKeyPattern(scanPrefix = "session_record:", regex = Regex("^session_record:[^:]+$")),
        ),
    )

    /**
     * `mindboost_session_runs`: the legacy primary/block-index shapes had one colon (current: two); the legacy
     * recent-index was a single GLOBAL key with NO colon at all (current: one, `crun_idx_recent:<subject>`).
     */
    val SessionRuns = LegacyPurgeSpec(
        markerKey = MARKER_KEY,
        markerVersion = MARKER_VERSION,
        patterns = listOf(
            LegacyKeyPattern(scanPrefix = "crun:", regex = Regex("^crun:[^:]+$")),
            LegacyKeyPattern(scanPrefix = "crun_idx_recent", regex = Regex("^crun_idx_recent$")),
            LegacyKeyPattern(scanPrefix = "crun_idx_block:", regex = Regex("^crun_idx_block:[^:]+$")),
        ),
    )
}

/**
 * Mechanical, per-instance legacy-key purge gate (SYSTEM-1 slice 6-R3, CEO closure contract §18 C1). Call
 * [ensurePurged] as the FIRST statement of every read/list/delete method of a subject-scoped store — see
 * [com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore] and
 * [com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore] — so no reader,
 * lister, or the orphan-recovery/index-reconcile scan built on top of them can EVER observe or destroy an old
 * subject-less row before it is swept: a MECHANICAL guarantee (every call-site is gated by construction), not a
 * convention a future caller must remember to apply. Write methods (`upsert`/`save`) are deliberately NOT gated:
 * they only ever compute and touch an EXACT subject-scoped key (never a prefix scan), so they cannot leak or
 * destroy a legacy row either way — gating them would add pure overhead with no isolation benefit.
 *
 * FAIL-CLOSED / crash-safe: the marker is written ONLY after every matched key across every
 * [LegacyPurgeSpec.patterns] has been removed. If the process dies mid-purge — or the backend genuinely faults
 * for any other reason — the marker stays absent, [completed] stays `false`, and the NEXT call (this instance,
 * or a freshly-constructed one over the SAME namespace) resumes from a clean scan; removing an already-removed
 * key is a no-op, so a partial retry can never corrupt or double-purge anything. A purge failure is swallowed
 * here (never propagated) so it can never change the throwing contract of whichever store method happens to be
 * the first one called after an upgrade.
 *
 * Cheap after the first success: [completed] short-circuits every later call on THIS instance for free (no
 * I/O). A brand-new instance constructed over an ALREADY-purged namespace (an account switch, or the next app
 * launch) still costs only one [SecureKeyValueStore.read] of the marker, never a re-scan — repeated sign-in /
 * sign-out never re-triggers the sweep once a namespace is clean.
 *
 * QUARANTINE: [SecureKeyValueStore.keys] never returns an already-quarantined key — both
 * [com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore] and
 * [com.mindos.app.features.mindboost.focus.data.local.IndexedSecureKeyValueStore] remove a quarantined key from
 * BOTH the backend and the enumerable index the instant [SecureKeyValueStore.quarantine] runs — so a legacy row
 * already quarantined by an earlier failed read is already gone before this scan ever runs; there is nothing
 * left for the purge to do about it.
 */
class LegacyNamespacePurge(private val spec: LegacyPurgeSpec) {
    private val mutex = Mutex()
    private var completed = false

    /** Idempotent and concurrency-safe. Never throws — a purge failure just leaves [completed] `false` to retry. */
    suspend fun ensurePurged(store: SecureKeyValueStore) {
        if (completed) return
        mutex.withLock {
            if (!completed) completed = attemptPurge(store)
        }
    }

    @Suppress("SwallowedException")
    private suspend fun attemptPurge(store: SecureKeyValueStore): Boolean = try {
        if (!markerAlreadyWritten(store)) purgeLegacyKeysAndWriteMarker(store)
        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        false
    }

    /** An unreadable / corrupt / absent marker is treated as "not yet purged" — never trusted as a false success. */
    private suspend fun markerAlreadyWritten(store: SecureKeyValueStore): Boolean =
        when (val result = store.read(spec.markerKey)) {
            is SecureReadResult.Value -> (result.value.toIntOrNull() ?: MARKER_UNREADABLE) >= spec.markerVersion
            SecureReadResult.Missing, is SecureReadResult.Failure -> false
        }

    /**
     * The marker's persisted value is JUST the plain decimal version (e.g. `"1"`) — no JSON, no field names, so
     * it can never accidentally leak into a consumer's or a test's raw-blob substring check.
     */
    private suspend fun purgeLegacyKeysAndWriteMarker(store: SecureKeyValueStore) {
        spec.patterns.forEach { pattern ->
            store.keys(pattern.scanPrefix).filter { pattern.regex.matches(it) }.forEach { key -> store.remove(key) }
        }
        store.put(spec.markerKey, spec.markerVersion.toString())
    }

    private companion object {
        const val MARKER_UNREADABLE = Int.MIN_VALUE
    }
}
