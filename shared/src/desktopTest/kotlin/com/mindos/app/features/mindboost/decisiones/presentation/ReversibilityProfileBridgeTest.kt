package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityScorer
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityBlockState
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityFeedback
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageSummary
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState
import com.mindverse.mindboost.core.mdp.reversib.ReversibilityDecisionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * E2 Reversibilidad bridge (Decisiones · P2) · a finished triage run maps 1:1 to domain trials carrying
 * only actuallyReversible/selectedReversible — feeding the honest scorer with no RT leakage.
 */
class ReversibilityProfileBridgeTest {

    private fun feedback(actual: Boolean, selected: Boolean) = ReversibilityFeedback(
        selectedReversible = selected,
        actuallyReversible = actual,
        correct = selected == actual,
        decisionType = if (actual) ReversibilityDecisionType.TYPE_2_REVERSIBLE else ReversibilityDecisionType.TYPE_1_IRREVERSIBLE,
        reactionTimeMs = 1234L,
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
    fun mapsEachCardToATrial_preservingTruthAndChoice() {
        val state = finished(
            listOf(
                feedback(actual = true, selected = true),
                feedback(actual = false, selected = true), // wrong: said reversible on an irreversible card
                feedback(actual = false, selected = false),
            ),
        )
        val trials = state.toReversibilityTrials()
        assertEquals(3, trials.size)
        assertEquals(listOf(true, false, false), trials.map { it.actuallyReversible })
        assertEquals(listOf(true, true, false), trials.map { it.selectedReversible })
        assertEquals(listOf(true, false, true), trials.map { it.correct })
    }

    @Test
    fun emptyRun_mapsToNoTrials() {
        assertEquals(emptyList(), finished(emptyList()).toReversibilityTrials())
    }

    @Test
    fun reversibilityTrialsOrEmpty_finished_returnsTheTrials() {
        val state: ReversibilityTriageUiState = finished(
            listOf(feedback(actual = true, selected = true), feedback(actual = false, selected = false)),
        )
        assertEquals(2, state.reversibilityTrialsOrEmpty().size)
    }

    @Test
    fun reversibilityTrialsOrEmpty_nonFinished_isEmpty() {
        assertTrue(ReversibilityTriageUiState.Idle.reversibilityTrialsOrEmpty().isEmpty())
        val running: ReversibilityTriageUiState =
            ReversibilityTriageUiState.Running(cardIndex = 0, cardCount = 6, prompt = "x")
        assertTrue(running.reversibilityTrialsOrEmpty().isEmpty(), "a mid-run state persists nothing")
    }

    @Test
    fun bridgedTrials_feedTheHonestScorer_perfectRunIsValid() {
        val perfect = (0 until 5).map { feedback(actual = true, selected = true) } +
            (0 until 5).map { feedback(actual = false, selected = false) }
        val block = ReversibilityScorer.scoreBlock(finished(perfect).toReversibilityTrials())
        assertEquals(ReversibilityBlockState.VALID, block.state)
        assertEquals(1.0, block.discriminationIndex)
    }
}
