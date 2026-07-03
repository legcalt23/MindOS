package com.mindos.app.features.mindboost.decisiones.presentation.state

import com.mindverse.mindboost.core.session.DecisionRound

/**
 * Honest per-scenario feedback for a Bayes estimation. Only Bayesian-update accuracy is reported:
 * a pure posterior-estimation task has no resolved binary outcome, so Brier / calibration / CAG are
 * undefined and deliberately absent.
 */
data class BayesEstimateFeedback(
    val userPosterior: Double,
    val mathPosterior: Double,
    val absoluteError: Double,
    val bayesianUpdateRatio: Double,
    val confidence: Double,
)

/** Aggregate over a finished Bayes Arena run. */
data class BayesArenaSummary(
    val rounds: Int,
    val meanAbsoluteError: Double,
    val meanBayesianUpdateRatio: Double,
    val meanConfidence: Double,
)

/** UI state for the Bayes Arena (EX-07/08) screen. */
sealed interface BayesArenaUiState {
    /** Before the user starts the scenarios. */
    data object Idle : BayesArenaUiState

    /**
     * A scenario is on screen. Only the inputs the user reasons from are exposed — never the
     * normative posterior — so the answer cannot leak into the UI before submission.
     */
    data class Running(
        val scenarioIndex: Int,
        val scenarioCount: Int,
        val baseRatePercent: Double,
        val sensitivityPercent: Double,
        val falsePositivePercent: Double,
    ) : BayesArenaUiState

    /** All scenarios answered: the built rounds (for later session assembly) plus honest feedback. */
    data class Finished(
        val rounds: List<DecisionRound>,
        val perScenario: List<BayesEstimateFeedback>,
        val summary: BayesArenaSummary,
    ) : BayesArenaUiState
}
