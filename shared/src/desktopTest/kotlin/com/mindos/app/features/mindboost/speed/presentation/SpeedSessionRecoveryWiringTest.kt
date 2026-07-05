package com.mindos.app.features.mindboost.speed.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.speed.di.speedSessionRecovery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * MC1c: the production recovery path — the `speedSessionRecovery` factory (what `SpeedRootScreen` runs once on
 * track entry) over a real [SecureSessionRecordStore] on the `mindboost_sessions` namespace. Marks stale
 * STARTED/CHECKPOINT orphans ABANDONED, keeps fresh-heartbeat and FINALIZED records intact, and is idempotent.
 * The factory takes ONLY the sessions store + clock — it has no reference to the Speed measurement store or
 * scoring, so recovery cannot touch them.
 */
class SpeedSessionRecoveryWiringTest {

    private companion object {
        const val NOW = 1_000_000L
        const val GRACE = 120_000L
    }

    private fun speedRecord(id: String, status: SessionStatus, updatedAtMs: Long) = SessionRecord(
        sessionId = id,
        block = ScoreModuleId.SPEED,
        status = status,
        startedAtMs = 0L,
        updatedAtMs = updatedAtMs,
        completedTrials = 0,
        plannedTrials = 12,
        activeDurationMs = 0L,
    )

    private suspend fun seed(vararg records: SessionRecord): InMemorySecureKeyValueStore {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        records.forEach { store.upsert(it) }
        return backing
    }

    @Test
    fun staleOrphan_becomesAbandoned() = runTest {
        val backing = seed(speedRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        speedSessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.ABANDONED, SecureSessionRecordStore(backing).find("old")?.status)
    }

    @Test
    fun freshHeartbeat_isLeftIntact() = runTest {
        val backing = seed(speedRecord("live", SessionStatus.CHECKPOINT, updatedAtMs = NOW - (GRACE / 2)))
        speedSessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.CHECKPOINT, SecureSessionRecordStore(backing).find("live")?.status)
    }

    @Test
    fun finalized_isNeverTouched() = runTest {
        val backing = seed(speedRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))
        speedSessionRecovery(backing, EpochClock { NOW }).recover()
        assertEquals(SessionStatus.FINALIZED, SecureSessionRecordStore(backing).find("done")?.status)
    }

    @Test
    fun recovery_isIdempotent() = runTest {
        val backing = seed(speedRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        val scanner = speedSessionRecovery(backing, EpochClock { NOW })
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover())
        assertEquals(1, SecureSessionRecordStore(backing).all().size)
    }
}
