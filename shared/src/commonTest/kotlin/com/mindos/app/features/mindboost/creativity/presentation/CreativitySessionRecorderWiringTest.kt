package com.mindos.app.features.mindboost.creativity.presentation

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
import com.mindos.app.features.mindboost.creativity.data.persistence.SecureCreativitySessionStore
import com.mindos.app.features.mindboost.creativity.di.creativitySessionRecorder
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.session.CreativitySessionCoordinator
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readValueOrNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * MC3a: the Creativity session holder's write-ahead lifecycle hooks. STARTED fires once at `startExercise`,
 * FINALIZED once on a valid `submit` (never on a rejection), with an opaque id stable across the round and no
 * checkpoint/abandon. A recorder storage failure never breaks the round or its own persistence, and the NoOp
 * default keeps the pre-existing behaviour. The epoch start reuses the coordinator's wall-clock stamp
 * (`ActiveCreativitySession.createdAtMs`) so the holder never touches `Clock.System`. The privacy invariant is
 * enforced end to end: only primitives (opaque id, block, epoch start, fixed round counts, duration) reach the
 * recorder — no idea text, prompt, or free-text ever lands in the `SessionRecord` blob.
 */
@OptIn(ExperimentalCoroutinesApi::class)
// A thorough write-ahead lifecycle suite (hooks, ordering, capture, privacy, production) — hence many small tests.
@Suppress("TooManyFunctions")
class CreativitySessionRecorderWiringTest {

    // A distinctive marker embedded in every submitted idea so the privacy scan can prove it never leaks.
    private val ideas = listOf("alphaMARKERZZ", "betaMARKERZZ", "gammaMARKERZZ")

    // Monotonic fake: each call advances 1s so elapsed (submit - start) is a positive duration.
    private fun incrementingMonotonic(): MonotonicMillis {
        var t = 0L
        return MonotonicMillis { t += 1_000; t }
    }

    // The coordinator's EpochClock is the wall-clock source; its createdAtMs becomes the SessionRecord start.
    private fun coordinatorOn(backing: InMemorySecureKeyValueStore, epoch: Long = 5_000L) =
        CreativitySessionCoordinator(SecureCreativitySessionStore(backing), EpochClock { epoch })

    private fun holder(scope: CoroutineScope, recorder: SessionRecorder, id: String) =
        CreativitySessionStateHolder(
            scope, coordinatorOn(InMemorySecureKeyValueStore()), "u1",
            incrementingMonotonic(), recorder, SessionIdGenerator { id },
        )

    @Test
    fun startAndValidSubmit_recordStartThenFinalize_noCheckpointNoAbandon() = runTest {
        val rec = RecordingRecorder()
        val h = holder(this, rec, id = "sess_fixed")

        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        advanceUntilIdle()
        assertEquals(1, rec.starts.size)
        val start = rec.starts.single()
        assertEquals("sess_fixed", start.sessionId)
        assertEquals(ScoreModuleId.CREATIVITY, start.block)
        assertEquals(5_000L, start.startedAtMs) // reused from the coordinator's createdAtMs stamp
        assertEquals(1, start.plannedTrials)

        h.submit(ideas)
        advanceUntilIdle()
        assertIs<CreativityUiState.Completed>(h.state.value)
        assertEquals(1, rec.finalizes.size)
        val finalize = rec.finalizes.single()
        assertEquals("sess_fixed", finalize.sessionId)
        assertEquals(1, finalize.completedTrials)
        assertTrue(finalize.activeDurationMs > 0)
        assertEquals(0, rec.checkpoints)
        assertEquals(0, rec.abandons)
    }

    @Test
    fun completed_isNotExposedUntilFinalizePersists() = runTest {
        // onStart blocks on a gate. A fast submit must stay in Submitting until STARTED persists AND onFinalize
        // runs — Completed must never surface before the write-ahead record is finalized.
        val gate = CompletableDeferred<Unit>()
        val rec = OrderRecordingRecorder(startGate = gate)
        val h = holder(this, rec, id = "sess_fixed")

        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        h.submit(ideas)
        advanceUntilIdle()
        // onStart is gated; onFinalize is blocked behind its join → no finalize yet, and state is still Submitting.
        assertEquals(listOf("start:sess_fixed"), rec.events)
        assertIs<CreativityUiState.Submitting>(h.state.value)

        gate.complete(Unit)
        advanceUntilIdle()
        // Order: start → start-done → finalize, and only THEN is Completed exposed.
        assertEquals(listOf("start:sess_fixed", "start-done:sess_fixed", "finalize:sess_fixed"), rec.events)
        assertIs<CreativityUiState.Completed>(h.state.value)
    }

    @Test
    fun finalize_usesTheCapturedRoundId_notALaterRoundId() = runTest {
        // Round A's onStart is gated, so its submit blocks on join while round B starts and reassigns the mutable
        // recorder fields. When A finally finalizes it must use the captured "sess_A", never the newer "sess_B".
        val gate = CompletableDeferred<Unit>()
        val rec = OrderRecordingRecorder(startGate = gate)
        val ids = mutableListOf("sess_A", "sess_B")
        val h = CreativitySessionStateHolder(
            this, coordinatorOn(InMemorySecureKeyValueStore()), "u1",
            incrementingMonotonic(), rec, SessionIdGenerator { ids.removeAt(0) },
        )
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA) // round A -> sess_A (onStart gated)
        h.submit(ideas) // captures sess_A + startJob A, then blocks on join
        advanceUntilIdle()
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA) // round B -> reassigns mutable fields to sess_B
        advanceUntilIdle()

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(rec.events.contains("finalize:sess_A"))
        assertTrue(!rec.events.contains("finalize:sess_B"))
    }

    @Test
    fun sessionId_isOpaqueAndStable_fromStartToFinalize_withRandomGenerator() = runTest {
        val rec = RecordingRecorder()
        val h = CreativitySessionStateHolder(
            this, coordinatorOn(InMemorySecureKeyValueStore()), "u1",
            incrementingMonotonic(), rec, RandomSessionIdGenerator(),
        )
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        advanceUntilIdle()
        h.submit(ideas)
        advanceUntilIdle()

        val startId = rec.starts.single().sessionId
        assertEquals(startId, rec.finalizes.single().sessionId)
        assertTrue(startId.startsWith("sess_"))
        assertTrue(!startId.contains("u1"))
    }

    @Test
    fun rejection_doesNotFinalize_thenResubmitFinalizesSameSession() = runTest {
        val rec = RecordingRecorder()
        val h = holder(this, rec, id = "sess_fixed")
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        advanceUntilIdle()

        h.submit(listOf("only-one"))
        advanceUntilIdle()
        assertIs<CreativityUiState.Rejected>(h.state.value)
        assertEquals(0, rec.finalizes.size)

        // Resubmit valid on the same still-open round: one finalize, same session id, no second start.
        h.submit(ideas)
        advanceUntilIdle()
        assertIs<CreativityUiState.Completed>(h.state.value)
        assertEquals(1, rec.starts.size)
        assertEquals(1, rec.finalizes.size)
        assertEquals("sess_fixed", rec.finalizes.single().sessionId)
    }

    @Test
    fun recorderStorageFailure_doesNotBreakCompletionOrCreativityPersistence() = runTest {
        val creativityBacking = InMemorySecureKeyValueStore()
        val store = SecureCreativitySessionStore(creativityBacking)
        val failing = SecureSessionRecorder(ThrowingSessionRecordStore(), EpochClock { 1L })
        val h = CreativitySessionStateHolder(
            this, CreativitySessionCoordinator(store, EpochClock { 5_000L }), "u1",
            incrementingMonotonic(), failing, SessionIdGenerator { "sess_fixed" },
        )
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        advanceUntilIdle()
        h.submit(ideas)
        advanceUntilIdle()

        val state = h.state.value
        assertIs<CreativityUiState.Completed>(state)
        // The creativity round still persisted despite the recorder's storage exploding on every call.
        assertEquals("u1", store.read(state.active.sessionId)?.session?.userId)
    }

    @Test
    fun noOpRecorderDefault_completesNormally() = runTest {
        val store = SecureCreativitySessionStore(InMemorySecureKeyValueStore())
        // Default recorder (NoOp) — the pre-existing production-compatible path is unchanged.
        val h = CreativitySessionStateHolder(
            this, CreativitySessionCoordinator(store, EpochClock { 5_000L }), "u1", incrementingMonotonic(),
        )
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        h.submit(ideas)
        advanceUntilIdle()
        assertIs<CreativityUiState.Completed>(h.state.value)
    }

    @Test
    fun writeAheadRecord_persistsFinalized_withNoCreativeContentInBlob() = runTest {
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = SecureSessionRecorder(SecureSessionRecordStore(sessionsBacking), EpochClock { 9_000L })
        val creativityStore = SecureCreativitySessionStore(InMemorySecureKeyValueStore())
        val h = CreativitySessionStateHolder(
            this, CreativitySessionCoordinator(creativityStore, EpochClock { 8_000L }), "u1",
            incrementingMonotonic(), recorder, SessionIdGenerator { "sess_fixed" },
        )
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        advanceUntilIdle()
        h.submit(ideas)
        advanceUntilIdle()

        // The canonical write-ahead record is FINALIZED for the CREATIVITY block...
        val persisted = SecureSessionRecordStore(sessionsBacking).find("sess_fixed")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.CREATIVITY, persisted?.block)
        assertEquals(8_000L, persisted?.startedAtMs)
        // ...and NO idea text ever reached the raw mindboost_sessions blob.
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        assertTrue(rawBlobs.isNotEmpty())
        assertTrue(rawBlobs.none { it.contains("MARKERZZ") })
    }

    @Test
    fun productionFactory_usesSessionsStore_persistsFinalized_withNoCreativeContent() = runTest {
        // The MC3b production factory — same wiring CreativityRootScreen mounts over mindboost_sessions.
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = creativitySessionRecorder(sessionsBacking, EpochClock { 8_000L })
        val creativityStore = SecureCreativitySessionStore(InMemorySecureKeyValueStore())
        val h = CreativitySessionStateHolder(
            this, CreativitySessionCoordinator(creativityStore, EpochClock { 8_000L }), "u1",
            incrementingMonotonic(), recorder, SessionIdGenerator { "sess_prod" },
        )
        h.startExercise(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)
        advanceUntilIdle()
        h.submit(ideas)
        advanceUntilIdle()

        val persisted = SecureSessionRecordStore(sessionsBacking).find("sess_prod")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.CREATIVITY, persisted?.block)
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        assertTrue(rawBlobs.none { it.contains("MARKERZZ") })
    }
}

/** Captures every lifecycle call so hook wiring (and its absence for checkpoint/abandon) is assertable. */
private class RecordingRecorder : SessionRecorder {
    val starts = mutableListOf<StartCall>()
    val finalizes = mutableListOf<FinalizeCall>()
    var checkpoints = 0
    var abandons = 0

    data class StartCall(
        val sessionId: String,
        val block: ScoreModuleId,
        val startedAtMs: Long,
        val plannedTrials: Int,
    )

    data class FinalizeCall(val sessionId: String, val completedTrials: Int, val activeDurationMs: Long)

    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
        starts += StartCall(sessionId, block, startedAtMs, plannedTrials)
    }

    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        checkpoints += 1
    }

    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        finalizes += FinalizeCall(sessionId, completedTrials, activeDurationMs)
    }

    override suspend fun onAbandon(sessionId: String) {
        abandons += 1
    }
}

/**
 * Records lifecycle events in program order, tagged with their session id, and can gate onStart so ordering
 * (STARTED before FINALIZED) and captured-id correctness are assertable under a fast/interleaved submit.
 */
private class OrderRecordingRecorder(private val startGate: CompletableDeferred<Unit>? = null) : SessionRecorder {
    val events = mutableListOf<String>()

    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
        events += "start:$sessionId"
        startGate?.await()
        events += "start-done:$sessionId"
    }

    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit

    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        events += "finalize:$sessionId"
    }

    override suspend fun onAbandon(sessionId: String) = Unit
}

/** A record store that fails on every call, to prove the recorder is total and never breaks a round. */
private class ThrowingSessionRecordStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = error("boom")
    override suspend fun find(sessionId: String): SessionRecord? = error("boom")
    override suspend fun all(): List<SessionRecord> = error("boom")
    override suspend fun delete(sessionId: String) = error("boom")
}
