package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.decisiones.presentation.state.BayesArenaUiState
import com.mindverse.mindboost.core.domain.BayesScenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BayesArenaStateHolderTest {
    @Test
    fun startPresentsFirstScenarioWithoutLeakingTheAnswer() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5), scenarioOf(prior = 0.1, math = 0.3))

        holder.start()

        val running = assertIs<BayesArenaUiState.Running>(holder.uiState)
        assertEquals(0, running.scenarioIndex)
        assertEquals(2, running.scenarioCount)
        assertEquals(20.0, running.baseRatePercent, 0.0001)
        assertEquals(90.0, running.sensitivityPercent, 0.0001)
        assertEquals(5.0, running.falsePositivePercent, 0.0001)
    }

    @Test
    fun perfectUpdateScoresBurOne() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5))

        holder.start()
        holder.submit(userPosterior = 0.5, confidence = 0.8, reactionTimeMs = 3_000L)

        val finished = assertIs<BayesArenaUiState.Finished>(holder.uiState)
        assertEquals(1, finished.summary.rounds)
        val feedback = finished.perScenario[0]
        assertEquals(1.0, feedback.bayesianUpdateRatio, 0.0001)
        assertEquals(0.0, feedback.absoluteError, 0.0001)
        assertEquals(0.5, feedback.mathPosterior, 0.0001)
        assertEquals(0.8, feedback.confidence, 0.0001)

        val round = finished.rounds[0]
        assertEquals(1.0, round.bayesianUpdateRatio, 0.0001)
        assertEquals(1.0, round.objectivesAlignment, 0.0001) // 1 - |0.5 - 0.5|
        assertFalse(round.isReversible)
    }

    @Test
    fun noUpdateScoresBurZero() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5))

        holder.start()
        holder.submit(userPosterior = 0.2, confidence = 0.7, reactionTimeMs = 3_000L)

        val finished = assertIs<BayesArenaUiState.Finished>(holder.uiState)
        val feedback = finished.perScenario[0]
        assertEquals(0.0, feedback.bayesianUpdateRatio, 0.0001)
        assertEquals(0.3, feedback.absoluteError, 0.0001) // |0.2 - 0.5|
        assertEquals(0.7, finished.rounds[0].objectivesAlignment, 0.0001) // 1 - 0.3
    }

    @Test
    fun summaryAggregatesAcrossScenarios() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5), scenarioOf(prior = 0.2, math = 0.5))

        holder.start()
        holder.submit(userPosterior = 0.5, confidence = 0.8, reactionTimeMs = 1_000L) // BUR 1.0, err 0.0
        holder.submit(userPosterior = 0.2, confidence = 0.6, reactionTimeMs = 1_000L) // BUR 0.0, err 0.3

        val finished = assertIs<BayesArenaUiState.Finished>(holder.uiState)
        assertEquals(2, finished.summary.rounds)
        assertEquals(0.5, finished.summary.meanBayesianUpdateRatio, 0.0001)
        assertEquals(0.15, finished.summary.meanAbsoluteError, 0.0001)
        assertEquals(0.7, finished.summary.meanConfidence, 0.0001)
        assertEquals(2, finished.rounds.size)
    }

    @Test
    fun confidenceIsClampedToFloor() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5))

        holder.start()
        holder.submit(userPosterior = 0.5, confidence = 0.3, reactionTimeMs = 1_000L)

        val finished = assertIs<BayesArenaUiState.Finished>(holder.uiState)
        assertEquals(0.5, finished.perScenario[0].confidence, 0.0001)
    }

    @Test
    fun posteriorIsClampedToUnitInterval() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5))

        holder.start()
        holder.submit(userPosterior = 1.5, confidence = 0.8, reactionTimeMs = 1_000L)

        val finished = assertIs<BayesArenaUiState.Finished>(holder.uiState)
        assertEquals(1.0, finished.perScenario[0].userPosterior, 0.0001)
        assertEquals(0.5, finished.perScenario[0].absoluteError, 0.0001) // |1.0 - 0.5|
        assertEquals(0.5, finished.rounds[0].objectivesAlignment, 0.0001)
    }

    @Test
    fun responsesBeforeStartAreIgnored() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5))

        holder.submit(userPosterior = 0.5, confidence = 0.8, reactionTimeMs = 1_000L)

        assertEquals(BayesArenaUiState.Idle, holder.uiState)
    }

    @Test
    fun responsesAfterFinishAreIgnored() {
        val holder = holderOf(scenarioOf(prior = 0.2, math = 0.5))

        holder.start()
        holder.submit(userPosterior = 0.5, confidence = 0.8, reactionTimeMs = 1_000L)
        val firstFinished = assertIs<BayesArenaUiState.Finished>(holder.uiState)
        holder.submit(userPosterior = 0.2, confidence = 0.9, reactionTimeMs = 1_000L)
        val stillFinished = assertIs<BayesArenaUiState.Finished>(holder.uiState)

        assertEquals(firstFinished.summary.rounds, stillFinished.summary.rounds)
        assertEquals(
            firstFinished.summary.meanBayesianUpdateRatio,
            stillFinished.summary.meanBayesianUpdateRatio,
            0.0001,
        )
    }

    @Test
    fun emptyScenariosAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            BayesArenaStateHolder(scenarios = emptyList())
        }
    }

    @Test
    fun generateScenariosIsDeterministicAndWellFormed() {
        val first = BayesArenaStateHolder.generateScenarios(count = 4, seed = 5L)
        val second = BayesArenaStateHolder.generateScenarios(count = 4, seed = 5L)

        assertEquals(first, second)
        assertEquals(4, first.size)
        assertTrue(first.all { it.expectedPosterior in 0.001..0.999 })
    }

    private fun scenarioOf(prior: Double, math: Double): BayesScenario = BayesScenario(
        baseRate = prior,
        sensitivity = 0.9,
        falsePositiveRate = 0.05,
        expectedPosterior = math,
    )

    private fun holderOf(vararg scenarios: BayesScenario) =
        BayesArenaStateHolder(scenarios = scenarios.toList())
}
