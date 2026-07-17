package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.sync.InMemorySyncQueueRepository
import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import com.mindos.app.features.mindboost.core.sync.RemoteSessionProbe
import com.mindos.app.features.mindboost.core.sync.SyncQueueRepository
import com.mindos.app.features.mindboost.core.sync.SyncQueueStatus
import com.mindos.app.features.mindboost.core.sync.SyncWriteResult
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncingFocusSessionHistoryRepositoryTest {

    private val clock = EpochClock { FIXED_NOW }

    private fun coordinator(
        scope: CoroutineScope,
        local: FakeLocalSessionHistoryRepository = FakeLocalSessionHistoryRepository(),
        remoteSource: FakeFocusSessionRemoteDataSource = FakeFocusSessionRemoteDataSource(),
        queue: SyncQueueRepository = InMemorySyncQueueRepository(),
        enabled: Boolean = true,
        userId: String? = "user-1",
    ) = SyncingFocusSessionHistoryRepository(
        local = local,
        remote = FocusSessionRemoteRepository(remoteSource),
        queue = queue,
        syncScope = scope,
        clock = clock,
        currentUserId = { userId },
        syncEnabled = { enabled },
    )

    @Test
    fun flagOff_savesLocalOnly_neverCallsRemote() = runTest {
        val local = FakeLocalSessionHistoryRepository()
        val remote = FakeFocusSessionRemoteDataSource()
        val repo = coordinator(this, local = local, remoteSource = remote, enabled = false)

        repo.append(focusSession(userId = "user-1"))
        advanceUntilIdle()

        assertEquals(1, local.appended.size, "local save always happens")
        assertEquals(0, remote.createCalls, "flag off: nothing reaches the cloud")
    }

    @Test
    fun flagOn_noAuthenticatedUser_savesLocalOnly() = runTest {
        val local = FakeLocalSessionHistoryRepository()
        val remote = FakeFocusSessionRemoteDataSource()
        val repo = coordinator(this, local = local, remoteSource = remote, enabled = true, userId = null)

        repo.append(focusSession(userId = "user-1"))
        advanceUntilIdle()

        assertEquals(1, local.appended.size)
        assertEquals(0, remote.createCalls, "no authenticated user: no cloud write")
    }

    @Test
    fun localFailure_propagates_andRemoteIsNeverCalled() = runTest {
        val local = FakeLocalSessionHistoryRepository(appendThrows = IllegalStateException("disk full"))
        val remote = FakeFocusSessionRemoteDataSource()
        val repo = coordinator(this, local = local, remoteSource = remote, enabled = true)

        assertFailsWith<IllegalStateException> { repo.append(focusSession(userId = "user-1")) }
        advanceUntilIdle()
        assertEquals(0, remote.createCalls, "a local failure must stop sync entirely")
    }

    @Test
    fun flagOn_withAuth_savesLocalThenMirrorsToCloud() = runTest {
        val local = FakeLocalSessionHistoryRepository()
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.SUCCESS)
        val queue = InMemorySyncQueueRepository()
        val repo = coordinator(this, local = local, remoteSource = remote, queue = queue, enabled = true)

        val session = focusSession(userId = "user-1")
        repo.append(session)
        advanceUntilIdle()

        assertEquals(1, local.appended.size)
        assertEquals(1, remote.createCalls)
        assertEquals(SyncQueueStatus.SYNCED, queue.find(session.sessionId.value)?.status)
    }

    @Test
    fun syncNow_rawTrialsNeverLeaveInTheCloudDto() = runTest {
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.SUCCESS)
        val repo = coordinator(this, remoteSource = remote)

        // The source session carries 3 real trials (RTs 320/410/305); the cloud DTO must carry none.
        repo.syncNow(focusSession(userId = "user-1"), userId = "user-1")

        val sent = remote.createdDtos.single()
        val json = Json.encodeToString(sent)
        // Field-name checks are collision-proof: the FNV-1a fingerprint is hex ([0-9a-f]) and can never
        // contain these letters, so a match would mean the structure was actually serialized.
        assertFalse(json.contains("reactionTime"), "no per-trial reaction times in the cloud payload")
        assertFalse(json.contains("trials"), "no trial list in the cloud payload")
        assertFalse(json.contains("blocks"), "no per-block breakdown in the cloud payload")
        assertFalse(json.contains("markers"), "no per-trial markers in the cloud payload")
        assertFalse(json.contains("subjectiveState"), "no health-adjacent self-report in the cloud payload")
    }

    @Test
    fun syncNow_permissionDenied_isNeverMarkedSynced() = runTest {
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.PERMISSION_DENIED)
        val queue = InMemorySyncQueueRepository()
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val session = focusSession(userId = "user-1")
        val result = repo.syncNow(session, userId = "user-1")

        assertEquals(SyncWriteResult.PermissionDenied, result)
        val row = queue.find(session.sessionId.value)
        assertEquals(SyncQueueStatus.FAILED, row?.status)
        assertEquals("PERMISSION_DENIED", row?.lastErrorCode)
    }

    @Test
    fun syncNow_networkFailure_keepsPending_localAlreadyDurable() = runTest {
        val local = FakeLocalSessionHistoryRepository()
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.NETWORK_ERROR)
        val queue = InMemorySyncQueueRepository()
        val repo = coordinator(this, local = local, remoteSource = remote, queue = queue)

        val session = focusSession(userId = "user-1")
        repo.append(session)
        advanceUntilIdle()

        assertEquals(1, local.appended.size, "session stays saved locally regardless of the network")
        val row = queue.find(session.sessionId.value)
        assertEquals(SyncQueueStatus.PENDING, row?.status, "network failure stays retryable (PENDING)")
        assertEquals("NETWORK", row?.lastErrorCode)
    }

    @Test
    fun syncNow_verifiedDuplicate_isLogicalSuccess_withoutASecondWrite() = runTest {
        val session = focusSession(userId = "user-1")
        val fingerprint = FocusSessionCloudMapper
            .fingerprintOf(FocusSessionCloudMapper.toCloudDtoOrNull(session, "user-1", FIXED_NOW)!!)
        val remote = FakeFocusSessionRemoteDataSource(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            probeResult = RemoteSessionProbe(exists = true, userId = "user-1", fingerprint = fingerprint),
        )
        val queue = InMemorySyncQueueRepository()
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val result = repo.syncNow(session, userId = "user-1")

        assertEquals(SyncWriteResult.AlreadySyncedDuplicate, result)
        assertEquals(SyncQueueStatus.SYNCED, queue.find(session.sessionId.value)?.status)
        assertEquals(1, remote.createCalls, "a duplicate is not re-created")
    }

    @Test
    fun syncNow_invalidOwner_isValidationFailure_neverWritten() = runTest {
        val remote = FakeFocusSessionRemoteDataSource()
        val queue = InMemorySyncQueueRepository()
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        // Authenticated 'intruder' does not own 'user-1''s session → never write.
        val session = focusSession(userId = "user-1")
        val result = repo.syncNow(session, userId = "intruder")

        assertEquals(SyncWriteResult.ValidationFailure, result)
        assertEquals(0, remote.createCalls)
        assertEquals(SyncQueueStatus.FAILED, queue.find(session.sessionId.value)?.status)
    }

    @Test
    fun recent_delegatesToLocal() = runTest {
        val local = FakeLocalSessionHistoryRepository()
        val repo = coordinator(this, local = local, enabled = false)
        val session = focusSession(userId = "user-1")
        repo.append(session)
        advanceUntilIdle()

        assertEquals(listOf(session), repo.recent("user-1", limit = 10))
        assertNull(repo.recent("other", limit = 10).firstOrNull())
        assertTrue(repo.recent("user-1", limit = 10).isNotEmpty())
    }

    @Test
    fun queueFindFailure_returnsUnknownFailure_andDoesNotCallRemote() = runTest {
        val queue = FailingSyncQueueRepository(failFind = true)
        val remote = FakeFocusSessionRemoteDataSource()
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val result = repo.syncNow(focusSession(userId = "user-1"), userId = "user-1")

        assertEquals(SyncWriteResult.UnknownFailure, result)
        assertEquals(0, remote.createCalls, "a queue read failure must not reach the cloud")
    }

    @Test
    fun queuePendingRecordFailure_returnsUnknownFailure_andDoesNotCallRemote() = runTest {
        val queue = FailingSyncQueueRepository(failRecordOnCall = 1) // the PENDING write throws
        val remote = FakeFocusSessionRemoteDataSource()
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val result = repo.syncNow(focusSession(userId = "user-1"), userId = "user-1")

        assertEquals(SyncWriteResult.UnknownFailure, result)
        assertEquals(0, remote.createCalls, "if the attempt cannot be traced (PENDING), do not call remote")
    }

    @Test
    fun queueFinalRecordFailure_doesNotMarkSyncedSilently() = runTest {
        val queue = FailingSyncQueueRepository(failRecordOnCall = 2) // the final (SYNCED) write throws
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.SUCCESS)
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val session = focusSession(userId = "user-1")
        val result = repo.syncNow(session, userId = "user-1")

        assertEquals(SyncWriteResult.Synced, result, "the real remote outcome is still returned")
        assertEquals(1, remote.createCalls)
        assertEquals(
            SyncQueueStatus.PENDING,
            queue.find(session.sessionId.value)?.status,
            "the queue must never fabricate SYNCED when it could not write the final row",
        )
    }

    @Test
    fun appendWithFailingQueue_flagOn_stillSavesLocal_withoutThrowing() = runTest {
        val local = FakeLocalSessionHistoryRepository()
        val queue = FailingSyncQueueRepository(failFind = true)
        val repo = coordinator(this, local = local, queue = queue, enabled = true)

        repo.append(focusSession(userId = "user-1")) // must not throw despite the broken queue
        advanceUntilIdle()

        assertEquals(1, local.appended.size, "training/local save is never broken by a queue failure")
    }

    // ── coordinator wired with the DURABLE queue (Phase 1.1, not mounted in production) ──

    @Test
    fun withDurableQueue_success_persistsSyncedRow() = runTest {
        val store = InMemorySecureKeyValueStore()
        val queue = SecureSyncQueueRepository(store)
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.SUCCESS)
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val session = focusSession(userId = "user-1")
        repo.append(session)
        advanceUntilIdle()

        // A fresh durable repo over the same store still sees the SYNCED row (durably persisted).
        assertEquals(
            SyncQueueStatus.SYNCED,
            SecureSyncQueueRepository(store).find(session.sessionId.value)?.status,
        )
    }

    @Test
    fun withDurableQueue_permissionDenied_persistsFailed_notSynced() = runTest {
        val store = InMemorySecureKeyValueStore()
        val queue = SecureSyncQueueRepository(store)
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.PERMISSION_DENIED)
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val session = focusSession(userId = "user-1")
        val result = repo.syncNow(session, userId = "user-1")

        assertEquals(SyncWriteResult.PermissionDenied, result)
        assertEquals(SyncQueueStatus.FAILED, queue.find(session.sessionId.value)?.status)
    }

    // ── observable queue contract via the REAL SecureSyncQueueRepository (the bug this patch fixes) ──

    @Test
    fun secureQueuePendingWriteFailure_doesNotCallRemote() = runTest {
        // Real SecureSyncQueueRepository over a throwing store: record(PENDING) returns false (not a throw),
        // so remote is never called and the outcome is UnknownFailure. This is the exact traceability bug.
        val local = FakeLocalSessionHistoryRepository()
        val remote = FakeFocusSessionRemoteDataSource()
        val queue = SecureSyncQueueRepository(ThrowingSecureKeyValueStore())
        val repo = coordinator(this, local = local, remoteSource = remote, queue = queue)

        val result = repo.syncNow(focusSession(userId = "user-1"), userId = "user-1")

        assertEquals(SyncWriteResult.UnknownFailure, result)
        assertEquals(0, remote.createCalls, "a PENDING row that could not be recorded must not reach the cloud")
    }

    @Test
    fun secureQueueFinalWriteFailure_returnsRemoteOutcome_withoutFalseSyncedQueue() = runTest {
        // PENDING write (put #1) succeeds; the final SYNCED write (put #2) fails → record returns false.
        val store = PutFailingSecureKeyValueStore(failPutOnCall = 2)
        val queue = SecureSyncQueueRepository(store)
        val remote = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.SUCCESS)
        val repo = coordinator(this, remoteSource = remote, queue = queue)

        val session = focusSession(userId = "user-1")
        val result = repo.syncNow(session, userId = "user-1")

        assertEquals(SyncWriteResult.Synced, result, "the real remote outcome is still returned")
        assertEquals(1, remote.createCalls)
        // The queue never fabricates a SYNCED it could not write: the row stays at its prior PENDING state.
        assertEquals(SyncQueueStatus.PENDING, queue.find(session.sessionId.value)?.status)
    }

    private companion object {
        const val FIXED_NOW = 1_700_000_100_000L
    }
}
