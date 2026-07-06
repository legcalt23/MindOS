package com.mindos.app.features.mindboost.memory.presentation

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
import com.mindos.app.features.mindboost.memory.di.deferredRecallSessionRecorder
import com.mindos.app.features.mindboost.memory.domain.adaptive.DefaultMemoryAdaptiveEngine
import com.mindos.app.features.mindboost.memory.domain.antiplateau.InterventionPlanner
import com.mindos.app.features.mindboost.memory.domain.antiplateau.PlateauDetector
import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
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
import com.mindos.app.features.mindboost.memory.presentation.state.DeferredRecallUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MC4a: write-ahead lifecycle hooks on [DeferredRecallSessionStateHolder]. STARTED fires only for a REAL
 * session (a due item + a valid discrimination test) tagged [ScoreModuleId.EPISODIC_MEMORY]; a NothingDue run
 * writes nothing. FINALIZED runs after the module's own persistence and BEFORE Finished is exposed; abort()
 * marks a started-but-unfinished session ABANDONED. The opaque id is stable and separate from the internal
 * "$userId:delayed:$now" scoring id. No word, lure, selection, hit count, or any episodic content reaches the
 * recorder — only lifecycle primitives.
 */
class DeferredRecallSessionRecorderWiringTest {

    private val content = defaultMemoryContent()
    private val deferred = InMemoryDeferredRecallRepository()
    private val deferredResult = InMemoryDeferredResultStore()
    private val now = 100_000_000_000L
    private val dayMillis = 24L * 3_600_000L
    private var clockMs = now

    private fun graph() = MemoryGraph(
        content = content,
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
        profileRepository = InMemoryMemoryProfileRepository(),
        sessionHistoryRepository = InMemoryMemorySessionHistoryRepository(),
        deferredRecallRepository = deferred,
        recentItemRegistryRepository = InMemoryRecentItemRegistryRepository(),
        spanResultStore = InMemorySpanResultStore(),
        pairsResultStore = InMemoryPairsResultStore(),
        deferredResultStore = deferredResult,
        epochClock = EpochClock { clockMs },
    )

    private suspend fun seedDueItem(count: Int = 5): List<String> {
        val studied = content.trainingWordPool().items.take(count)
        deferred.save(
            "u1",
            listOf(
                DeferredRecallItem(
                    id = "u1:1",
                    exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                    language = Language.SPANISH_ES,
                    studiedWordIds = studied.map { it.id },
                    studiedAtEpoch = now - dayMillis, // 24h elapsed → DUE
                ),
            ),
        )
        return studied.map { it.id.value }
    }

    @Test
    fun nothingDue_writesNoStartedRecord() = runTest {
        val rec = RecordingRecorder()
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.NothingDue>(h.uiState)
        assertTrue(rec.starts.isEmpty(), "NothingDue must not emit STARTED")
        assertTrue(rec.finalizes.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun dueSession_emitsStartEpisodicMemory_thenFinalize_noCheckpointNoAbandon() = runTest {
        val studiedIds = seedDueItem().toSet()
        val rec = RecordingRecorder()
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        assertEquals(1, rec.starts.size)
        assertEquals(ScoreModuleId.EPISODIC_MEMORY, rec.starts.single().block)
        assertEquals("sess_fixed", rec.starts.single().sessionId)
        assertEquals(1, rec.starts.single().plannedTrials)

        h.submitRecall(recall.candidates.filter { it.id in studiedIds }.map { it.id }.toSet())
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.Finished>(h.uiState)
        assertEquals(1, rec.finalizes.size)
        assertEquals("sess_fixed", rec.finalizes.single().sessionId)
        assertEquals(1, rec.finalizes.single().completedTrials)
        assertEquals(0, rec.checkpoints)
        assertTrue(rec.abandons.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun finished_isNotExposedUntilFinalizePersists() = runTest {
        seedDueItem()
        val gate = CompletableDeferred<Unit>()
        val rec = RecordingRecorder(finalizeGate = gate)
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        h.submitRecall(recall.candidates.map { it.id }.toSet())
        advanceUntilIdle()
        // onFinalize is gated → state must still be Recall (never Finished) and no finalize recorded yet.
        assertIs<DeferredRecallUiState.Recall>(h.uiState)
        assertTrue(rec.finalizes.isEmpty())

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("start:sess_fixed", "finalize:sess_fixed"), rec.events)
        assertIs<DeferredRecallUiState.Finished>(h.uiState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun activeDuration_isMeasuredFromRecallOnset() = runTest {
        seedDueItem()
        val rec = RecordingRecorder()
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        clockMs = now + 5_000 // time spent recalling
        h.submitRecall(recall.candidates.map { it.id }.toSet())
        advanceUntilIdle()
        assertEquals(5_000L, rec.finalizes.single().activeDurationMs)
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_ordersStartThenAbandonThenAborted() = runTest {
        seedDueItem()
        val rec = RecordingRecorder()
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.Recall>(h.uiState)
        h.abortAndAwait()
        // Ordered: STARTED persisted, then ABANDONED, and Aborted exposed only after onAbandon returned.
        assertEquals(listOf("start:sess_fixed", "abandon:sess_fixed"), rec.events)
        assertIs<DeferredRecallUiState.Aborted>(h.uiState)
        assertTrue(rec.finalizes.isEmpty())
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_doesNotExposeAbortedUntilOnAbandonCompletes() = runTest {
        seedDueItem()
        val gate = CompletableDeferred<Unit>()
        val rec = RecordingRecorder(abandonGate = gate)
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.Recall>(h.uiState)
        val abortJob = launch { h.abortAndAwait() }
        advanceUntilIdle()
        // onAbandon is gated → Aborted must NOT be exposed yet and onAbandon has not completed.
        assertTrue(rec.abandons.isEmpty())
        assertIs<DeferredRecallUiState.Recall>(h.uiState)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("sess_fixed"), rec.abandons)
        assertIs<DeferredRecallUiState.Aborted>(h.uiState)
        abortJob.join()
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_withFailingRecorder_stillReachesAborted() = runTest {
        seedDueItem()
        val failing = SecureSessionRecorder(ThrowingSessionRecordStore(), EpochClock { now })
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = failing,
            sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.Recall>(h.uiState)
        h.abortAndAwait()
        // A total recorder swallows the storage error (best-effort) → the exit still reaches Aborted.
        assertIs<DeferredRecallUiState.Aborted>(h.uiState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_beforeRealSession_createsNoAbandonedRecord() = runTest {
        // No due item → NothingDue → aborting must not fabricate an ABANDONED record (no sessionId exists).
        val rec = RecordingRecorder()
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.NothingDue>(h.uiState)
        h.abortAndAwait()
        assertTrue(rec.starts.isEmpty())
        assertTrue(rec.abandons.isEmpty(), "a pre-session abort must not create an artificial ABANDONED")
        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_afterFinished_doesNotAbandonAFinalizedSession() = runTest {
        seedDueItem()
        val rec = RecordingRecorder()
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        h.submitRecall(recall.candidates.map { it.id }.toSet())
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.Finished>(h.uiState)
        h.abortAndAwait() // user backs out after finishing
        assertEquals(1, rec.finalizes.size)
        assertTrue(rec.abandons.isEmpty(), "a finalized session is never abandoned")
        coroutineContext.cancelChildren()
    }

    @Test
    fun sessionId_isOpaque_stable_andSeparateFromInternalScoringId() = runTest {
        seedDueItem()
        val rec = RecordingRecorder()
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = rec, // real random generator
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        h.submitRecall(recall.candidates.map { it.id }.toSet())
        advanceUntilIdle()
        val startId = rec.starts.single().sessionId
        assertEquals(startId, rec.finalizes.single().sessionId, "id stable across the run")
        assertTrue(startId.startsWith("sess_"))
        assertTrue(!startId.contains("u1"))
        assertTrue(!startId.contains("delayed"))
        coroutineContext.cancelChildren()
    }

    @Test
    fun recorderStorageFailure_doesNotBreakTheRoundOrMemoryPersistence() = runTest {
        val studiedIds = seedDueItem().toSet()
        val failing = SecureSessionRecorder(ThrowingSessionRecordStore(), EpochClock { now })
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = failing,
            sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        h.submitRecall(recall.candidates.filter { it.id in studiedIds }.map { it.id }.toSet())
        advanceUntilIdle()
        // The round completes and the module's OWN persistence is intact despite the recorder exploding.
        assertIs<DeferredRecallUiState.Finished>(h.uiState)
        assertEquals(5, assertNotNull(h.lastResult).outcome.hits)
        assertEquals(1, deferredResult.load("u1").outcomes.size)
        coroutineContext.cancelChildren()
    }

    @Test
    fun noOpDefaultRecorder_keepsExistingBehaviourAndScoring() = runTest {
        val studiedIds = seedDueItem().toSet()
        // Default recorder (NoOp) — the pre-existing production-compatible path.
        val h = DeferredRecallSessionStateHolder(graph(), "u1", this, seed = 7)
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        h.submitRecall(recall.candidates.filter { it.id in studiedIds }.map { it.id }.toSet())
        advanceUntilIdle()
        val result = assertNotNull(h.lastResult)
        assertEquals(5, result.outcome.studiedCount)
        assertEquals(5, result.outcome.hits)
        assertTrue(deferred.load("u1").isEmpty(), "the tested item is still consumed")
        assertEquals(1, deferredResult.load("u1").outcomes.size)
        coroutineContext.cancelChildren()
    }

    @Test
    fun sessionRecordBlob_containsNoWordsSelectionsOrEpisodicContent() = runTest {
        val studiedIds = seedDueItem().toSet()
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = SecureSessionRecorder(SecureSessionRecordStore(sessionsBacking), EpochClock { now })
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = recorder,
            sessionIdGenerator = SessionIdGenerator { "sess_fixed" },
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        val candidateTexts = recall.candidates.map { it.text }
        h.submitRecall(recall.candidates.filter { it.id in studiedIds }.map { it.id }.toSet())
        advanceUntilIdle()

        val persisted = SecureSessionRecordStore(sessionsBacking).find("sess_fixed")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.EPISODIC_MEMORY, persisted?.block)
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        assertTrue(rawBlobs.isNotEmpty())
        // No studied/candidate word id or text (the raw episodic content) may appear in the session blob.
        val forbidden = studiedIds + candidateTexts
        assertTrue(forbidden.all { token -> rawBlobs.none { it.contains(token) } })
        coroutineContext.cancelChildren()
    }

    @Test
    fun productionFactory_persistsFinalizedEpisodic_overSessionsStore_withNoContent() = runTest {
        // The MC4b production factory — same wiring rememberDeferredRecallSessionStateHolder mounts.
        val studiedIds = seedDueItem().toSet()
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = deferredRecallSessionRecorder(sessionsBacking, EpochClock { now })
        val h = DeferredRecallSessionStateHolder(
            graph(), "u1", this, seed = 7, recorder = recorder,
            sessionIdGenerator = SessionIdGenerator { "sess_prod" },
        )
        h.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(h.uiState)
        val candidateTexts = recall.candidates.map { it.text }
        h.submitRecall(recall.candidates.filter { it.id in studiedIds }.map { it.id }.toSet())
        advanceUntilIdle()

        val persisted = SecureSessionRecordStore(sessionsBacking).find("sess_prod")
        assertEquals(SessionStatus.FINALIZED, persisted?.status)
        assertEquals(ScoreModuleId.EPISODIC_MEMORY, persisted?.block)
        val rawBlobs = sessionsBacking.keys("").mapNotNull { sessionsBacking.readValueOrNull(it) }
        val forbidden = studiedIds + candidateTexts
        assertTrue(forbidden.all { token -> rawBlobs.none { it.contains(token) } })
        coroutineContext.cancelChildren()
    }
}

/** Captures every lifecycle call (and can gate onFinalize) so hook wiring and ordering are assertable. */
private class RecordingRecorder(
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

/** A record store that fails on every call, to prove the recorder is total and never breaks a round. */
private class ThrowingSessionRecordStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = error("boom")
    override suspend fun find(sessionId: String): SessionRecord? = error("boom")
    override suspend fun all(): List<SessionRecord> = error("boom")
    override suspend fun delete(sessionId: String) = error("boom")
}
