package com.mindos.app.features.mindboost.decisiones.presentation.state

import com.mindverse.mindboost.core.mdp.reversib.ReversibilityDecisionType
import com.mindverse.mindboost.core.session.DecisionRound

/** Honest per-card feedback for one reversibility triage decision. */
data class ReversibilityFeedback(
    val selectedReversible: Boolean,
    val actuallyReversible: Boolean,
    val correct: Boolean,
    val decisionType: ReversibilityDecisionType,
    val reactionTimeMs: Long,
    val lockRespected: Boolean,
    val delayPenalty: Double,
)

/** Aggregate over a finished triage run. */
data class ReversibilityTriageSummary(
    val rounds: Int,
    val accuracy: Double,
    val meanReactionMs: Long,
    val lockViolations: Int,
    val meanDelayPenalty: Double,
)

/** UI state for the Reversibility Triaje (EX-19) screen. */
sealed interface ReversibilityTriageUiState {
    /** Before the user starts. */
    data object Idle : ReversibilityTriageUiState

    /**
     * A card is on screen. Only the prompt is exposed — never the true reversibility or the lock
     * duration — so classifying the card stays an honest decision. The screen reads the lock for the
     * confirmation step separately, only after the user chooses "irreversible".
     */
    data class Running(
        val cardIndex: Int,
        val cardCount: Int,
        val prompt: String,
    ) : ReversibilityTriageUiState

    /** All cards decided: built rounds (for later assembly) plus honest feedback. */
    data class Finished(
        val rounds: List<DecisionRound>,
        val perCard: List<ReversibilityFeedback>,
        val summary: ReversibilityTriageSummary,
    ) : ReversibilityTriageUiState
}
