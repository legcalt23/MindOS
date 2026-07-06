@file:Suppress("MagicNumber", "MaxLineLength", "TooManyFunctions", "LargeClass", "LongParameterList") // Comprehensive lifecycle suite + fixtures.

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
import com.mindos.app.features.mindboost.core.session.SessionRecoveryScanner
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionPendingTerminalDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionAggregateFactory
import com.mindos.app.features.mindboost.emotions.data.local.EmotionJson
import com.mindos.app.features.mindboost.emotions.data.local.EmotionRepository
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalReconciler
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
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadProxy
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadSignals
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadThresholds
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.SecureReadResult
import com.mindos.app.features.mindboost.focus.data.local.readValueOrNull
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    // 7 (MC8g). Result is exposed only AFTER the CANONICAL summary is durable (the SessionRecord transition is now
    // best-effort, so the UI gates on the summary — not the recorder).
    @Test
    fun result_is_not_exposed_before_summary_is_durable() = runTest {
        val hang = CompletableDeferred<Unit>()
        val repository = FakeEmotionRepository(hangGate = hang)
        val holder = buildHolder(repository = repository)
        driveToSelfReportAfter(holder)
        holder.submitSelfReport(SelfReport(3, 3, 2))
        advanceUntilIdle() // finalize parked inside the (hanging) canonical summary save
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "Result must not appear before the summary is durable")
        hang.complete(Unit)
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        assertEquals(1, repository.summaries.size)
        coroutineContext.cancelChildren()
    }

    // 8 (MC8g). Exited is exposed only AFTER the abandoned terminal is durable (summary saved; pending already written).
    @Test
    fun exited_is_not_exposed_before_summary_is_durable() = runTest {
        val hang = CompletableDeferred<Unit>()
        val repository = FakeEmotionRepository(hangGate = hang)
        val holder = buildHolder(repository = repository)
        holder.start()
        advanceUntilIdle()
        holder.onExit()
        advanceUntilIdle() // finalize(ABANDONED) parked inside the hanging summary save
        assertFalse(holder.uiState is EmotionSessionUiState.Exited, "Exited must not appear before the abandoned summary is durable")
        hang.complete(Unit)
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Exited)
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
        val hang = CompletableDeferred<Unit>()
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val repository = FakeEmotionRepository(hangGate = hang)
        val holder = buildHolder(recorder = recorder, repository = repository)
        driveToSelfReportAfter(holder)
        holder.submitSelfReport(SelfReport(3, 3, 2))
        advanceUntilIdle() // finalize(COMPLETED) parked in the (hanging) CRITICAL summary save; phase == TERMINAL
        holder.onExit() // sees phase == TERMINAL → strict no-op
        advanceUntilIdle()
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "no terminal UI while the summary save is in flight")
        assertFalse(holder.uiState is EmotionSessionUiState.Exited)
        hang.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.summaries.size)
        assertTrue(repository.summaries.single().completed, "the winning summary stays completed")
        assertFalse(repository.summaries.single().abandoned, "onExit must NOT overwrite it as abandoned")
        assertEquals(SessionStatus.FINALIZED, store.all().single().status)
        assertEquals(0, recorder.onAbandonCount, "the losing ABANDONED never abandons")
        assertEquals(1, recorder.onFinalizeCount, "exactly one finalize (best-effort)")
        assertEquals(listOf("start", "finalize"), recorder.events)
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "aggregate counts the completion once")
        assertTrue(holder.uiState is EmotionSessionUiState.Result, "Result is not overwritten by Exited")
        coroutineContext.cancelChildren()
    }

    // 2. A safety stop that WINS a concurrent onExit: one non-abandoned summary; FINALIZED; no onAbandon; the
    // safe-close Result stays visible (never Exited).
    @Test
    fun safety_stop_wins_over_concurrent_onExit() = runTest {
        val hang = CompletableDeferred<Unit>()
        val store = InMemorySessionRecordStore()
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val repository = FakeEmotionRepository(hangGate = hang)
        val holder = buildHolder(
            exerciseId = EmotionExerciseId.EM2_RECUPERACION,
            recorder = recorder,
            repository = repository,
            adaptiveEngine = SafetyStopEngine,
        )
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(6) { advanceUntilIdle() } // finalize(SAFETY_STOP) parked in the hanging summary save; phase == TERMINAL
        holder.onExit() // no-op
        advanceUntilIdle()
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "no terminal UI while the summary save is in flight")
        hang.complete(Unit)
        advanceUntilIdle()
        val summaries = repository.summaries
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
    fun decided_safety_stop_survives_concurrent_onExit() = runTest {
        val hang = CompletableDeferred<Unit>()
        val recorder = EmotionRecordingRecorder()
        val repository = FakeEmotionRepository(hangGate = hang)
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, recorder = recorder, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(6) { advanceUntilIdle() } // SAFETY_STOP decided, parked in the NonCancellable critical summary save
        holder.onExit() // concurrent exit — must never turn a decided safety-stop into an abandon
        advanceUntilIdle()
        hang.complete(Unit)
        advanceUntilIdle()
        assertEquals(0, recorder.onAbandonCount, "onExit cannot turn a decided safety-stop into an abandon")
        assertFalse(repository.summaries.single().abandoned)
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

    // P1.7 (MC8j) — a safety stop: completed=false, abandoned=false, endReason=SAFETY_STOP, plus its explicit event.
    @Test
    fun safety_stop_summary_has_non_completed_non_abandoned_flags_and_endReason_safety_stop() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2)); repeat(6) { advanceUntilIdle() }
        val summary = repository.recentSummaries("u1").single()
        assertFalse(summary.completed)
        assertFalse(summary.abandoned)
        assertEquals("SAFETY_STOP", summary.endReason, "endReason is the primary terminal authority")
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

    // ---- MC8g: durable terminal recovery + full late-input guard ----

    // P0.1(1) — a completed session whose summary save fails writes a durable PENDING terminal for retry.
    @Test
    fun completed_summary_save_failure_writes_pending_terminal_for_retry() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        driveToResult(buildHolder(repository = repository))
        assertTrue(repository.summaries.isEmpty(), "summary not yet saved")
        assertEquals(1, repository.pendingTerminals.size, "a durable pending terminal is retained")
        assertTrue(repository.pendingTerminals.single().summary.completed)
        coroutineContext.cancelChildren()
    }

    // P0.1(2,4) — the reconciler retries a pending terminal, saves the summary, and deletes the pending.
    @Test
    fun pending_completed_terminal_retries_and_saves_summary_then_deletes_pending() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        driveToResult(buildHolder(repository = repository)) // creates the pending; summary NOT saved
        repository.failSummarySave = false // storage recovers
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 1L }, "u1").reconcile()
        assertEquals(1, repository.summaries.size, "retry saved the canonical summary")
        assertTrue(repository.summaries.single().completed)
        assertTrue(repository.pendingTerminals.isEmpty(), "pending deleted after success")
        coroutineContext.cancelChildren()
    }

    // P0.1(3) — retrying is idempotent: a second reconcile does not duplicate the summary.
    @Test
    fun pending_terminal_retry_is_idempotent() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        driveToResult(buildHolder(repository = repository))
        repository.failSummarySave = false
        val reconciler = EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 1L }, "u1")
        reconciler.reconcile()
        reconciler.reconcile()
        assertEquals(1, repository.summaries.size, "retry is idempotent")
        assertTrue(repository.pendingTerminals.isEmpty())
        coroutineContext.cancelChildren()
    }

    // P0.1(5) — a save-failed completed session recovers as its canonical terminal (retried + FINALIZED), NOT as
    // a plain abandoned one.
    @Test
    fun summary_save_failure_does_not_recover_as_plain_abandoned_when_pending_exists() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        val recordStore = InMemorySessionRecordStore()
        driveToResult(buildHolder(recorder = SecureSessionRecorder(recordStore, testClock()), repository = repository))
        repository.failSummarySave = false
        EmotionTerminalReconciler(repository, recordStore, EpochClock { 1L }, "u1").reconcile()
        assertTrue(repository.summaries.single().completed, "recovered as completed, not lost")
        assertEquals(SessionStatus.FINALIZED, recordStore.all().single().status, "record recovered as FINALIZED, not abandoned")
        coroutineContext.cancelChildren()
    }

    // P0.2(6,7) — recovery repairs a STARTED record that has a completed emotion terminal to FINALIZED (never abandons it).
    @Test
    fun recovery_repairs_started_record_with_completed_emotion_terminal_to_finalized() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sX", completed = true, abandoned = false))
        store.upsert(emotionRecord("sX", SessionStatus.STARTED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 5L }, "u1").reconcile()
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("sX")).status)
        coroutineContext.cancelChildren()
    }

    // P0.2(8) — a safety-stop terminal also repairs the record to FINALIZED.
    @Test
    fun recovery_repairs_started_record_with_safety_stop_terminal_to_finalized() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sS", completed = false, abandoned = false)) // safety-stop
        store.upsert(emotionRecord("sS", SessionStatus.STARTED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 5L }, "u1").reconcile()
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("sS")).status)
        coroutineContext.cancelChildren()
    }

    // P0.2(9) — recovery abandons a STARTED record ONLY when it has no emotion terminal (true orphan).
    @Test
    fun recovery_abandons_started_record_only_when_no_emotion_terminal_exists() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("done", completed = true, abandoned = false))
        store.upsert(emotionRecord("done", SessionStatus.STARTED, updatedAtMs = 0L))
        store.upsert(emotionRecord("orphan", SessionStatus.STARTED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 1_000_000L }, "u1").reconcile()
        SessionRecoveryScanner(store, EpochClock { 1_000_000L }).recover()
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("done")).status, "repaired, not abandoned")
        assertEquals(SessionStatus.ABANDONED, assertNotNull(store.find("orphan")).status, "true orphan abandoned")
        coroutineContext.cancelChildren()
    }

    // P0.2(10) — recorder finalize failure + recovery: NEVER a durable completed-summary + abandoned-record.
    @Test
    fun recorder_finalize_failure_followed_by_recovery_does_not_create_completed_plus_abandoned() = runTest {
        val recordStore = InMemorySessionRecordStore()
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        driveToResult(buildHolder(recorder = PartialFinalizeRecorder(recordStore), repository = repository))
        assertEquals(SessionStatus.STARTED, recordStore.all().single().status, "finalize failed → STARTED (pre-recovery)")
        EmotionTerminalReconciler(repository, recordStore, EpochClock { 5L }, "u1").reconcile()
        assertTrue(repository.recentSummaries("u1").single().completed)
        assertEquals(SessionStatus.FINALIZED, recordStore.all().single().status, "recovery repairs the contradiction")
        coroutineContext.cancelChildren()
    }

    // P1.3(12) — onPause after a Result is a no-op.
    @Test
    fun onPause_after_result_is_noop() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        val terminal = holder.uiState
        holder.onPause()
        advanceUntilIdle()
        assertTrue(holder.uiState === terminal)
        assertEquals(1, repository.recentSummaries("u1").size)
        coroutineContext.cancelChildren()
    }

    // P1.3(13) — onPause after Exited is a no-op.
    @Test
    fun onPause_after_exited_is_noop() = runTest {
        val holder = buildHolder()
        holder.start(); advanceUntilIdle(); holder.onExit(); advanceUntilIdle()
        val exited = holder.uiState
        holder.onPause()
        advanceUntilIdle()
        assertTrue(holder.uiState === exited)
        coroutineContext.cancelChildren()
    }

    // P1.3(11,14) — onPause/onResume during a terminal in-flight (phase TERMINAL, UI still Running) are no-ops.
    @Test
    fun onPause_and_onResume_during_terminal_in_flight_are_noops() = runTest {
        val hang = CompletableDeferred<Unit>()
        val repository = FakeEmotionRepository(hangGate = hang)
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2))
        repeat(6) { advanceUntilIdle() } // finalize(SAFETY_STOP) parked in the CRITICAL summary save; phase == TERMINAL
        val inflight = holder.uiState
        holder.onPause()
        holder.onResumeFromMicroPause()
        advanceUntilIdle()
        assertTrue(holder.uiState === inflight, "safety inputs during a terminal in-flight do not mutate the UI")
        hang.complete(Unit); advanceUntilIdle()
        coroutineContext.cancelChildren()
    }

    // P1.4(16) — onReduceIntensity before the first self-report is a no-op (no safety event recorded).
    @Test
    fun reduce_intensity_before_self_report_submission_is_noop() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        holder.start()
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.SelfReportBefore)
        holder.onReduceIntensity() // safety bar not usable during self-report → no-op
        holder.submitSelfReport(SelfReport(3, 3, 2))
        driveRemainderToResult(holder)
        assertTrue(
            repository.recentSummaries("u1").single().safetyEvents.none { it.type == SafetyEventType.REDUCE_INTENSITY.name },
            "reduce during self-report records no safety event",
        )
        coroutineContext.cancelChildren()
    }

    // P1.4(18) — onReduceIntensity during a live Running trial records exactly one safety event.
    @Test
    fun reduce_intensity_during_running_records_once() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        assertTrue(advanceToRunning(holder), "reached a live Running trial")
        holder.onReduceIntensity()
        driveRemainderToResult(holder)
        assertEquals(
            1,
            repository.recentSummaries("u1").single().safetyEvents.count { it.type == SafetyEventType.REDUCE_INTENSITY.name },
            "one reduce during Running records exactly one event",
        )
        coroutineContext.cancelChildren()
    }

    // P1.5(19,21) — a hanging recorder.onStart never freezes a completed terminal; the summary stays canonical.
    @Test
    fun hanging_recorder_onStart_does_not_freeze_completed_terminal() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        driveToResult(buildHolder(recorder = HangingStartRecorder, repository = repository))
        // (driveToResult returns only once the terminal is exposed; the join timed out rather than hanging.)
        assertEquals(1, repository.recentSummaries("u1").size, "summary stays canonical despite the hanging recorder")
        coroutineContext.cancelChildren()
    }

    // P1.5(20) — a hanging recorder.onStart never freezes an abandoned exit.
    @Test
    fun hanging_recorder_onStart_does_not_freeze_abandoned_exit() = runTest {
        val holder = buildHolder(recorder = HangingStartRecorder)
        holder.start()
        advanceUntilIdle()
        holder.onExit()
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.Exited, "abandoned exit is not frozen by a hanging recorder")
        coroutineContext.cancelChildren()
    }

    // P1.6(22) — a hanging aggregate save does not prevent the Result (aggregate is best-effort, outside the critical path).
    @Test
    fun aggregate_hang_does_not_prevent_result_after_summary_saved() = runTest {
        val aggHang = CompletableDeferred<Unit>()
        val repository = FakeEmotionRepository(aggregateHangGate = aggHang)
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result, "Result shows even while the aggregate save hangs")
        assertEquals(1, repository.summaries.size)
        aggHang.complete(Unit); advanceUntilIdle()
        coroutineContext.cancelChildren()
    }

    // P1.6(23) — a failing aggregate save does not prevent the Result.
    @Test
    fun aggregate_failure_does_not_prevent_result_after_summary_saved() = runTest {
        val repository = FakeEmotionRepository(failAggregateSave = true)
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        assertEquals(1, repository.summaries.size)
        assertNull(repository.readAggregate("u1"), "aggregate failed (best-effort), summary intact")
        coroutineContext.cancelChildren()
    }

    // P1.7(25) — onExit during a MidCheckIn clears the pending intensity; a late submit is a no-op.
    @Test
    fun onExit_during_mid_check_in_clears_pending_intensity_and_late_intensity_is_noop() = runTest {
        val store = InMemorySessionRecordStore()
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val recorder = EmotionRecordingRecorder(delegate = SecureSessionRecorder(store, testClock()))
        val holder = buildHolder(
            exerciseId = EmotionExerciseId.EM2_RECUPERACION,
            recorder = recorder,
            repository = repository,
            overloadDetector = AlwaysOverloadDetector,
        )
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(3, 3, 2))
        advanceUntilIdle() // block 0 ends → proxies armed → MidCheckIn
        assertTrue(holder.uiState is EmotionSessionUiState.MidCheckIn, "reached MidCheckIn, got ${holder.uiState}")
        holder.onExit()
        advanceUntilIdle()
        val exited = holder.uiState
        assertTrue(exited is EmotionSessionUiState.Exited)
        holder.submitCheckInIntensity(5) // late — must not revive the session
        advanceUntilIdle()
        assertTrue(holder.uiState === exited, "late check-in intensity after exit is a no-op")
        assertEquals(1, repository.recentSummaries("u1").size)
        assertTrue(repository.recentSummaries("u1").single().abandoned)
        assertEquals(SessionStatus.ABANDONED, store.all().single().status)
        assertNull(repository.readAggregate("u1"))
        coroutineContext.cancelChildren()
    }

    // P2.9(26,27,28) — the summary carries an explicit endReason for each terminal.
    @Test
    fun completed_summary_has_endReason_completed() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        driveToResult(buildHolder(repository = repository))
        assertEquals("COMPLETED", repository.recentSummaries("u1").single().endReason)
        coroutineContext.cancelChildren()
    }

    @Test
    fun abandoned_summary_has_endReason_abandoned() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(repository = repository)
        holder.start(); advanceUntilIdle(); holder.onExit(); advanceUntilIdle()
        assertEquals("ABANDONED", repository.recentSummaries("u1").single().endReason)
        coroutineContext.cancelChildren()
    }

    @Test
    fun safety_stop_summary_has_endReason_safety_stop() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2)); repeat(6) { advanceUntilIdle() }
        assertEquals("SAFETY_STOP", repository.recentSummaries("u1").single().endReason)
        coroutineContext.cancelChildren()
    }

    // ---- MC8h: real pending / reconciler / aggregate guarantees ----

    // §1 invariant 1 — BOTH the summary and the pending fail → NOT recoverable: honest SaveFailed, no false leave.
    @Test
    fun summary_and_pending_save_failure_is_not_reported_as_recoverable() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true, failPendingSave = true)
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.SaveFailed, "total storage failure shows SaveFailed, got ${holder.uiState}")
        assertFalse(holder.uiState is EmotionSessionUiState.Result, "never a false success")
        assertFalse(holder.uiState is EmotionSessionUiState.Exited, "never a neutral leave sold as recoverable")
        assertTrue(repository.summaries.isEmpty())
        assertTrue(repository.pendingTerminals.isEmpty(), "no durable pending was actually written")
        coroutineContext.cancelChildren()
    }

    // §1 — summary fails but the pending IS durable → recoverable (neutral leave; recovery finishes it).
    @Test
    fun summary_save_failure_with_pending_saved_is_recoverable() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true) // pending save OK
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Exited, "recoverable via the durable pending, got ${holder.uiState}")
        assertEquals(1, repository.pendingTerminals.size, "the durable pending was written")
        assertTrue(repository.summaries.isEmpty())
        coroutineContext.cancelChildren()
    }

    // §1 — the pending write fails but the canonical summary succeeds → success (the summary is the authority).
    @Test
    fun pending_save_failure_but_summary_save_success_is_ok() = runTest {
        val repository = FakeEmotionRepository(failPendingSave = true) // summary save OK
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result, "summary durable → success despite a pending write failure")
        assertEquals(1, repository.summaries.size)
        assertTrue(repository.pendingTerminals.isEmpty(), "no pending lingers when the summary is durable")
        coroutineContext.cancelChildren()
    }

    // §2 invariant 2 — a completed summary whose recorder never wrote a record → the reconciler CREATES a FINALIZED one.
    @Test
    fun reconciler_creates_missing_finalized_record_for_completed_summary() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sC", completed = true, abandoned = false, startedAt = 12L, durationMs = 34L))
        EmotionTerminalReconciler(repository, store, EpochClock { 7L }, "u1").reconcile()
        val record = assertNotNull(store.find("sC"))
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertEquals(ScoreModuleId.EMOTIONS, record.block)
        assertEquals(1, record.completedTrials)
        assertEquals(1, record.plannedTrials)
        assertEquals(12L, record.startedAtMs)
        assertEquals(34L, record.activeDurationMs)
        coroutineContext.cancelChildren()
    }

    // §2 — a safety-stop summary also creates a FINALIZED record.
    @Test
    fun reconciler_creates_missing_finalized_record_for_safety_stop_summary() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sS", completed = false, abandoned = false))
        EmotionTerminalReconciler(repository, store, EpochClock { 7L }, "u1").reconcile()
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("sS")).status)
        coroutineContext.cancelChildren()
    }

    // §2 — an abandoned summary creates an ABANDONED record (0 completed trials).
    @Test
    fun reconciler_creates_missing_abandoned_record_for_abandoned_summary() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sA", completed = false, abandoned = true, durationMs = 9L))
        EmotionTerminalReconciler(repository, store, EpochClock { 7L }, "u1").reconcile()
        val record = assertNotNull(store.find("sA"))
        assertEquals(SessionStatus.ABANDONED, record.status)
        assertEquals(0, record.completedTrials)
        coroutineContext.cancelChildren()
    }

    // §2 — end-to-end: a hanging recorder.onStart never wrote a record, yet reconcile creates it from the summary.
    @Test
    fun hanging_recorder_onStart_then_reconcile_creates_record_from_summary() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val recordStore = InMemorySessionRecordStore()
        driveToResult(buildHolder(recorder = HangingStartRecorder, repository = repository))
        assertTrue(recordStore.all().isEmpty(), "the hanging recorder never wrote a record")
        val sessionId = assertNotNull(repository.recentSummaries("u1").single().sessionId)
        EmotionTerminalReconciler(repository, recordStore, EpochClock { 7L }, "u1").reconcile()
        val record = assertNotNull(recordStore.find(sessionId))
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertEquals(ScoreModuleId.EMOTIONS, record.block)
        assertEquals(1, record.completedTrials)
        coroutineContext.cancelChildren()
    }

    // §3 invariant 3 — repairing a STARTED record to FINALIZED also repairs its trials + duration (never left corrupt).
    @Test
    fun reconciler_repairs_completed_trials_and_duration_for_completed_summary() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sX", completed = true, abandoned = false, durationMs = 1234L))
        store.upsert(emotionRecord("sX", SessionStatus.STARTED, updatedAtMs = 0L)) // completedTrials 0, duration 0
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        val record = assertNotNull(store.find("sX"))
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertTrue(record.completedTrials >= 1, "completed trials repaired to at least one")
        assertEquals(1234L, record.activeDurationMs, "duration repaired from the summary")
        assertEquals(9L, record.updatedAtMs)
        coroutineContext.cancelChildren()
    }

    // §3 — repairing to ABANDONED still lifts the duration from the summary.
    @Test
    fun reconciler_repairs_duration_for_abandoned_summary() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sA", completed = false, abandoned = true, durationMs = 500L))
        store.upsert(emotionRecord("sA", SessionStatus.STARTED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        val record = assertNotNull(store.find("sA"))
        assertEquals(SessionStatus.ABANDONED, record.status)
        assertEquals(500L, record.activeDurationMs)
        coroutineContext.cancelChildren()
    }

    // §3 — an ABANDONED record with a COMPLETED summary is forced to FINALIZED with coherent trials + duration.
    @Test
    fun reconciler_repairs_abandoned_record_to_finalized_with_duration_when_summary_completed() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sZ", completed = true, abandoned = false, durationMs = 800L))
        store.upsert(emotionRecord("sZ", SessionStatus.ABANDONED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        val record = assertNotNull(store.find("sZ"))
        assertEquals(SessionStatus.FINALIZED, record.status, "summary is the authority: abandoned record repaired to finalized")
        assertTrue(record.completedTrials >= 1)
        assertEquals(800L, record.activeDurationMs)
        coroutineContext.cancelChildren()
    }

    // §3 — repair is idempotent: a second reconcile does not re-bump an already-coherent record.
    @Test
    fun reconciler_record_repair_is_idempotent() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", emotionSummary("sI", completed = true, abandoned = false, durationMs = 40L))
        store.upsert(emotionRecord("sI", SessionStatus.STARTED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 5L }, "u1").reconcile()
        val afterFirst = assertNotNull(store.find("sI"))
        EmotionTerminalReconciler(repository, store, EpochClock { 999L }, "u1").reconcile()
        assertEquals(afterFirst, store.find("sI"), "an already-coherent record is not re-written (no updatedAt bump)")
        coroutineContext.cancelChildren()
    }

    // §4 invariant 4 — a completed session recovered by pending-retry advances the aggregate, no future session needed.
    @Test
    fun pending_completed_retry_recomputes_aggregate() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        driveToResult(buildHolder(repository = repository))
        assertNull(repository.readAggregate("u1"), "no aggregate while the summary is unsaved")
        repository.failSummarySave = false
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "the recovered completion advances the aggregate")
        coroutineContext.cancelChildren()
    }

    // §4 — a recovered SAFETY_STOP saves its summary but never advances calibration.
    @Test
    fun pending_safety_stop_retry_does_not_advance_aggregate() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        val holder = buildHolder(exerciseId = EmotionExerciseId.EM2_RECUPERACION, repository = repository, adaptiveEngine = SafetyStopEngine)
        holder.start(); advanceUntilIdle(); holder.submitSelfReport(SelfReport(3, 3, 2)); repeat(6) { advanceUntilIdle() }
        assertEquals(1, repository.pendingTerminals.size, "safety-stop pending retained")
        repository.failSummarySave = false
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertEquals(1, repository.summaries.size, "safety-stop summary saved on retry")
        assertNull(repository.readAggregate("u1"), "a safety-stop never advances the aggregate")
        coroutineContext.cancelChildren()
    }

    // §4 — a recovered ABANDONED session never advances calibration.
    @Test
    fun pending_abandoned_retry_does_not_advance_aggregate() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        val holder = buildHolder(repository = repository)
        holder.start(); advanceUntilIdle(); holder.onExit(); advanceUntilIdle()
        assertEquals(1, repository.pendingTerminals.size, "abandoned pending retained")
        repository.failSummarySave = false
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertNull(repository.readAggregate("u1"), "an abandoned session never advances the aggregate")
        coroutineContext.cancelChildren()
    }

    // §4 — recomputing the aggregate is idempotent.
    @Test
    fun aggregate_recompute_is_idempotent() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        driveToResult(buildHolder(repository = repository))
        repository.failSummarySave = false
        val reconciler = EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1")
        reconciler.reconcile()
        reconciler.reconcile()
        assertEquals(1, repository.summaries.size)
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "recompute is idempotent")
        coroutineContext.cancelChildren()
    }

    // §5 invariant 5 — a prune/maintenance failure never turns a successful canonical summary save into a false failure.
    @Test
    fun summary_put_success_prune_failure_still_counts_as_summary_saved() = runTest {
        val store = PruneFailingStore(InMemorySecureKeyValueStore())
        val repository = LocalEmotionRepository(store)
        store.failKeys = true // the prune's keys(...) call throws
        repository.saveSessionSummary("u1", emotionSummary("sess_p", completed = true, abandoned = false)) // must NOT throw
        store.failKeys = false
        assertEquals(1, repository.recentSummaries("u1").size, "the canonical write survived the prune failure")
        coroutineContext.cancelChildren()
    }

    // §7 invariant 7 — the summary is keyed by sessionId (primary identity), not by startedAtMillis.
    @Test
    fun pending_retry_uses_sessionId_key() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repository = LocalEmotionRepository(store)
        repository.saveSessionSummary("u1", emotionSummary("sess_K", completed = true, abandoned = false, startedAt = 999L))
        val keys = store.keys("session:u1:")
        assertTrue(keys.any { it.endsWith("sess_K") }, "keyed by sessionId, got $keys")
        assertTrue(keys.none { it.endsWith(":999") }, "not keyed by startedAtMillis")
        coroutineContext.cancelChildren()
    }

    // §7 — two sessions that share a startedAtMillis but differ in sessionId do not overwrite each other.
    @Test
    fun two_summaries_same_startedAt_different_sessionId_do_not_overwrite() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repository.saveSessionSummary("u1", emotionSummary("sess_a", completed = true, abandoned = false, startedAt = 0L))
        repository.saveSessionSummary("u1", emotionSummary("sess_b", completed = true, abandoned = false, startedAt = 0L))
        assertEquals(2, repository.recentSummaries("u1").size, "distinct sessionIds coexist despite a shared startedAt")
        coroutineContext.cancelChildren()
    }

    // §7 — a legacy summary with no sessionId still reads back (keyed by startedAtMillis).
    @Test
    fun legacy_summary_without_sessionId_is_still_read() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val legacy = EmotionSessionSummaryDto(
            exerciseId = "EM1_PAUSA",
            startedAtMillis = 777L,
            durationMs = 0L,
            difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
            metricsSummary = emptyList(),
            completed = true,
            abandoned = false,
            sessionId = null, // legacy row
        )
        repository.saveSessionSummary("u1", legacy)
        assertEquals(1, repository.recentSummaries("u1").size, "a legacy sessionId-less summary is still readable")
        coroutineContext.cancelChildren()
    }

    // §8 invariant — attemptCount increments on a failed retry (real resilience telemetry, not decorative).
    @Test
    fun pending_retry_failure_increments_attempt_count() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        driveToResult(buildHolder(repository = repository))
        assertEquals(1, repository.pendingTerminals.single().attemptCount, "created with attempt 1")
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 42L }, "u1").reconcile() // still failing
        val pending = repository.pendingTerminals.single()
        assertEquals(2, pending.attemptCount, "a failed retry increments the attempt count")
        assertEquals(42L, pending.lastAttemptAtMillis, "records when it was last attempted")
        coroutineContext.cancelChildren()
    }

    // §8 — a successful retry deletes the pending.
    @Test
    fun pending_retry_success_deletes_pending() = runTest {
        val repository = FakeEmotionRepository(failSummarySave = true)
        driveToResult(buildHolder(repository = repository))
        assertEquals(1, repository.pendingTerminals.size)
        repository.failSummarySave = false
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 1L }, "u1").reconcile()
        assertTrue(repository.pendingTerminals.isEmpty(), "a successful retry deletes the pending")
        coroutineContext.cancelChildren()
    }

    // ---- MC8i: dedupe/migration safety + aggregate self-healing + reconciler correctness ----

    // §1 invariant 1/2 — the SAME session under a legacy startedAt key AND a new sessionId key is returned once.
    @Test
    fun legacy_startedAt_key_and_new_sessionId_key_same_summary_are_deduped() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalEmotionRepository(store)
        val summary = emotionSummary("sid1", completed = true, abandoned = false, startedAt = 1_710_000_000_000L)
        putRawSummary(store, "session:u1:1710000000000", summary) // legacy key, DTO carries sessionId
        putRawSummary(store, "session:u1:sid1", summary) // migrated new key
        assertEquals(1, repo.recentSummaries("u1").size, "same session under two keys dedups to one")
        coroutineContext.cancelChildren()
    }

    // §1 — saving a sessionId summary drops the legacy startedAt-keyed row for THIS session (best-effort migration).
    @Test
    fun saving_sessionId_summary_removes_legacy_startedAt_key_best_effort() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalEmotionRepository(store)
        val summary = emotionSummary("sid1", completed = true, abandoned = false, startedAt = 1_710_000_000_000L)
        putRawSummary(store, "session:u1:1710000000000", summary) // pre-existing legacy row carrying the sessionId
        repo.saveSessionSummary("u1", summary) // saves under session:u1:sid1 and migrates the legacy key away
        val keys = store.keys("session:u1:")
        assertTrue(keys.none { it.endsWith(":1710000000000") }, "legacy startedAt key removed, got $keys")
        assertTrue(keys.any { it.endsWith(":sid1") }, "new sessionId key present")
        assertEquals(1, repo.recentSummaries("u1").size)
        coroutineContext.cancelChildren()
    }

    // §1 invariant 1 — the aggregate never double-counts a legacy+new duplicate of one session.
    @Test
    fun aggregate_does_not_double_count_legacy_and_new_key_duplicate() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalEmotionRepository(store)
        val summary = emotionSummary("sid1", completed = true, abandoned = false, startedAt = 5L)
        putRawSummary(store, "session:u1:5", summary)
        putRawSummary(store, "session:u1:sid1", summary)
        assertEquals(1, EmotionAggregateFactory.fromSummaries(repo.recentSummaries("u1"), 1L)?.sessionsCompleted)
        coroutineContext.cancelChildren()
    }

    // §1 — the reconciler processes a duplicated session once (one record, not two).
    @Test
    fun reconciler_processes_duplicate_sessionId_summary_once() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalEmotionRepository(store)
        val recordStore = InMemorySessionRecordStore()
        val summary = emotionSummary("sid1", completed = true, abandoned = false, startedAt = 5L)
        putRawSummary(store, "session:u1:5", summary)
        putRawSummary(store, "session:u1:sid1", summary)
        EmotionTerminalReconciler(repo, recordStore, EpochClock { 9L }, "u1").reconcile()
        assertEquals(1, recordStore.all().size, "one record for the deduped session")
        assertEquals(SessionStatus.FINALIZED, assertNotNull(recordStore.find("sid1")).status)
        coroutineContext.cancelChildren()
    }

    // §1 invariant — two genuinely distinct sessions that share a startedAt still BOTH survive (migration is scoped).
    @Test
    fun two_summaries_same_startedAt_different_sessionId_still_both_exist() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repo.saveSessionSummary("u1", emotionSummary("sidA", completed = true, abandoned = false, startedAt = 7L))
        repo.saveSessionSummary("u1", emotionSummary("sidB", completed = true, abandoned = false, startedAt = 7L))
        assertEquals(2, repo.recentSummaries("u1").size, "distinct sessionIds with a shared startedAt both survive")
        coroutineContext.cancelChildren()
    }

    // §2 invariant 3 — the reconciler recomputes a MISSING aggregate from an existing completed summary (no pending).
    @Test
    fun reconciler_recomputes_missing_aggregate_from_existing_completed_summary() = runTest {
        val repository = FakeEmotionRepository()
        repository.saveSessionSummary("u1", emotionSummary("s1", completed = true, abandoned = false))
        assertNull(repository.readAggregate("u1"))
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "aggregate self-heals from summaries")
        coroutineContext.cancelChildren()
    }

    // §2 — the reconciler repairs a CORRUPT aggregate even without any pending recovery.
    @Test
    fun reconciler_repairs_corrupt_aggregate_even_without_pending_recovery() = runTest {
        val repository = FakeEmotionRepository()
        repository.saveSessionSummary("u1", emotionSummary("s1", completed = true, abandoned = false))
        repository.savedAggregate = staleAggregate(sessionsCompleted = 99)
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "corrupt aggregate recomputed to the true count")
        coroutineContext.cancelChildren()
    }

    // §2 — the recomputed aggregate uses DEDUPED summaries (legacy+new duplicate counts once).
    @Test
    fun reconciler_recomputed_aggregate_uses_deduped_summaries() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repo = LocalEmotionRepository(store)
        val summary = emotionSummary("sid1", completed = true, abandoned = false, startedAt = 5L)
        putRawSummary(store, "session:u1:5", summary)
        putRawSummary(store, "session:u1:sid1", summary)
        EmotionTerminalReconciler(repo, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertEquals(1, repo.readAggregate("u1")?.sessionsCompleted, "recomputed aggregate counts the deduped session once")
        coroutineContext.cancelChildren()
    }

    // §2 — safety-stop + abandoned summaries never create an aggregate.
    @Test
    fun safety_stop_and_abandoned_existing_summaries_do_not_create_aggregate() = runTest {
        val repository = FakeEmotionRepository()
        repository.saveSessionSummary("u1", summaryWith("s1", completed = false, abandoned = false, endReason = "SAFETY_STOP"))
        repository.saveSessionSummary("u1", summaryWith("s2", completed = false, abandoned = true, endReason = "ABANDONED"))
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertNull(repository.readAggregate("u1"), "no completed session → no aggregate")
        coroutineContext.cancelChildren()
    }

    // §2 — a stale/phantom aggregate is DELETED when no completed session remains.
    @Test
    fun reconciler_deletes_stale_aggregate_when_no_completed_summaries_exist() = runTest {
        val repository = FakeEmotionRepository()
        repository.saveSessionSummary("u1", summaryWith("s1", completed = false, abandoned = true, endReason = "ABANDONED"))
        repository.savedAggregate = staleAggregate(sessionsCompleted = 7)
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 3L }, "u1").reconcile()
        assertNull(repository.readAggregate("u1"), "stale phantom aggregate deleted when no completed session remains")
        coroutineContext.cancelChildren()
    }

    // §3 invariant 4 — a FINALIZED record whose canonical terminal is ABANDONED is repaired to ABANDONED, trials cleared.
    @Test
    fun reconciler_repairs_finalized_record_to_abandoned_and_clears_completedTrials() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", summaryWith("sA", completed = false, abandoned = true, endReason = "ABANDONED", durationMs = 300L))
        store.upsert(SessionRecord("sA", ScoreModuleId.EMOTIONS, SessionStatus.FINALIZED, 0L, 0L, completedTrials = 1, plannedTrials = 1, activeDurationMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        val record = assertNotNull(store.find("sA"))
        assertEquals(SessionStatus.ABANDONED, record.status)
        assertEquals(0, record.completedTrials, "an abandoned record never claims completed work")
        coroutineContext.cancelChildren()
    }

    // §3 — a created ABANDONED record has zero completed trials.
    @Test
    fun reconciler_abandoned_record_has_zero_completedTrials() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", summaryWith("sB", completed = false, abandoned = true, endReason = "ABANDONED"))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        assertEquals(0, assertNotNull(store.find("sB")).completedTrials)
        coroutineContext.cancelChildren()
    }

    // §3 — a repaired ABANDONED record lifts its duration from the summary.
    @Test
    fun reconciler_abandoned_record_preserves_or_repairs_duration() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", summaryWith("sC", completed = false, abandoned = true, endReason = "ABANDONED", durationMs = 250L))
        store.upsert(SessionRecord("sC", ScoreModuleId.EMOTIONS, SessionStatus.STARTED, 0L, 0L, 0, 1, activeDurationMs = 100L))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        val record = assertNotNull(store.find("sC"))
        assertEquals(SessionStatus.ABANDONED, record.status)
        assertEquals(250L, record.activeDurationMs, "duration lifted from the summary")
        coroutineContext.cancelChildren()
    }

    // §4 invariant 5 — endReason=ABANDONED overrides an inconsistent completed=true flag (record + aggregate).
    @Test
    fun endReason_abandoned_overrides_inconsistent_completed_flag() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", summaryWith("sX", completed = true, abandoned = false, endReason = "ABANDONED"))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        assertEquals(SessionStatus.ABANDONED, assertNotNull(store.find("sX")).status, "endReason governs over the boolean")
        assertNull(repository.readAggregate("u1"), "endReason=ABANDONED never advances calibration despite completed=true")
        coroutineContext.cancelChildren()
    }

    // §4 — endReason=COMPLETED overrides a missing/false completed flag.
    @Test
    fun endReason_completed_overrides_missing_completed_flag() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", summaryWith("sY", completed = false, abandoned = false, endReason = "COMPLETED"))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("sY")).status)
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "endReason=COMPLETED counts despite completed=false")
        coroutineContext.cancelChildren()
    }

    // §4 — endReason=SAFETY_STOP finalizes the record but does not advance the aggregate.
    @Test
    fun endReason_safety_stop_finalizes_record_but_does_not_advance_aggregate() = runTest {
        val repository = FakeEmotionRepository()
        val store = InMemorySessionRecordStore()
        repository.saveSessionSummary("u1", summaryWith("sZ", completed = false, abandoned = false, endReason = "SAFETY_STOP"))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("sZ")).status)
        assertNull(repository.readAggregate("u1"), "safety-stop finalizes the record but never advances calibration")
        coroutineContext.cancelChildren()
    }

    // §4 — a legacy completed row (no endReason) still counts toward the aggregate.
    @Test
    fun legacy_completed_without_endReason_still_counts() = runTest {
        val repository = FakeEmotionRepository()
        repository.saveSessionSummary("u1", summaryWith("sL", completed = true, abandoned = false, endReason = null))
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 9L }, "u1").reconcile()
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "legacy completed row (no endReason) still counts")
        coroutineContext.cancelChildren()
    }

    // §5 invariant 6 — a pending-list failure does not throw; record reconciliation still runs.
    @Test
    fun reconcile_pending_list_failure_does_not_throw() = runTest {
        val repository = FakeEmotionRepository(failPendingList = true)
        repository.saveSessionSummary("u1", emotionSummary("s1", completed = true, abandoned = false))
        val store = InMemorySessionRecordStore()
        store.upsert(emotionRecord("s1", SessionStatus.STARTED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile() // must not throw
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("s1")).status, "record reconciliation still runs")
        coroutineContext.cancelChildren()
    }

    // §5 — a recentSummaries failure does not throw AND never deletes/resets a valid aggregate.
    @Test
    fun reconcile_recent_summaries_failure_does_not_throw() = runTest {
        val repository = FakeEmotionRepository(failRecentRead = true)
        repository.savedAggregate = staleAggregate(sessionsCompleted = 5)
        EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 9L }, "u1").reconcile() // must not throw
        assertEquals(5, repository.readAggregate("u1")?.sessionsCompleted, "a read failure never resets/deletes the aggregate")
        coroutineContext.cancelChildren()
    }

    // §5 — a recordStore.all() failure does not throw; pending retry + aggregate self-heal still ran.
    @Test
    fun reconcile_record_store_all_failure_does_not_throw() = runTest {
        val repository = FakeEmotionRepository()
        repository.saveSessionSummary("u1", emotionSummary("s1", completed = true, abandoned = false))
        EmotionTerminalReconciler(repository, ThrowingRecordStore, EpochClock { 9L }, "u1").reconcile() // must not throw
        assertEquals(1, repository.readAggregate("u1")?.sessionsCompleted, "aggregate still self-healed despite a record store failure")
        coroutineContext.cancelChildren()
    }

    // §5 invariant 6 — an aggregate save failure does not prevent record repair.
    @Test
    fun aggregate_failure_does_not_prevent_record_repair() = runTest {
        val repository = FakeEmotionRepository(failAggregateSave = true)
        repository.saveSessionSummary("u1", emotionSummary("s1", completed = true, abandoned = false))
        val store = InMemorySessionRecordStore()
        store.upsert(emotionRecord("s1", SessionStatus.STARTED, updatedAtMs = 0L))
        EmotionTerminalReconciler(repository, store, EpochClock { 9L }, "u1").reconcile()
        assertEquals(SessionStatus.FINALIZED, assertNotNull(store.find("s1")).status, "records repaired even when aggregate save fails")
        coroutineContext.cancelChildren()
    }

    // ---- MC8j: final consistency polish (tier semantics · cancellation · keyspace validation) ----

    // §1 invariant 1 — the Result tier uses endReason-authoritative aggregate semantics, not the raw `completed` boolean.
    // (The Result tier == aggregate.tier by construction — one computed value — so asserting the aggregate asserts the tier.)
    @Test
    fun holder_result_tier_uses_endReason_semantics_not_completed_boolean() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        // completed=true but endReason=SAFETY_STOP → must NOT count toward the tier
        repository.saveSessionSummary("u1", summaryWith("bad", completed = true, abandoned = false, endReason = "SAFETY_STOP", startedAt = 1L))
        val holder = buildHolder(repository = repository)
        driveToResult(holder)
        assertTrue(holder.uiState is EmotionSessionUiState.Result)
        val aggregate = assertNotNull(repository.readAggregate("u1"))
        assertEquals(1, aggregate.sessionsCompleted, "only the real completion counts (endReason), not completed=true+SAFETY_STOP")
        assertEquals("EXPLORATORY", aggregate.tier, "the Result tier equals aggregate.tier (same source)")
        coroutineContext.cancelChildren()
    }

    // §1 — an inconsistent endReason=ABANDONED + completed=true row is ignored for the tier count.
    @Test
    fun holder_result_tier_ignores_inconsistent_endReason_abandoned_completed_true_summary() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repository.saveSessionSummary("u1", summaryWith("bad", completed = true, abandoned = false, endReason = "ABANDONED", startedAt = 1L))
        driveToResult(buildHolder(repository = repository))
        val aggregate = assertNotNull(repository.readAggregate("u1"))
        assertEquals(1, aggregate.sessionsCompleted, "endReason=ABANDONED+completed=true is ignored")
        assertEquals("EXPLORATORY", aggregate.tier)
        coroutineContext.cancelChildren()
    }

    // §1 — a legacy completed row (no endReason) still counts, so the tier rises with the new completion.
    @Test
    fun holder_result_tier_counts_legacy_completed_without_endReason() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())
        repository.saveSessionSummary("u1", summaryWith("legacy", completed = true, abandoned = false, endReason = null, startedAt = 1L))
        driveToResult(buildHolder(repository = repository))
        val aggregate = assertNotNull(repository.readAggregate("u1"))
        assertEquals(2, aggregate.sessionsCompleted, "legacy completed (no endReason) + the new completion both count")
        assertEquals("PRELIMINARY", aggregate.tier, "tierFor(2) == PRELIMINARY")
        coroutineContext.cancelChildren()
    }

    // §2 invariant 2 — cooperative cancellation is RETHROWN by the reconciler, never swallowed as a recoverable fault.
    @Test
    fun reconcile_pending_list_cancellation_is_rethrown() = runTest {
        val repository = FakeEmotionRepository(cancelPendingList = true)
        assertFailsWith<CancellationException> {
            EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 1L }, "u1").reconcile()
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun reconcile_recent_summaries_cancellation_is_rethrown() = runTest {
        val repository = FakeEmotionRepository(cancelRecentRead = true)
        assertFailsWith<CancellationException> {
            EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 1L }, "u1").reconcile()
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun reconcile_record_store_all_cancellation_is_rethrown() = runTest {
        val repository = FakeEmotionRepository()
        repository.saveSessionSummary("u1", emotionSummary("s1", completed = true, abandoned = false))
        assertFailsWith<CancellationException> {
            EmotionTerminalReconciler(repository, CancellingAllRecordStore, EpochClock { 1L }, "u1").reconcile()
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun pending_retry_summary_save_cancellation_is_rethrown() = runTest {
        val repository = FakeEmotionRepository(cancelSummarySave = true)
        repository.savePendingTerminal(
            "u1",
            EmotionPendingTerminalDto(summary = summaryWith("s1", completed = true, abandoned = false, endReason = "COMPLETED"), attemptCount = 1, createdAtMillis = 0L),
        )
        assertFailsWith<CancellationException> {
            EmotionTerminalReconciler(repository, InMemorySessionRecordStore(), EpochClock { 1L }, "u1").reconcile()
        }
        coroutineContext.cancelChildren()
    }

    // §2 — the repository's best-effort maintenance (prune/migration) also rethrows cancellation, never swallows it.
    @Test
    fun summary_save_maintenance_cancellation_is_rethrown_or_explicitly_documented() = runTest {
        val store = PruneFailingStore(InMemorySecureKeyValueStore())
        val repo = LocalEmotionRepository(store)
        store.cancelKeys = true // the prune's keys(...) is cancelled
        assertFailsWith<CancellationException> {
            repo.saveSessionSummary("u1", emotionSummary("sess_c", completed = true, abandoned = false, startedAt = 5L))
        }
        coroutineContext.cancelChildren()
    }

    // §6 invariant 5 — the repository rejects a sessionId that would break its keyspace (contains the delimiter).
    @Test
    fun save_summary_rejects_sessionId_with_key_separator() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        assertFailsWith<IllegalArgumentException> {
            repo.saveSessionSummary("u1", emotionSummary("bad:id", completed = true, abandoned = false))
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun save_pending_rejects_sessionId_with_key_separator() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        assertFailsWith<IllegalArgumentException> {
            repo.savePendingTerminal(
                "u1",
                EmotionPendingTerminalDto(summary = emotionSummary("bad:id", completed = true, abandoned = false), attemptCount = 1, createdAtMillis = 0L),
            )
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun delete_pending_rejects_sessionId_with_key_separator() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        assertFailsWith<IllegalArgumentException> {
            repo.deletePendingTerminal("u1", "bad:id")
        }
        coroutineContext.cancelChildren()
    }

    // ---- fixtures ----

    private var idCounter = 0

    /** Full control over completed/abandoned/endReason (for legacy + endReason-authority tests). */
    private fun summaryWith(
        sessionId: String?,
        completed: Boolean,
        abandoned: Boolean,
        endReason: String?,
        startedAt: Long = 0L,
        durationMs: Long = 0L,
    ): EmotionSessionSummaryDto = EmotionSessionSummaryDto(
        exerciseId = "EM1_PAUSA",
        startedAtMillis = startedAt,
        durationMs = durationMs,
        difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
        metricsSummary = emptyList(),
        completed = completed,
        abandoned = abandoned,
        sessionId = sessionId,
        endReason = endReason,
    )

    /** Writes a raw summary at an EXACT store key (to simulate a legacy startedAt-keyed row that carries a sessionId). */
    private suspend fun putRawSummary(store: InMemorySecureKeyValueStore, key: String, summary: EmotionSessionSummaryDto) {
        store.put(key, EmotionJson.encodeToString(summary))
    }

    private fun staleAggregate(sessionsCompleted: Int): EmotionAggregateDto = EmotionAggregateDto(
        sessionsCompleted = sessionsCompleted,
        tier = "STABLE",
        dataSufficiency = 0.9,
        safetyLoad = 0.0,
        transferConfidence = "NOT_DEMONSTRATED",
        confidence = 0.9,
        trend = "UNKNOWN",
        updatedAtMillis = 1L,
    )

    private fun emotionSummary(
        sessionId: String,
        completed: Boolean,
        abandoned: Boolean,
        startedAt: Long = 0L,
        durationMs: Long = 0L,
    ): EmotionSessionSummaryDto =
        EmotionSessionSummaryDto(
            exerciseId = "EM1_PAUSA",
            startedAtMillis = startedAt,
            durationMs = durationMs,
            difficultySummary = DifficultySummaryDto("INHIBITORY_DEMAND", 0.3, 1),
            metricsSummary = emptyList(),
            completed = completed,
            abandoned = abandoned,
            sessionId = sessionId,
            endReason = when {
                completed -> "COMPLETED"
                abandoned -> "ABANDONED"
                else -> "SAFETY_STOP"
            },
        )

    /** Advances virtual time until a live trial is Running, then pauses — to reach the MicroPause overlay. */
    private fun TestScope.reachRunningThenPause(holder: EmotionSessionStateHolder) {
        if (!advanceToRunning(holder)) error("did not reach a Running state to pause")
        holder.onPause()
    }

    /** Advances virtual time until a live trial is Running; returns false if none appeared. */
    private fun TestScope.advanceToRunning(holder: EmotionSessionStateHolder): Boolean {
        repeat(400) {
            if (holder.uiState is EmotionSessionUiState.Running) return true
            advanceTimeBy(20)
        }
        return false
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
        overloadDetector: OverloadDetector = DefaultOverloadDetector(),
    ): EmotionGraph = EmotionGraph(
        generators = EmotionExerciseGenerators.registry,
        metricsEngine = DefaultEmotionMetricsEngine(),
        assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
        adaptiveEngine = adaptiveEngine,
        overloadDetector = overloadDetector,
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
        overloadDetector: OverloadDetector = DefaultOverloadDetector(),
    ): EmotionSessionStateHolder {
        val graph = emotionGraph(repository, clock, adaptiveEngine, overloadDetector)
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

/** A record store whose `all()` throws a CancellationException, to prove the reconciler rethrows cancellation. */
private object CancellingAllRecordStore : SessionRecordStore {
    override suspend fun upsert(record: SessionRecord) = Unit
    override suspend fun find(sessionId: String): SessionRecord? = null
    override suspend fun all(): List<SessionRecord> = throw CancellationException("cancelled")
    override suspend fun delete(sessionId: String) = Unit
}

/** Deterministically forces a SAFETY_STOP at the first adaptive decision (test double, not production). */
private object SafetyStopEngine : EmotionAdaptiveEngine {
    override fun decide(input: EmotionAdaptiveInput): EmotionAdaptiveDecision =
        EmotionAdaptiveDecision(AdaptiveAction.SAFETY_STOP, input.currentDifficulty, input.intensity)
}

/** Always arms an overload proxy, so a mid-session check-in fires deterministically after block 0. */
private object AlwaysOverloadDetector : OverloadDetector {
    override fun detect(signals: OverloadSignals, thresholds: OverloadThresholds): Set<OverloadProxy> =
        setOf(OverloadProxy.ERROR_STREAK)
}

/** A recorder whose onStart never returns, to prove the best-effort SessionRecord path times out (never freezes). */
private object HangingStartRecorder : SessionRecorder {
    override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
        CompletableDeferred<Unit>().await() // hang forever (until the terminal's best-effort timeout / test cleanup)
    }
    override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit
    override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) = Unit
    override suspend fun onAbandon(sessionId: String) = Unit
}

/** In-memory emotion repository with injectable failures/latency/hang, so those paths are assertable. */
private class FakeEmotionRepository(
    var failSummarySave: Boolean = false,
    private val failRecentRead: Boolean = false,
    private val failAggregateSave: Boolean = false,
    private val failPendingSave: Boolean = false,
    private val failPendingList: Boolean = false,
    private val cancelSummarySave: Boolean = false,
    private val cancelRecentRead: Boolean = false,
    private val cancelPendingList: Boolean = false,
    private val onSave: () -> Unit = {},
    private val hangGate: CompletableDeferred<Unit>? = null,
    private val aggregateHangGate: CompletableDeferred<Unit>? = null,
) : EmotionRepository {
    val summaries = mutableListOf<EmotionSessionSummaryDto>()
    var savedAggregate: EmotionAggregateDto? = null
    private val pendings = mutableMapOf<String, EmotionPendingTerminalDto>()
    val pendingTerminals: List<EmotionPendingTerminalDto> get() = pendings.values.toList()

    override suspend fun saveSessionSummary(userId: String, summary: EmotionSessionSummaryDto) {
        hangGate?.await()
        onSave()
        if (cancelSummarySave) throw CancellationException("cancelled")
        if (failSummarySave) error("summary save failed")
        // Mirror the real repo: sessionId is the primary identity → same session overwrites, not duplicates.
        summaries.removeAll { it.sessionId != null && it.sessionId == summary.sessionId }
        summaries.add(summary)
    }

    override suspend fun recentSummaries(userId: String): List<EmotionSessionSummaryDto> {
        if (cancelRecentRead) throw CancellationException("cancelled")
        if (failRecentRead) error("recent read failed")
        return summaries.toList()
    }

    override suspend fun saveAggregate(userId: String, aggregate: EmotionAggregateDto) {
        aggregateHangGate?.await()
        if (failAggregateSave) error("aggregate save failed")
        savedAggregate = aggregate
    }

    override suspend fun readAggregate(userId: String): EmotionAggregateDto? = savedAggregate

    override suspend fun deleteAggregate(userId: String) {
        savedAggregate = null
    }

    override suspend fun savePendingTerminal(userId: String, pending: EmotionPendingTerminalDto) {
        if (failPendingSave) error("pending save failed")
        pendings[pending.summary.sessionId.orEmpty()] = pending
    }

    override suspend fun listPendingTerminals(userId: String): List<EmotionPendingTerminalDto> {
        if (cancelPendingList) throw CancellationException("cancelled")
        if (failPendingList) error("pending list failed")
        return pendings.values.toList()
    }

    override suspend fun deletePendingTerminal(userId: String, sessionId: String) {
        pendings.remove(sessionId)
    }

    override suspend fun deleteAll(userId: String) {
        summaries.clear()
        savedAggregate = null
        pendings.clear()
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

/**
 * Delegates to a real store but can be told to FAIL `keys(...)` — the call the summary prune makes — so we can
 * prove a prune/maintenance failure never turns a successful canonical `put` into a false failure (MC8h P1).
 */
private class PruneFailingStore(private val delegate: SecureKeyValueStore) : SecureKeyValueStore {
    var failKeys = false
    var cancelKeys = false
    override suspend fun read(key: String): SecureReadResult = delegate.read(key)
    override suspend fun put(key: String, value: String) = delegate.put(key, value)
    override suspend fun remove(key: String) = delegate.remove(key)
    override suspend fun quarantine(key: String, failure: SecureReadResult.Failure) = delegate.quarantine(key, failure)
    override suspend fun keys(prefix: String): Set<String> = when {
        cancelKeys -> throw CancellationException("cancelled")
        failKeys -> error("keys failed")
        else -> delegate.keys(prefix)
    }
    override suspend fun clearNamespace() = delegate.clearNamespace()
}
