package com.mindos.app.features.mindboost.core.sync

/**
 * The classified outcome of a single session sync write.
 *
 * Security invariant: a `PERMISSION_DENIED` is NEVER conflated with success, and a bare "already exists" is
 * NEVER treated as a duplicate without positive verification (same user, same session, same fingerprint).
 */
sealed interface SyncWriteResult {
    /** The write succeeded: the remote session document now matches local. */
    data object Synced : SyncWriteResult

    /** The remote document already existed AND was verified identical (same user, session and fingerprint). */
    data object AlreadySyncedDuplicate : SyncWriteResult

    /** The remote document exists but diverges from local — needs resolution, NOT a silent success. */
    data class Conflict(val reason: ConflictReason) : SyncWriteResult

    /** Authorization failure. This is NEVER success and must not advance the sync state to SYNCED. */
    data object PermissionDenied : SyncWriteResult

    /** Transient / transport failure: keep the session PENDING and retryable. */
    data object NetworkFailure : SyncWriteResult

    /** The payload is structurally invalid: do NOT retry indefinitely. */
    data object ValidationFailure : SyncWriteResult

    /** Anything unclassified: fail closed (not success, not retried forever). */
    data object UnknownFailure : SyncWriteResult
}

/** Why an existing remote document could not be accepted as a clean duplicate. */
enum class ConflictReason {
    /** The remote document was not read/verified, so "already exists" cannot be trusted as a duplicate. */
    UNVERIFIED_EXISTING,

    /** The remote document belongs to a different user id. */
    DIFFERENT_USER,

    /** The remote document has a different session fingerprint (content diverged). */
    DIFFERENT_FINGERPRINT,
}

/**
 * The normalized low-level result of a Firestore write, mapped from the platform SDK by the (future) sync
 * layer. Phase 0 models the OUTCOMES only; it does not talk to Firestore.
 */
enum class RawWriteOutcome {
    SUCCESS,

    /** A create() was rejected because a document with the same id already exists. */
    ALREADY_EXISTS,
    PERMISSION_DENIED,
    NETWORK_ERROR,
    INVALID_ARGUMENT,
    UNKNOWN,
}

/** A minimal, verified read of the existing remote session document, used to classify an ALREADY_EXISTS. */
data class RemoteSessionProbe(
    val exists: Boolean,
    val userId: String,
    val fingerprint: SessionFingerprint,
)

/**
 * Pure classifier turning a low-level [RawWriteOutcome] (plus an optional verified [RemoteSessionProbe]) into
 * a typed [SyncWriteResult]. Side-effect-free and fully testable.
 */
object SyncWriteClassifier {
    /**
     * [expected] is the fingerprint of the LOCAL session being written; [remote] is a verified read of the
     * existing remote document (null when it was not probed). An ALREADY_EXISTS is only a duplicate when the
     * remote document is present, owned by the same user, and matches [expected]; anything else is a
     * Conflict — never a silent success. PERMISSION_DENIED is always [SyncWriteResult.PermissionDenied].
     */
    fun classify(
        outcome: RawWriteOutcome,
        expected: SessionFingerprint,
        remote: RemoteSessionProbe? = null,
    ): SyncWriteResult = when (outcome) {
        RawWriteOutcome.SUCCESS -> SyncWriteResult.Synced
        RawWriteOutcome.PERMISSION_DENIED -> SyncWriteResult.PermissionDenied
        RawWriteOutcome.NETWORK_ERROR -> SyncWriteResult.NetworkFailure
        RawWriteOutcome.INVALID_ARGUMENT -> SyncWriteResult.ValidationFailure
        RawWriteOutcome.UNKNOWN -> SyncWriteResult.UnknownFailure
        RawWriteOutcome.ALREADY_EXISTS -> classifyExisting(expected, remote)
    }

    private fun classifyExisting(expected: SessionFingerprint, remote: RemoteSessionProbe?): SyncWriteResult = when {
        remote == null || !remote.exists -> SyncWriteResult.Conflict(ConflictReason.UNVERIFIED_EXISTING)
        remote.userId != expected.userId -> SyncWriteResult.Conflict(ConflictReason.DIFFERENT_USER)
        remote.fingerprint != expected -> SyncWriteResult.Conflict(ConflictReason.DIFFERENT_FINGERPRINT)
        else -> SyncWriteResult.AlreadySyncedDuplicate
    }
}
