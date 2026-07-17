// This store is a TOTAL, best-effort persistence surface, deliberately decomposed into many small,
// single-purpose top-level helpers (so the class stays lean and each step is unit-testable). Every
// backend-touching helper maps a broad storage failure to a TYPED result and never leaks the Throwable
// (telemetry minimization), rethrowing only CancellationException — so the file-level function count and the
// swallowed catches are intentional (same pattern as SecureSessionRecorder).
@file:Suppress("TooManyFunctions", "SwallowedException")

package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.GuardedRead
import com.mindos.app.features.mindboost.core.session.GuardedWrite
import com.mindos.app.features.mindboost.core.session.SubjectRevocationAuthority
import com.mindos.app.features.mindboost.core.session.SubjectRevocationState
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.storage.LegacyNamespacePurge
import com.mindos.app.features.mindboost.storage.MindBoostLegacyPurgeSpecs
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json

private const val PRIMARY_PREFIX = "crun:"
private const val RECENT_INDEX_PREFIX = "crun_idx_recent:"
private const val BLOCK_INDEX_PREFIX = "crun_idx_block:"

/** Delimiter separating the subject from the rest of a key. Reserved: [validateSubject] rejects it in [subject]. */
private const val SUBJECT_DELIMITER = ':'
private const val MAX_LIST_LIMIT = 500

/**
 * SYSTEM-1 slice 6-R3 (P0 fix) · Every physical key this store touches is scoped by [subject] so two different
 * subjects sharing the SAME underlying [SecureKeyValueStore] namespace (today's reality: `mindboost_session_runs`
 * is one device-wide physical file/backend, NOT a per-account one — see [CognitiveSessionRunStore]) can never
 * read, list, or delete one another's rows. [subject] must not contain [SUBJECT_DELIMITER] (the key-prefix
 * delimiter) — otherwise one subject's prefix could be a prefix of a DIFFERENT subject's keys (e.g. subject
 * `"abc"` vs `"abc:def"`), reopening the isolation gap this fix closes.
 */
private fun validateSubject(subject: String) {
    require(subject.isNotBlank()) { "subject must not be blank (a pseudonymous per-account scope label)." }
    require(SUBJECT_DELIMITER !in subject) {
        "subject must not contain '$SUBJECT_DELIMITER' (the key-prefix delimiter)."
    }
}

private fun primaryPrefix(subject: String): String = "$PRIMARY_PREFIX$subject$SUBJECT_DELIMITER"

private fun primaryKey(subject: String, runId: String): String = "${primaryPrefix(subject)}$runId"

private fun recentIndexKey(subject: String): String = "$RECENT_INDEX_PREFIX$subject"

private fun blockIndexPrefix(subject: String): String = "$BLOCK_INDEX_PREFIX$subject$SUBJECT_DELIMITER"

private fun blockIndexKey(subject: String, blockId: ScoreModuleId): String =
    "${blockIndexPrefix(subject)}${blockId.name}"

/** How a save's existing row classifies against the incoming run. */
private enum class SaveClassification { NEW, IDENTICAL, CONFLICT, UNREADABLE, BACKEND_ERROR }

/**
 * SYSTEM-1 slice 2 (subject-scoped since slice 6-R3) · Durable [CognitiveSessionRunStore] over a
 * [SecureKeyValueStore] (the SAME instance used by production, tests, and partial-failure fakes). Primary rows
 * are the source of truth (`crun:{subject}:{runId}`); the `crun_idx_recent:{subject}` and
 * `crun_idx_block:{subject}:{blockId}` indices are best-effort projections written AFTER the primary and rebuilt
 * by [reconcileIndices]. Holds no sync state, no userId in the serialized payload — [subject] is a mandatory,
 * first-class constructor parameter (never a naming convention the caller must remember to apply per-call), so a
 * `SecureCognitiveSessionRunStore` instance can only ever touch ITS OWN subject's keys. The heavy lifting is in
 * top-level helpers so the class stays well under detekt's per-class function threshold.
 *
 * SECURITY NOTE (6R3-P0C-01): [store] itself is namespace-scoped, NOT account-scoped (one physical backend per
 * namespace, shared by every locally-authenticated account on the device — see
 * [com.mindos.app.features.mindboost.focus.data.local.IndexedSecureKeyValueStore]). Isolation therefore depends
 * entirely on every construction site supplying the CURRENT authenticated user's id as [subject] (see
 * `SpeedRootScreen`) — this class cannot verify that on its own, exactly like
 * [com.mindos.app.features.mindboost.core.session.cognitive.evidence.SubjectScopedSessionBatch] cannot verify its
 * `runs` truly belong to one subject.
 */
class SecureCognitiveSessionRunStore(
    private val store: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    private val subject: String,
    private val json: Json = cognitiveRunJson,
) : CognitiveSessionRunStore {

    // SYSTEM-1 slice 6-R3 (CEO closure contract, §18 C1): sweeps pre-6R3 subject-less rows the first time ANY
    // read/list/delete/reconcile method below runs — see LegacyNamespacePurge's KDoc for why `save` is exempt.
    private val legacyPurge = LegacyNamespacePurge(MindBoostLegacyPurgeSpecs.SessionRuns)

    // SYSTEM-1 slice 6-R3 (P2-01): THE single per-subject revocation authority over the dedicated
    // `mindboost_revocation` namespace, SHARED with SecureSessionRecordStore (same subject → same tombstone + same
    // process-wide lock via SubjectLocks). The TRIADIC status closes the prior fail-open read exposure.
    private val revocation = SubjectRevocationAuthority(revocationStore)

    init {
        validateSubject(subject)
    }

    override suspend fun save(run: CognitiveSessionRun): SaveResult {
        val guarded = revocation.guardedWrite(subject) { saveRun(store, json, subject, run) }
        return when (guarded) {
            is GuardedWrite.Written -> guarded.value
            GuardedWrite.SubjectRevoked -> SaveResult.Failed(StoreFailureReason.SUBJECT_TOMBSTONED)
            is GuardedWrite.StatusUnavailable ->
                SaveResult.Failed(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE)
        }
    }

    // SYSTEM-1 slice 6-R3 (P2-01 triadic §40.7.8 R1): check + payload read run inside ONE per-subject lock
    // (guardedRead — no TOCTOU). REVOKED → Missing (payload never touched); STATUS_UNAVAILABLE → Failed(retryable)
    // — [ReadResult] HAS a failure channel, so the honest `null≠failure` result is expressible here (unlike the
    // channel-less list reads below). A genuine payload backend fault while ACTIVE still maps to Failed via getRun.
    override suspend fun get(runId: String): ReadResult<CognitiveSessionRun> {
        legacyPurge.ensurePurged(store)
        val guarded = revocation.guardedRead(subject) { getRun(store, json, subject, runId) }
        return when (guarded) {
            is GuardedRead.Allowed -> guarded.value
            GuardedRead.SubjectRevoked -> ReadResult.Missing
            is GuardedRead.StatusUnavailable ->
                ReadResult.Failed(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE)
        }
    }

    // Channel-less list reads: no Failed-representable return, so STATUS_UNAVAILABLE breadcrumbs + returns empty
    // (transient, self-correcting; these feed the history read-model, not the profile). REVOKED → empty.
    override suspend fun listRecent(limit: Int): List<CognitiveSessionRun> {
        legacyPurge.ensurePurged(store)
        return listGuarded(revocation.guardedRead(subject) {
            listFromIndex(store, json, subject, recentIndexKey(subject), limit)
        })
    }

    override suspend fun listByBlock(blockId: ScoreModuleId, limit: Int): List<CognitiveSessionRun> {
        legacyPurge.ensurePurged(store)
        return listGuarded(revocation.guardedRead(subject) {
            listFromIndex(store, json, subject, blockIndexKey(subject, blockId), limit)
        })
    }

    /**
     * RESIDUAL (6R3-ERASURE-P2-01): the post-delete re-scan in [deleteAllRuns] proves no ENUMERABLE subject key
     * survives, but this store never quarantines a subject-scoped row (no `readParsed`/`quarantine` call — see
     * [getRun]), so it has NO quarantine-retention residual — unlike `SecureSessionRecordStore.deleteAll`, which
     * does and documents it.
     *
     * P2-01 HOLD-THROUGH-ERASE: markRevoked (the durable deletion intent) is the FIRST action inside the
     * per-subject lock, BEFORE the removal loop, so a concurrent save() queued behind the lock always sees the
     * subject already revoked. Deadlock-free: the removal loop only calls `store.remove`, never a guarded op.
     */
    override suspend fun deleteAll(): DeleteResult {
        legacyPurge.ensurePurged(store)
        return revocation.withSubjectLock(subject) {
            if (!revocation.markRevoked(subject)) {
                DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR)
            } else {
                deleteAllRuns(store, subject)
            }
        }
    }

    /**
     * SYSTEM-1 slice 6-R3 (P2-01): a read-then-WRITE (it rebuilds index rows), so it runs under the SAME
     * per-subject lock as [save]/[deleteAll] — a reconcile that acquires the lock AFTER a concurrent [deleteAll]
     * must never resurrect `crun_idx_recent`/`crun_idx_block` rows for a now-tombstoned subject. Even serialized
     * AFTER deleteAll, rebuilding from zero primaries would still WRITE an empty-list index row (a physical
     * residual), so a non-ACTIVE subject (REVOKED or STATUS_UNAVAILABLE) is skipped entirely — nothing read/written.
     */
    override suspend fun reconcileIndices(): ReconcileResult {
        legacyPurge.ensurePurged(store)
        return revocation.withSubjectLock(subject) {
            when (revocation.statusOf(subject)) {
                SubjectRevocationState.Active -> reconcile(store, json, subject)
                // A revoked subject has nothing legitimate to rebuild → no-op success.
                SubjectRevocationState.Revoked -> ReconcileResult.Reconciled(0)
                // Status unknown (transient fault): a repair must NOT report a false "reconciled" — retryable, honest.
                is SubjectRevocationState.StatusUnavailable ->
                    ReconcileResult.Failed(StoreFailureReason.REVOCATION_STATUS_UNAVAILABLE)
            }
        }
    }
}

/** Maps a channel-less guarded list read: REVOKED/STATUS_UNAVAILABLE → empty (the latter breadcrumbed). */
private fun listGuarded(guarded: GuardedRead<List<CognitiveSessionRun>>): List<CognitiveSessionRun> =
    when (guarded) {
        is GuardedRead.Allowed -> guarded.value
        GuardedRead.SubjectRevoked -> emptyList()
        is GuardedRead.StatusUnavailable -> {
            ObservabilityHub.breadcrumb("cognitive_run_status_unavailable_on_read")
            emptyList()
        }
    }

// ---- save ----

private suspend fun saveRun(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
    run: CognitiveSessionRun,
): SaveResult =
    when (classifyExisting(store, json, run, primaryKey(subject, run.sessionId))) {
        SaveClassification.NEW -> persistNew(store, json, subject, run, primaryKey(subject, run.sessionId))
        SaveClassification.IDENTICAL -> {
            upsertIndices(store, json, subject, run) // self-heal a missing index on an idempotent re-save
            SaveResult.IdempotentDuplicate
        }
        SaveClassification.CONFLICT -> SaveResult.DuplicateRunConflict(run.sessionId)
        SaveClassification.UNREADABLE -> SaveResult.Failed(StoreFailureReason.EXISTING_UNREADABLE)
        SaveClassification.BACKEND_ERROR -> SaveResult.Failed(StoreFailureReason.BACKEND_ERROR)
    }

private suspend fun classifyExisting(
    store: SecureKeyValueStore,
    json: Json,
    run: CognitiveSessionRun,
    key: String,
): SaveClassification {
    val existing = try {
        store.read(key)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        return SaveClassification.BACKEND_ERROR
    }
    return when (existing) {
        SecureReadResult.Missing -> SaveClassification.NEW
        is SecureReadResult.Value -> classifyValue(json, run, existing.value)
        is SecureReadResult.Failure -> SaveClassification.UNREADABLE
    }
}

private fun classifyValue(json: Json, run: CognitiveSessionRun, raw: String): SaveClassification {
    val decoded = runCatching { decodeRun(json, raw) }.getOrNull() ?: return SaveClassification.CONFLICT
    return if (decoded == run) SaveClassification.IDENTICAL else SaveClassification.CONFLICT
}

private suspend fun persistNew(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
    run: CognitiveSessionRun,
    key: String,
): SaveResult {
    if (!tryPut(store, key, encodeRun(json, run))) return SaveResult.Failed(StoreFailureReason.BACKEND_ERROR)
    // Primary is durable; indices follow (best-effort). A failed index write is surfaced (not hidden) so the
    // caller knows the run may be missing from listings until reconcileIndices() repairs the projection.
    return if (upsertIndices(store, json, subject, run)) SaveResult.Saved else SaveResult.SavedIndexRepairNeeded
}

private suspend fun tryPut(store: SecureKeyValueStore, key: String, value: String): Boolean = try {
    store.put(key, value)
    true
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    false
}

// ---- indices ----

/** Writes the run into both indices; returns true only if BOTH writes succeeded (best-effort, never throws). */
private suspend fun upsertIndices(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
    run: CognitiveSessionRun,
): Boolean {
    val entry = run.toIndexEntry()
    val recentOk = upsertIndex(store, json, recentIndexKey(subject), entry)
    val blockOk = upsertIndex(store, json, blockIndexKey(subject, run.blockId), entry)
    return recentOk && blockOk
}

@Suppress("TooGenericExceptionCaught")
private suspend fun upsertIndex(
    store: SecureKeyValueStore,
    json: Json,
    indexKey: String,
    entry: RunIndexEntry,
): Boolean = try {
    val merged = readIndex(store, json, indexKey).filterNot { it.runId == entry.runId } + entry
    store.put(indexKey, encodeIndex(json, merged))
    true
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    // Primary is already durable; a failed index write is not fatal and is repaired by reconcileIndices().
    ObservabilityHub.breadcrumb("cognitive_run_index_upsert_failed")
    false
}

private suspend fun readIndex(store: SecureKeyValueStore, json: Json, indexKey: String): List<RunIndexEntry> {
    val raw = readRawOrNull(store, indexKey) ?: return emptyList()
    return runCatching { decodeIndex(json, raw) }.getOrElse { emptyList() }
}

private suspend fun readRawOrNull(store: SecureKeyValueStore, key: String): String? =
    when (val result = store.read(key)) {
        is SecureReadResult.Value -> result.value
        SecureReadResult.Missing, is SecureReadResult.Failure -> null
    }

// ---- get ----

private suspend fun getRun(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
    runId: String,
): ReadResult<CognitiveSessionRun> {
    val result = try {
        store.read(primaryKey(subject, runId))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        return ReadResult.Failed(StoreFailureReason.BACKEND_ERROR)
    }
    return when (result) {
        is SecureReadResult.Value -> decodeToResult(json, runId, result.value)
        SecureReadResult.Missing -> ReadResult.Missing
        // A KV-layer Failure (corrupt / key-invalidated / unsupported-version) is an existing-but-unreadable row,
        // NOT a transient backend fault — a thrown read is handled above and maps to ReadResult.Failed.
        is SecureReadResult.Failure -> ReadResult.Corrupt(runId)
    }
}

private fun decodeToResult(json: Json, runId: String, raw: String): ReadResult<CognitiveSessionRun> =
    runCatching { decodeRun(json, raw) }.fold(
        onSuccess = { ReadResult.Found(it) },
        onFailure = { ReadResult.Corrupt(runId) },
    )

// ---- list ----

private suspend fun listFromIndex(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
    indexKey: String,
    limit: Int,
): List<CognitiveSessionRun> {
    if (limit <= 0) return emptyList()
    val entries = sortedEntriesOrEmpty(store, json, indexKey)
    return loadInOrder(store, json, subject, entries, minOf(limit, MAX_LIST_LIMIT))
}

private suspend fun sortedEntriesOrEmpty(
    store: SecureKeyValueStore,
    json: Json,
    indexKey: String,
): List<RunIndexEntry> =
    try {
        readIndex(store, json, indexKey).sortedWith(RUN_RECENCY_COMPARATOR)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        emptyList()
    }

private suspend fun loadInOrder(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
    entries: List<RunIndexEntry>,
    limit: Int,
): List<CognitiveSessionRun> {
    val result = ArrayList<CognitiveSessionRun>()
    for (entry in entries) {
        if (result.size >= limit) break
        val found = getRun(store, json, subject, entry.runId) // skips orphan index (Missing) and corrupt primaries
        if (found is ReadResult.Found) result.add(found.value)
    }
    return result
}

// ---- delete ----

/**
 * Removes ONLY [subject]'s own rows (primaries + recent index + block indices), via per-key [SecureKeyValueStore
 * .remove] — NEVER [SecureKeyValueStore.clearNamespace], which would destroy every OTHER subject's rows sharing
 * this same physical namespace (6R3-P0C-01). Best-effort total, mirroring `IndexedSecureKeyValueStore
 * .clearNamespace`'s own contract: attempts every matched key even if one fails, surfaces the first failure.
 *
 * R3-AUD-P1-02 (fail-closed post-delete verification): confirmed that NO production read path in this store
 * ever calls `readValueOrNull`/`readParsed`/`quarantine` against a subject-scoped row (every read here goes
 * through [SecureKeyValueStore.read] directly — see [getRun]) — so a row can never vanish from [SecureKeyValueStore
 * .keys] via a quarantine side effect this scan would be blind to. Given that, [stillHasSubjectRows] re-scans
 * AFTER the removal loop reports no failure: a backend whose `remove()` call returns without throwing yet does
 * not durably persist the delete (e.g. index/backend desync) must still surface as [DeleteResult.Failed], never
 * a false [DeleteResult.Cleared] while a physical row for [subject] remains.
 */
private suspend fun deleteAllRuns(store: SecureKeyValueStore, subject: String): DeleteResult = try {
    val primaryKeys = store.keys(primaryPrefix(subject))
    val blockIndexKeys = store.keys(blockIndexPrefix(subject))
    val allKeys = primaryKeys + blockIndexKeys + recentIndexKey(subject)
    // Best-effort TOTAL erasure (GDPR fail-closed): attempt EVERY key even if one faults — a single mid-loop
    // backend failure must never abandon the rest of the deleted subject's rows in the shared namespace. Surface
    // the FIRST failure only AFTER every key was attempted (mirrors IndexedSecureKeyValueStore.clearNamespace's
    // own contract, which a plain `forEach { remove }` — aborting on the first throw — silently violated).
    var firstFailure: Throwable? = null
    for (key in allKeys) {
        val failure = removeCatching(store, key)
        if (firstFailure == null) firstFailure = failure
    }
    when {
        firstFailure != null -> DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR)
        stillHasSubjectRows(store, subject) -> DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR)
        else -> DeleteResult.Cleared(primaryKeys.size)
    }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    DeleteResult.Failed(StoreFailureReason.BACKEND_ERROR)
}

/**
 * R3-AUD-P1-02: never report [DeleteResult.Cleared] while ANY physical row for [subject] survives — across ALL
 * THREE key categories [deleteAllRuns] removes: primaries, block indices, AND the singleton recent index. The
 * recent-index key ([recentIndexKey]) is checked by an EXACT [SecureKeyValueStore.read], never a prefix scan:
 * it has no trailing [SUBJECT_DELIMITER], so `keys("crun_idx_recent:a")` would also match subject `ab`'s key
 * (a cross-subject false `Failed`). The exact read is also fail-closed: a [SecureReadResult.Failure] — or a
 * genuinely-throwing backend read (propagated to [deleteAllRuns]'s catch → [DeleteResult.Failed]) — counts as
 * "row may survive", never a false `Cleared`.
 */
private suspend fun stillHasSubjectRows(store: SecureKeyValueStore, subject: String): Boolean =
    store.keys(primaryPrefix(subject)).isNotEmpty() ||
        store.keys(blockIndexPrefix(subject)).isNotEmpty() ||
        store.read(recentIndexKey(subject)) != SecureReadResult.Missing

/** Removes [key], returning the failure (if any) instead of throwing. [CancellationException] always propagates. */
private suspend fun removeCatching(store: SecureKeyValueStore, key: String): Throwable? = try {
    store.remove(key)
    null
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    error
}

// ---- reconcile (rebuild indices from primary rows) ----

private suspend fun reconcile(store: SecureKeyValueStore, json: Json, subject: String): ReconcileResult = try {
    val entries = collectPrimaryEntries(store, json, subject)
    rebuildIndices(store, json, subject, entries)
    ReconcileResult.Reconciled(entries.size)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    // A repair must never crash startup/recovery — a backend fault yields a typed Failed, not an exception.
    ReconcileResult.Failed(StoreFailureReason.BACKEND_ERROR)
}

private suspend fun collectPrimaryEntries(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
): List<RunIndexEntry> {
    val prefix = primaryPrefix(subject)
    return store.keys(prefix).mapNotNull { key ->
        (getRun(store, json, subject, key.removePrefix(prefix)) as? ReadResult.Found)?.value?.toIndexEntry()
    }
}

private suspend fun rebuildIndices(
    store: SecureKeyValueStore,
    json: Json,
    subject: String,
    entries: List<RunIndexEntry>,
) {
    store.put(recentIndexKey(subject), encodeIndex(json, entries))
    val byBlock = entries.groupBy { it.blockId }
    removeStaleBlockIndices(store, subject, byBlock.keys)
    byBlock.forEach { (block, blockEntries) ->
        store.put(blockIndexKey(subject, block), encodeIndex(json, blockEntries))
    }
}

private suspend fun removeStaleBlockIndices(
    store: SecureKeyValueStore,
    subject: String,
    liveBlocks: Set<ScoreModuleId>,
) {
    val liveNames = liveBlocks.mapTo(HashSet()) { it.name }
    val prefix = blockIndexPrefix(subject)
    store.keys(prefix).forEach { key ->
        if (key.removePrefix(prefix) !in liveNames) store.remove(key)
    }
}
