package com.mindos.app.features.mindboost.decisiones.presentation.ui

import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityFeedback
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageSummary
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState
import com.mindverse.mindboost.core.mdp.reversib.ReversibilityDecisionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Closes the P3b live-glue coverage gap: the root's persist-on-finish orchestration is now a pure function,
 * so we can assert it persists the captured trials BEFORE advancing the flow, without driving Compose.
 */
class HandleReversibilityFinishTest {

    private fun feedback(actual: Boolean, selected: Boolean) = ReversibilityFeedback(
        selectedReversible = selected,
        actuallyReversible = actual,
        correct = selected == actual,
        decisionType = if (actual) ReversibilityDecisionType.TYPE_2_REVERSIBLE else ReversibilityDecisionType.TYPE_1_IRREVERSIBLE,
        reactionTimeMs = 500L,
        lockRespected = true,
        delayPenalty = 0.0,
    )

    private fun finished(perCard: List<ReversibilityFeedback>) = ReversibilityTriageUiState.Finished(
        rounds = emptyList(),
        perCard = perCard,
        summary = ReversibilityTriageSummary(
            rounds = perCard.size,
            accuracy = 0.0,
            meanReactionMs = 0L,
            lockViolations = 0,
            meanDelayPenalty = 0.0,
        ),
    )

    @Test
    fun finished_persistsCapturedTrials_thenAdvances() {
        val calls = mutableListOf<String>()
        var persisted: List<ReversibilityTrial>? = null
        handleReversibilityFinish(
            finishedState = finished(listOf(feedback(true, true), feedback(false, false))),
            rounds = emptyList(),
            persist = { calls += "persist"; persisted = it },
            advance = { calls += "advance" },
        )
        assertEquals(listOf("persist", "advance"), calls, "persist must run before advancing the flow")
        assertEquals(2, assertNotNull(persisted).size, "both finished cards are captured as trials")
    }

    @Test
    fun nonFinished_persistsNoTrials_butStillAdvances() {
        val calls = mutableListOf<String>()
        var persisted: List<ReversibilityTrial>? = null
        handleReversibilityFinish(
            finishedState = ReversibilityTriageUiState.Idle,
            rounds = emptyList(),
            persist = { calls += "persist"; persisted = it },
            advance = { calls += "advance" },
        )
        assertEquals(listOf("persist", "advance"), calls)
        assertTrue(assertNotNull(persisted).isEmpty(), "a non-finished state persists no trials")
    }
}
