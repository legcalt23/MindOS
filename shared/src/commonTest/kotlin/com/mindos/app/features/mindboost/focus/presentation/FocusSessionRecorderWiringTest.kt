package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readValueOrNull
import com.mindos.app.features.mindboost.focus.di.focusSessionRecorder
import com.mindos.app.features.mindboost.focus.di.focusSessionRecovery
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * MC7a: write-ahead lifecycle hooks on [FocusSessionStateHolder]. STARTED (tagged [ScoreModuleId.FOCUS], opaque
 * id separate from the internal "$userId:$endedAtMs" id) fires only for a real session (never in Intro);
 * FINALIZED runs after the block's own persistence (history + profile) and BEFORE Finished is exposed;
 * abortAndAwait marks a started-but-unfinished session ABANDONED before Aborted. No stimulus, trial, RT, error,
 * or score content reaches the recorder.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions")
class FocusSessionRecorderWiringTest {

    private fun holder(
        scope: CoroutineScope,
        rec: SessionRecorder,
        profiles: InMemoryFocusProfileRepository,
        id: String = "sess_fixed",
    ) = FocusSessionStateHolder(
        graph = FocusTestFixtures.focusGraph(profileRepository = profiles),
        exerciseId = ExerciseId.E3_GATILLO,
        userId = "u1",
        scope = scope,
        seed = 7L,
        recorder = rec,
        sessionIdGenerator = SessionIdGenerator { id },
    )

    @Test
    fun intro_beforeStart_emitsNoStartedRecord() = runTest {
        val rec = FocusRecordingRecorder()
        val h = holder(this, rec, InMemoryFocusProfileRepository())
        // No start() → the holder stays in Intro; a pre-session run writes no STARTED.
        advanceUntilIdle()
        assertIs<FocusSessionUiState.Intro>(h.uiState)
        assertTrue(rec.starts.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun realSession_emitsStartFocus_thenFinalize_noCheckpoint() = runTest {
        val rec = FocusRecordingRecorder()
        val h = holder(this, rec, InMemoryFocusProfileRepository())
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()
        assertIs<FocusSessionUiState.Finished>(h.uiState)
        assertEquals(1, rec.starts.size)
        assertEquals(ScoreModuleId.FOCUS, rec.starts.single().block)
        assertEquals(1, rec.starts.single().plannedTrials)
        assertEquals(1, rec.finalizes.size)
        assertEquals(1, rec.finalizes.single().completedTrials)
        assertEquals(0, rec.checkpoints)
        coroutineContext.cancelChildren()
    }

    @Test
    fun sessionId_isOpaque_andSeparateFromInternalId() = runTest {
        val rec = FocusRecordingRecorder()
        val h = holder(this, rec, InMemoryFocusProfileRepository()) // fixed "sess_fixed"
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()
        val id = rec.starts.single().sessionId
        assertEquals(id, rec.finalizes.single().sessionId)
        assertEquals("sess_fixed", id)
        assertTrue(!id.contains("u1"), "the opaque id must not embed the internal \"\$userId:\$endedAtMs\" id")
        coroutineContext.cancelChildren()
    }

    @Test
    fun finalize_runsAfterOwnPersistence_andFinishedIsNotExposedBeforeIt() = runTest {
        val gate = CompletableDeferred<Unit>()
        val rec = FocusRecordingRecorder(finalizeGate = gate)
        val profiles = InMemoryFocusProfileRepository()
        val h = holder(this, rec, profiles)
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()
        // Parked at the gated onFinalize: own persistence (profile.save) already happened, but no finalize and
        // NOT Finished yet.
        assertEquals(1, profiles.load("u1")?.totalSessions, "profile persisted before finalize")
        assertTrue(rec.finalizes.isEmpty())
        assertTrue(h.uiState !is FocusSessionUiState.Finished)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("start:sess_fixed", "finalize:sess_fixed"), rec.events)
        assertIs<FocusSessionUiState.Finished>(h.uiState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_afterStart_ordersStartThenAbandonThenAborted() = runTest {
        val rec = FocusRecordingRecorder()
        val h = holder(this, rec, InMemoryFocusProfileRepository())
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceTimeBy(1) // let STARTED fire; session is still mid-run (WARMUP)
        assertTrue(h.uiState is FocusSessionUiState.Running)
        h.abortAndAwait()
        assertEquals(listOf("start:sess_fixed", "abandon:sess_fixed"), rec.events)
        assertIs<FocusSessionUiState.Aborted>(h.uiState)
        assertTrue(rec.finalizes.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_doesNotExposeAbortedUntilOnAbandonCompletes() = runTest {
        val gate = CompletableDeferred<Unit>()
        val rec = FocusRecordingRecorder(abandonGate = gate)
        val h = holder(this, rec, InMemoryFocusProfileRepository())
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceTimeBy(1)
        val job = launch { h.abortAndAwait() }
        advanceUntilIdle()
        assertTrue(rec.abandons.isEmpty())
        assertTrue(h.uiState !is FocusSessionUiState.Aborted)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("sess_fixed"), rec.abandons)
        assertIs<FocusSessionUiState.Aborted>(h.uiState)
        job.join()
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_beforeRealSession_createsNoAbandonedRecord() = runTest {
        val rec = FocusRecordingRecorder()
        val h = holder(this, rec, InMemoryFocusProfileRepository())
        h.abortAndAwait() // never started
        assertIs<FocusSessionUiState.Aborted>(h.uiState)
        assertTrue(rec.starts.isEmpty())
        assertTrue(rec.abandons.isEmpty(), "a pre-session abort must not create an artificial ABANDONED")
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_afterFinished_doesNotAbandonAFinalizedSession() = runTest {
        val rec = FocusRecordingRecorder()
        val h = holder(this, rec, InMemoryFocusProfileRepository())
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()
        assertIs<FocusSessionUiState.Finished>(h.uiState)
        h.abortAndAwait()
        assertEquals(1, rec.finalizes.size)
        assertTrue(rec.abandons.isEmpty(), "a finalized session is never abandoned")
        coroutineContext.cancelChildren()
    }

    @Test
    fun recorderStorageFailure_doesNotBreakSessionOrPersistence() = runTest {
        val failing = SecureSessionRecorder(FocusThrowingStore(), EpochClock { 0L })
        val profiles = InMemoryFocusProfileRepository()
        val h = holder(this, failing, profiles)
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()
        assertIs<FocusSessionUiState.Finished>(h.uiState)
        assertEquals(1, profiles.load("u1")?.totalSessions, "the session's own profile persistence is intact")
        coroutineContext.cancelChildren()
    }

    @Test
    fun noOpDefaultRecorder_keepsExistingBehaviour() = runTest {
        val profiles = InMemoryFocusProfileRepository()
        // Default recorder (NoOp) — the pre-existing production-compatible path.
        val h = FocusSessionStateHolder(
            graph = FocusTestFixtures.focusGraph(profileRepository = profiles),
            exerciseId = ExerciseId.E3_GATILLO,
            userId = "u1",
            scope = this,
            seed = 7L,
        )
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()
        assertIs<FocusSessionUiState.Finished>(h.uiState)
        assertEquals(1, profiles.load("u1")?.totalSessions)
        coroutineContext.cancelChildren()
    }

    @Test
    fun sessionRecordBlob_containsNoFocusContentOrInternalId() = runTest {
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = SecureSessionRecorder(SecureSessionRecordStore(sessionsBacking), EpochClock { 0L })
        val h = holder(this, recorder, InMemoryFocusProfileRepository(), id = "sess_fixed")
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()

        val persisted = SecureSessionRecordStore(sessionsBacking).find("sess_fixed")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.FOCUS, persisted?.block)
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        assertTrue(rawBlobs.isNotEmpty())
        // No userId / internal "$userId:$endedAtMs" id (nor any focus content) may appear in the session blob.
        assertTrue(rawBlobs.none { it.contains("u1") })
        coroutineContext.cancelChildren()
    }

    // MC7c: the block-agnostic recovery FocusRootScreen runs on Enfoque entry, over a real mindboost_sessions store.

    private fun focusRecord(id: String, status: SessionStatus, updatedAtMs: Long) = SessionRecord(
        sessionId = id,
        block = ScoreModuleId.FOCUS,
        status = status,
        startedAtMs = 0L,
        updatedAtMs = updatedAtMs,
        completedTrials = 0,
        plannedTrials = 1,
        activeDurationMs = 0L,
    )

    @Test
    fun recovery_staleStartedOrphan_becomesAbandoned() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(focusRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        focusSessionRecovery(backing, EpochClock { 1_000_000L }).recover()
        assertEquals(SessionStatus.ABANDONED, store.find("old")?.status)
    }

    @Test
    fun recovery_freshHeartbeat_isLeftIntact() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(focusRecord("live", SessionStatus.STARTED, updatedAtMs = 1_000_000L - 60_000L))
        focusSessionRecovery(backing, EpochClock { 1_000_000L }).recover()
        assertEquals(SessionStatus.STARTED, store.find("live")?.status)
    }

    @Test
    fun recovery_finalized_isNeverTouched() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(focusRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))
        focusSessionRecovery(backing, EpochClock { 1_000_000L }).recover()
        assertEquals(SessionStatus.FINALIZED, store.find("done")?.status)
    }

    @Test
    fun recovery_isIdempotent() = runTest {
        val backing = InMemorySecureKeyValueStore()
        val store = SecureSessionRecordStore(backing)
        store.upsert(focusRecord("old", SessionStatus.STARTED, updatedAtMs = 0L))
        val scanner = focusSessionRecovery(backing, EpochClock { 1_000_000L })
        assertEquals(1, scanner.recover())
        assertEquals(0, scanner.recover())
        assertEquals(1, store.all().size)
    }

    @Test
    fun productionFactory_persistsFinalizedFocus_overSessionsStore_withNoContent() = runTest {
        // The MC7b production factory — same wiring rememberFocusSessionStateHolder mounts.
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = focusSessionRecorder(sessionsBacking, EpochClock { 0L })
        val h = holder(this, recorder, InMemoryFocusProfileRepository(), id = "sess_prod")
        h.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()

        val persisted = SecureSessionRecordStore(sessionsBacking).find("sess_prod")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.FOCUS, persisted?.block)
        assertEquals(1, persisted?.completedTrials)
        assertEquals(1, persisted?.plannedTrials)
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        assertTrue(rawBlobs.none { it.contains("u1") })
        coroutineContext.cancelChildren()
    }
}

/** Records lifecycle calls (and can gate onFinalize/onAbandon) so hook wiring and ordering are assertable. */
private class FocusRecordingRecorder(
    private val finalizeGate: CompletableDeferred<Unit>? = null,
    private val abandonGate: CompletableDeferred<Unit>? = null,
) : SessionRecorder {
    val starts = mutableListOf<StartCall>()
    val finalizes = mutableListOf<FinalizeCall>()
    val abandons = mutableListOf<String>()
    var checkpoints = 0
    val events = mutableListOf<String>()

    data class StartCall(
        val sessionId: String,
        val block: ScoreModuleId,
        val startedAtMs: Long,
        val plannedTrials: Int,
    )

    data class FinalizeCall(val sessionId: String, val completedTrials: Int, val activeDurationMs: Long)

    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
        starts += StartCall(sessionId, block, startedAtMs, plannedTrials)
        events += "start:$sessionId"
    }

    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        checkpoints += 1
    }

    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        finalizeGate?.await()
        finalizes += FinalizeCall(sessionId, completedTrials, activeDurationMs)
        events += "finalize:$sessionId"
    }

    override suspend fun onAbandon(sessionId: String) {
        abandonGate?.await()
        abandons += sessionId
        events += "abandon:$sessionId"
    }
}

/** A record store that fails on every call, to prove the recorder is total and never breaks a session. */
private class FocusThrowingStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = error("boom")
    override suspend fun find(sessionId: String): SessionRecord? = error("boom")
    override suspend fun all(): List<SessionRecord> = error("boom")
    override suspend fun delete(sessionId: String) = error("boom")
}
