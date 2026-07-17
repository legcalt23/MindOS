package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import com.mindos.app.features.mindboost.core.sync.SessionFingerprint

/**
 * Raw I/O seam over Firestore for one Enfoque session. The real implementation ([GitLiveFocusSessionFirestoreGateway])
 * is emulator-gated; a fake drives the pure [FirestoreFocusSessionRemoteDataSource] tests.
 *
 * Implementations map every SDK error to a typed result and MUST NOT throw a raw SDK exception — only
 * `CancellationException` may propagate. A `PERMISSION_DENIED` surfaces as [RawWriteOutcome.PERMISSION_DENIED],
 * never as success.
 */
interface FocusSessionFirestoreGateway {
    /** Create-only write to `users/{userId}/sessions/{sessionId}`; returns the normalized outcome. */
    suspend fun createSession(dto: FocusSessionCloudDto): RawWriteOutcome

    /** Verified read of the existing remote session document, used to classify an ALREADY_EXISTS. */
    suspend fun readSession(userId: String, sessionId: String): FirestoreSessionRead
}

/** The outcome of a verified remote read. */
sealed interface FirestoreSessionRead {
    /** The document exists; its owner and fingerprint were reconstructed from the stored fields. */
    data class Found(val userId: String, val fingerprint: SessionFingerprint) : FirestoreSessionRead

    /** The document does not exist. */
    data object NotFound : FirestoreSessionRead

    /** The document could not be read/verified (permission, network, corrupt) — never treat as a duplicate. */
    data object Unreadable : FirestoreSessionRead
}
