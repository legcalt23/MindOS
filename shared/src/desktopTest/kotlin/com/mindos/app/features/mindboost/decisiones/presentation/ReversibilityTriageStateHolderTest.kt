package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState
import com.mindverse.mindboost.core.mdp.reversib.ReversibilityDecisionType
import com.mindverse.mindboost.core.domain.ReversibilityScenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReversibilityTriageStateHolderTest {
    @Test
    fun startPresentsFirstCard() {
        val holder = holderOf(reversible(), irreversible())

        holder.start()

        val running = assertIs<ReversibilityTriageUiState.Running>(holder.uiState)
        assertEquals(0, running.cardIndex)
        assertEquals(2, running.cardCount)
        assertEquals("reversible card", running.prompt)
    }

    @Test
    fun correctReversibleClassificationScoresFullAlignment() {
        val holder = holderOf(reversible())

        holder.start()
        holder.commit(selectedReversible = true, reactionTimeMs = 1_000L)

        val finished = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)
        val card = finished.perCard[0]
        assertTrue(card.correct)
        assertEquals(ReversibilityDecisionType.TYPE_2_REVERSIBLE, card.decisionType)
        assertEquals(1.0, finished.rounds[0].objectivesAlignment, 0.0001)
        assertTrue(finished.rounds[0].isReversible)
        assertEquals(1.0, finished.summary.accuracy, 0.0001)
    }

    @Test
    fun wrongClassificationScoresZeroAlignment() {
        val holder = holderOf(reversible())

        holder.start()
        holder.commit(selectedReversible = false, reactionTimeMs = 1_000L)

        val finished = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)
        assertFalse(finished.perCard[0].correct)
        assertEquals(0.0, finished.rounds[0].objectivesAlignment, 0.0001)
        assertEquals(0.0, finished.summary.accuracy, 0.0001)
    }

    @Test
    fun irreversibleConfirmedAfterLockIsRespected() {
        val holder = holderOf(irreversible(lockMs = 3_000L))

        holder.start()
        holder.commit(selectedReversible = false, reactionTimeMs = 3_500L)

        val finished = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)
        val card = finished.perCard[0]
        assertEquals(ReversibilityDecisionType.TYPE_1_IRREVERSIBLE, card.decisionType)
        assertTrue(card.correct)
        assertTrue(card.lockRespected)
        assertEquals(0, finished.summary.lockViolations)
    }

    @Test
    fun irreversibleConfirmedBeforeLockIsAViolation() {
        val holder = holderOf(irreversible(lockMs = 3_000L))

        holder.start()
        holder.commit(selectedReversible = false, reactionTimeMs = 1_000L)

        val finished = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)
        assertFalse(finished.perCard[0].lockRespected)
        assertEquals(1, finished.summary.lockViolations)
    }

    @Test
    fun slowReversibleDecisionAccruesDelayPenalty() {
        val holder = holderOf(reversible(delayCostRate = 2.0))

        holder.start()
        holder.commit(selectedReversible = true, reactionTimeMs = 4_000L)

        val finished = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)
        assertEquals(8.0, finished.perCard[0].delayPenalty, 0.0001) // 4.0s * 2.0
        assertEquals(8.0, finished.summary.meanDelayPenalty, 0.0001)
    }

    @Test
    fun currentLockDurationIsHiddenUntilAwaitingAnIrreversibleCard() {
        val holder = holderOf(irreversible(lockMs = 3_000L))

        assertEquals(0L, holder.currentLockDurationMs()) // before start
        holder.start()
        assertEquals(3_000L, holder.currentLockDurationMs()) // awaiting the card
        holder.commit(selectedReversible = false, reactionTimeMs = 3_500L)
        assertEquals(0L, holder.currentLockDurationMs()) // finished

        val reversibleHolder = holderOf(reversible())
        reversibleHolder.start()
        assertEquals(0L, reversibleHolder.currentLockDurationMs()) // reversible cards never lock
    }

    @Test
    fun summaryAggregatesAcrossCards() {
        val holder = holderOf(reversible(delayCostRate = 2.0), irreversible(lockMs = 3_000L))

        holder.start()
        holder.commit(selectedReversible = true, reactionTimeMs = 1_000L)
        holder.commit(selectedReversible = false, reactionTimeMs = 3_500L)

        val finished = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)
        assertEquals(2, finished.summary.rounds)
        assertEquals(1.0, finished.summary.accuracy, 0.0001)
        assertEquals(2_250L, finished.summary.meanReactionMs)
        assertEquals(0, finished.summary.lockViolations)
        assertEquals(2.0, finished.summary.meanDelayPenalty, 0.0001) // only the reversible card
    }

    @Test
    fun commitsBeforeStartAreIgnored() {
        val holder = holderOf(reversible())

        holder.commit(selectedReversible = true, reactionTimeMs = 1_000L)

        assertEquals(ReversibilityTriageUiState.Idle, holder.uiState)
    }

    @Test
    fun commitsAfterFinishAreIgnored() {
        val holder = holderOf(reversible())

        holder.start()
        holder.commit(selectedReversible = true, reactionTimeMs = 1_000L)
        val first = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)
        holder.commit(selectedReversible = false, reactionTimeMs = 1_000L)
        val second = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState)

        assertEquals(first.summary.rounds, second.summary.rounds)
        assertEquals(first.summary.accuracy, second.summary.accuracy, 0.0001)
    }

    @Test
    fun emptyScenariosAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ReversibilityTriageStateHolder(scenarios = emptyList())
        }
    }

    @Test
    fun generateScenariosIsDeterministic() {
        val first = ReversibilityTriageStateHolder.generateScenarios(count = 5, seed = 3L)
        val second = ReversibilityTriageStateHolder.generateScenarios(count = 5, seed = 3L)

        assertEquals(first, second)
        assertEquals(5, first.size)
    }

    private fun reversible(delayCostRate: Double = 1.0): ReversibilityScenario = ReversibilityScenario(
        id = "r",
        description = "reversible card",
        isReversible = true,
        delayCostRate = delayCostRate,
        lockedDurationMs = 0L,
    )

    private fun irreversible(lockMs: Long = 3_000L): ReversibilityScenario = ReversibilityScenario(
        id = "i",
        description = "irreversible card",
        isReversible = false,
        delayCostRate = 0.0,
        lockedDurationMs = lockMs,
    )

    private fun holderOf(vararg scenarios: ReversibilityScenario) =
        ReversibilityTriageStateHolder(scenarios = scenarios.toList())
}
