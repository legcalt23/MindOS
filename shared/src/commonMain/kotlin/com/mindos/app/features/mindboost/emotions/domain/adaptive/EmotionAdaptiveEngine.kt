@file:Suppress("MagicNumber", "MaxLineLength") // Zone/staircase thresholds + dense decision expressions.

package com.mindos.app.features.mindboost.emotions.domain.adaptive

import com.mindos.app.features.mindboost.emotions.domain.catalog.AdaptiveZone
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.valueIfConfident
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadProxy

interface EmotionAdaptiveEngine {
    fun decide(input: EmotionAdaptiveInput): EmotionAdaptiveDecision
}

/**
 * Default adaptive engine. Evaluates an ORDERED list of rules — safety strictly first — and the first
 * rule that applies wins; otherwise the block holds. Encoding the precedence as a rule list keeps each
 * branch a single expression (no multi-return) while preserving the exact safety-first order:
 * SafetyStop → ReduceIntensity → CheckIn → Cooldown → StepDown → Hold → StepUp.
 *
 * Intensity (the 1..5 safety scale) is NEVER auto-raised — only difficulty rises. Levels 4/5 arrive
 * solely from a self-report/check-in and map to ReduceIntensity/SafetyStop. Step-up is gated on the
 * safety state, no consecutive errors, zone mastery and a calm self-report.
 */
@Suppress("TooManyFunctions") // One private rule-function per ordered safety/performance rule.
class DefaultEmotionAdaptiveEngine : EmotionAdaptiveEngine {

    private val rules: List<(EmotionAdaptiveInput) -> EmotionAdaptiveDecision?> = listOf(
        ::safetyStop,
        ::reduceIntensity,
        ::checkIn,
        ::cooldown,
        ::stepDown,
        ::holdForHysteresis,
        ::stepUp,
    )

    override fun decide(input: EmotionAdaptiveInput): EmotionAdaptiveDecision =
        rules.firstNotNullOfOrNull { it(input) } ?: hold(input)

    private fun safetyStop(input: EmotionAdaptiveInput): EmotionAdaptiveDecision? =
        if (input.intensity.requiresStop) {
            EmotionAdaptiveDecision(AdaptiveAction.SAFETY_STOP, input.currentDifficulty, input.intensity)
        } else {
            null
        }

    private fun reduceIntensity(input: EmotionAdaptiveInput): EmotionAdaptiveDecision? =
        if (input.intensity.requiresReduce) {
            EmotionAdaptiveDecision(
                AdaptiveAction.REDUCE_INTENSITY,
                stepDownAxis(input.currentDifficulty, input.governingAxis),
                input.intensity.reducedBy(),
            )
        } else {
            null
        }

    private fun checkIn(input: EmotionAdaptiveInput): EmotionAdaptiveDecision? =
        if (input.safety.firedProxies.isNotEmpty() && !input.safety.checkInResolvedThisBlock) {
            EmotionAdaptiveDecision(AdaptiveAction.CHECK_IN, input.currentDifficulty, input.intensity)
        } else {
            null
        }

    private fun cooldown(input: EmotionAdaptiveInput): EmotionAdaptiveDecision? =
        if (OverloadProxy.SUSTAINED_SLOWING in input.safety.firedProxies && input.safety.checkInResolvedThisBlock) {
            EmotionAdaptiveDecision(
                AdaptiveAction.COOLDOWN,
                stepDownAxis(input.currentDifficulty, EmotionAxis.TIME_ON_TASK),
                input.intensity,
            )
        } else {
            null
        }

    private fun stepDown(input: EmotionAdaptiveInput): EmotionAdaptiveDecision? =
        if (shouldStepDown(input)) {
            EmotionAdaptiveDecision(
                AdaptiveAction.STEP_DOWN,
                stepDownAxis(input.currentDifficulty, input.governingAxis),
                input.intensity,
            )
        } else {
            null
        }

    private fun holdForHysteresis(input: EmotionAdaptiveInput): EmotionAdaptiveDecision? =
        if (input.blocksSinceChange < COOLDOWN_AFTER_DECISIONS) hold(input) else null

    private fun stepUp(input: EmotionAdaptiveInput): EmotionAdaptiveDecision? =
        if (canStepUp(input)) {
            EmotionAdaptiveDecision(
                AdaptiveAction.STEP_UP,
                stepUpAxis(input.currentDifficulty, input.governingAxis),
                input.intensity,
            )
        } else {
            null
        }

    private fun hold(input: EmotionAdaptiveInput): EmotionAdaptiveDecision =
        EmotionAdaptiveDecision(AdaptiveAction.HOLD, input.currentDifficulty, input.intensity)

    private fun shouldStepDown(input: EmotionAdaptiveInput): Boolean {
        val zoneFail = when (input.zone) {
            AdaptiveZone.GENERAL -> (input.metrics.valueIfConfident(EmotionMetricKey.ACCURACY) ?: 1.0) < GENERAL_DOWN
            AdaptiveZone.TOLERANCE -> (input.metrics.valueIfConfident(EmotionMetricKey.ACCURACY) ?: 1.0) < TOLERANCE_DOWN
            AdaptiveZone.COMMISSION -> (input.metrics.valueIfConfident(EmotionMetricKey.COMMISSION_RATE) ?: 0.0) >= COMMISSION_DOWN
        }
        return zoneFail || input.consecutiveErrors >= CONSECUTIVE_ERRORS_DOWN
    }

    private fun canStepUp(input: EmotionAdaptiveInput): Boolean {
        val safe = !input.safety.escalationLocked && input.consecutiveErrors == 0
        val reportCalm = input.selfReport == null || input.selfReport.intensity <= SELF_REPORT_STEPUP_MAX
        return safe && reportCalm && zoneMastered(input)
    }

    private fun zoneMastered(input: EmotionAdaptiveInput): Boolean = when (input.zone) {
        AdaptiveZone.GENERAL -> (input.metrics.valueIfConfident(EmotionMetricKey.ACCURACY) ?: 0.0) >= GENERAL_UP
        AdaptiveZone.TOLERANCE -> (input.metrics.valueIfConfident(EmotionMetricKey.ACCURACY) ?: 0.0) >= TOLERANCE_UP
        AdaptiveZone.COMMISSION ->
            input.metrics.valueIfConfident(EmotionMetricKey.COMMISSION_RATE)?.let { it <= COMMISSION_UP } ?: false
    }

    private fun stepUpAxis(difficulty: EmotionDifficultyVector, axis: EmotionAxis): EmotionDifficultyVector =
        difficulty.with(axis, (difficulty.level(axis) + STEP_UP).coerceAtMost(1.0))

    private fun stepDownAxis(difficulty: EmotionDifficultyVector, axis: EmotionAxis): EmotionDifficultyVector =
        difficulty.with(axis, (difficulty.level(axis) - STEP_DOWN).coerceAtLeast(0.0))

    companion object {
        private const val STEP_UP = 0.07
        private const val STEP_DOWN = 0.12
        private const val GENERAL_UP = 0.85
        private const val GENERAL_DOWN = 0.78
        private const val TOLERANCE_UP = 0.80
        private const val TOLERANCE_DOWN = 0.70
        private const val COMMISSION_UP = 0.10
        private const val COMMISSION_DOWN = 0.25
        private const val CONSECUTIVE_ERRORS_DOWN = 3
        private const val COOLDOWN_AFTER_DECISIONS = 2
        private const val SELF_REPORT_STEPUP_MAX = 2
    }
}
