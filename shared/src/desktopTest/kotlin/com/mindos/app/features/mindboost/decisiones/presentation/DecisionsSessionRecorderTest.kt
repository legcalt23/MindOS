package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MC6a: the [DecisionsSessionRecorder] coordinator. STARTED (tagged [ScoreModuleId.DECISIONS], opaque id,
 * plannedTrials = 1) fires on start; finalize/abandon await STARTED before their own transition; abandon never
 * fabricates a record without a start and never touches a finalized session; a recorder storage failure never
 * throws. Only lifecycle primitives reach the recorder — no decision content can, since the API accepts none.
 */
class DecisionsSessionRecorderTest {

    private fun decisionsRecord(id: String, status: SessionStatus, updatedAtMs: Long) = SessionRecord(
        sessionId = id,
        block = ScoreModuleId.DECISIONS,
        status = status,
        startedAtMs = 0L,
        updatedAtMs = updatedAtMs,
        completedTrials = 0,
        plannedTrials = 1,
        activeDurationMs = 0L,
    )

    @Test
    fun start_callsOnStartWithDecisions() = runTest {
        val rec = DecisionsRecordingRecorder()
        val coord = DecisionsSessionRecorder(rec, SessionIdGenerator { "sess_fixed" }, EpochClock { 5_000L })
        coord.start(this)
        advanceUntilIdle()
        assertEquals(1, rec.starts.size)
        val start = rec.starts.single()
        assertEquals("sess_fixed", start.sessionId)
        assertEquals(ScoreModuleId.DECISIONS, start.block)
        assertEquals(5_000L, start.startedAtMs)
        assertEquals(1, start.plannedTrials)
        coroutineContext.cancelChildren()
    }

    @Test
    fun sessionId_isOpaqueAndStable() = runTest {
        val rec = DecisionsRecordingRecorder()
        val coord = DecisionsSessionRecorder(rec, RandomSessionIdGenerator(), EpochClock { 1L })
        coord.start(this)
        advanceUntilIdle()
        coord.finalize()
        val startId = rec.starts.single().sessionId
        assertEquals(startId, rec.finalizes.single().sessionId)
        assertTrue(startId.startsWith("sess_"))
        assertTrue(!startId.contains(":"))
        coroutineContext.cancelChildren()
    }

    @Test
    fun finalize_awaitsStart() = runTest {
        val gate = CompletableDeferred<Unit>()
        val rec = DecisionsRecordingRecorder(startGate = gate)
        val coord = DecisionsSessionRecorder(rec, SessionIdGenerator { "sess_fixed" }, EpochClock { 1L })
        coord.start(this)
        val job = launch { coord.finalize() }
        advanceUntilIdle()
        assertTrue(rec.finalizes.isEmpty(), "finalize must wait for STARTED to complete")

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("start:sess_fixed", "start-done:sess_fixed", "finalize:sess_fixed"), rec.events)
        job.join()
        coroutineContext.cancelChildren()
    }

    @Test
    fun finalize_isCalledOnce() = runTest {
        val rec = DecisionsRecordingRecorder()
        val coord = DecisionsSessionRecorder(rec, SessionIdGenerator { "sess_fixed" }, EpochClock { 1L })
        coord.start(this)
        advanceUntilIdle()
        coord.finalize()
        coord.finalize() // second call is a no-op
        assertEquals(1, rec.finalizes.size)
        coroutineContext.cancelChildren()
    }

    @Test
    fun abandon_awaitsStart() = runTest {
        val gate = CompletableDeferred<Unit>()
        val rec = DecisionsRecordingRecorder(startGate = gate)
        val coord = DecisionsSessionRecorder(rec, SessionIdGenerator { "sess_fixed" }, EpochClock { 1L })
        coord.start(this)
        val job = launch { coord.abandon() }
        advanceUntilIdle()
        assertTrue(rec.abandons.isEmpty(), "abandon must wait for STARTED to complete")

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("sess_fixed"), rec.abandons)
        job.join()
        coroutineContext.cancelChildren()
    }

    @Test
    fun abandon_createsNoSessionWhenNeverStarted() = runTest {
        val rec = DecisionsRecordingRecorder()
        val coord = DecisionsSessionRecorder(rec, SessionIdGenerator { "sess_fixed" }, EpochClock { 1L })
        coord.abandon() // never started
        assertTrue(rec.starts.isEmpty())
        assertTrue(rec.abandons.isEmpty(), "a pre-start abandon must not fabricate a record")
        coroutineContext.cancelChildren()
    }

    @Test
    fun abandon_doesNothingAfterFinalized() = runTest {
        val rec = DecisionsRecordingRecorder()
        val coord = DecisionsSessionRecorder(rec, SessionIdGenerator { "sess_fixed" }, EpochClock { 1L })
        coord.start(this)
        advanceUntilIdle()
        coord.finalize()
        coord.abandon() // must not abandon a finalized session
        assertEquals(1, rec.finalizes.size)
        assertTrue(rec.abandons.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun recorderStorageFailure_doesNotThrow() = runTest {
        val failing = SecureSessionRecorder(DecisionsThrowingStore(), EpochClock { 1L })
        val coord = DecisionsSessionRecorder(failing, SessionIdGenerator { "sess_fixed" }, EpochClock { 1L })
        coord.start(this)
        advanceUntilIdle()
        coord.finalize() // total recorder swallows the storage error → no throw
        coord.abandon()
        coroutineContext.cancelChildren()
    }

    @Test
    fun onlyLifecyclePrimitivesReachTheRecorder() = runTest {
        val rec = DecisionsRecordingRecorder()
        val coord = DecisionsSessionRecorder(rec, SessionIdGenerator { "sess_fixed" }, EpochClock { 7_000L })
        coord.start(this)
        advanceUntilIdle()
        coord.finalize()
        // The coordinator's API accepts NO decision content; the recorder only ever sees minimized lifecycle.
        assertEquals(ScoreModuleId.DECISIONS, rec.starts.single().block)
        assertEquals(1, rec.starts.single().plannedTrials)
        assertEquals(1, rec.finalizes.single().completedTrials)
        assertEquals("sess_fixed", rec.finalizes.single().sessionId)
        coroutineContext.cancelChildren()
    }

    @Test
    fun defaultNoOpRecorder_completesWithoutError() = runTest {
        // Default recorder (NoOp) + explicit clock (avoid SystemEpochClock in desktopTest).
        val coord = DecisionsSessionRecorder(clock = EpochClock { 1L })
        coord.start(this)
        advanceUntilIdle()
        coord.finalize()
        coord.abandon()
        coroutineContext.cancelChildren()
    }

    @Test
    fun productionRecorder_persistsFinalizedDecisions() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val recorder = SecureSessionRecorder(SecureSessionRecordStore(backing), EpochClock { 9_000L })
        val coord = DecisionsSessionRecorder(recorder, SessionIdGenerator { "sess_prod" }, EpochClock { 8_000L })
        coord.start(this)
        advanceUntilIdle()
        coord.finalize()
        val persisted = SecureSessionRecordStore(backing).find("sess_prod")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.DECISIONS, persisted?.block)
        assertEquals(1, persisted?.completedTrials)
        assertEquals(1, persisted?.plannedTrials)
        coroutineContext.cancelChildren()
    }

    @Test
    fun seal_persistsOwnDataThenFinalizesThenNavigates() = runTest {
        val order = mutableListOf<String>()
        sealDecisionsSession(
            persistOwnData = { order += "persist" },
            finalize = { order += "finalize" },
            navigate = { order += "navigate" },
        )
        assertEquals(listOf("persist", "finalize", "navigate"), order)
    }

    @Test
    fun abandon_marksAbandonedThenNavigates() = runTest {
        val order = mutableListOf<String>()
        abandonDecisionsSession(
            abandon = { order += "abandon" },
            navigate = { order += "navigate" },
        )
        assertEquals(listOf("abandon", "navigate"), order)
    }

    // MC6c: the block-agnostic recovery the hub runs on Decisiones entry, over a real store on mindboost_sessions.

    @Test
    fun recovery_staleStartedOrphan_becomesAbandoned() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(decisionsRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        decisionsSessionRecovery(backing, EpochClock { 1_000_000L }).recover()
        assertEquals(SessionStatus.ABANDONED, store.find("old")?.status)
    }

    @Test
    fun recovery_freshHeartbeat_isLeftIntact() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(decisionsRecord("live", SessionStatus.STARTED, updatedAtMs = 1_000_000L - 60_000L))
        decisionsSessionRecovery(backing, EpochClock { 1_000_000L }).recover()
        assertEquals(SessionStatus.STARTED, store.find("live")?.status)
    }

    @Test
    fun recovery_finalized_isNeverTouched() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(decisionsRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))
        decisionsSessionRecovery(backing, EpochClock { 1_000_000L }).recover()
        assertEquals(SessionStatus.FINALIZED, store.find("done")?.status)
    }

    @Test
    fun recovery_isIdempotent() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(decisionsRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        val scanner = decisionsSessionRecovery(backing, EpochClock { 1_000_000L })
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover())
        assertEquals(1, store.all().size)
    }
}

/** Records lifecycle calls (and can gate onStart) so ordering and tags are assertable. */
private class DecisionsRecordingRecorder(
    private val startGate: CompletableDeferred<Unit>? = null,
) : SessionRecorder {
    val starts = mutableListOf<StartCall>()
    val finalizes = mutableListOf<FinalizeCall>()
    val abandons = mutableListOf<String>()
    val events = mutableListOf<String>()

    data class StartCall(
        val sessionId: String,
        val block: ScoreModuleId,
        val startedAtMs: Long,
        val plannedTrials: Int,
    )

    data class FinalizeCall(val sessionId: String, val completedTrials: Int, val activeDurationMs: Long)

    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
        events += "start:$sessionId"
        startGate?.await()
        starts += StartCall(sessionId, block, startedAtMs, plannedTrials)
        events += "start-done:$sessionId"
    }

    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit

    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        finalizes += FinalizeCall(sessionId, completedTrials, activeDurationMs)
        events += "finalize:$sessionId"
    }

    override suspend fun onAbandon(sessionId: String) {
        abandons += sessionId
        events += "abandon:$sessionId"
    }
}

/** A record store that fails on every call, to prove the recorder is total and never breaks the flow. */
private class DecisionsThrowingStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = error("boom")
    override suspend fun find(sessionId: String): SessionRecord? = error("boom")
    override suspend fun all(): List<SessionRecord> = error("boom")
    override suspend fun delete(sessionId: String) = error("boom")
}
