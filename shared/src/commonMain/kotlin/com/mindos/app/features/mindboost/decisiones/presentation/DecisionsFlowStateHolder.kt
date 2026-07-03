package com.mindos.app.features.mindboost.decisiones.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionRound

/** The ordered stages of one Decisiones session. */
enum class DecisionsFlowStage { CheckIn, BayesArena, Reversibility, Result }

/**
 * Sequences a Decisiones session: check-in -> Bayes Arena -> (Reversibility) -> Result. It collects
 * the [CheckInInput] and every exercise's [DecisionRound]s and hands them to the result screen, which
 * assembles the session. Pure and clock-free (session id and timestamp are supplied by the caller),
 * so it is fully unit-testable; each completion callback is a no-op unless it matches the live stage,
 * which keeps the flow monotonic.
 *
 * [includeReversibility] is `false` by default, so the Reversibility stage is HIDDEN from the live flow
 * (Bayes advances straight to Result). Reversibility's live generator is degenerate — a template prompt
 * plus a coin-flip label — which makes the task unjudgeable, and its candidate replacement
 * (`ReversibilityScenarioBank`) is not yet human-validated (hence the `SMALL_UNVALIDATED` block on
 * `ReversibilityScore`). The stage's code is kept intact; flipping this flag to `true` re-enables it
 * unchanged once inter-rater validation lands.
 */
class DecisionsFlowStateHolder(
    private val sessionId: String,
    private val timestampMs: Long,
    private val includeReversibility: Boolean = false,
) {
    var stage: DecisionsFlowStage by mutableStateOf(DecisionsFlowStage.CheckIn)
        private set

    private var collectedCheckIn: CheckInInput? = null
    private val collectedRounds = ArrayList<DecisionRound>()

    /** Inputs the result screen needs to assemble the session. Valid only at [DecisionsFlowStage.Result]. */
    val resultSessionId: String get() = sessionId
    val resultTimestampMs: Long get() = timestampMs
    val rounds: List<DecisionRound> get() = collectedRounds.toList()

    fun checkInInput(): CheckInInput =
        checkNotNull(collectedCheckIn) { "Check-in must complete before the result is available." }

    fun onCheckInComplete(input: CheckInInput) {
        if (stage != DecisionsFlowStage.CheckIn) return
        collectedCheckIn = input
        stage = DecisionsFlowStage.BayesArena
    }

    fun onBayesComplete(rounds: List<DecisionRound>) {
        if (stage != DecisionsFlowStage.BayesArena) return
        collectedRounds += rounds
        // Reversibility is hidden by default (see class KDoc): skip straight to Result. The orchestrator
        // folds the Bayes-only rounds cleanly (empty/partial rounds -> zeroed contribution, not a crash).
        stage = if (includeReversibility) DecisionsFlowStage.Reversibility else DecisionsFlowStage.Result
    }

    fun onReversibilityComplete(rounds: List<DecisionRound>) {
        if (stage != DecisionsFlowStage.Reversibility) return
        collectedRounds += rounds
        stage = DecisionsFlowStage.Result
    }
}
