package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.SyncEntityType
import com.mindos.app.features.mindboost.core.sync.SyncQueueEntry
import com.mindos.app.features.mindboost.core.sync.SyncQueueStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureSyncQueueRepositoryTest {

    private fun entry(
        sessionId: String,
        userId: String = "user-1",
        status: SyncQueueStatus = SyncQueueStatus.PENDING,
        attemptCount: Int = 1,
        updatedAtMs: Long = 10L,
        errorCode: String? = null,
    ) = SyncQueueEntry(
        entryId = sessionId,
        userId = userId,
        sessionId = sessionId,
        entityType = SyncEntityType.FOCUS_SESSION,
        status = status,
        attemptCount = attemptCount,
        lastAttemptAtMs = updatedAtMs,
        lastErrorCode = errorCode,
        fingerprint = "fp-$sessionId",
        createdAtMs = updatedAtMs,
        updatedAtMs = updatedAtMs,
    )

    // A raw persisted row so tests can inject unknown-enum values the typed DTO could not produce.
    private fun rowJson(
        sessionId: String,
        userId: String = "user-1",
        entityType: String = "FOCUS_SESSION",
        status: String = "PENDING",
    ): String =
        """{"entryId":"$sessionId","userId":"$userId","sessionId":"$sessionId",""" +
            """"entityType":"$entityType","status":"$status","attemptCount":1,""" +
            """"lastAttemptAtMs":10,"lastErrorCode":null,"fingerprint":"fp",""" +
            """"createdAtMs":10,"updatedAtMs":10,"schemaVersion":1}"""

    // ── core behaviour ────────────────────────────────────────────────────────

    @Test
    fun insertsPending_andFindsIt() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("user-1:100"))
        assertEquals(SyncQueueStatus.PENDING, repo.find("user-1:100")?.status)
    }

    @Test
    fun recordUpserts_neverDuplicatesBySessionId() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = SecureSyncQueueRepository(store)
        repo.record(entry("user-1:100", status = SyncQueueStatus.PENDING))
        repo.record(entry("user-1:100", status = SyncQueueStatus.SYNCED, updatedAtMs = 20L))
        assertEquals(1, store.keys("entry:").size, "same sessionId upserts, never duplicates")
        assertEquals(SyncQueueStatus.SYNCED, repo.find("user-1:100")?.status)
    }

    @Test
    fun transitionsThroughSyncedFailedConflict() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("s", status = SyncQueueStatus.PENDING))
        repo.record(entry("s", status = SyncQueueStatus.SYNCED))
        assertEquals(SyncQueueStatus.SYNCED, repo.find("s")?.status)
        repo.record(entry("s", status = SyncQueueStatus.CONFLICT, errorCode = "DIFFERENT_USER"))
        assertEquals(SyncQueueStatus.CONFLICT, repo.find("s")?.status)
        assertEquals("DIFFERENT_USER", repo.find("s")?.lastErrorCode)
    }

    @Test
    fun permissionDeniedRow_isFailed_neverSynced() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("s", status = SyncQueueStatus.FAILED, errorCode = "PERMISSION_DENIED"))
        val row = repo.find("s")
        assertEquals(SyncQueueStatus.FAILED, row?.status)
        assertTrue(row?.status != SyncQueueStatus.SYNCED)
    }

    @Test
    fun networkFailureRow_staysPending_retryable() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("s", status = SyncQueueStatus.PENDING, errorCode = "NETWORK", attemptCount = 2))
        assertEquals(SyncQueueStatus.PENDING, repo.find("s")?.status)
        assertEquals(1, repo.pending("user-1", limit = 10).size, "network failures remain retryable in pending")
    }

    @Test
    fun incrementsAttemptCount_onReRecord() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("s", attemptCount = 1))
        repo.record(entry("s", attemptCount = 2, updatedAtMs = 20L))
        assertEquals(2, repo.find("s")?.attemptCount)
    }

    @Test
    fun pendingFiltersByUser() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("user-1:1", userId = "user-1", status = SyncQueueStatus.PENDING, updatedAtMs = 1L))
        repo.record(entry("user-1:2", userId = "user-1", status = SyncQueueStatus.SYNCED, updatedAtMs = 2L))
        repo.record(entry("other:3", userId = "other", status = SyncQueueStatus.PENDING, updatedAtMs = 3L))
        val pending = repo.pending("user-1", limit = 10)
        assertEquals(1, pending.size)
        assertEquals("user-1:1", pending.single().sessionId)
    }

    @Test
    fun pendingRespectsLimit() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("user-1:1", updatedAtMs = 1L))
        repo.record(entry("user-1:2", updatedAtMs = 2L))
        repo.record(entry("user-1:3", updatedAtMs = 3L))
        assertEquals(2, repo.pending("user-1", limit = 2).size)
    }

    @Test
    fun deletesByUser_onlyThatUser() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("user-1:1", userId = "user-1"))
        repo.record(entry("user-1:2", userId = "user-1"))
        repo.record(entry("other:3", userId = "other"))
        assertEquals(2, repo.deleteByUser("user-1"))
        assertNull(repo.find("user-1:1"))
        assertEquals(SyncQueueStatus.PENDING, repo.find("other:3")?.status)
    }

    @Test
    fun persistsPreservingEntityType_acrossRepositoryInstances() = runTest {
        val store = InMemorySecureKeyValueStore()
        SecureSyncQueueRepository(store).record(entry("user-1:100", status = SyncQueueStatus.PENDING))
        // A fresh repository over the SAME store still sees the row and its entityType round-trips.
        val reopened = SecureSyncQueueRepository(store)
        assertEquals(SyncEntityType.FOCUS_SESSION, reopened.find("user-1:100")?.entityType)
        assertEquals(SyncQueueStatus.PENDING, reopened.find("user-1:100")?.status)
    }

    // ── total contract: corrupt / unknown-enum / non-positive limit ───────────

    @Test
    fun pending_withNonPositiveLimit_returnsEmptyList() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        repo.record(entry("user-1:1"))
        assertTrue(repo.pending("user-1", limit = 0).isEmpty())
        assertTrue(repo.pending("user-1", limit = -5).isEmpty())
    }

    @Test
    fun find_withCorruptRow_returnsNull() = runTest {
        val store = InMemorySecureKeyValueStore()
        store.put("entry:bad", "{ not valid json")
        assertNull(SecureSyncQueueRepository(store).find("bad"))
    }

    @Test
    fun pending_withCorruptRow_skipsIt() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = SecureSyncQueueRepository(store)
        repo.record(entry("good", userId = "user-1", status = SyncQueueStatus.PENDING))
        store.put("entry:bad", "{ not valid json")
        assertEquals(listOf("good"), repo.pending("user-1", limit = 10).map { it.sessionId })
    }

    @Test
    fun pending_withUnknownEntityType_skipsIt() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = SecureSyncQueueRepository(store)
        repo.record(entry("good", userId = "user-1", status = SyncQueueStatus.PENDING))
        store.put("entry:future", rowJson("future", entityType = "FUTURE_TYPE"))
        assertEquals(listOf("good"), repo.pending("user-1", limit = 10).map { it.sessionId })
    }

    @Test
    fun pending_withUnknownStatus_skipsIt() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = SecureSyncQueueRepository(store)
        repo.record(entry("good", userId = "user-1", status = SyncQueueStatus.PENDING))
        store.put("entry:future", rowJson("future", status = "FUTURE_STATUS"))
        assertEquals(listOf("good"), repo.pending("user-1", limit = 10).map { it.sessionId })
    }

    @Test
    fun deleteByUser_withCorruptRow_doesNotThrow() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = SecureSyncQueueRepository(store)
        repo.record(entry("user-1:1", userId = "user-1"))
        store.put("entry:bad", "{ not valid json")
        // The valid row is removed; the un-attributable corrupt row is left for the namespace eraser.
        assertEquals(1, repo.deleteByUser("user-1"))
    }

    // ── total contract: store failures never propagate ────────────────────────

    @Test
    fun record_whenStoreWorks_returnsTrue() = runTest {
        val repo = SecureSyncQueueRepository(InMemorySecureKeyValueStore())
        assertTrue(repo.record(entry("x")))
    }

    @Test
    fun record_whenStoreThrows_returnsFalse() = runTest {
        // Total AND observable: no throw, but reports the write did NOT persist.
        val repo = SecureSyncQueueRepository(ThrowingSecureKeyValueStore())
        assertFalse(repo.record(entry("x")))
    }

    @Test
    fun find_whenStoreThrows_returnsNull() = runTest {
        assertNull(SecureSyncQueueRepository(ThrowingSecureKeyValueStore()).find("x"))
    }

    @Test
    fun pending_whenStoreThrows_returnsEmptyList() = runTest {
        assertTrue(SecureSyncQueueRepository(ThrowingSecureKeyValueStore()).pending("user-1", limit = 10).isEmpty())
    }

    @Test
    fun deleteByUser_whenStoreThrows_returnsZero() = runTest {
        assertEquals(0, SecureSyncQueueRepository(ThrowingSecureKeyValueStore()).deleteByUser("user-1"))
    }
}
