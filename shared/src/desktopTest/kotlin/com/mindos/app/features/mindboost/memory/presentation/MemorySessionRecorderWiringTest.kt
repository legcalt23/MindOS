package com.mindos.app.features.mindboost.memory.presentation

import com.mindos.app.features.mindboost.core.session.ReproducibleSessionIdentity
import com.mindos.app.features.mindboost.core.session.SessionRecordUpsertOutcome

import com.mindos.app.features.mindboost.core.session.testSessionRecordStore

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readValueOrNull
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryPairsResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryRecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.data.InMemorySpanResultStore
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.di.deferredRecallSessionRecovery
import com.mindos.app.features.mindboost.memory.di.mainMemorySessionRecorder
import com.mindos.app.features.mindboost.memory.domain.adaptive.DefaultMemoryAdaptiveEngine
import com.mindos.app.features.mindboost.memory.domain.antiplateau.InterventionPlanner
import com.mindos.app.features.mindboost.memory.domain.antiplateau.PlateauDetector
import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallScheduler
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex1SpanGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex3ListGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex4PairsGenerator
import com.mindos.app.features.mindboost.memory.domain.metrics.DefaultMemoryMetricsEngine
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.personalization.DefaultArchetypeInference
import com.mindos.app.features.mindboost.memory.domain.reporting.MemoryFitnessCalculator
import com.mindos.app.features.mindboost.memory.domain.session.DefaultMemoryRecoveryPolicy
import com.mindos.app.features.mindboost.memory.domain.session.MemorySessionAssembler
import com.mindos.app.features.mindboost.memory.presentation.state.MemorySessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * MC5a/b: write-ahead lifecycle hooks on the MAIN [MemorySessionStateHolder] (span/list/pairs). STARTED fires
 * only for a REAL session (a valid plan, at Study) tagged [ScoreModuleId.MEMORY]; unavailable-exercise/no-plan
 * early-outs write nothing. FINALIZED runs after the block's own persistence and BEFORE Finished is exposed;
 * abortAndAwait marks a started-but-unfinished session ABANDONED before Aborted. The opaque id is stable and
 * separate from the internal "$userId:$endedAt" ids, and no word/pair/sequence/selection reaches the recorder.
 * MC5c: the existing block-agnostic MemoryRootScreen recovery already covers MAIN (MEMORY) orphans.
 */
@Suppress("TooManyFunctions") // a thorough write-ahead suite: start/finalize/abandon ordering, privacy, production
class MemorySessionRecorderWiringTest {

    private val profiles = InMemoryMemoryProfileRepository()
    private val history = InMemoryMemorySessionHistoryRepository()
    private val deferred = InMemoryDeferredRecallRepository()
    private val spanStore = InMemorySpanResultStore()

    private fun graph() = MemoryGraph(
        content = defaultMemoryContent(),
        generators = mapOf(
            MemoryExerciseId.EX1_SPAN_COMPLEJO to Ex1SpanGenerator(),
            MemoryExerciseId.EX3_LISTA_ESTRATEGIA to Ex3ListGenerator(),
            MemoryExerciseId.EX4_PARES_ASOCIADOS to Ex4PairsGenerator(),
        ),
        metricsEngine = DefaultMemoryMetricsEngine(),
        adaptiveEngine = DefaultMemoryAdaptiveEngine(),
        assembler = MemorySessionAssembler(),
        recoveryPolicy = DefaultMemoryRecoveryPolicy(),
        deferredScheduler = DeferredRecallScheduler(),
        archetypeInference = DefaultArchetypeInference(),
        plateauDetector = PlateauDetector(),
        interventionPlanner = InterventionPlanner(),
        fitnessCalculator = MemoryFitnessCalculator(),
        profileRepository = profiles,
        sessionHistoryRepository = history,
        deferredRecallRepository = deferred,
        recentItemRegistryRepository = InMemoryRecentItemRegistryRepository(),
        spanResultStore = spanStore,
        pairsResultStore = InMemoryPairsResultStore(),
        deferredResultStore = InMemoryDeferredResultStore(),
        epochClock = EpochClock { 1_000L },
    )

    /** Drives a full EX3 (list) session to Finished, returning (studied texts, candidate texts) for privacy scans. */
    private suspend fun TestScope.driveFullList(h: MemorySessionStateHolder): Pair<Set<String>, Set<String>> {
        h.start(MemoryExerciseId.EX3_LISTA_ESTRATEGIA)
        advanceUntilIdle()
        val study = assertIs<MemorySessionUiState.Study>(h.uiState)
        val studied = study.items.toSet()
        h.continueFromStudy()
        advanceUntilIdle()
        val recall = assertIs<MemorySessionUiState.Recall>(h.uiState)
        val candidateTexts = recall.candidates.map { it.text }.toSet()
        h.submitRecall(recall.candidates.filter { it.text in studied }.map { it.id }.toSet())
        advanceUntilIdle()
        return studied to candidateTexts
    }

    @Test
    fun realSession_emitsStartMemory_thenFinalize_noCheckpointNoAbandon() = runTest {
        val rec = MainRecordingRecorder()
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        driveFullList(h)
        assertIs<MemorySessionUiState.Finished>(h.uiState)
        assertEquals(1, rec.starts.size)
        assertEquals(ScoreModuleId.MEMORY, rec.starts.single().block)
        assertEquals("sess_fixed", rec.starts.single().sessionId)
        assertEquals(1, rec.starts.single().plannedTrials)
        assertEquals(1, rec.finalizes.size)
        assertEquals(1, rec.finalizes.single().completedTrials)
        assertEquals(0, rec.checkpoints)
        assertTrue(rec.abandons.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun unavailableExercise_writesNoStartedRecord() = runTest {
        val rec = MainRecordingRecorder()
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start(MemoryExerciseId.EX2_ACTUALIZACION_SET) // no generator → Aborted, not a real session
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Aborted>(h.uiState)
        assertTrue(rec.starts.isEmpty(), "an unavailable exercise must not emit STARTED")
        coroutineContext.cancelChildren()
    }

    @Test
    fun finished_isNotExposedUntilFinalizePersists() = runTest {
        val gate = CompletableDeferred<Unit>()
        val rec = MainRecordingRecorder(finalizeGate = gate)
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start(MemoryExerciseId.EX3_LISTA_ESTRATEGIA)
        advanceUntilIdle()
        val study = assertIs<MemorySessionUiState.Study>(h.uiState)
        h.continueFromStudy()
        advanceUntilIdle()
        val recall = assertIs<MemorySessionUiState.Recall>(h.uiState)
        h.submitRecall(recall.candidates.filter { it.text in study.items.toSet() }.map { it.id }.toSet())
        advanceUntilIdle()
        // onFinalize is gated → state must still be Recall (never Finished) and no finalize recorded yet.
        assertIs<MemorySessionUiState.Recall>(h.uiState)
        assertTrue(rec.finalizes.isEmpty())

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("start:sess_fixed", "finalize:sess_fixed"), rec.events)
        assertIs<MemorySessionUiState.Finished>(h.uiState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun sessionId_isOpaque_stable_andSeparateFromInternalIds() = runTest {
        val rec = MainRecordingRecorder()
        val h = MemorySessionStateHolder(graph(), "u1", this, seed = 42, recorder = rec) // real random generator
        driveFullList(h)
        val startId = rec.starts.single().sessionId
        assertEquals(startId, rec.finalizes.single().sessionId, "id stable across the session")
        assertTrue(startId.startsWith("sess_"))
        assertTrue(!startId.contains("u1"))
        assertTrue(!startId.contains(":"))
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_ordersStartThenAbandonThenAborted() = runTest {
        val rec = MainRecordingRecorder()
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start(MemoryExerciseId.EX1_SPAN_COMPLEJO)
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Study>(h.uiState)
        h.abortAndAwait()
        assertEquals(listOf("start:sess_fixed", "abandon:sess_fixed"), rec.events)
        assertIs<MemorySessionUiState.Aborted>(h.uiState)
        assertTrue(rec.finalizes.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_beforeRealSession_createsNoAbandonedRecord() = runTest {
        val rec = MainRecordingRecorder()
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.prepare(MemoryExerciseId.EX3_LISTA_ESTRATEGIA) // Intro, no session started
        h.abortAndAwait()
        assertIs<MemorySessionUiState.Aborted>(h.uiState)
        assertTrue(rec.starts.isEmpty())
        assertTrue(rec.abandons.isEmpty(), "a pre-session abort must not create an artificial ABANDONED")
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_afterFinished_doesNotAbandonAFinalizedSession() = runTest {
        val rec = MainRecordingRecorder()
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        driveFullList(h)
        assertIs<MemorySessionUiState.Finished>(h.uiState)
        h.abortAndAwait()
        assertEquals(1, rec.finalizes.size)
        assertTrue(rec.abandons.isEmpty(), "a finalized session is never abandoned")
        coroutineContext.cancelChildren()
    }

    @Test
    fun recorderStorageFailure_doesNotBreakSessionOrMemoryPersistence() = runTest {
        val failing = SecureSessionRecorder(MainThrowingSessionRecordStore(), EpochClock { 1_000L })
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = failing,
            sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        driveFullList(h)
        // The session completes and the module's OWN persistence is intact despite the recorder exploding.
        assertIs<MemorySessionUiState.Finished>(h.uiState)
        assertTrue(profiles.load("u1") != null)
        assertTrue(history.recent("u1", 10).isNotEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun noOpDefaultRecorder_keepsExistingBehaviourAndPersistence() = runTest {
        val h = MemorySessionStateHolder(graph(), "u1", this, seed = 42) // default NoOp recorder
        driveFullList(h)
        assertIs<MemorySessionUiState.Finished>(h.uiState)
        assertTrue(profiles.load("u1") != null)
        assertTrue(deferred.load("u1").isNotEmpty(), "the 24h deferred item is still registered")
        coroutineContext.cancelChildren()
    }

    @Test
    fun sessionRecordBlob_containsNoWordsOrSelections() = runTest {
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder =
            SecureSessionRecorder(testSessionRecordStore(sessionsBacking, subject = "u1"), EpochClock { 1_000L })
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = recorder,
            sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        val (studied, candidateTexts) = driveFullList(h)

        val persisted = testSessionRecordStore(sessionsBacking, subject = "u1").find("sess_fixed")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.MEMORY, persisted?.block)
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        assertTrue(rawBlobs.isNotEmpty())
        val forbidden = studied + candidateTexts
        assertTrue(forbidden.all { token -> rawBlobs.none { it.contains(token) } })
        coroutineContext.cancelChildren()
    }

    @Test
    fun productionFactory_persistsFinalizedMemory_overSessionsStore_withNoContent() = runTest {
        // The MC5b production factory — same wiring rememberMemorySessionStateHolder mounts.
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = mainMemorySessionRecorder(sessionsBacking, InMemorySecureKeyValueStore(), EpochClock { 1_000L }, subject = "u1")
        val h = MemorySessionStateHolder(
            graph(), "u1", this, seed = 42, recorder = recorder,
            sessionIdGenerator = SessionIdGenerator { "sess_prod" },
        )
        val (studied, candidateTexts) = driveFullList(h)

        val persisted = testSessionRecordStore(sessionsBacking, subject = "u1").find("sess_prod")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.MEMORY, persisted?.block)
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        val forbidden = studied + candidateTexts
        assertTrue(forbidden.all { token -> rawBlobs.none { it.contains(token) } })
        coroutineContext.cancelChildren()
    }

    @Test
    fun mc5c_existingBlockAgnosticRecovery_recoversAMainMemoryOrphan() = runTest {
        // MC5c needs no new recovery: the MemoryRootScreen scanner (deferredRecallSessionRecovery) is
        // block-agnostic, so a stale MAIN-session (MEMORY) STARTED orphan is recovered to ABANDONED too.
        val sessionsBacking = InMemorySecureKeyValueStore()
        val store = testSessionRecordStore(sessionsBacking, subject = "u1")
        store.upsert(
            SessionRecord(
                sessionId = "old-main",
                block = ScoreModuleId.MEMORY,
                status = SessionStatus.STARTED,
                startedAtMs = 0L,
                updatedAtMs = 0L,
                completedTrials = 0,
                plannedTrials = 1,
                activeDurationMs = 0L,
            ),
        )
        deferredRecallSessionRecovery(sessionsBacking, InMemorySecureKeyValueStore(), EpochClock { 1_000_000L }, subject = "u1").recover()
        assertEquals(SessionStatus.ABANDONED, store.find("old-main")?.status)
    }
}

/** Captures every lifecycle call (and can gate onFinalize/onAbandon) so hook wiring and ordering are assertable. */
private class MainRecordingRecorder(
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

    override suspend fun onStart(
        sessionId: String,
        block: ScoreModuleId,
        startedAtMs: Long,
        plannedTrials: Int,
        identity: ReproducibleSessionIdentity?,
    ) = SessionRecordUpsertOutcome.Stored.also {
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
private class MainThrowingSessionRecordStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = error("boom")
    override suspend fun find(sessionId: String): SessionRecord? = error("boom")
    override suspend fun all(): List<SessionRecord> = error("boom")
    override suspend fun delete(sessionId: String) = error("boom")
}
