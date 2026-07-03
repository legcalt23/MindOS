@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.decisiones.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityFeedback
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageSummary
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState
import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.domain.ReversibilityScenario
import com.mindverse.mindboost.core.mdp.reversib.ReversibilityDecisionType
import com.mindverse.mindboost.core.mdp.reversib.ReversibilityEngine
import com.mindverse.mindboost.core.session.DecisionRound
import com.mindverse.mindboost.core.session.DecisionSessionOrchestrator

/**
 * Drives the Reversibility Triaje (EX-19) screen. Pure and clock-free: the screen owns timing and the
 * irreversible-confirmation lock UI, feeding the elapsed time in. Per card the user classifies the
 * decision as reversible (Type 2) or irreversible (Type 1); this holder records correctness, whether
 * the irreversible lock was respected, and the over-deliberation penalty for reversible cards, then
 * builds a [DecisionRound] via [DecisionSessionOrchestrator.reversibilityRound] for later assembly.
 * Fully unit-testable without Compose or coroutines.
 */
class ReversibilityTriageStateHolder(
    private val scenarios: List<ReversibilityScenario>,
    private val difficulty: DifficultyVector = DEFAULT_DIFFICULTY,
    private val orchestrator: DecisionSessionOrchestrator = DecisionSessionOrchestrator(),
    private val timeLimitMs: Long = DEFAULT_TIME_LIMIT_MS,
) {
    init {
        require(scenarios.isNotEmpty()) { "Reversibility triage needs at least one scenario." }
    }

    var uiState: ReversibilityTriageUiState by mutableStateOf(ReversibilityTriageUiState.Idle)
        private set

    private val rounds = ArrayList<DecisionRound>(scenarios.size)
    private val feedback = ArrayList<ReversibilityFeedback>(scenarios.size)
    private var awaitingResponse = false

    /** Begins (or restarts) at the first card. */
    fun start() {
        rounds.clear()
        feedback.clear()
        awaitingResponse = true
        uiState = runningState(0)
    }

    /**
     * The confirmation lock (ms) for the current card. The screen calls this only after the user
     * chooses "irreversible", so the lock never leaks the answer before the decision is made. Returns
     * 0 when not awaiting a response or when the current card is reversible.
     */
    fun currentLockDurationMs(): Long =
        if (awaitingResponse && rounds.size < scenarios.size) scenarios[rounds.size].lockedDurationMs else 0L

    /** Commits the user's classification for the current card. [reactionTimeMs] is screen-measured. */
    fun commit(selectedReversible: Boolean, reactionTimeMs: Long) {
        if (!awaitingResponse) return

        val index = rounds.size
        val scenario = scenarios[index]
        val reactionMs = reactionTimeMs.coerceAtLeast(0L)
        val round = orchestrator.reversibilityRound(
            difficulty = difficulty,
            scenario = scenario,
            selectedReversible = selectedReversible,
            alternativesScannedRatio = 1.0, // Binary triage: no broader alternative set to scan.
            reactionTimeMs = reactionMs,
            timeLimitMs = timeLimitMs,
            spatialEntropy = 1.0, // No spatial grid in a card task: no spatial-gaming signal.
            isDialDefault = false,
        )
        rounds += round
        feedback += ReversibilityFeedback(
            selectedReversible = selectedReversible,
            actuallyReversible = scenario.isReversible,
            correct = selectedReversible == scenario.isReversible,
            decisionType = ReversibilityEngine.classifyDecision(scenario),
            reactionTimeMs = reactionMs,
            lockRespected = ReversibilityEngine.validateLockDuration(scenario, reactionMs),
            delayPenalty = ReversibilityEngine.calculateDelayPenalty(scenario, reactionMs),
        )

        val next = index + 1
        if (next < scenarios.size) {
            uiState = runningState(next)
        } else {
            awaitingResponse = false
            uiState = finish()
        }
    }

    private fun runningState(index: Int): ReversibilityTriageUiState.Running {
        val scenario = scenarios[index]
        return ReversibilityTriageUiState.Running(
            cardIndex = index,
            cardCount = scenarios.size,
            prompt = scenario.description,
        )
    }

    private fun finish(): ReversibilityTriageUiState.Finished {
        val correct = feedback.count { it.correct }
        val reversiblePenalties = feedback.filter { it.actuallyReversible }.map { it.delayPenalty }
        val summary = ReversibilityTriageSummary(
            rounds = feedback.size,
            accuracy = correct.toDouble() / feedback.size,
            meanReactionMs = feedback.map { it.reactionTimeMs }.meanMsOrZero(),
            lockViolations = feedback.count {
                it.decisionType == ReversibilityDecisionType.TYPE_1_IRREVERSIBLE && !it.lockRespected
            },
            meanDelayPenalty = if (reversiblePenalties.isEmpty()) 0.0 else reversiblePenalties.average(),
        )
        return ReversibilityTriageUiState.Finished(
            rounds = rounds.toList(),
            perCard = feedback.toList(),
            summary = summary,
        )
    }

    private fun List<Long>.meanMsOrZero(): Long = if (isEmpty()) 0L else average().toLong()

    companion object {
        const val DEFAULT_TIME_LIMIT_MS = 15_000L
        const val DEFAULT_CARDS = 6
        const val DEFAULT_SEED = 0L

        private val DEFAULT_DIFFICULTY = DifficultyVector(
            numberOfVariables = 3,
            numberOfAlternatives = 2,
            timePressure = 0.5,
            noiseLevel = 0.5,
            ambiguityLevel = 0.5,
            irreversibility = 0.5,
            informationCost = 0.3,
            evidenceContradiction = 0.3,
            memoryLoad = 3,
            justificationDemand = 0.5,
        )

        /** Deterministic card set: seeded reversibility scenarios for a reproducible run. */
        fun generateScenarios(
            count: Int = DEFAULT_CARDS,
            difficulty: DifficultyVector = DEFAULT_DIFFICULTY,
            seed: Long = DEFAULT_SEED,
        ): List<ReversibilityScenario> {
            require(count > 0) { "count must be > 0." }

            val random = kotlin.random.Random(seed)
            return List(count) { ReversibilityEngine.generateScenario(difficulty, random) }
        }
    }
}
