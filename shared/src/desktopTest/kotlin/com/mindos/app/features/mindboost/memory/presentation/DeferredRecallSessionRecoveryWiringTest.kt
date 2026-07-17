package com.mindos.app.features.mindboost.memory.presentation

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

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
 * idempotent. The factory takes ONLY the sessions store + clock + subject — no reference to the memory stores,
 * scoring, or any word/lure/selection, so recovery can never touch raw memory content.
 *
 * SYSTEM-1 slice 6-R3 (§14 fold-in): `subject` is now mandatory on [SecureSessionRecordStore]; a dedicated test
 * proves recovery cannot see/mutate another subject's orphaned rows sharing the SAME backing store.
 */
class DeferredRecallSessionRecoveryWiringTest {

    private companion object {
        const val NOW = 1_000_000L
        const val GRACE = 120_000L
        const val SUBJECT = "u1"
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
        val store = testSessionRecordStore(backing, subject = SUBJECT)
        records.forEach { store.upsert(it) }
        return backing
    }

    @Test
    fun staleStartedOrphan_becomesAbandoned() = runTest {
        val backing = seed(episodicRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        deferredRecallSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT).recover()
        assertEquals(SessionStatus.ABANDONED, testSessionRecordStore(backing, subject = SUBJECT).find("old")?.status)
    }

    @Test
    fun staleCheckpointOrphan_becomesAbandoned() = runTest {
        val backing = seed(episodicRecord("cp", SessionStatus.CHECKPOINT, updatedAtMs = 0L))
        deferredRecallSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT).recover()
        assertEquals(SessionStatus.ABANDONED, testSessionRecordStore(backing, subject = SUBJECT).find("cp")?.status)
    }

    @Test
    fun freshHeartbeat_isLeftIntact() = runTest {
        val backing = seed(episodicRecord("live", SessionStatus.STARTED, updatedAtMs = NOW - (GRACE / 2)))
        deferredRecallSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT).recover()
        assertEquals(SessionStatus.STARTED, testSessionRecordStore(backing, subject = SUBJECT).find("live")?.status)
    }

    @Test
    fun finalized_isNeverTouched() = runTest {
        val backing = seed(episodicRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))
        deferredRecallSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT).recover()
        assertEquals(SessionStatus.FINALIZED, testSessionRecordStore(backing, subject = SUBJECT).find("done")?.status)
    }

    @Test
    fun recovery_isIdempotent() = runTest {
        val backing = seed(episodicRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        val scanner = deferredRecallSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, SUBJECT)
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover())
        assertEquals(1, testSessionRecordStore(backing, subject = SUBJECT).all().size)
    }

    @Test
    fun recovery_neverSeesOrMutatesAnotherSubjectsOrphan_sharingTheSameBackingStore() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        alice.upsert(episodicRecord("alice_old", SessionStatus.STARTED, updatedAtMs = 0L))

        val recoveredForBob = deferredRecallSessionRecovery(backing, InMemorySecureKeyValueStore(), EpochClock { NOW }, subject = "bob").recover()

        assertEquals(0, recoveredForBob, "bob's recovery must not count alice's orphan")
        assertEquals(
            SessionStatus.STARTED,
            alice.find("alice_old")?.status,
            "alice's orphan must remain STARTED — bob's recovery pass must never see, let alone mutate it",
        )
    }
}
