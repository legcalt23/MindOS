package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionRecoveryScannerTest {

    private companion object {
        const val NOW = 1_000_000L
        const val GRACE = 120_000L
    }

    // updatedAtMs defaults to startedAtMs but can be set independently to model a live heartbeat.
    private fun record(
        sessionId: String,
        status: SessionStatus,
        startedAtMs: Long,
        updatedAtMs: Long = startedAtMs,
    ) = SessionRecord(sessionId, ScoreModuleId.FOCUS, status, startedAtMs, updatedAtMs, 0, 10, 0L)

    private suspend fun seed(vararg records: SessionRecord): InMemorySessionRecordStore {
        val store = InMemorySessionRecordStore()
        records.forEach { store.upsert(it) }
        return store
    }

    private fun scanner(store: SessionRecordStore) =
        SessionRecoveryScanner(store, EpochClock { NOW }, graceMs = GRACE)

    @Test
    fun staleStartedOrphan_becomesAbandoned() = runTest {
        val store = seed(record("old", SessionStatus.STARTED, startedAtMs = 0L, updatedAtMs = 0L))
        assertEquals(1, scanner(store).recover())
        assertEquals(SessionStatus.ABANDONED, store.find("old")!!.status)
        assertEquals(NOW, store.find("old")!!.updatedAtMs)
    }

    @Test
    fun staleCheckpointOrphan_becomesAbandoned() = runTest {
        val store = seed(record("cp", SessionStatus.CHECKPOINT, startedAtMs = 0L, updatedAtMs = 0L))
        assertEquals(1, scanner(store).recover())
        assertEquals(SessionStatus.ABANDONED, store.find("cp")!!.status)
    }

    @Test
    fun longSession_withRecentHeartbeat_isLeftIntact() = runTest {
        // Started long ago (an honest 30-min session) but last checkpoint is recent → NOT abandoned.
        val store = seed(
            record("live", SessionStatus.CHECKPOINT, startedAtMs = 0L, updatedAtMs = NOW - (GRACE / 2)),
        )
        assertEquals(0, scanner(store).recover())
        assertEquals(SessionStatus.CHECKPOINT, store.find("live")!!.status)
    }

    @Test
    fun recentOrphan_withinGrace_isLeftIntact() = runTest {
        val store = seed(record("recent", SessionStatus.STARTED, startedAtMs = NOW - (GRACE / 2)))
        assertEquals(0, scanner(store).recover())
        assertEquals(SessionStatus.STARTED, store.find("recent")!!.status)
    }

    @Test
    fun staleFinalized_isNeverTouched() = runTest {
        val store = seed(record("done", SessionStatus.FINALIZED, startedAtMs = 0L, updatedAtMs = 0L))
        assertEquals(0, scanner(store).recover())
        assertEquals(SessionStatus.FINALIZED, store.find("done")!!.status)
    }

    @Test
    fun recover_isIdempotent_noDuplicates() = runTest {
        val store = seed(record("old", SessionStatus.STARTED, startedAtMs = 0L, updatedAtMs = 0L))
        val scanner = scanner(store)
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover()) // second pass finds nothing new
        assertEquals(1, store.all().size)
    }

    // ---- per-subject isolation (SYSTEM-1 slice 6-R3, §14 fold-in) — the canonical, module-agnostic proof that a
    // scanner built over a subject-bound SecureSessionRecordStore inherits its isolation for free. ----

    @Test
    fun scanner_overASubjectScopedStore_neverSeesAnotherSubjectsOrphan_sharingTheSameBackingStore() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val alice = testSessionRecordStore(backing, subject = "alice")
        val bob = testSessionRecordStore(backing, subject = "bob")
        alice.upsert(record("alice_old", SessionStatus.STARTED, startedAtMs = 0L, updatedAtMs = 0L))
        bob.upsert(record("bob_old", SessionStatus.STARTED, startedAtMs = 0L, updatedAtMs = 0L))

        val bobScanner = SessionRecoveryScanner(bob, EpochClock { NOW }, graceMs = GRACE)
        val recovered = bobScanner.recover()

        assertEquals(1, recovered, "bob's scanner recovers ONLY bob's own orphan")
        assertEquals(SessionStatus.ABANDONED, bob.find("bob_old")?.status)
        assertEquals(
            SessionStatus.STARTED,
            alice.find("alice_old")?.status,
            "alice's orphan must remain STARTED — bob's scanner must never see, let alone mutate it",
        )
    }
}
