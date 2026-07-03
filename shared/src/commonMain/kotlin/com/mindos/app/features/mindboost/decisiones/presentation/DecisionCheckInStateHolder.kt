@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.decisiones.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.decisiones.presentation.state.CheckInSummary
import com.mindos.app.features.mindboost.decisiones.presentation.state.DecisionCheckInUiState
import com.mindos.app.features.mindboost.decisiones.presentation.state.GoNoGoStimulus
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionSessionOrchestrator
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Drives the Check-in (CRI / Go-No-go) screen. Pure and clock-free: the screen owns timing and
 * feeds reaction times in, while this holder records responses, computes Go/No-go accuracy and
 * reaction-time variability, and folds them through [DecisionSessionOrchestrator.checkIn] into a
 * [com.mindverse.mindboost.core.domain.CognitiveReadinessState]. No coroutines, so it is fully
 * unit-testable by scripting onTap/onTimeout calls.
 */
class DecisionCheckInStateHolder(
    private val stimuli: List<GoNoGoStimulus>,
    private val orchestrator: DecisionSessionOrchestrator = DecisionSessionOrchestrator(),
    private val responseWindowMs: Long = DEFAULT_WINDOW_MS,
) {
    init {
        require(stimuli.isNotEmpty()) { "Check-in needs at least one stimulus." }
    }

    var uiState: DecisionCheckInUiState by mutableStateOf(DecisionCheckInUiState.Idle)
        private set

    private val responses = ArrayList<TrialResponse>(stimuli.size)
    private var subjectiveFatigue = DEFAULT_FATIGUE
    private var awaitingResponse = false

    /** Begins (or restarts) the sequence at the first stimulus. */
    fun start() {
        responses.clear()
        awaitingResponse = true
        uiState = runningState(0)
    }

    /** Pre-task self-report of readiness in [0,1] (1 = fully rested, 0 = maximum fatigue). */
    fun setSubjectiveFatigue(value: Double) {
        subjectiveFatigue = value.coerceIn(0.0, 1.0)
    }

    /** The user tapped [reactionTimeMs] after the current stimulus appeared. */
    fun onTap(reactionTimeMs: Long) = record(tapped = true, reactionTimeMs = reactionTimeMs.coerceAtLeast(0L))

    /** The response window elapsed with no tap. */
    fun onTimeout() = record(tapped = false, reactionTimeMs = null)

    private fun record(tapped: Boolean, reactionTimeMs: Long?) {
        if (!awaitingResponse) return

        val index = responses.size
        responses += TrialResponse(stimulus = stimuli[index], tapped = tapped, reactionTimeMs = reactionTimeMs)

        val next = index + 1
        if (next < stimuli.size) {
            uiState = runningState(next)
        } else {
            awaitingResponse = false
            uiState = finish()
        }
    }

    private fun runningState(index: Int) = DecisionCheckInUiState.Running(
        trialIndex = index,
        trialCount = stimuli.size,
        stimulus = stimuli[index],
        responseWindowMs = responseWindowMs,
    )

    private fun finish(): DecisionCheckInUiState.Finished {
        val correct = responses.count { it.isCorrect }
        val accuracy = correct.toDouble() / responses.size
        val hitReactionTimes = responses
            .filter { it.stimulus == GoNoGoStimulus.GO && it.isCorrect }
            .mapNotNull { it.reactionTimeMs }
        val meanReaction = if (hitReactionTimes.isEmpty()) 0.0 else hitReactionTimes.average()
        val cv = coefficientOfVariation(hitReactionTimes)

        val checkInInput = CheckInInput(
            goNoGoAccuracy = accuracy,
            reactionTimeCV = cv,
            subjectiveFatigue = subjectiveFatigue,
            reactionTimeBaselineMs = meanReaction.toLong(),
        )
        return DecisionCheckInUiState.Finished(
            readiness = orchestrator.checkIn(checkInInput),
            summary = CheckInSummary(
                accuracy = accuracy,
                reactionTimeCv = cv,
                meanReactionMs = meanReaction.toLong(),
                correctResponses = correct,
                totalTrials = responses.size,
            ),
            checkInInput = checkInInput,
        )
    }

    private fun coefficientOfVariation(values: List<Long>): Double {
        if (values.size < 2) return 0.0

        val mean = values.average()
        val variance = values.sumOf {
            val deviation = it - mean
            deviation * deviation
        } / (values.size - 1)
        return if (mean <= 0.0) 0.0 else (sqrt(variance) / mean).coerceIn(0.0, 1.0)
    }

    private data class TrialResponse(
        val stimulus: GoNoGoStimulus,
        val tapped: Boolean,
        val reactionTimeMs: Long?,
    ) {
        val isCorrect: Boolean
            get() = when (stimulus) {
                GoNoGoStimulus.GO -> tapped
                GoNoGoStimulus.NO_GO -> !tapped
            }
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 1_500L
        const val DEFAULT_TRIALS = 20
        const val DEFAULT_GO_RATIO = 0.7
        const val DEFAULT_SEED = 0L
        private const val DEFAULT_FATIGUE = 0.7

        /**
         * Deterministic Go/No-go sequence. Guarantees at least one of each category when
         * [count] >= 2 so the check-in always exercises both response types.
         */
        fun generateStimuli(
            count: Int = DEFAULT_TRIALS,
            goRatio: Double = DEFAULT_GO_RATIO,
            seed: Long = DEFAULT_SEED,
        ): List<GoNoGoStimulus> {
            require(count > 0) { "count must be > 0." }

            val random = Random(seed)
            val ratio = goRatio.coerceIn(0.0, 1.0)
            val sequence = MutableList(count) {
                if (random.nextDouble() < ratio) GoNoGoStimulus.GO else GoNoGoStimulus.NO_GO
            }
            if (count >= 2) {
                if (sequence.none { it == GoNoGoStimulus.GO }) sequence[0] = GoNoGoStimulus.GO
                if (sequence.none { it == GoNoGoStimulus.NO_GO }) sequence[count - 1] = GoNoGoStimulus.NO_GO
            }
            return sequence
        }
    }
}
