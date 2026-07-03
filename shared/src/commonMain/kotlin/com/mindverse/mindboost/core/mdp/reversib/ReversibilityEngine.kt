package com.mindverse.mindboost.core.mdp.reversib

import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.domain.ReversibilityScenario
import kotlin.random.Random

enum class ReversibilityDecisionType {
    TYPE_1_IRREVERSIBLE,
    TYPE_2_REVERSIBLE,
}

object ReversibilityEngine {
    fun generateScenario(
        difficulty: DifficultyVector,
        random: Random = Random.Default,
    ): ReversibilityScenario {
        val irreversibility = difficulty.irreversibility.coerceIn(0.0, 1.0)
        val isReversible = random.nextDouble() >= irreversibility
        val delayCost = if (isReversible) {
            (MIN_DELAY_COST + MAX_DELAY_COST_SPAN * difficulty.timePressure.coerceIn(0.0, 1.0))
                .coerceIn(MIN_DELAY_COST, MAX_DELAY_COST)
        } else {
            0.0
        }
        val lockedDurationMs = if (isReversible) {
            0L
        } else {
            (MIN_LOCK_MS + (MAX_LOCK_EXTENSION_MS * difficulty.irreversibility.coerceIn(0.0, 1.0))).toLong()
                .coerceAtMost(MAX_LOCK_MS)
        }

        return ReversibilityScenario(
            id = scenarioId(difficulty, random),
            description = "Reversibility triage level " +
                difficulty.numberOfVariables.coerceIn(MIN_VARIABLES, MAX_VARIABLES),
            isReversible = isReversible,
            delayCostRate = delayCost,
            lockedDurationMs = lockedDurationMs,
        )
    }

    fun calculateDelayPenalty(
        scenario: ReversibilityScenario,
        reactionTimeMs: Long,
    ): Double {
        if (!scenario.isReversible) return 0.0

        val seconds = reactionTimeMs.coerceAtLeast(0L).toDouble() / MILLIS_PER_SECOND

        return seconds * scenario.delayCostRate.coerceIn(0.0, MAX_DELAY_COST)
    }

    fun validateLockDuration(scenario: ReversibilityScenario): Boolean =
        if (scenario.isReversible) {
            scenario.lockedDurationMs == 0L
        } else {
            scenario.lockedDurationMs in MIN_LOCK_MS..MAX_LOCK_MS
        }

    fun validateLockDuration(
        scenario: ReversibilityScenario,
        elapsedMs: Long,
    ): Boolean =
        validateLockDuration(scenario) &&
            (scenario.isReversible || elapsedMs.coerceAtLeast(0L) >= scenario.lockedDurationMs)

    fun classifyDecision(scenario: ReversibilityScenario): ReversibilityDecisionType =
        classifyDecision(scenario.isReversible)

    fun classifyDecision(isReversible: Boolean): ReversibilityDecisionType =
        if (isReversible) {
            ReversibilityDecisionType.TYPE_2_REVERSIBLE
        } else {
            ReversibilityDecisionType.TYPE_1_IRREVERSIBLE
        }

    private fun scenarioId(difficulty: DifficultyVector, random: Random): String {
        val variables = difficulty.numberOfVariables.coerceIn(MIN_VARIABLES, MAX_VARIABLES)
        val alternatives = difficulty.numberOfAlternatives.coerceIn(MIN_ALTERNATIVES, MAX_ALTERNATIVES)
        val irreversibility = (difficulty.irreversibility.coerceIn(0.0, 1.0) * PERCENT_SCALE).toInt()

        return "REV-$variables-$alternatives-$irreversibility-${random.nextInt(ID_MIN, ID_MAX)}"
    }

    private const val MIN_VARIABLES = 1
    private const val MAX_VARIABLES = 10
    private const val MIN_ALTERNATIVES = 2
    private const val MAX_ALTERNATIVES = 15
    private const val PERCENT_SCALE = 100
    private const val MIN_DELAY_COST = 1.0
    private const val MAX_DELAY_COST_SPAN = 9.0
    private const val MAX_DELAY_COST = 10.0
    private const val MIN_LOCK_MS = 2_000L
    private const val MAX_LOCK_MS = 8_000L
    private const val MAX_LOCK_EXTENSION_MS = 6_000.0
    private const val MILLIS_PER_SECOND = 1_000.0
    private const val ID_MIN = 1_000
    private const val ID_MAX = 10_000
}
