package com.mindos.app.features.mindboost.memory.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.memory.di.deferredRecallSessionRecovery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MC4c: the production recovery path — the `deferredRecallSessionRecovery` factory (what `MemoryRootScreen` runs
 * once on track entry) over a real [SecureSessionRecordStore] on `mindboost_sessions`. Marks stale
 * STARTED/CHECKPOINT EPISODIC_MEMORY orphans ABANDONED, keeps fresh-heartbeat and FINALIZED intact, and is
 * idempotent. The factory takes ONLY the sessions store + clock — no reference to the memory stores, scoring,
 * or any word/lure/selection, so recovery can never touch raw memory content.
 */
class DeferredRecallSessionRecoveryWiringTest {

    private companion object {
        const val NOW = 1_000_000L
        const val GRACE = 120_000L
    }

    private fun episodicRecord(id: String, status: SessionStatus, updatedAtMs: Long) = SessionRecord(
        sessionId = id,
        block = ScoreModuleId.EPISODIC_MEMORY,
        status = status,
        startedAtMs = 0L,
        updatedAtMs = updatedAtMs,
        completedTrials = 0,
        plannedTrials = 1,
        activeDurationMs = 0L,
    )

    private suspend fun seed(vararg records: SessionRecord): InMemorySecureKeyValueStore {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        records.forEach { store.upsert(it) }
        return backing
    }

    @Test
    fun staleStartedOrphan_becomesAbandoned() = runTest {
        val backing = seed(episodicRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        deferredRecallSessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.ABANDONED, SecureSessionRecordStore(backing).find("old")?.status)
    }

    @Test
    fun staleCheckpointOrphan_becomesAbandoned() = runTest {
        val backing = seed(episodicRecord("cp", SessionStatus.CHECKPOINT, updatedAtMs = 0L))
        deferredRecallSessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.ABANDONED, SecureSessionRecordStore(backing).find("cp")?.status)
    }

    @Test
    fun freshHeartbeat_isLeftIntact() = runTest {
        val backing = seed(episodicRecord("live", SessionStatus.STARTED, updatedAtMs = NOW - (GRACE / 2)))
        deferredRecallSessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.STARTED, SecureSessionRecordStore(backing).find("live")?.status)
    }

    @Test
    fun finalized_isNeverTouched() = runTest {
        val backing = seed(episodicRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))
        deferredRecallSessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.FINALIZED, SecureSessionRecordStore(backing).find("done")?.status)
    }

    @Test
    fun recovery_isIdempotent() = runTest {
        val backing = seed(episodicRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        val scanner = deferredRecallSessionRecovery(backing, EpochClock { NOW })
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover())
        assertEquals(1, SecureSessionRecordStore(backing).all().size)
    }
}
