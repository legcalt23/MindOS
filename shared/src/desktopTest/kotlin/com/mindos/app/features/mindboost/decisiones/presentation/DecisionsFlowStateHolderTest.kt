package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionRound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DecisionsFlowStateHolderTest {
    @Test
    fun flowAdvancesThroughStagesAndCollectsInputs() {
        // Opt into the (currently hidden) Reversibility stage to keep the full 4-stage path covered for
        // when the candidate bank is validated and includeReversibility is flipped back on.
        val flow = DecisionsFlowStateHolder(sessionId = "s-1", timestampMs = 7L, includeReversibility = true)
        assertEquals(DecisionsFlowStage.CheckIn, flow.stage)

        flow.onCheckInComplete(checkInInput())
        assertEquals(DecisionsFlowStage.BayesArena, flow.stage)

        flow.onBayesComplete(listOf(round(), round()))
        assertEquals(DecisionsFlowStage.Reversibility, flow.stage)

        flow.onReversibilityComplete(listOf(round()))
        assertEquals(DecisionsFlowStage.Result, flow.stage)

        assertEquals(3, flow.rounds.size)
        assertEquals("s-1", flow.resultSessionId)
        assertEquals(7L, flow.resultTimestampMs)
        assertEquals(0.8, flow.checkInInput().goNoGoAccuracy, 0.0001)
    }

    @Test
    fun reversibilityHiddenByDefault_bayesSkipsStraightToResult() {
        // Default (includeReversibility = false): the degenerate Reversibility stage is hidden, so Bayes
        // advances directly to Result and a late onReversibilityComplete is a no-op (never in that stage).
        val flow = DecisionsFlowStateHolder(sessionId = "s-h", timestampMs = 3L)

        flow.onCheckInComplete(checkInInput())
        assertEquals(DecisionsFlowStage.BayesArena, flow.stage)

        flow.onBayesComplete(listOf(round(), round()))
        assertEquals(DecisionsFlowStage.Result, flow.stage, "Reversibility stage is skipped when hidden")

        flow.onReversibilityComplete(listOf(round()))
        assertEquals(DecisionsFlowStage.Result, flow.stage)
        assertEquals(2, flow.rounds.size, "only the Bayes rounds are collected; no reversibility rounds")
    }

    @Test
    fun outOfOrderCompletionsAreIgnored() {
        val flow = DecisionsFlowStateHolder(sessionId = "s", timestampMs = 1L)

        flow.onBayesComplete(listOf(round()))
        flow.onReversibilityComplete(listOf(round()))

        assertEquals(DecisionsFlowStage.CheckIn, flow.stage)
        assertTrue(flow.rounds.isEmpty())
    }

    @Test
    fun checkInInputBeforeCompletionThrows() {
        val flow = DecisionsFlowStateHolder(sessionId = "s", timestampMs = 1L)

        assertFailsWith<IllegalStateException> { flow.checkInInput() }
    }

    @Test
    fun completionsAfterAdvancingAreIgnored() {
        // Uses the full path (includeReversibility = true) so the "ignored after Result" guard is exercised
        // across all four stages, independent of the default hidden-Reversibility flow.
        val flow = DecisionsFlowStateHolder(sessionId = "s", timestampMs = 1L, includeReversibility = true)

        flow.onCheckInComplete(checkInInput())
        flow.onBayesComplete(listOf(round()))
        flow.onReversibilityComplete(listOf(round()))
        assertEquals(DecisionsFlowStage.Result, flow.stage)

        // Extra completions at the wrong stage do nothing.
        flow.onCheckInComplete(checkInInput())
        flow.onReversibilityComplete(listOf(round()))

        assertEquals(DecisionsFlowStage.Result, flow.stage)
        assertEquals(2, flow.rounds.size)
    }

    private fun checkInInput() = CheckInInput(
        goNoGoAccuracy = 0.8,
        reactionTimeCV = 0.2,
        subjectiveFatigue = 0.5,
        reactionTimeBaselineMs = 1_000L,
    )

    private fun round() = DecisionRound(
        difficulty = baseDifficulty(),
        objectivesAlignment = 1.0,
        bayesianUpdateRatio = 1.0,
        biasDetected = false,
        alternativesScannedRatio = 1.0,
        reactionTimeMs = 1_000L,
        timeLimitMs = 10_000L,
        isReversible = false,
        spatialEntropy = 0.5,
        isDialDefault = false,
    )

    private fun baseDifficulty() = DifficultyVector(
        numberOfVariables = 3,
        numberOfAlternatives = 2,
        timePressure = 0.5,
        noiseLevel = 0.5,
        ambiguityLevel = 0.5,
        irreversibility = 0.5,
        informationCost = 0.5,
        evidenceContradiction = 0.5,
        memoryLoad = 3,
        justificationDemand = 0.5,
    )
}
