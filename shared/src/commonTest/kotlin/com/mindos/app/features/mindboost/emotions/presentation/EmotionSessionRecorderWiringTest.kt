@file:Suppress("MagicNumber", "MaxLineLength", "TooManyFunctions") // Fixtures + dense drive/graph wiring under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.InMemorySessionRecordStore
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.emotions.data.local.EmotionRepository
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.di.emotionSessionRecorder
import com.mindos.app.features.mindboost.emotions.di.emotionSessionRecovery
import com.mindos.app.features.mindboost.emotions.domain.adaptive.AdaptiveAction
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveDecision
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveInput
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionLabel
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readValueOrNull
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * MC8a — write-ahead SessionRecord lifecycle wiring for Emociones. The recorder receives ONLY lifecycle
 * primitives; COMPLETED/SAFETY_STOP → FINALIZED, ABANDONED → ABANDONED; terminal UI (Result/Exited) is exposed
 * only AFTER the canonical transition; own summary persistence precedes it; and no raw emotional content
 * reaches `mindboost_sessions`.
 */
class EmotionSessionRecorderWiringTest {

    // 1. The menu / pre-session (holder built but never started) must NOT create a STARTED record.
    @Test
    fun menu_before_start_records_nothing() = runTest {
        val recorder = EmotionRecordingRecorder()
        buildHolder(recorder = recorder)
        advanceUntilIdle()
        assertEquals(0, recorder.onStartCount, "no session started → no onStart")
        assertTrue(recorder.events.isEmpty())
        coroutineContext.cancelChildren()
    }

    // 2. A real session begins by recording onStart tagged EMOTIONS.
    @Test
    fun real_session_calls_onStart_with_emotions() = runTest {
        val recorder = EmotionRecordingRecorder()
        val holder = buildHolder(recorder = recorder)
        holder.start()
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.SelfReportBefore)
        assertEquals(1, recorder.onStartCount)
        assertEquals(ScoreModuleId.EMOTIONS, recorder.startBlock)
        assertEquals(listOf("start"), recorder.events)
        coroutineContext.cancelChildren()
    }

    // 3. The session id is opaque, stable across the session, and never derived from the user id.
    @Test
    fun sessionId_is_opaque_and_stable_and_not_derived_from_user() = runTest {
        val recorder = EmotionRecordingRecorder()
        val holder = buildHolder(recorder = recorder)
        driveToResult(holder)
        val id = assertNotNull(recorder.startSessionId)
        assertTrue(id.startsWith("sess_"), "opaque prefixed id, got $id")
        assertFalse(id.contains("u1"), "id must not embed the user id")
        assertEquals(id, recorder.finalizeSessionId, "same id start → finalize")
        coroutineContext.cancelChildren()
    }

    // 4. A normally completed session calls onFinalize (after onStart) and persists FINALIZED.
    @Test
    fun completed_session_calls_onFinalize_and_persists_finalized() = runTest {
        val store = InMemorySessionRecordStore()
        val holder = buildHolder(recorder = SecureSessionRecorder(store, testClock()))
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        val record = store.all().single()
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertEquals(ScoreModuleId.EMOTIONS, record.block)
        assertEquals(1, record.plannedTrials)
        assertEquals(1, record.completedTrials)
        coroutineContext.cancelChildren()
    }

    // 5 & 9. A SAFETY_STOP is a graceful close → onFinalize (never onAbandon); onStart precedes it.
    @Test
    fun safety_stop_calls_onFinalize_not_onAbandon() = runTest {
        val recorder = EmotionRecordingRecorder()
        // Fake engine forces SAFETY_STOP at the first decision; TwoAlt + timeouts arm no overload proxies.
        val holder = buildHolder(
            exerciseId = EmotionExerciseId.EM2_RECUPERACION,
            recorder = recorder,
            adaptiveEngine = SafetyStopEngine,
        )
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(6) { advanceUntilIdle() }
        val state = holder.uiState
        assertTrue(state is EmotionSessionUiState.Result && state.stoppedForSafety, "expected a safety-stop Result, got $state")
        assertEquals(listOf("start", "finalize"), recorder.events)
        assertEquals(0, recorder.onAbandonCount)
        coroutineContext.cancelChildren()
    }

    // 6 & 10. An explicit exit calls onAbandon (after onStart) and persists ABANDONED.
    @Test
    fun abandoned_via_onExit_calls_onAbandon_and_persists_abandoned() = runTest {
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val holder = buildHolder(recorder = recorder)
        holder.start()
        advanceUntilIdle()
        holder.onExit()
        advanceUntilIdle()
        assertEquals(listOf("start", "abandon"), recorder.events)
        assertEquals(SessionStatus.ABANDONED, store.all().single().status)
        assertTrue(holder.uiState is EmotionSessionUiState.Exited)
        coroutineContext.cancelChildren()
    }

    // 7. Result is exposed only AFTER onFinalize; the own summary is saved BEFORE the transition.
    @Test
    fun result_is_not_exposed_before_onFinalize() = runTest {
        val gate = CompletableDeferred<Unit>()
        val recorder = EmotionRecordingRecorder(finalizeGate = gate)
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(recorder = recorder, repository = repository)
        driveToSelfReportAfter(holder)
        holder.submitSelfReport(SelfReport(3, 3, 2))
        advanceUntilIdle() // finalize runs and blocks inside onFinalize (gate held)
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "Result must not appear before onFinalize")
        assertEquals(0, recorder.onFinalizeCount)
        assertTrue(repository.recentSummaries("u1").isNotEmpty(), "own summary persists BEFORE the terminal transition")
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        assertEquals(1, recorder.onFinalizeCount)
        coroutineContext.cancelChildren()
    }

    // 8. Option A: Exited is exposed only AFTER onAbandon (the holder stays mounted until then).
    @Test
    fun exited_is_not_exposed_before_onAbandon() = runTest {
        val gate = CompletableDeferred<Unit>()
        val recorder = EmotionRecordingRecorder(abandonGate = gate)
        val holder = buildHolder(recorder = recorder)
        holder.start()
        advanceUntilIdle()
        holder.onExit()
        advanceUntilIdle() // finalize(ABANDONED) runs and blocks inside onAbandon (gate held)
        assertFalse(holder.uiState is EmotionSessionUiState.Exited, "Exited must not appear before onAbandon")
        assertEquals(0, recorder.onAbandonCount)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Exited)
        assertEquals(1, recorder.onAbandonCount)
        coroutineContext.cancelChildren()
    }

    // 11 & 13. First-wins terminal: an explicit exit AFTER a finalize never abandons a FINALIZED session.
    @Test
    fun onExit_after_finalize_does_not_abandon_a_finalized_session() = runTest {
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val holder = buildHolder(recorder = recorder)
        driveToResult(holder)
        holder.onExit()
        advanceUntilIdle()
        assertEquals(SessionStatus.FINALIZED, store.all().single().status)
        assertEquals(1, recorder.onFinalizeCount)
        assertEquals(0, recorder.onAbandonCount)
        assertFalse(recorder.events.contains("abandon"))
        coroutineContext.cancelChildren()
    }

    // 12. An exit before any real session started never fabricates an ABANDONED record.
    @Test
    fun onExit_before_real_session_records_no_abandon() = runTest {
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val holder = buildHolder(recorder = recorder)
        holder.onExit() // never started
        advanceUntilIdle()
        assertEquals(0, recorder.onAbandonCount)
        assertTrue(store.all().isEmpty(), "no STARTED existed → no record to abandon")
        coroutineContext.cancelChildren()
    }

    // 14. A non-fatal recorder/storage failure never breaks the session (recorder is total by contract).
    @Test
    fun recorder_storage_failure_does_not_break_session() = runTest {
        val holder = buildHolder(recorder = SecureSessionRecorder(ThrowingRecordStore, testClock()))
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result, "session must complete despite storage failure")
        coroutineContext.cancelChildren()
    }

    // 15. The NoOp default keeps a session working with zero opt-in.
    @Test
    fun noop_default_keeps_session_working() = runTest {
        val holder = buildHolder(recorder = NoOpSessionRecorder)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        coroutineContext.cancelChildren()
    }

    // 16 & 17. The block's own summary is written exactly once and coexists with the write-ahead record;
    // the scoring/history authority (the emotion summary) is untouched by the recorder.
    @Test
    fun own_summary_is_not_duplicated_and_coexists_with_record() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val store = InMemorySessionRecordStore()
        val holder = buildHolder(recorder = SecureSessionRecorder(store, testClock()), repository = repository)
        driveToResult(holder)
        assertEquals(1, repository.recentSummaries("u1").size, "exactly one domain summary")
        assertEquals(1, store.all().size, "exactly one write-ahead record")
        coroutineContext.cancelChildren()
    }

    // 18. The persisted SessionRecord blob carries no raw emotional content — only opaque lifecycle.
    @Test
    fun session_record_blob_contains_no_emotional_content() = runTest {
        val sessionsStore = InMemorySecureKeyValueStore()
        val holder = buildHolder(recorder = SecureSessionRecorder(SecureSessionRecordStore(sessionsStore), testClock()))
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(valence = 4, arousal = 5, intensity = 3, label = EmotionLabel.FRUSTRACION))
        driveRemainderToResult(holder)
        val blobs = sessionsStore.keys("session_record:").mapNotNull { sessionsStore.readValueOrNull(it) }
        assertTrue(blobs.isNotEmpty(), "a record must have been written")
        blobs.forEach { blob ->
            assertFalse(blob.contains("u1"), "no user id in the record blob")
            assertFalse(blob.contains("FRUSTRACION"), "no self-report label")
            assertFalse(blob.contains("valence"), "no valence field")
            assertFalse(blob.contains("arousal"), "no arousal field")
            assertTrue(blob.contains("EMOTIONS") && blob.contains("FINALIZED"), "record IS a real lifecycle row")
        }
        coroutineContext.cancelChildren()
    }

    // ---- MC8b: production factory (emotionSessionRecorder over the real SecureSessionRecordStore) ----

    // 1,3,5,6,7. The production factory persists a FINALIZED EMOTIONS record (1/1) with no emotional content.
    @Test
    fun productionFactory_completed_persists_finalized_emotions_without_content() = runTest {
        val sessionsStore = InMemorySecureKeyValueStore()
        val holder = buildHolder(recorder = emotionSessionRecorder(sessionsStore, testClock()))
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(valence = 4, arousal = 5, intensity = 3, label = EmotionLabel.FRUSTRACION))
        driveRemainderToResult(holder)
        val record = SecureSessionRecordStore(sessionsStore).all().single()
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertEquals(ScoreModuleId.EMOTIONS, record.block)
        assertEquals(1, record.plannedTrials)
        assertEquals(1, record.completedTrials)
        val blob = sessionsStore.keys("session_record:").mapNotNull { sessionsStore.readValueOrNull(it) }.single()
        assertFalse(blob.contains("u1") || blob.contains("FRUSTRACION") || blob.contains("valence"), "no emotional content")
        coroutineContext.cancelChildren()
    }

    // 4. A safety-stop persists FINALIZED (a graceful close, indistinguishable from a normal completion).
    @Test
    fun productionFactory_safety_stop_persists_finalized() = runTest {
        val sessionsStore = InMemorySecureKeyValueStore()
        val holder = buildHolder(
            exerciseId = EmotionExerciseId.EM2_RECUPERACION,
            recorder = emotionSessionRecorder(sessionsStore, testClock()),
            adaptiveEngine = SafetyStopEngine,
        )
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(6) { advanceUntilIdle() }
        val record = SecureSessionRecordStore(sessionsStore).all().single()
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertEquals(ScoreModuleId.EMOTIONS, record.block)
        coroutineContext.cancelChildren()
    }

    // ---- MC8c: local orphan recovery (emotionSessionRecovery over mindboost_sessions) ----

    // 1-4. A stale EMOTIONS orphan is marked ABANDONED; a fresh heartbeat and an old FINALIZED are untouched;
    // the scan is idempotent.
    @Test
    fun recovery_marks_stale_emotions_orphan_abandoned_and_is_idempotent() = runTest {
        val sessionsStore = InMemorySecureKeyValueStore()
        val recordStore = SecureSessionRecordStore(sessionsStore)
        recordStore.upsert(emotionRecord("stale", SessionStatus.STARTED, updatedAtMs = 0L))
        recordStore.upsert(emotionRecord("fresh", SessionStatus.STARTED, updatedAtMs = 999_000L))
        recordStore.upsert(emotionRecord("done", SessionStatus.FINALIZED, updatedAtMs = 0L))

        val scanner = emotionSessionRecovery(sessionsStore, EpochClock { 1_000_000L })
        assertEquals(1, scanner.recover(), "only the stale STARTED orphan is recovered")
        assertEquals(SessionStatus.ABANDONED, assertNotNull(recordStore.find("stale")).status)
        assertEquals(SessionStatus.STARTED, assertNotNull(recordStore.find("fresh")).status)
        assertEquals(SessionStatus.FINALIZED, assertNotNull(recordStore.find("done")).status)
        assertEquals(0, scanner.recover(), "second scan is a no-op (idempotent)")
        coroutineContext.cancelChildren()
    }

    // 5,6,7. Recovery operates only on mindboost_sessions and never touches the emotions namespace (summary /
    // raw emotional content).
    @Test
    fun recovery_never_touches_emotions_namespace() = runTest {
        val sessionsStore = InMemorySecureKeyValueStore()
        SecureSessionRecordStore(sessionsStore).upsert(emotionRecord("stale", SessionStatus.STARTED, updatedAtMs = 0L))
        val emotionsStore = InMemorySecureKeyValueStore()
        emotionsStore.put("emotion_summary:u1:1", "valence=4;label=FRUSTRACION")

        emotionSessionRecovery(sessionsStore, EpochClock { 1_000_000L }).recover()

        assertEquals("valence=4;label=FRUSTRACION", emotionsStore.readValueOrNull("emotion_summary:u1:1"), "the emotions summary is left untouched")
        coroutineContext.cancelChildren()
    }

    // ---- fixtures ----

    private var idCounter = 0

    private fun emotionRecord(id: String, status: SessionStatus, updatedAtMs: Long): SessionRecord =
        SessionRecord(
            sessionId = id,
            block = ScoreModuleId.EMOTIONS,
            status = status,
            startedAtMs = 0L,
            updatedAtMs = updatedAtMs,
            completedTrials = if (status == SessionStatus.FINALIZED) 1 else 0,
            plannedTrials = 1,
            activeDurationMs = 0L,
        )

    private fun testClock(): EpochClock {
        var now = 1_000L
        return EpochClock { now += 50; now }
    }

    private fun emotionGraph(
        repository: EmotionRepository,
        clock: EpochClock,
        adaptiveEngine: EmotionAdaptiveEngine,
    ): EmotionGraph = EmotionGraph(
        generators = EmotionExerciseGenerators.registry,
        metricsEngine = DefaultEmotionMetricsEngine(),
        assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
        adaptiveEngine = adaptiveEngine,
        overloadDetector = DefaultOverloadDetector(),
        pauseGovernor = PauseGovernor(),
        feedbackBuilder = EmotionFeedbackBuilder(),
        repository = repository,
        epochClock = clock,
        monotonicClock = SystemMonotonicClock(),
    )

    private fun TestScope.buildHolder(
        exerciseId: EmotionExerciseId = EmotionExerciseId.EM1_PAUSA,
        recorder: SessionRecorder = NoOpSessionRecorder,
        repository: EmotionRepository = LocalEmotionRepository(InMemorySecureKeyValueStore()),
        adaptiveEngine: EmotionAdaptiveEngine = DefaultEmotionAdaptiveEngine(),
    ): EmotionSessionStateHolder {
        val graph = emotionGraph(repository, testClock(), adaptiveEngine)
        return EmotionSessionStateHolder(
            graph = graph,
            exerciseId = exerciseId,
            userId = "u1",
            scope = this,
            seed = 1L,
            recorder = recorder,
            sessionIdGenerator = SessionIdGenerator { "sess_${idCounter++}" },
        )
    }

    private fun TestScope.driveToResult(holder: EmotionSessionStateHolder) {
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        driveRemainderToResult(holder)
    }

    private fun TestScope.driveRemainderToResult(holder: EmotionSessionStateHolder) {
        repeat(16) {
            advanceUntilIdle()
            when (val s = holder.uiState) {
                is EmotionSessionUiState.MidCheckIn -> holder.submitCheckInIntensity(2)
                is EmotionSessionUiState.MicroPause -> holder.onResumeFromMicroPause()
                is EmotionSessionUiState.SelfReportAfter -> holder.submitSelfReport(SelfReport(3, 3, 2))
                else -> if (s is EmotionSessionUiState.Result) return
            }
        }
        advanceUntilIdle()
    }

    private fun TestScope.driveToSelfReportAfter(holder: EmotionSessionStateHolder) {
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(16) {
            advanceUntilIdle()
            when (holder.uiState) {
                is EmotionSessionUiState.MidCheckIn -> holder.submitCheckInIntensity(2)
                is EmotionSessionUiState.MicroPause -> holder.onResumeFromMicroPause()
                is EmotionSessionUiState.SelfReportAfter -> return
                else -> Unit
            }
        }
    }
}

/**
 * Records lifecycle calls in order and (optionally) delegates to a real recorder so persistence AND ordering can
 * be asserted together. Optional gates block a terminal op so terminal-after-transition ordering is provable.
 */
private class EmotionRecordingRecorder(
    private val delegate: SessionRecorder? = null,
    private val finalizeGate: CompletableDeferred<Unit>? = null,
    private val abandonGate: CompletableDeferred<Unit>? = null,
) : SessionRecorder {
    val events = mutableListOf<String>()
    var startBlock: ScoreModuleId? = null
    var startSessionId: String? = null
    var finalizeSessionId: String? = null
    var onStartCount = 0
    var onFinalizeCount = 0
    var onAbandonCount = 0

    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
        onStartCount++
        startBlock = block
        startSessionId = sessionId
        events += "start"
        delegate?.onStart(sessionId, block, startedAtMs, plannedTrials)
    }

    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        events += "checkpoint"
        delegate?.onCheckpoint(sessionId, completedTrials, activeDurationMs)
    }

    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
        finalizeGate?.await()
        onFinalizeCount++
        finalizeSessionId = sessionId
        events += "finalize"
        delegate?.onFinalize(sessionId, completedTrials, activeDurationMs)
    }

    override suspend fun onAbandon(sessionId: String) {
        abandonGate?.await()
        onAbandonCount++
        events += "abandon"
        delegate?.onAbandon(sessionId)
    }
}

/** A store whose every op throws, to prove the SecureSessionRecorder swallows storage failures. */
private object ThrowingRecordStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord): Unit = error("boom")
    override suspend fun find(sessionId: String): SessionRecord? = error("boom")
    override suspend fun all(): List<SessionRecord> = error("boom")
    override suspend fun delete(sessionId: String): Unit = error("boom")
}

/** Deterministically forces a SAFETY_STOP at the first adaptive decision (test double, not production). */
private object SafetyStopEngine : EmotionAdaptiveEngine {
    override fun decide(input: EmotionAdaptiveInput): EmotionAdaptiveDecision =
        EmotionAdaptiveDecision(AdaptiveAction.SAFETY_STOP, input.currentDifficulty, input.intensity)
}
