package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.decisiones.presentation.state.DecisionCheckInUiState
import com.mindos.app.features.mindboost.decisiones.presentation.state.GoNoGoStimulus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DecisionCheckInStateHolderTest {
    @Test
    fun startPresentsFirstStimulus() {
        val holder = holderOf(GoNoGoStimulus.GO, GoNoGoStimulus.NO_GO)

        holder.start()

        val running = assertIs<DecisionCheckInUiState.Running>(holder.uiState)
        assertEquals(0, running.trialIndex)
        assertEquals(2, running.trialCount)
        assertEquals(GoNoGoStimulus.GO, running.stimulus)
    }

    @Test
    fun finishExposesTheCheckInInputForSessionAssembly() {
        val holder = holderOf(
            GoNoGoStimulus.GO,
            GoNoGoStimulus.GO,
            GoNoGoStimulus.NO_GO,
            GoNoGoStimulus.NO_GO,
        )

        holder.start()
        holder.onTap(400L)
        holder.onTap(600L)
        holder.onTimeout()
        holder.onTimeout()

        val finished = assertIs<DecisionCheckInUiState.Finished>(holder.uiState)
        assertEquals(1.0, finished.checkInInput.goNoGoAccuracy, 0.0001)
        assertEquals(500L, finished.checkInInput.reactionTimeBaselineMs)
    }

    @Test
    fun perfectRunComputesAccuracyAndReactionVariability() {
        val holder = holderOf(
            GoNoGoStimulus.GO,
            GoNoGoStimulus.GO,
            GoNoGoStimulus.NO_GO,
            GoNoGoStimulus.NO_GO,
        )

        holder.start()
        holder.onTap(400L)
        holder.onTap(600L)
        holder.onTimeout()
        holder.onTimeout()

        val finished = assertIs<DecisionCheckInUiState.Finished>(holder.uiState)
        assertEquals(1.0, finished.summary.accuracy, 0.0001)
        assertEquals(4, finished.summary.correctResponses)
        assertEquals(4, finished.summary.totalTrials)
        assertEquals(500L, finished.summary.meanReactionMs)
        // CV = sd/mean over hits [400,600] = sqrt(20000)/500 = 0.2828427
        assertEquals(0.2828427, finished.summary.reactionTimeCv, 0.0001)
        // CRI = 50*1 + 30*(1-0.2828427) + 20*0.7 (default fatigue)
        assertEquals(85.514718, finished.readiness.criValue, 0.001)
        assertEquals(1.0, finished.readiness.goNoGoAccuracy, 0.0001)
    }

    @Test
    fun missAndFalseAlarmDropAccuracy() {
        val holder = holderOf(GoNoGoStimulus.GO, GoNoGoStimulus.NO_GO)

        holder.start()
        holder.onTimeout() // GO missed
        holder.onTap(300L) // NO_GO false alarm

        val finished = assertIs<DecisionCheckInUiState.Finished>(holder.uiState)
        assertEquals(0.0, finished.summary.accuracy, 0.0001)
        assertEquals(0, finished.summary.correctResponses)
        assertEquals(0.0, finished.summary.reactionTimeCv, 0.0001)
        // CRI = 50*0 + 30*1 + 20*0.7
        assertEquals(44.0, finished.readiness.criValue, 0.0001)
    }

    @Test
    fun subjectiveFatigueFeedsThroughToCri() {
        val holder = holderOf(GoNoGoStimulus.GO)

        holder.setSubjectiveFatigue(0.0)
        holder.start()
        holder.onTap(500L)

        val finished = assertIs<DecisionCheckInUiState.Finished>(holder.uiState)
        // CRI = 50*1 + 30*1 + 20*0 = 80
        assertEquals(80.0, finished.readiness.criValue, 0.0001)
    }

    @Test
    fun responsesBeforeStartAreIgnored() {
        val holder = holderOf(GoNoGoStimulus.GO)

        holder.onTap(100L)

        assertEquals(DecisionCheckInUiState.Idle, holder.uiState)
    }

    @Test
    fun responsesAfterFinishAreIgnored() {
        val holder = holderOf(GoNoGoStimulus.GO)

        holder.start()
        holder.onTap(200L)
        val firstFinished = assertIs<DecisionCheckInUiState.Finished>(holder.uiState)
        holder.onTap(999L)
        val stillFinished = assertIs<DecisionCheckInUiState.Finished>(holder.uiState)

        assertEquals(firstFinished.summary.totalTrials, stillFinished.summary.totalTrials)
        assertEquals(firstFinished.readiness.criValue, stillFinished.readiness.criValue, 0.0001)
    }

    @Test
    fun generateStimuliIsDeterministicAndCoversBothCategories() {
        val first = DecisionCheckInStateHolder.generateStimuli(count = 12, seed = 7L)
        val second = DecisionCheckInStateHolder.generateStimuli(count = 12, seed = 7L)

        assertEquals(first, second)
        assertTrue(first.any { it == GoNoGoStimulus.GO })
        assertTrue(first.any { it == GoNoGoStimulus.NO_GO })
    }

    @Test
    fun emptyStimuliIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            DecisionCheckInStateHolder(stimuli = emptyList())
        }
    }

    private fun holderOf(vararg stimuli: GoNoGoStimulus) =
        DecisionCheckInStateHolder(stimuli = stimuli.toList())
}
