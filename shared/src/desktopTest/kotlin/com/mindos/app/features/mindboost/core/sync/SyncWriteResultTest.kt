package com.mindos.app.features.mindboost.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncWriteResultTest {

    private val fp = SessionFingerprint(
        userId = "u1",
        sessionId = "enfoque-1-abc",
        moduleId = "ENFOQUE",
        exerciseId = "E1",
        startedAtMs = 100L,
        completedAtMs = 220L,
        durationMs = 120L,
        scoringVersion = "v1",
        normalizedScore = 0.63,
    )

    private fun probe(userId: String = "u1", fingerprint: SessionFingerprint = fp, exists: Boolean = true) =
        RemoteSessionProbe(exists = exists, userId = userId, fingerprint = fingerprint)

    @Test
    fun permissionDenied_isNeverSynced() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.PERMISSION_DENIED, fp, probe())
        assertEquals(SyncWriteResult.PermissionDenied, result)
        assertTrue(result !is SyncWriteResult.Synced)
        assertTrue(result !is SyncWriteResult.AlreadySyncedDuplicate)
    }

    @Test
    fun success_isSynced() {
        assertEquals(SyncWriteResult.Synced, SyncWriteClassifier.classify(RawWriteOutcome.SUCCESS, fp))
    }

    @Test
    fun alreadyExists_matchingRemote_isDuplicate() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.ALREADY_EXISTS, fp, probe(fingerprint = fp))
        assertEquals(SyncWriteResult.AlreadySyncedDuplicate, result)
    }

    @Test
    fun alreadyExists_differentUser_isConflict() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.ALREADY_EXISTS, fp, probe(userId = "other"))
        assertEquals(SyncWriteResult.Conflict(ConflictReason.DIFFERENT_USER), result)
    }

    @Test
    fun alreadyExists_divergentFingerprint_isConflict() {
        val diverged = fp.copy(normalizedScore = 0.90)
        val result = SyncWriteClassifier.classify(RawWriteOutcome.ALREADY_EXISTS, fp, probe(fingerprint = diverged))
        assertEquals(SyncWriteResult.Conflict(ConflictReason.DIFFERENT_FINGERPRINT), result)
    }

    @Test
    fun alreadyExists_withoutProbe_isConflict_notDuplicate() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.ALREADY_EXISTS, fp, remote = null)
        assertEquals(SyncWriteResult.Conflict(ConflictReason.UNVERIFIED_EXISTING), result)
    }

    @Test
    fun networkError_isRetryableFailure() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.NETWORK_ERROR, fp)
        assertEquals(SyncWriteResult.NetworkFailure, result)
    }

    @Test
    fun invalidArgument_isNonRetryableValidationFailure() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.INVALID_ARGUMENT, fp)
        assertEquals(SyncWriteResult.ValidationFailure, result)
    }

    @Test
    fun unknown_isUnknownFailure() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.UNKNOWN, fp)
        assertEquals(SyncWriteResult.UnknownFailure, result)
    }

    @Test
    fun alreadyExists_remoteDoesNotExist_isUnverifiedConflict() {
        val result = SyncWriteClassifier.classify(RawWriteOutcome.ALREADY_EXISTS, fp, probe(exists = false))
        assertEquals(SyncWriteResult.Conflict(ConflictReason.UNVERIFIED_EXISTING), result)
    }

    @Test
    fun alreadyExists_differentRemoteSessionId_isConflict() {
        // sessionId lives inside the fingerprint, so a divergent remote sessionId is a fingerprint mismatch.
        val diverged = fp.copy(sessionId = "enfoque-2-different")
        val result = SyncWriteClassifier.classify(RawWriteOutcome.ALREADY_EXISTS, fp, probe(fingerprint = diverged))
        assertEquals(SyncWriteResult.Conflict(ConflictReason.DIFFERENT_FINGERPRINT), result)
    }
}
