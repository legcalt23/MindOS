package com.mindos.app.features.mindboost.decisiones.presentation.state

import com.mindverse.mindboost.core.domain.CognitiveReadinessState
import com.mindverse.mindboost.core.session.CheckInInput

/** A single Go/No-go stimulus: tap on [GO] (green), withhold on [NO_GO] (red). */
enum class GoNoGoStimulus { GO, NO_GO }

/** Plain summary of one finished check-in, shown on the results screen and asserted in tests. */
data class CheckInSummary(
    val accuracy: Double,
    val reactionTimeCv: Double,
    val meanReactionMs: Long,
    val correctResponses: Int,
    val totalTrials: Int,
)

/** UI state for the Check-in (CRI / Go-No-go) screen. */
sealed interface DecisionCheckInUiState {
    /** Before the user starts the sequence. */
    data object Idle : DecisionCheckInUiState

    /** A stimulus is on screen waiting for a tap or the response window to elapse. */
    data class Running(
        val trialIndex: Int,
        val trialCount: Int,
        val stimulus: GoNoGoStimulus,
        val responseWindowMs: Long,
    ) : DecisionCheckInUiState

    /**
     * The sequence finished and basal readiness (CRI) was computed. [checkInInput] is the exact input
     * used, exposed so the session flow can feed it back into the orchestrator's session assembly.
     */
    data class Finished(
        val readiness: CognitiveReadinessState,
        val summary: CheckInSummary,
        val checkInInput: CheckInInput,
    ) : DecisionCheckInUiState
}
