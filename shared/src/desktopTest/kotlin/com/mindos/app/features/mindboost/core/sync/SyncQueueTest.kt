package com.mindos.app.features.mindboost.core.sync

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncQueueTest {

    @Test
    fun toQueueStatus_mapsEachOutcome_failClosed() {
        assertEquals(SyncQueueStatus.SYNCED, SyncWriteResult.Synced.toQueueStatus())
        assertEquals(SyncQueueStatus.SYNCED, SyncWriteResult.AlreadySyncedDuplicate.toQueueStatus())
        assertEquals(SyncQueueStatus.CONFLICT, SyncWriteResult.Conflict(ConflictReason.DIFFERENT_USER).toQueueStatus())
        assertEquals(SyncQueueStatus.PENDING, SyncWriteResult.NetworkFailure.toQueueStatus(), "network is retryable")
        assertEquals(SyncQueueStatus.FAILED, SyncWriteResult.PermissionDenied.toQueueStatus(), "denied is never synced")
        assertEquals(SyncQueueStatus.FAILED, SyncWriteResult.ValidationFailure.toQueueStatus())
        assertEquals(SyncQueueStatus.FAILED, SyncWriteResult.UnknownFailure.toQueueStatus())
    }

    @Test
    fun toErrorCode_isNullOnSuccess_andNamesTheFailure() {
        assertEquals(null, SyncWriteResult.Synced.toErrorCode())
        assertEquals(null, SyncWriteResult.AlreadySyncedDuplicate.toErrorCode())
        val differentFingerprint = SyncWriteResult.Conflict(ConflictReason.DIFFERENT_FINGERPRINT)
        assertEquals("DIFFERENT_FINGERPRINT", differentFingerprint.toErrorCode())
        assertEquals("PERMISSION_DENIED", SyncWriteResult.PermissionDenied.toErrorCode())
        assertEquals("NETWORK", SyncWriteResult.NetworkFailure.toErrorCode())
        assertEquals("VALIDATION", SyncWriteResult.ValidationFailure.toErrorCode())
        assertEquals("UNKNOWN", SyncWriteResult.UnknownFailure.toErrorCode())
    }

    @Test
    fun record_upsertsByEntryId_ratherThanDuplicating() = runTest {
        val queue = InMemorySyncQueueRepository()
        val base = SyncQueueEntry(
            entryId = "user-1:1000",
            userId = "user-1",
            sessionId = "user-1:1000",
            entityType = SyncEntityType.FOCUS_SESSION,
            status = SyncQueueStatus.PENDING,
            attemptCount = 1,
            lastAttemptAtMs = 10L,
            lastErrorCode = null,
            fingerprint = "abc",
            createdAtMs = 10L,
            updatedAtMs = 10L,
        )
        queue.record(base)
        queue.record(base.copy(status = SyncQueueStatus.SYNCED, updatedAtMs = 20L))

        assertEquals(SyncQueueStatus.SYNCED, queue.find("user-1:1000")?.status)
        assertEquals(0, queue.pending("user-1", limit = 10).size, "the SYNCED row is no longer pending")
    }

    @Test
    fun pending_returnsOnlyPendingRowsForUser_bounded() = runTest {
        val queue = InMemorySyncQueueRepository()
        queue.record(entry("user-1", "s1", SyncQueueStatus.PENDING, updatedAtMs = 1L))
        queue.record(entry("user-1", "s2", SyncQueueStatus.SYNCED, updatedAtMs = 2L))
        queue.record(entry("other", "s3", SyncQueueStatus.PENDING, updatedAtMs = 3L))

        val pending = queue.pending("user-1", limit = 10)
        assertEquals(1, pending.size)
        assertEquals("s1", pending.single().sessionId)
    }

    @Test
    fun inMemoryRecord_returnsTrue() = runTest {
        val queue = InMemorySyncQueueRepository()
        assertTrue(queue.record(entry("user-1", "s1", SyncQueueStatus.PENDING, updatedAtMs = 1L)))
    }

    @Test
    fun inMemoryPending_withNonPositiveLimit_returnsEmptyList() = runTest {
        val queue = InMemorySyncQueueRepository()
        queue.record(entry("user-1", "s1", SyncQueueStatus.PENDING, updatedAtMs = 1L))
        assertTrue(queue.pending("user-1", limit = 0).isEmpty())
        assertTrue(queue.pending("user-1", limit = -3).isEmpty())
    }

    private fun entry(userId: String, sessionId: String, status: SyncQueueStatus, updatedAtMs: Long) =
        SyncQueueEntry(
            entryId = sessionId,
            userId = userId,
            sessionId = sessionId,
            entityType = SyncEntityType.FOCUS_SESSION,
            status = status,
            attemptCount = 1,
            lastAttemptAtMs = updatedAtMs,
            lastErrorCode = null,
            fingerprint = "fp-$sessionId",
            createdAtMs = updatedAtMs,
            updatedAtMs = updatedAtMs,
        )
}
