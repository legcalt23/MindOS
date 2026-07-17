package com.mindos.app.features.mindboost.speed.presentation

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

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
 * The factory takes ONLY the sessions store + clock + subject — it has no reference to the Speed measurement
 * store or scoring, so recovery cannot touch them.
 *
 * SYSTEM-1 slice 6-R3 (§14 fold-in): `subject` is now mandatory on [SecureSessionRecordStore], so every test
 * below binds a fixed test subject ("u1"); a dedicated test proves recovery cannot see/mutate another subject's
 * orphaned rows sharing the SAME backing store.
 */
class SpeedSessionRecoveryWiringTest {

    private companion object {
        const val NOW = 1_000_000L
        const val GRACE = 120_000L
        const val SUBJECT = "u1"
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
        val store = testSessionRecordStore(backing, subject = SUBJECT)
        records.forEach { store.upsert(it) }
        return backing
    }

    @Test
    fun staleOrphan_becomesAbandoned() = runTest {
        val backing = seed(speedRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        speedSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT).recover()
        assertEquals(SessionStatus.ABANDONED, testSessionRecordStore(backing, subject = SUBJECT).find("old")?.status)
    }

    @Test
    fun freshHeartbeat_isLeftIntact() = runTest {
        val backing = seed(speedRecord("live", SessionStatus.CHECKPOINT, updatedAtMs = NOW - (GRACE / 2)))
        speedSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT).recover()
        assertEquals(
            SessionStatus.CHECKPOINT,
            testSessionRecordStore(backing, subject = SUBJECT).find("live")?.status,
        )
    }

    @Test
    fun finalized_isNeverTouched() = runTest {
        val backing = seed(speedRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))
        speedSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT).recover()
        assertEquals(
            SessionStatus.FINALIZED,
            testSessionRecordStore(backing, subject = SUBJECT).find("done")?.status,
        )
    }

    @Test
    fun recovery_isIdempotent() = runTest {
        val backing = seed(speedRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        val scanner = speedSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT)
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover())
        assertEquals(1, testSessionRecordStore(backing, subject = SUBJECT).all().size)
    }

    @Test
    fun recovery_neverSeesOrMutatesAnotherSubjectsOrphan_sharingTheSameBackingStore() = runTest {
        // Two accounts on the same device share the SAME physical `mindboost_sessions` backing store — the
        // exact scenario 6R3-P0C-01's sibling P1 fixed. Alice's stale orphan must survive untouched when
        // recovery runs bound to Bob's subject.
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        alice.upsert(speedRecord("alice_old", SessionStatus.STARTED, updatedAtMs = 0L))

        val recoveredForBob = speedSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, subject = "bob").recover()

        assertEquals(0, recoveredForBob, "bob's recovery must not count alice's orphan")
        assertEquals(
            SessionStatus.STARTED,
            alice.find("alice_old")?.status,
            "alice's orphan must remain STARTED — bob's recovery pass must never see, let alone mutate it",
        )
    }
}
