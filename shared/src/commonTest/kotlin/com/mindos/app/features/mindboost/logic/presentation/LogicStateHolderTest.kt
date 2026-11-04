package com.mindos.app.features.mindboost.logic.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.logic.data.local.LocalLogicTrainingRepository
import com.mindos.app.features.mindboost.logic.di.LogicGraph
import com.mindos.app.features.mindboost.logic.domain.adaptive.DefaultLogicDifficultyAdapter
import com.mindos.app.features.mindboost.logic.domain.content.DefaultLogicContentProvider
import com.mindos.app.features.mindboost.logic.domain.feedback.DefaultLogicFeedbackEngine
import com.mindos.app.features.mindboost.logic.domain.model.LogicScoringVersion
import com.mindos.app.features.mindboost.logic.domain.scoring.DefaultReasoningScorer
import com.mindos.app.features.mindboost.logic.presentation.state.LogicUiState
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class LogicStateHolderTest {

    @Test
    fun completeSessionPersistsAttemptsProgressAndSummary() = runTest {
        val repository = LocalLogicTrainingRepository(InMemorySecureKeyValueStore())
        val holder = holder(repository)

        holder.loadHome()
        runCurrent()
        assertIs<LogicUiState.Home>(holder.uiState)

        holder.startSession()
        runCurrent()
        repeat(DefaultLogicContentProvider().sessionItems().size) {
            answerCurrentCorrectly(holder)
            assertIs<LogicUiState.Feedback>(holder.uiState)
            holder.continueSession()
            runCurrent()
        }

        val summary = assertIs<LogicUiState.Summary>(holder.uiState)
        assertEquals(1.0, summary.result.inferentialAccuracy)
        assertNotNull(summary.result.transferConsistency)
        val saved = repository.recentSessions("user-1", 1).single()
        assertEquals(6, saved.attempts.size)
        // The producer must stamp the instrument that measured the session. If this regresses, every
        // new session is filed as legacy, the trend guard sees a single version forever, and it stops
        // being able to tell a real decline from an instrument change.
        assertEquals(LogicScoringVersion.CURRENT, saved.result.scoringVersion)
        assertEquals(1, repository.loadSkillStates("user-1").first().attemptsCount)
    }

    @Test
    fun failedIsomorphicProbeStoresZeroTransferConsistency() = runTest {
        val repository = LocalLogicTrainingRepository(InMemorySecureKeyValueStore())
        val holder = holder(repository)
        holder.startSession()
        runCurrent()

        repeat(DefaultLogicContentProvider().sessionItems().size) {
            val question = assertIs<LogicUiState.Question>(holder.uiState)
            answer(holder, correct = !question.item.isIsomorphicProbe)
            holder.continueSession()
            runCurrent()
        }

        val probe = repository.recentSessions("user-1", 1).single().attempts.first { it.isIsomorphicProbe }
        assertEquals(0.0, probe.scoreBreakdown.transferConsistencyScore)
    }

    private fun TestScope.holder(repository: LocalLogicTrainingRepository): LogicStateHolder {
        var now = 1_000L
        val graph = LogicGraph(
            contentProvider = DefaultLogicContentProvider(),
            scorer = DefaultReasoningScorer(),
            difficultyAdapter = DefaultLogicDifficultyAdapter(),
            feedbackEngine = DefaultLogicFeedbackEngine(),
            repository = repository,
            epochClock = EpochClock { now.also { now += 3_000 } },
        )
        return LogicStateHolder(graph, "user-1", "Ada", this)
    }

    private fun answerCurrentCorrectly(holder: LogicStateHolder) {
        val question = assertIs<LogicUiState.Question>(holder.uiState)
        answer(holder, correct = true, question = question)
    }

    private fun answer(
        holder: LogicStateHolder,
        correct: Boolean,
        question: LogicUiState.Question = assertIs(holder.uiState),
    ) {
        question.item.requiredPremiseIds.forEach(holder::togglePremise)
        val answerId = if (correct) {
            question.item.correctAnswerId
        } else {
            question.item.answerOptions.first { !it.isCorrect }.id
        }
        holder.selectAnswer(answerId)
        holder.selectRule(question.item.validReasoningRules.first())
        holder.selectClaim(question.item.expectedJustificationClaim)
        holder.selectConfidence(com.mindos.app.features.mindboost.logic.domain.model.LogicConfidence.HIGH)
        holder.submitAnswer()
    }
}
