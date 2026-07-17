// statusOf/markRevoked map a backend fault to a TYPED result (StatusUnavailable / false) and never leak the
// Throwable (telemetry minimization); CancellationException is always rethrown. The swallow is intentional.
@file:Suppress("SwallowedException")

package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide, per-SUBJECT [Mutex] registry (mirrors [SecureStoreValueCache]'s own registry pattern). Keyed by
 * `subject` ALONE (no store-type discriminator), so a `SecureSessionRecordStore` op and a
 * `SecureCognitiveSessionRunStore` op for the SAME subject serialize against EACH OTHER — the single-authority
 * requirement — and an in-flight write for a subject serializes against a freshly-constructed account-deletion
 * eraser instance for that same subject.
 *
 * ponytail: entries are never evicted (bounded in practice by the number of distinct accounts ever seen on one
 * device, not by request volume); revisit with a size cap only if that assumption is ever wrong (shared/kiosk).
 */
private object SubjectLocks {
    private val registryLock = Mutex()
    private val locks = mutableMapOf<String, Mutex>()

    suspend fun forKey(subject: String): Mutex = registryLock.withLock { locks.getOrPut(subject) { Mutex() } }
}

/**
 * TRIADIC per-subject revocation status (SYSTEM-1 slice 6-R3, P2-01 ENMIENDA 2026-07-14). Replaces the binary
 * `isRevoked`/`isDefinitelyRevoked` booleans, whose fail-OPEN read variant was an EXPOSURE: when the tombstone
 * could not be read it proceeded to read the payload. Here an unconfirmable status is [StatusUnavailable], a
 * THIRD state distinct from both [Active] and [Revoked] — it NEVER grants a read/write and is NEVER persisted as
 * a real revocation.
 */
sealed interface SubjectRevocationState {
    /** No durable tombstone: the subject is live. */
    data object Active : SubjectRevocationState

    /** A durable, positive tombstone: the subject was erased on this physical backend — permanent, fail-closed. */
    data object Revoked : SubjectRevocationState

    /** The tombstone could NOT be determined (transient backend fault). NOT Active, NOT Revoked; retryable. */
    data class StatusUnavailable(val reason: String) : SubjectRevocationState
}

/** Result of a [SubjectRevocationAuthority.guardedRead]: the payload read ran ONLY in [Allowed]. */
sealed interface GuardedRead<out T> {
    /** ACTIVE: the read ran and produced [value]. */
    data class Allowed<T>(val value: T) : GuardedRead<T>

    /** REVOKED: the read did NOT run — payload never touched. */
    data object SubjectRevoked : GuardedRead<Nothing>

    /** STATUS_UNAVAILABLE: the read did NOT run — payload never touched; retryable. */
    data class StatusUnavailable(val reason: String) : GuardedRead<Nothing>
}

/** Result of a [SubjectRevocationAuthority.guardedWrite]: the payload write ran ONLY in [Written]. */
sealed interface GuardedWrite<out T> {
    /** ACTIVE: the write ran and produced [value]. */
    data class Written<T>(val value: T) : GuardedWrite<T>

    /** REVOKED: the write did NOT run — terminal refusal. */
    data object SubjectRevoked : GuardedWrite<Nothing>

    /** STATUS_UNAVAILABLE: the write did NOT run — retryable refusal (never a silent drop). */
    data class StatusUnavailable(val reason: String) : GuardedWrite<Nothing>
}

/**
 * SYSTEM-1 slice 6-R3 (P2-01) · THE single, per-subject revocation authority, shared by every subject-scoped
 * store whose read/write path can otherwise race a concurrent account-deletion. The durable tombstone lives in a
 * DEDICATED namespace ([store] = `mindboost_revocation`), NOT inside any data namespace — so (a) one tombstone
 * per subject is consulted by BOTH `SecureSessionRecordStore` and `SecureCognitiveSessionRunStore` (no more two
 * independent per-store tombstones), and (b) a test can fail the STATE store while the PAYLOAD store still works,
 * making STATUS_UNAVAILABLE genuinely testable.
 *
 * PERMANENT MARKER: [markRevoked] writes ONCE and the tombstone is NEVER cleared (mirrors
 * [com.mindos.app.features.mindboost.storage.LegacyNamespacePurge]'s reserved-marker idiom) — once a subject is
 * erased on this backend, no later write for it is ever allowed again, even from a coroutine mid-flight before
 * the erasure completed. The `mindboost_revocation` namespace MUST NOT be added to
 * [com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces.AccountBound]: blanket-clearing it on
 * account deletion would resurrect deleted subjects.
 *
 * NON-REENTRANCY INVARIANT (kotlinx `Mutex` is NOT reentrant and has no timeout): a [guardedRead]/[guardedWrite]
 * body MUST NOT call another [guardedRead]/[guardedWrite]/[withSubjectLock] for the SAME subject — that would
 * deadlock silently. The check + the payload op run inside ONE lock ([guardedRead]/[guardedWrite]), so there is
 * no TOCTOU gap; the erasure primitive holds the lock across markRevoked + the whole delete loop (hold-through-
 * erase), which is deadlock-free because the delete loop only calls `store.remove`, never a guarded op.
 *
 * PROCESS MODEL: the guarantee covers ONE process (the process-wide [SubjectLocks] + the durable tombstone).
 * Across processes the [Mutex] is not shared, but the durable REVOKED tombstone (read on every guarded op) still
 * fail-closes a stale multi-process writer that goes through this authority. Multi-process ordering beyond that
 * is a declared risk (ledger `uid-tombstone-migration-retention`), not a claimed guarantee.
 */
class SubjectRevocationAuthority(private val store: SecureKeyValueStore) {

    /**
     * The current triadic status. `Value` → [SubjectRevocationState.Revoked]; `Missing` →
     * [SubjectRevocationState.Active]; a `Failure` OR a throwing backend read → [SubjectRevocationState
     * .StatusUnavailable] (NEVER silently treated as Active — that would be the fail-open exposure). Does NOT take
     * the lock; callers that need check+op atomicity use [guardedRead]/[guardedWrite].
     */
    suspend fun statusOf(subject: String): SubjectRevocationState {
        require(subject.isNotBlank()) { "subject must not be blank (a pseudonymous per-account scope label)." }
        return try {
            when (store.read(tombstoneKey(subject))) {
                is SecureReadResult.Value -> SubjectRevocationState.Revoked
                SecureReadResult.Missing -> SubjectRevocationState.Active
                is SecureReadResult.Failure -> SubjectRevocationState.StatusUnavailable(READ_FAILURE)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            SubjectRevocationState.StatusUnavailable(READ_THREW)
        }
    }

    /**
     * Runs [read] ONLY if the subject is [SubjectRevocationState.Active], inside the per-subject lock (no TOCTOU).
     * On REVOKED/STATUS_UNAVAILABLE the [read] block is NEVER invoked — the payload is never touched.
     */
    suspend fun <T> guardedRead(subject: String, read: suspend () -> T): GuardedRead<T> =
        withSubjectLock(subject) {
            when (val state = statusOf(subject)) {
                SubjectRevocationState.Active -> GuardedRead.Allowed(read())
                SubjectRevocationState.Revoked -> GuardedRead.SubjectRevoked
                is SubjectRevocationState.StatusUnavailable -> GuardedRead.StatusUnavailable(state.reason)
            }
        }

    /**
     * Runs [write] ONLY if the subject is [SubjectRevocationState.Active], inside the per-subject lock. On
     * REVOKED/STATUS_UNAVAILABLE the [write] block is NEVER invoked — never a silent write to a revoked subject.
     */
    suspend fun <T> guardedWrite(subject: String, write: suspend () -> T): GuardedWrite<T> =
        withSubjectLock(subject) {
            when (val state = statusOf(subject)) {
                SubjectRevocationState.Active -> GuardedWrite.Written(write())
                SubjectRevocationState.Revoked -> GuardedWrite.SubjectRevoked
                is SubjectRevocationState.StatusUnavailable -> GuardedWrite.StatusUnavailable(state.reason)
            }
        }

    /**
     * Serializes [block] against every OTHER caller of this authority for the SAME subject. Used by the erasure
     * primitive to hold the lock across markRevoked + the physical delete loop (hold-through-erase). [block] MUST
     * NOT call a guarded op for the same subject (non-reentrant lock).
     */
    suspend fun <T> withSubjectLock(subject: String, block: suspend () -> T): T {
        require(subject.isNotBlank()) { "subject must not be blank (a pseudonymous per-account scope label)." }
        return SubjectLocks.forKey(subject).withLock { block() }
    }

    /**
     * Durably marks [subject] revoked (the deletion intent). Returns `false` on a backend fault (the caller fails
     * the erasure closed); rethrows [CancellationException]. Idempotent: writing the same tombstone twice is safe.
     */
    suspend fun markRevoked(subject: String): Boolean {
        require(subject.isNotBlank()) { "subject must not be blank (a pseudonymous per-account scope label)." }
        return try {
            store.put(tombstoneKey(subject), TOMBSTONE_VALUE)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            false
        }
    }

    private fun tombstoneKey(subject: String): String = "$TOMBSTONE_PREFIX$subject"

    private companion object {
        // Exact-key lookups (never a prefix scan), so distinct subjects always map to distinct tombstone keys.
        const val TOMBSTONE_PREFIX = "revocation:"

        // A plain flag (no JSON/field names) — presence == revoked, mirrors LegacyNamespacePurge's convention.
        const val TOMBSTONE_VALUE = "1"

        const val READ_FAILURE = "revocation_read_failure"
        const val READ_THREW = "revocation_read_threw"
    }
}
