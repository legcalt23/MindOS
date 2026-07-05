package com.mindos.app.features.mindboost.creativity.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.creativity.di.creativitySessionRecovery
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MC3c: the production recovery path — the `creativitySessionRecovery` factory (what `CreativityRootScreen` runs
 * once on track entry) over a real [SecureSessionRecordStore] on `mindboost_sessions`. Marks stale
 * STARTED/CHECKPOINT CREATIVITY orphans ABANDONED, keeps fresh-heartbeat and FINALIZED intact, and is idempotent.
 * The factory takes ONLY the sessions store + clock — no reference to the creativity store, scoring, or any idea
 * text, so recovery can never touch creative content.
 */
class CreativitySessionRecoveryWiringTest {

    private companion object {
        const val NOW = 1_000_000L
        const val GRACE = 120_000L
    }

    private fun creativityRecord(id: String, status: SessionStatus, updatedAtMs: Long) = SessionRecord(
        sessionId = id,
        block = ScoreModuleId.CREATIVITY,
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
        val backing = seed(creativityRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        creativitySessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.ABANDONED, SecureSessionRecordStore(backing).find("old")?.status)
    }

    @Test
    fun staleCheckpointOrphan_becomesAbandoned() = runTest {
        val backing = seed(creativityRecord("cp", SessionStatus.CHECKPOINT, updatedAtMs = 0L))
        creativitySessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.ABANDONED, SecureSessionRecordStore(backing).find("cp")?.status)
    }

    @Test
    fun freshHeartbeat_isLeftIntact() = runTest {
        val backing = seed(creativityRecord("live", SessionStatus.STARTED, updatedAtMs = NOW - (GRACE / 2)))
        creativitySessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.STARTED, SecureSessionRecordStore(backing).find("live")?.status)
    }

    @Test
    fun finalized_isNeverTouched() = runTest {
        val backing = seed(creativityRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))
        creativitySessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.FINALIZED, SecureSessionRecordStore(backing).find("done")?.status)
    }

    @Test
    fun recovery_isIdempotent() = runTest {
        val backing = seed(creativityRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        val scanner = creativitySessionRecovery(backing, EpochClock { NOW })
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover())
        assertEquals(1, SecureSessionRecordStore(backing).all().size)
    }
}
