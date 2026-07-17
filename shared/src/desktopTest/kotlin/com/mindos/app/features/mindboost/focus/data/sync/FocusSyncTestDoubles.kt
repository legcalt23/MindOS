package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.InMemorySyncQueueRepository
import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import com.mindos.app.features.mindboost.core.sync.RemoteSessionProbe
import com.mindos.app.features.mindboost.core.sync.SyncQueueEntry
import com.mindos.app.features.mindboost.core.sync.SyncQueueRepository
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository

/** A focus session whose block carries REAL trials, so tests can prove the cloud DTO drops them. */
fun focusSession(
    userId: String = "user-1",
    endedAtMs: Long = 1_700_000_000_000L,
    startedAtMs: Long = endedAtMs - 60_000L,
    exercise: ExerciseId = ExerciseId.E1_VIGIA,
): SessionResult {
    val block = BlockResult(
        blockId = BlockId("$userId:$endedAtMs:b0"),
        exerciseId = exercise,
        mode = Mode.TRAINING,
        trials = FocusTestFixtures.hits(listOf(320L, 410L, 305L)),
        difficultyStart = DifficultyVector.EASIEST,
        difficultyEnd = DifficultyVector.EASIEST,
        metrics = emptyMap(),
    )
    return SessionResult(
        sessionId = SessionId("$userId:$endedAtMs"),
        mode = Mode.TRAINING,
        exerciseFocus = exercise,
        blocks = listOf(block),
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        subjectiveState = null,
        sessionMetrics = emptyMap(),
    )
}

/** Configurable remote seam that records the DTOs it is asked to write and how often it is probed. */
class FakeFocusSessionRemoteDataSource(
    var createOutcome: RawWriteOutcome = RawWriteOutcome.SUCCESS,
    var probeResult: RemoteSessionProbe? = null,
    var createThrows: Throwable? = null,
) : FocusSessionRemoteDataSource {
    val createdDtos = mutableListOf<FocusSessionCloudDto>()
    var probeCalls = 0
        private set

    val createCalls: Int get() = createdDtos.size

    override suspend fun create(dto: FocusSessionCloudDto): RawWriteOutcome {
        createdDtos += dto
        createThrows?.let { throw it }
        return createOutcome
    }

    override suspend fun probe(userId: String, sessionId: String): RemoteSessionProbe? {
        probeCalls++
        return probeResult
    }
}

/** In-memory local history that can be made to fail its write, to prove local failures stop sync. */
class FakeLocalSessionHistoryRepository(
    var appendThrows: Throwable? = null,
) : SessionHistoryRepository {
    val appended = mutableListOf<SessionResult>()

    override suspend fun append(result: SessionResult) {
        appendThrows?.let { throw it }
        appended += result
    }

    override suspend fun recent(userId: String, limit: Int): List<SessionResult> {
        require(limit > 0) { "limit must be > 0." }
        return appended
            .filter { it.sessionId.value.substringBefore(':') == userId }
            .sortedByDescending { it.endedAtMs }
            .take(limit)
    }
}

/**
 * A queue that can fail on demand, to prove queue failures never break training nor fabricate a SYNCED row.
 * Reads/records that do not fail delegate to a real in-memory queue, so tests can still inspect the surviving row.
 */
class FailingSyncQueueRepository(
    var failFind: Boolean = false,
    var failRecordOnCall: Int? = null,
    private val delegate: InMemorySyncQueueRepository = InMemorySyncQueueRepository(),
) : SyncQueueRepository {
    private var recordCalls = 0

    override suspend fun record(entry: SyncQueueEntry): Boolean {
        recordCalls++
        if (failRecordOnCall == recordCalls) error("queue record failed (call $recordCalls)")
        return delegate.record(entry)
    }

    override suspend fun find(entryId: String): SyncQueueEntry? {
        if (failFind) error("queue find failed")
        return delegate.find(entryId)
    }

    override suspend fun pending(userId: String, limit: Int): List<SyncQueueEntry> = delegate.pending(userId, limit)

    override suspend fun deleteByUser(userId: String): Int = delegate.deleteByUser(userId)
}

/** A store whose every operation throws, to prove the queue is total (a store failure never propagates). */
class ThrowingSecureKeyValueStore : SecureKeyValueStore {
    override suspend fun read(key: String): SecureReadResult = error("boom")
    override suspend fun put(key: String, value: String): Unit = error("boom")
    override suspend fun remove(key: String): Unit = error("boom")
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure): Unit = error("boom")
    override suspend fun keys(prefix: String): Set<String> = error("boom")
    override suspend fun clearNamespace(): Unit = error("boom")
}

/** Delegates to an in-memory store but throws on the Nth `put`, to simulate a partial (final) write failure. */
class PutFailingSecureKeyValueStore(
    private val failPutOnCall: Int,
    private val delegate: InMemorySecureKeyValueStore = InMemorySecureKeyValueStore(),
) : SecureKeyValueStore {
    private var putCalls = 0

    override suspend fun read(key: String): SecureReadResult = delegate.read(key)

    override suspend fun put(key: String, value: String) {
        putCalls++
        if (putCalls == failPutOnCall) error("put failed (call $putCalls)")
        delegate.put(key, value)
    }

    override suspend fun remove(key: String) = delegate.remove(key)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = delegate.keys(prefix)
    override suspend fun clearNamespace() = delegate.clearNamespace()
}

/** Configurable Firestore gateway seam: records created DTOs and returns a canned read/outcome. */
class FakeFocusSessionFirestoreGateway(
    var createOutcome: RawWriteOutcome = RawWriteOutcome.SUCCESS,
    var read: FirestoreSessionRead = FirestoreSessionRead.NotFound,
    var createThrows: Throwable? = null,
) : FocusSessionFirestoreGateway {
    val created = mutableListOf<FocusSessionCloudDto>()
    var readCalls = 0
        private set

    override suspend fun createSession(dto: FocusSessionCloudDto): RawWriteOutcome {
        created += dto
        createThrows?.let { throw it }
        return createOutcome
    }

    override suspend fun readSession(userId: String, sessionId: String): FirestoreSessionRead {
        readCalls++
        return read
    }
}
