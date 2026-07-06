@file:Suppress("MagicNumber", "MaxLineLength", "TooManyFunctions", "LargeClass") // Comprehensive lifecycle suite + fixtures.

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
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
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
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEventType
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    // ---- MC8d: first-wins for the FULL terminal (own summary + aggregate + SessionRecord + UI) ----

    // 1 & 5. A normal completion that WINS a concurrent onExit: exactly one, non-abandoned summary; FINALIZED;
    // onAbandon never runs; the UI stays Result (never overwritten by Exited).
    @Test
    fun completed_wins_over_concurrent_onExit() = runTest {
        val gate = CompletableDeferred<Unit>()
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()), finalizeGate = gate)
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(recorder = recorder, repository = repository)
        driveToSelfReportAfter(holder)
        holder.submitSelfReport(SelfReport(3, 3, 2))
        advanceUntilIdle() // finalize(COMPLETED) holds terminalMutex, blocked inside onFinalize (gate held)
        holder.onExit()
        advanceUntilIdle() // finalize(ABANDONED) parks on terminalMutex
        assertEquals(1, repository.recentSummaries("u1").size, "only the completed summary is saved so far")
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "no terminal UI while the terminal is in flight")
        assertFalse(holder.uiState is EmotionSessionUiState.Exited)
        gate.complete(Unit)
        advanceUntilIdle()
        val summary = repository.recentSummaries("u1").single()
        assertTrue(summary.completed, "the winning summary stays completed")
        assertFalse(summary.abandoned, "onExit must NOT overwrite it as abandoned")
        assertEquals(SessionStatus.FINALIZED, store.all().single().status)
        assertEquals(0, recorder.onAbandonCount, "the losing ABANDONED never abandons")
        assertEquals(1, recorder.onFinalizeCount, "exactly one finalize")
        assertEquals(listOf("start", "finalize"), recorder.events)
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "aggregate counts the one completion once")
        assertTrue(holder.uiState is EmotionSessionUiState.Result, "Result is not overwritten by Exited")
        coroutineContext.cancelChildren()
    }

    // 2. A safety stop that WINS a concurrent onExit: one non-abandoned summary; FINALIZED; no onAbandon; the
    // safe-close Result stays visible (never Exited).
    @Test
    fun safety_stop_wins_over_concurrent_onExit() = runTest {
        val gate = CompletableDeferred<Unit>()
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()), finalizeGate = gate)
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(
            exerciseId = EmotionExerciseId.EM2_RECUPERACION,
            recorder = recorder,
            repository = repository,
            adaptiveEngine = SafetyStopEngine,
        )
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(6) { advanceUntilIdle() } // finalize(SAFETY_STOP) parks inside onFinalize (gate held)
        holder.onExit()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        val summaries = repository.recentSummaries("u1")
        assertEquals(1, summaries.size, "exactly one summary")
        assertFalse(summaries.single().abandoned, "the safety-stop summary is not overwritten as abandoned")
        assertTrue(summaries.single().safetyEvents.any { it.type == SafetyEventType.SAFETY_STOP.name }, "safety event present")
        assertEquals(SessionStatus.FINALIZED, store.all().single().status)
        assertEquals(1, recorder.onFinalizeCount)
        assertEquals(0, recorder.onAbandonCount)
        val state = holder.uiState
        assertTrue(state is EmotionSessionUiState.Result && state.stoppedForSafety, "safe-close Result stays, got $state")
        coroutineContext.cancelChildren()
    }

    // 3. When onExit wins (it precedes the final terminal): one abandoned summary; ABANDONED; onFinalize never
    // runs; the UI is Exited.
    @Test
    fun onExit_wins_when_it_precedes_the_final_terminal() = runTest {
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(recorder = recorder, repository = repository)
        holder.start()
        advanceUntilIdle()
        holder.onExit() // before the session could complete
        advanceUntilIdle()
        val summary = repository.recentSummaries("u1").single()
        assertTrue(summary.abandoned, "the winning terminal is abandoned")
        assertFalse(summary.completed)
        assertEquals(SessionStatus.ABANDONED, store.all().single().status)
        assertEquals(0, recorder.onFinalizeCount, "no finalize wins after an abandon")
        assertTrue(holder.uiState is EmotionSessionUiState.Exited)
        coroutineContext.cancelChildren()
    }

    // 4. Double onExit applies the terminal exactly once: one summary, one onAbandon, one record.
    @Test
    fun double_onExit_does_not_duplicate_summary_or_abandon() = runTest {
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(recorder = recorder, repository = repository)
        holder.start()
        advanceUntilIdle()
        holder.onExit()
        holder.onExit()
        advanceUntilIdle()
        assertEquals(1, repository.recentSummaries("u1").size)
        assertEquals(1, recorder.onAbandonCount, "exactly one abandon")
        assertEquals(listOf("start", "abandon"), recorder.events)
        assertEquals(1, store.all().size)
        assertEquals(SessionStatus.ABANDONED, store.all().single().status)
        assertNull(repository.readAggregate("u1"), "an abandon never advances the aggregate")
        assertTrue(holder.uiState is EmotionSessionUiState.Exited)
        coroutineContext.cancelChildren()
    }

    // ---- MC8e: terminal hardening (pre-start, aggregate semantics, save-failure, single-use, duration) ----

    // P0.1 — a created-but-never-started holder must not persist a summary, aggregate, or SessionRecord.
    @Test
    fun onExit_before_start_persists_no_emotion_summary_or_aggregate() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val store = InMemorySessionRecordStore()
        val holder = buildHolder(recorder = SecureSessionRecorder(store, testClock()), repository = repository)
        holder.onExit() // never started
        advanceUntilIdle()
        assertTrue(repository.recentSummaries("u1").isEmpty(), "no summary for a never-started holder")
        assertNull(repository.readAggregate("u1"), "no aggregate for a never-started holder")
        assertTrue(store.all().isEmpty(), "no SessionRecord")
        assertTrue(holder.uiState is EmotionSessionUiState.Exited, "exit is UI-local only")
        coroutineContext.cancelChildren()
    }

    // P0.2 — only a COMPLETED session advances the aggregate, and exactly once.
    @Test
    fun completed_session_increments_valid_sessions_once() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        driveToResult(buildHolder(repository = repository))
        assertEquals(1, assertNotNull(repository.readAggregate("u1")).sessionsCompleted, "one completion counts once")
        coroutineContext.cancelChildren()
    }

    // P0.2 — an abandon never advances the aggregate (tier/confidence/sessionsCompleted).
    @Test
    fun abandoned_session_does_not_advance_aggregate() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        holder.start(); advanceUntilIdle(); holder.onExit(); advanceUntilIdle()
        assertNull(repository.readAggregate("u1"), "an abandon must not create or advance the aggregate")
        coroutineContext.cancelChildren()
    }

    // P0.2 — a safety stop is a safe close, not calibration progress: it never advances the aggregate.
    @Test
    fun safety_stop_does_not_advance_aggregate() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2)); repeat(6) { advanceUntilIdle() }
        assertTrue((holder.uiState as EmotionSessionUiState.Result).stoppedForSafety)
        assertNull(repository.readAggregate("u1"), "a safety-stop must not advance calibration")
        coroutineContext.cancelChildren()
    }

    // P0.2 — abandoning many times cannot make the user look more calibrated.
    @Test
    fun repeated_abandons_never_advance_progress() = runTest {
        val store = InMemorySecureKeyValueStore()
        repeat(5) {
            val holder = buildHolder(repository = LocalEmotionRepository(store))
            holder.start(); advanceUntilIdle(); holder.onExit(); advanceUntilIdle()
        }
        assertNull(LocalEmotionRepository(store).readAggregate("u1"), "repeated abandons never advance the aggregate")
        coroutineContext.cancelChildren()
    }

    // P0.3 — a summary save failure must not show success nor finalize the SessionRecord (recovery backstop).
    @Test
    fun summary_save_failure_does_not_show_success_or_finalize_record() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        val store = InMemorySessionRecordStore()
        val holder = buildHolder(recorder = SecureSessionRecorder(store, testClock()), repository = repository)
        driveToResult(holder)
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "no false success when the canonical summary is lost")
        assertTrue(repository.summaries.isEmpty(), "summary genuinely not saved")
        assertTrue(store.all().none { it.status == SessionStatus.FINALIZED }, "record stays pre-terminal for recovery")
        coroutineContext.cancelChildren()
    }

    // P0.3 — a recentSummaries read failure skips the aggregate update; it never silently resets progress to 1.
    @Test
    fun recent_summaries_read_failure_does_not_reset_aggregate() = runTest {
        val repository = FakeEmotionRepository(failRecentRead = true)
        repository.savedAggregate = EmotionAggregateDto(
            sessionsCompleted = 7, tier = "STABLE", dataSufficiency = 0.9, safetyLoad = 0.0,
            transferConfidence = "NOT_DEMONSTRATED", confidence = 0.9, trend = "UNKNOWN", updatedAtMillis = 1L,
        )
        driveToResult(buildHolder(repository = repository))
        assertEquals(7, repository.readAggregate("u1")?.sessionsCompleted, "a read failure never resets progress to 1")
        coroutineContext.cancelChildren()
    }

    // P1.4 — a DECIDED safety stop that is in-flight survives onExit's cooperative cancellation; never abandoned.
    @Test
    fun decided_safety_stop_survives_onExit_cancellation() = runTest {
        val gate = CompletableDeferred<Unit>()
        val recorder = EmotionRecordingRecorder(finalizeGate = gate)
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, recorder = recorder, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(6) { advanceUntilIdle() } // finalize(SAFETY_STOP) decided, parked inside onFinalize
        holder.onExit() // cancels sessionJob mid-terminal
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, recorder.onFinalizeCount)
        assertEquals(0, recorder.onAbandonCount, "onExit's cancel cannot turn a decided safety-stop into an abandon")
        assertFalse(repository.recentSummaries("u1").single().abandoned)
        assertTrue((holder.uiState as EmotionSessionUiState.Result).stoppedForSafety)
        coroutineContext.cancelChildren()
    }

    // P1.5 — single-use: start() after a Result is a no-op (no restart, no second session).
    @Test
    fun start_after_result_is_noop() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        holder.start()
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Result, "start after a terminal does not restart")
        assertEquals(1, repository.recentSummaries("u1").size, "no second session was run")
        coroutineContext.cancelChildren()
    }

    // P1.5 — single-use: start() after Exited is a no-op.
    @Test
    fun start_after_exited_is_noop() = runTest {
        val holder = buildHolder()
        holder.start(); advanceUntilIdle(); holder.onExit(); advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Exited)
        holder.start()
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Exited, "start after exit does not restart")
        coroutineContext.cancelChildren()
    }

    // P1.5 — a double start runs exactly one real session.
    @Test
    fun double_start_runs_only_one_session() = runTest {
        val recorder = EmotionRecordingRecorder()
        val holder = buildHolder(recorder = recorder)
        holder.start()
        holder.start() // ignored — single-use
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        driveRemainderToResult(holder)
        assertEquals(1, recorder.onStartCount, "only one session actually started")
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        coroutineContext.cancelChildren()
    }

    // P1.6 — the SessionRecord's active duration is the training time captured once, not persistence latency.
    @Test
    fun record_terminal_duration_excludes_persistence_latency() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val store = InMemorySessionRecordStore()
        driveToResult(buildHolder(recorder = SecureSessionRecorder(store, testClock()), repository = repository))
        val summary = repository.recentSummaries("u1").single()
        val record = store.all().single()
        assertEquals(summary.durationMs, record.activeDurationMs, "record duration == summary duration (captured once)")
        assertTrue(record.activeDurationMs >= 0)
        coroutineContext.cancelChildren()
    }

    // P1.7 — a safety stop is an implicit third state: completed=false, abandoned=false, disambiguated ONLY by an
    // explicit SAFETY_STOP safety event (until a dedicated endReason DTO field exists — see report follow-up).
    @Test
    fun safety_stop_is_represented_as_non_completed_non_abandoned_with_explicit_safety_event() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2)); repeat(6) { advanceUntilIdle() }
        val summary = repository.recentSummaries("u1").single()
        assertFalse(summary.completed)
        assertFalse(summary.abandoned)
        assertTrue(summary.safetyEvents.any { it.type == SafetyEventType.SAFETY_STOP.name }, "safety stop is identifiable via its event")
        coroutineContext.cancelChildren()
    }

    // P2.8 — late inputs after a terminal never mutate the terminal UI or persist anything new.
    @Test
    fun late_inputs_after_terminal_do_not_mutate_state() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        val terminal = holder.uiState
        assertTrue(terminal is EmotionSessionUiState.Result)
        holder.submitSelfReport(SelfReport(1, 1, 1))
        holder.submitCheckInIntensity(5)
        holder.onTap(); holder.onLeft(); holder.onRight()
        holder.onPause()
        holder.onReduceIntensity()
        holder.onResumeFromMicroPause()
        advanceUntilIdle()
        assertTrue(holder.uiState === terminal, "late inputs after a terminal must not mutate the UI")
        assertEquals(1, repository.recentSummaries("u1").size, "late inputs persist nothing new")
        coroutineContext.cancelChildren()
    }

    // P2.9 — a contract-violating (throwing) recorder never breaks the terminal nor loses the canonical summary.
    @Test
    fun throwing_recorder_does_not_break_terminal_or_lose_summary() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        driveToResult(buildHolder(recorder = ThrowingFinalizeRecorder, repository = repository))
        assertEquals(1, repository.recentSummaries("u1").size, "the canonical summary is still saved")
        coroutineContext.cancelChildren()
    }

    // ---- MC8f: interleaving + recoverability hardening ----

    // P0.1 — start() then an immediate onExit() records BOTH an abandoned summary AND a matching ABANDONED record.
    @Test
    fun immediate_onExit_after_start_records_abandoned_summary_and_abandoned_record_consistently() = runTest {
        val store = InMemorySessionRecordStore()
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val holder = buildHolder(recorder = recorder, repository = repository)
        holder.start()
        holder.onExit()
        advanceUntilIdle()
        val summaries = repository.recentSummaries("u1")
        assertEquals(1, summaries.size)
        assertTrue(summaries.single().abandoned)
        assertFalse(summaries.single().completed)
        assertEquals(1, store.all().size)
        assertEquals(SessionStatus.ABANDONED, store.all().single().status)
        assertEquals(listOf("start", "abandon"), recorder.events)
        assertNull(repository.readAggregate("u1"))
        coroutineContext.cancelChildren()
    }

    // P0.2 — a completed session whose summary save FAILS leaves a recoverable pending marker (STARTED record),
    // never a silent success. NOTE (report): the rich session data IS lost (recovery abandons it); a durable
    // summary RETRY needs repo/UI work — a BLOCKING CEO follow-up, NOT claimed as "no progress loss".
    @Test
    fun completed_summary_save_failure_leaves_recoverable_pending_not_silent_loss() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        val store = InMemorySessionRecordStore()
        val holder = buildHolder(recorder = SecureSessionRecorder(store, testClock()), repository = repository)
        driveToResult(holder)
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "no false success")
        assertTrue(repository.summaries.isEmpty(), "summary genuinely not saved")
        assertEquals(SessionStatus.STARTED, store.all().single().status, "STARTED is the recoverable pending marker")
        coroutineContext.cancelChildren()
    }

    // P1.3 — recorder finalize failure: the SUMMARY stays canonical (completed, saved) and the terminal is not
    // broken. The SessionRecord is a BEST-EFFORT write-ahead aid; a failed finalize leaves a STARTED row (report:
    // strictly reconciling it against the completed summary needs recovery/repo changes — a CEO decision).
    @Test
    fun recorder_finalize_failure_keeps_summary_canonical_and_flags_record_best_effort() = runTest {
        val recordStore = InMemorySessionRecordStore()
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(recorder = PartialFinalizeRecorder(recordStore), repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result, "the terminal is not broken by a recorder failure")
        assertTrue(repository.recentSummaries("u1").single().completed, "the canonical summary stays completed and durable")
        assertEquals(SessionStatus.STARTED, recordStore.all().single().status, "explicit policy: record stays STARTED (best-effort)")
        coroutineContext.cancelChildren()
    }

    // P1.4 — onExit during the initial self-report clears the pending self-report; a late submit is a no-op.
    @Test
    fun onExit_during_self_report_before_does_not_leave_pending_self_report() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        holder.start()
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.SelfReportBefore)
        holder.onExit()
        advanceUntilIdle()
        val exited = holder.uiState
        assertTrue(exited is EmotionSessionUiState.Exited)
        holder.submitSelfReport(SelfReport(5, 5, 5)) // late — must not revive the session
        advanceUntilIdle()
        assertTrue(holder.uiState === exited, "late self-report after exit is a no-op")
        assertEquals(1, repository.recentSummaries("u1").size, "no second summary")
        coroutineContext.cancelChildren()
    }

    // P1.4 — onExit during a micro-pause clears the pending resume; a late resume is a no-op.
    @Test
    fun onExit_during_micro_pause_does_not_leave_pending_resume() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        reachRunningThenPause(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.MicroPause)
        holder.onExit()
        advanceUntilIdle()
        val exited = holder.uiState
        assertTrue(exited is EmotionSessionUiState.Exited)
        holder.onResumeFromMicroPause() // late — must not resume a terminated session
        advanceUntilIdle()
        assertTrue(holder.uiState === exited, "late resume after exit is a no-op")
        assertEquals(1, repository.recentSummaries("u1").size, "no second summary")
        coroutineContext.cancelChildren()
    }

    // P1.5 — onReduceIntensity after a Result is a no-op (no mutation / no new persistence).
    @Test
    fun onReduceIntensity_after_result_is_noop() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        val terminal = holder.uiState
        holder.onReduceIntensity()
        advanceUntilIdle()
        assertTrue(holder.uiState === terminal)
        assertEquals(1, repository.recentSummaries("u1").size)
        coroutineContext.cancelChildren()
    }

    // P1.5 — onReduceIntensity after Exited is a no-op.
    @Test
    fun onReduceIntensity_after_exited_is_noop() = runTest {
        val holder = buildHolder()
        holder.start(); advanceUntilIdle(); holder.onExit(); advanceUntilIdle()
        val exited = holder.uiState
        assertTrue(exited is EmotionSessionUiState.Exited)
        holder.onReduceIntensity()
        advanceUntilIdle()
        assertTrue(holder.uiState === exited)
        coroutineContext.cancelChildren()
    }

    // P1.6 — while the canonical summary save HANGS, the terminal never exposes a (false) success Result.
    @Test
    fun terminal_does_not_expose_result_while_summary_save_hangs() = runTest {
        val hang = CompletableDeferred<Unit>()
        val repository = FakeEmotionRepository(hangGate = hang)
        val holder = buildHolder(repository = repository)
        driveToSelfReportAfter(holder)
        holder.submitSelfReport(SelfReport(3, 3, 2))
        advanceUntilIdle() // finalize is parked inside the hanging saveSessionSummary
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "no Result while the canonical save hangs")
        hang.complete(Unit) // release so the coroutine can finish (test cleanup)
        advanceUntilIdle()
        coroutineContext.cancelChildren()
    }

    // P2.7 — the record's active duration is captured BEFORE the summary save, so save latency is excluded.
    @Test
    fun record_terminal_duration_excludes_summary_save_latency() = runTest {
        val clock = SharedTestClock()
        val store = InMemorySessionRecordStore()
        val repository = FakeEmotionRepository(onSave = { clock.now += 100_000L }) // huge persistence latency
        val holder = buildHolder(recorder = SecureSessionRecorder(store, testClock()), repository = repository, clock = clock)
        driveToResult(holder)
        val summary = repository.summaries.single()
        val record = store.all().single()
        assertEquals(summary.durationMs, record.activeDurationMs, "record duration == summary duration (captured once)")
        assertTrue(record.activeDurationMs < 100_000L, "active duration excludes the 100s save latency")
        coroutineContext.cancelChildren()
    }

    // ---- fixtures ----

    private var idCounter = 0

    /** Advances virtual time until a live trial is Running, then pauses — to reach the MicroPause overlay. */
    private fun TestScope.reachRunningThenPause(holder: EmotionSessionStateHolder) {
        repeat(400) {
            if (holder.uiState is EmotionSessionUiState.Running) {
                holder.onPause()
                return
            }
            advanceTimeBy(20)
        }
        error("did not reach a Running state to pause")
    }

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
        clock: EpochClock = testClock(),
    ): EmotionSessionStateHolder {
        val graph = emotionGraph(repository, clock, adaptiveEngine)
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

/** In-memory emotion repository with injectable failures/latency/hang, so those paths are assertable. */
private class FakeEmotionRepository(
    private val failSummarySave: Boolean = false,
    private val failRecentRead: Boolean = false,
    private val onSave: () -> Unit = {},
    private val hangGate: CompletableDeferred<Unit>? = null,
) : EmotionRepository {
    val summaries = mutableListOf<EmotionSessionSummaryDto>()
    var savedAggregate: EmotionAggregateDto? = null

    override suspend fun saveSessionSummary(userId: String, summary: EmotionSessionSummaryDto) {
        hangGate?.await()
        onSave()
        if (failSummarySave) error("summary save failed")
        summaries.add(summary)
    }

    override suspend fun recentSummaries(userId: String): List<EmotionSessionSummaryDto> {
        if (failRecentRead) error("recent read failed")
        return summaries.toList()
    }

    override suspend fun saveAggregate(userId: String, aggregate: EmotionAggregateDto) {
        savedAggregate = aggregate
    }

    override suspend fun readAggregate(userId: String): EmotionAggregateDto? = savedAggregate

    override suspend fun deleteAll(userId: String) {
        summaries.clear()
        savedAggregate = null
    }
}

/** A recorder that violates its total contract by throwing on finalize (used to prove the holder is robust). */
private object ThrowingFinalizeRecorder : SessionRecorder {
    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) = Unit
    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit
    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long): Unit = error("recorder boom")
    override suspend fun onAbandon(sessionId: String) = Unit
}

/** A clock whose value can be jumped by a collaborator (e.g. to simulate persistence latency during a save). */
private class SharedTestClock(var now: Long = 1_000L) : EpochClock {
    override fun nowMillis(): Long {
        now += 1
        return now
    }
}

/** Writes a real STARTED row but FAILS the finalize transition, to document the best-effort SessionRecord path. */
private class PartialFinalizeRecorder(private val store: SessionRecordStore) : SessionRecorder {
    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
        store.upsert(SessionRecord(sessionId, block, SessionStatus.STARTED, startedAtMs, startedAtMs, 0, plannedTrials, 0))
    }
    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit
    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long): Unit = error("finalize storage failed")
    override suspend fun onAbandon(sessionId: String) {
        store.find(sessionId)?.let { store.upsert(it.copy(status = SessionStatus.ABANDONED)) }
    }
}
