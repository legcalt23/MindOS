package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId

/**
 * Outcome of persisting a SEALED run. A sealed [CognitiveSessionRun] is IMMUTABLE: the same [runId] with a
 * different payload NEVER overwrites — it is a [DuplicateRunConflict]. Re-saving the identical run is an
 * idempotent success.
 */
sealed interface SaveResult {
    /** The run was newly persisted (primary written; indices best-effort, self-healing via reconcile). */
    data object Saved : SaveResult

    /** The same runId already held the identical payload — a logical success, no overwrite. */
    data object IdempotentDuplicate : SaveResult

    /** The same runId already held a DIFFERENT (or unverifiable) payload — refused, never overwritten. */
    data class DuplicateRunConflict(val runId: String) : SaveResult

    /**
     * The primary row is durably saved, but an index write failed: the run is retrievable by [get] yet may not
     * appear in [listRecent]/[listByBlock] until [reconcileIndices] rebuilds the projection. NOT a plain success.
     */
    data object SavedIndexRepairNeeded : SaveResult

    /** A storage-level failure (backend threw, or the existing row could not be read to verify identity). */
    data class Failed(val reason: StoreFailureReason) : SaveResult
}

/** Outcome of reading one run. Corrupt/missing are DISTINCT (never conflated); neither crashes. */
sealed interface ReadResult<out T> {
    data class Found<T>(val value: T) : ReadResult<T>
    data object Missing : ReadResult<Nothing>

    /**
     * A row EXISTS for [runId] but is unusable: it failed to decode (bad JSON, unknown enum/version, invalid
     * payload) OR the KV layer reported it unreadable at the storage/crypto level (a `SecureReadResult.Failure`:
     * corrupt / key-invalidated / unsupported-version). Distinct from a transient backend fault ([Failed]).
     */
    data class Corrupt(val runId: String) : ReadResult<Nothing>

    /** A transient storage/backend fault: the KV read THREW — a data-independent failure, not corruption. */
    data class Failed(val reason: StoreFailureReason) : ReadResult<Nothing>
}

/** Outcome of clearing the store. */
sealed interface DeleteResult {
    data class Cleared(val runCount: Int) : DeleteResult
    data class Failed(val reason: StoreFailureReason) : DeleteResult
}

/** Coarse, PII-free failure reason for a store operation. */
enum class StoreFailureReason {
    BACKEND_ERROR,
    EXISTING_UNREADABLE,

    /** SYSTEM-1 slice 6-R3 (P2-01): refused because this subject was already erased on this physical backend
     * (see [com.mindos.app.features.mindboost.core.session.SubjectRevocationAuthority]) — never a stale/transient
     * fault, and never retried automatically. */
    SUBJECT_TOMBSTONED,

    /** SYSTEM-1 slice 6-R3 (P2-01 triadic): the revocation status could NOT be determined (transient tombstone
     * read fault) — the op was refused WITHOUT touching payload; retryable, never a fabricated absence. */
    REVOCATION_STATUS_UNAVAILABLE,
}

/** Outcome of an index reconcile/repair. The repair path NEVER throws, so startup/recovery stays safe. */
sealed interface ReconcileResult {
    data class Reconciled(val indexedRunCount: Int) : ReconcileResult
    data class Failed(val reason: StoreFailureReason) : ReconcileResult
}

/**
 * SYSTEM-1 slice 2 · Local authority for SEALED cognitive-outcome runs ONLY. It persists and lists runs — it
 * holds NO sync/pending/retry state (that is the `SyncQueue`'s job) and NO userId in the serialized payload
 * (identity is never stored inside a [CognitiveSessionRun]).
 *
 * SUBJECT SCOPING (SYSTEM-1 slice 6-R3, P0 fix — 6R3-P0C-01): a [CognitiveSessionRunStore] wraps a namespace
 * that is a SINGLE PHYSICAL BACKEND shared by every locally-authenticated account on the device (it is NOT
 * per-user on its own — see
 * [com.mindos.app.features.mindboost.focus.data.local.IndexedSecureKeyValueStore]). Every implementation MUST
 * therefore scope its physical keys by a mandatory, first-class subject parameter bound at construction (see
 * [SecureCognitiveSessionRunStore]) — never by a naming convention the caller must remember to apply per call —
 * so that two different accounts sharing this namespace can never read, list, or delete one another's rows, and
 * so `save`/`get`/`listRecent`/`listByBlock`/`deleteAll`/`reconcileIndices` on one instance can only ever affect
 * ITS OWN subject's rows.
 *
 * [listByBlock] takes [ScoreModuleId] — the run's OWN [CognitiveSessionRun.blockId] type. The profile-taxonomy
 * mapping (ScoreModuleId → CognitiveModuleId) lives in the profile-side ingestor, so this store never depends
 * on the profile feature.
 *
 * Ordering (listRecent/listByBlock): most-recent first by `completedAtEpochMs`, falling back to
 * `startedAtEpochMs`, with a stable final tiebreak by runId. A non-positive [limit] returns an empty list; an
 * excessive [limit] is capped. Corrupt rows and orphaned index entries are skipped, never crashing a list.
 */
interface CognitiveSessionRunStore {
    suspend fun save(run: CognitiveSessionRun): SaveResult

    suspend fun get(runId: String): ReadResult<CognitiveSessionRun>

    suspend fun listRecent(limit: Int): List<CognitiveSessionRun>

    suspend fun listByBlock(blockId: ScoreModuleId, limit: Int): List<CognitiveSessionRun>

    /**
     * Erases every row belonging to THIS instance's own subject only (never the whole namespace — see the class
     * KDoc). This is the ACCOUNT-DELETION primitive: it destroys data and must NEVER be invoked on sign-out
     * (sign-out pauses access by simply no longer constructing a store bound to the signed-out subject; it never
     * destroys that subject's rows, which may still be the only unsynced copy).
     */
    suspend fun deleteAll(): DeleteResult

    /**
     * Rebuilds the recent/block indices from the primary rows (the source of truth), repairing orphaned entries
     * (index → missing primary) and missing entries (primary → not indexed) left by a partial write. Returns a
     * typed [ReconcileResult] and NEVER throws — safe to call from init/recovery. NOT the hot path (a bounded scan).
     */
    suspend fun reconcileIndices(): ReconcileResult
}
