@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.decisiones.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.decisiones.presentation.state.BayesArenaSummary
import com.mindos.app.features.mindboost.decisiones.presentation.state.BayesArenaUiState
import com.mindos.app.features.mindboost.decisiones.presentation.state.BayesEstimateFeedback
import com.mindverse.mindboost.core.domain.BayesScenario
import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.mdp.bayes.BayesEngine
import com.mindverse.mindboost.core.session.DecisionRound
import com.mindverse.mindboost.core.session.DecisionSessionOrchestrator
import kotlin.math.abs
import kotlin.random.Random

/**
 * Drives the Bayes Arena (EX-07/08) screen. Pure and clock-free: the screen owns timing and feeds
 * reaction times in. Per scenario it captures the user's posterior estimate and confidence, builds a
 * [DecisionRound] via [DecisionSessionOrchestrator.bayesRound] (for later session assembly in B5/B6),
 * and records honest feedback — Bayesian-update accuracy (BUR) and absolute error only. No Brier /
 * calibration / CAG, which are undefined without a resolved binary outcome. Fully unit-testable.
 */
class BayesArenaStateHolder(
    private val scenarios: List<BayesScenario>,
    private val difficulty: DifficultyVector = DEFAULT_DIFFICULTY,
    private val orchestrator: DecisionSessionOrchestrator = DecisionSessionOrchestrator(),
    private val timeLimitMs: Long = DEFAULT_TIME_LIMIT_MS,
) {
    init {
        require(scenarios.isNotEmpty()) { "Bayes Arena needs at least one scenario." }
    }

    var uiState: BayesArenaUiState by mutableStateOf(BayesArenaUiState.Idle)
        private set

    private val rounds = ArrayList<DecisionRound>(scenarios.size)
    private val feedback = ArrayList<BayesEstimateFeedback>(scenarios.size)
    private var awaitingResponse = false

    /** Begins (or restarts) at the first scenario. */
    fun start() {
        rounds.clear()
        feedback.clear()
        awaitingResponse = true
        uiState = runningState(0)
    }

    /**
     * Records the estimate for the current scenario. [userPosterior] is the user's P(H|E) in [0,1];
     * [confidence] is subjective certainty in [0.5,1]; [reactionTimeMs] is screen-measured.
     */
    fun submit(userPosterior: Double, confidence: Double, reactionTimeMs: Long) {
        if (!awaitingResponse) return

        val index = rounds.size
        val scenario = scenarios[index]
        val posterior = userPosterior.coerceIn(0.0, 1.0)
        val round = orchestrator.bayesRound(
            difficulty = difficulty,
            scenario = scenario,
            userPosterior = posterior,
            // In this exercise the decision objective IS the normative posterior, so alignment is proximity.
            objectivesAlignment = (1.0 - abs(posterior - scenario.expectedPosterior)).coerceIn(0.0, 1.0),
            biasDetected = false, // No bias probe in a pure estimation task.
            alternativesScannedRatio = 1.0, // No competing alternatives to scan: vacuously complete.
            reactionTimeMs = reactionTimeMs.coerceAtLeast(0L),
            timeLimitMs = timeLimitMs,
            spatialEntropy = 1.0, // No spatial grid in a slider task: no spatial-gaming signal.
            isDialDefault = false,
        )
        rounds += round
        feedback += BayesEstimateFeedback(
            userPosterior = posterior,
            mathPosterior = scenario.expectedPosterior,
            absoluteError = abs(posterior - scenario.expectedPosterior),
            bayesianUpdateRatio = round.bayesianUpdateRatio,
            confidence = confidence.coerceIn(MIN_CONFIDENCE, 1.0),
        )

        val next = index + 1
        if (next < scenarios.size) {
            uiState = runningState(next)
        } else {
            awaitingResponse = false
            uiState = finish()
        }
    }

    private fun runningState(index: Int): BayesArenaUiState.Running {
        val scenario = scenarios[index]
        return BayesArenaUiState.Running(
            scenarioIndex = index,
            scenarioCount = scenarios.size,
            baseRatePercent = scenario.baseRate * PERCENT,
            sensitivityPercent = scenario.sensitivity * PERCENT,
            falsePositivePercent = scenario.falsePositiveRate * PERCENT,
        )
    }

    private fun finish(): BayesArenaUiState.Finished = BayesArenaUiState.Finished(
        rounds = rounds.toList(),
        perScenario = feedback.toList(),
        summary = BayesArenaSummary(
            rounds = feedback.size,
            meanAbsoluteError = feedback.map { it.absoluteError }.meanOrZero(),
            meanBayesianUpdateRatio = feedback.map { it.bayesianUpdateRatio }.meanOrZero(),
            meanConfidence = feedback.map { it.confidence }.meanOrZero(),
        ),
    )

    private fun List<Double>.meanOrZero(): Double = if (isEmpty()) 0.0 else average()

    companion object {
        const val DEFAULT_TIME_LIMIT_MS = 45_000L
        const val DEFAULT_SCENARIOS = 5
        const val DEFAULT_SEED = 0L
        private const val MIN_CONFIDENCE = 0.5
        private const val PERCENT = 100.0

        private val DEFAULT_DIFFICULTY = DifficultyVector(
            numberOfVariables = 3,
            numberOfAlternatives = 2,
            timePressure = 0.3,
            noiseLevel = 0.5,
            ambiguityLevel = 0.5,
            irreversibility = 0.3,
            informationCost = 0.3,
            evidenceContradiction = 0.3,
            memoryLoad = 3,
            justificationDemand = 0.5,
        )

        /** Deterministic scenario set: distinct seeded Bayes problems for a reproducible session. */
        fun generateScenarios(
            count: Int = DEFAULT_SCENARIOS,
            difficulty: DifficultyVector = DEFAULT_DIFFICULTY,
            seed: Long = DEFAULT_SEED,
        ): List<BayesScenario> {
            require(count > 0) { "count must be > 0." }

            val random = Random(seed)
            return List(count) { BayesEngine.generateScenario(difficulty, random) }
        }
    }
}
