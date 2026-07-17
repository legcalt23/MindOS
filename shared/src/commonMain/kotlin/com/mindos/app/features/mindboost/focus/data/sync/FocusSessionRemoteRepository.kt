package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import com.mindos.app.features.mindboost.core.sync.RemoteSessionProbe
import com.mindos.app.features.mindboost.core.sync.SessionFingerprint
import com.mindos.app.features.mindboost.core.sync.SyncWriteClassifier
import com.mindos.app.features.mindboost.core.sync.SyncWriteResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Low-level remote seam for a focus session. The real (gitlive Firestore) implementation is deferred to
 * Phase 1.1 (it needs SDK error → [RawWriteOutcome] mapping and emulator verification); Phase 1 ships this
 * port and a fake so the whole classification path is testable off-device.
 *
 * Implementations write to `users/{userId}/sessions/{sessionId}` and MUST normalize every SDK error into a
 * [RawWriteOutcome] rather than throwing — a `PERMISSION_DENIED` must surface as data, never as an exception
 * that could be mistaken for success.
 */
interface FocusSessionRemoteDataSource {
    /** Create-only write (no overwrite). Returns the normalized outcome; an existing id → ALREADY_EXISTS. */
    suspend fun create(dto: FocusSessionCloudDto): RawWriteOutcome

    /** Verified read of the existing remote document, used to classify an ALREADY_EXISTS; null if unreadable. */
    suspend fun probe(userId: String, sessionId: String): RemoteSessionProbe?
}

/**
 * Minimal remote repository for Enfoque sessions. Orchestrates create-only write + (on ALREADY_EXISTS) a
 * verified probe, then classifies via the Phase 0 [SyncWriteClassifier].
 *
 * Total by contract: it NEVER throws a raw SDK exception to the caller (each data-source failure maps to a
 * fail-closed [SyncWriteResult]); it never treats a permission denial as success; it never overwrites a
 * divergent remote document (create-only + conflict classification).
 */
class FocusSessionRemoteRepository(
    private val dataSource: FocusSessionRemoteDataSource,
) {
    suspend fun write(dto: FocusSessionCloudDto, expected: SessionFingerprint): SyncWriteResult {
        val outcome = runCatching { dataSource.create(dto) }
            .getOrElse { throwable -> throwable.rethrowIfCancellation(); RawWriteOutcome.UNKNOWN }
        val probe = if (outcome == RawWriteOutcome.ALREADY_EXISTS) {
            runCatching { dataSource.probe(dto.userId, dto.sessionId) }
                .getOrElse { throwable -> throwable.rethrowIfCancellation(); null }
        } else {
            null
        }
        return SyncWriteClassifier.classify(outcome, expected, probe)
    }
}

private fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
