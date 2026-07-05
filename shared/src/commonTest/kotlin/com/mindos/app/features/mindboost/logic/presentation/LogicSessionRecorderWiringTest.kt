package com.mindos.app.features.mindboost.logic.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorderDiagnostics
import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.logic.data.local.LocalLogicTrainingRepository
import com.mindos.app.features.mindboost.logic.di.LogicGraph
import com.mindos.app.features.mindboost.logic.di.logicSessionRecorder
import com.mindos.app.features.mindboost.logic.domain.adaptive.DefaultLogicDifficultyAdapter
import com.mindos.app.features.mindboost.logic.domain.content.DefaultLogicContentProvider
import com.mindos.app.features.mindboost.logic.domain.feedback.DefaultLogicFeedbackEngine
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfidence
import com.mindos.app.features.mindboost.logic.domain.scoring.DefaultReasoningScorer
import com.mindos.app.features.mindboost.logic.presentation.state.LogicUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * MC2a: verifies the ADDITIVE write-ahead SessionRecorder wiring in Lógica. Drives a real turn-based session
 * with a fake recorder/generator and asserts STARTED->FINALIZED (no checkpoint — Lógica has no sub-block), an
 * opaque stable sessionId not derived from the user, and that a recorder storage failure never breaks the
 * Lógica session (scoring/persistence stay intact).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogicSessionRecorderWiringTest {

    private class FakeRecorder : SessionRecorder {
        val ops = mutableListOf<String>()
        val ids = mutableListOf<String>()
        var startBlock: ScoreModuleId? = null
        var startPlanned: Int = -1
        var finalizeCompleted: Int = -1

        override suspend fun onStart(sessionId: String, block: ScoreModuleId, startedAtMs: Long, plannedTrials: Int) {
            ops += "onStart"; ids += sessionId; startBlock = block; startPlanned = plannedTrials
        }

        override suspend fun onCheckpoint(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
            ops += "onCheckpoint"; ids += sessionId
        }

        override suspend fun onFinalize(sessionId: String, completedTrials: Int, activeDurationMs: Long) {
            ops += "onFinalize"; ids += sessionId; finalizeCompleted = completedTrials
        }

        override suspend fun onAbandon(sessionId: String) {
            ops += "onAbandon"; ids += sessionId
        }
    }

    private class ThrowingSessionRecordStore : SessionRecordStore {
        override suspend fun upsert(record: SessionRecord) = error("boom")
        override suspend fun find(sessionId: String): SessionRecord? = error("boom")
        override suspend fun all(): List<SessionRecord> = error("boom")
        override suspend fun delete(sessionId: String) = error("boom")
    }

    private class RecordingDiagnostics : SessionRecorderDiagnostics {
        val ops = mutableListOf<String>()
        override fun onRecorderError(operation: String) {
            ops += operation
        }
    }

    private fun TestScope.holder(
        repository: LocalLogicTrainingRepository,
        recorder: SessionRecorder,
        generator: SessionIdGenerator = SessionIdGenerator { "sess_logic" },
    ): LogicStateHolder {
        var now = 1_000L
        val graph = LogicGraph(
            contentProvider = DefaultLogicContentProvider(),
            scorer = DefaultReasoningScorer(),
            difficultyAdapter = DefaultLogicDifficultyAdapter(),
            feedbackEngine = DefaultLogicFeedbackEngine(),
            repository = repository,
            epochClock = EpochClock { now.also { now += 3_000 } },
        )
        return LogicStateHolder(graph, "user-1", "Ada", this, recorder, generator)
    }

    private fun answer(holder: LogicStateHolder) {
        val question = assertIs<LogicUiState.Question>(holder.uiState)
        question.item.requiredPremiseIds.forEach(holder::togglePremise)
        holder.selectAnswer(question.item.correctAnswerId)
        holder.selectRule(question.item.validReasoningRules.first())
        holder.selectClaim(question.item.expectedJustificationClaim)
        holder.selectConfidence(LogicConfidence.HIGH)
        holder.submitAnswer()
    }

    private fun TestScope.drive(holder: LogicStateHolder) {
        holder.startSession()
        runCurrent()
        repeat(DefaultLogicContentProvider().sessionItems().size) {
            answer(holder)
            holder.continueSession()
            runCurrent()
        }
    }

    @Test
    fun session_startThenFinalize_noCheckpoint_opaqueStableId() = runTest {
        val rec = FakeRecorder()
        val holder = holder(LocalLogicTrainingRepository(InMemorySecureKeyValueStore()), rec)
        drive(holder)
        assertEquals(listOf("onStart", "onFinalize"), rec.ops)
        assertEquals(ScoreModuleId.LOGIC, rec.startBlock)
        assertEquals(6, rec.startPlanned)
        assertEquals(6, rec.finalizeCompleted)
        assertTrue(rec.ids.all { it == "sess_logic" }, "sessionId is generated once and stable")
        assertTrue(rec.ids.none { it.contains("user-1") }, "sessionId must not embed the userId")
    }

    @Test
    fun recorderStorageFailure_doesNotBreakSession_orScoring() = runTest {
        val repository = LocalLogicTrainingRepository(InMemorySecureKeyValueStore())
        val diag = RecordingDiagnostics()
        val recorder = SecureSessionRecorder(ThrowingSessionRecordStore(), EpochClock { 1L }, diag)
        val holder = holder(repository, recorder)
        drive(holder)
        assertIs<LogicUiState.Summary>(holder.uiState)
        assertEquals(6, repository.recentSessions("user-1", 1).single().attempts.size)
        assertTrue(diag.ops.isNotEmpty(), "storage failure must be diagnosed, not swallowed silently")
    }

    @Test
    fun productionFactory_persistsFinalizedSessionRecord_toSessionsStore() = runTest {
        // Exercises the REAL production wiring: logicSessionRecorder (the factory rememberLogicStateHolder
        // injects) over a real SecureSessionRecordStore backed by an in-memory secure store.
        val sessionsBacking = InMemorySecureKeyValueStore()
        val recorder = logicSessionRecorder(sessionsBacking, EpochClock { 1_000L })
        val holder = holder(
            LocalLogicTrainingRepository(InMemorySecureKeyValueStore()), recorder, RandomSessionIdGenerator(),
        )
        drive(holder)
        val recorded = SecureSessionRecordStore(sessionsBacking).all()
        assertEquals(1, recorded.size, "exactly one SessionRecord persisted")
        val record = recorded.single()
        assertEquals(SessionStatus.FINALIZED, record.status)
        assertEquals(ScoreModuleId.LOGIC, record.block)
        assertEquals(6, record.completedTrials)
    }
}
