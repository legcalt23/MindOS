@file:Suppress("MagicNumber", "MaxLineLength") // Zone/staircase thresholds + dense decision expressions.

package com.mindos.app.features.mindboost.emotions.domain.adaptive

import com.mindos.app.features.mindboost.emotions.domain.catalog.AdaptiveZone
import com.mindos.app.features.mindboost.emotions.domain.feedback.MetricDirection
import com.mindos.app.features.mindboost.emotions.domain.feedback.directionOf
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

    private fun shouldStepDown(input: EmotionAdaptiveInput): Boolean =
        governingBelowFloor(input) || input.consecutiveErrors >= CONSECUTIVE_ERRORS_DOWN

    /**
     * Ola 0 · S2 — the exercise's OWN governing metric, compared against the zone's failure floor.
     *
     * Before this wave the zone compared a HARD-CODED key: GENERAL/TOLERANCE read `ACCURACY`, COMMISSION read
     * `COMMISSION_RATE`. Zone and paradigm are independent axes, so a classification exercise — whose metrics
     * engine emits `CATEGORY_ACCURACY` and never `ACCURACY` — sat in the GENERAL zone reading a key that could not
     * exist in its metric set. Every comparison fell through to the absent-value default, so the zone was inert:
     * no classification block could ever fail its floor or prove mastery, whatever the user did.
     *
     * Reading the governing metric fixes that generally, but ONLY together with its direction. GENERAL also
     * declares lower-is-better governing metrics elsewhere in the catalog, so a bare `>=`/`<` would rank the most
     * unstable or most miscalibrated reading as the strongest and raise difficulty for it. [directionOf] is the
     * module's single direction authority; thresholds are therefore expressed in "goodness" and each direction
     * gets the mirrored comparison, which reproduces every previous boundary exactly — including COMMISSION's
     * inclusive floor, where 2 false alarms in 8 no-go trials lands precisely ON the threshold.
     *
     * Fails closed twice over: a reading that is absent or not confident cannot prove failure, and neither can a
     * metric the module has declared to have no honest good/bad direction (RANGE / INFORMATIONAL_ONLY).
     */
    private fun governingBelowFloor(input: EmotionAdaptiveInput): Boolean {
        val reading = input.metrics.valueIfConfident(input.governingMetric) ?: return false
        val floor = zoneFloor(input.zone)
        return when (directionOf(input.governingMetric)) {
            MetricDirection.HIGHER_IS_BETTER -> reading < floor
            MetricDirection.LOWER_IS_BETTER -> reading >= 1.0 - floor
            MetricDirection.RANGE_IS_BETTER, MetricDirection.INFORMATIONAL_ONLY -> false
        }
    }

    private fun canStepUp(input: EmotionAdaptiveInput): Boolean {
        val safe = !input.safety.escalationLocked && input.consecutiveErrors == 0
        val reportCalm = input.selfReport == null || input.selfReport.intensity <= SELF_REPORT_STEPUP_MAX
        return safe && reportCalm && zoneMastered(input) && engagementSufficientForStepUp(input)
    }

    /**
     * MC8u-B §1B — speed-precision guard: mastery (accuracy) is NECESSARY but not SUFFICIENT to raise difficulty. A
     * trivially slow strategy (high accuracy at excessive RT), extreme response-time variability, or high omissions
     * (few GO hits on go/no-go, or many no-responses on choice) must NOT earn a step-up. Each signal only vetoes when
     * it is CONFIDENTLY present (an absent signal cannot prove disengagement), so this never over-blocks a real block.
     */
    private fun engagementSufficientForStepUp(input: EmotionAdaptiveInput): Boolean {
        val rtOk = input.metrics.valueIfConfident(EmotionMetricKey.RT_MEDIAN)?.let { it <= MAX_STEP_UP_RT_MS } ?: true
        val cvOk = input.metrics.valueIfConfident(EmotionMetricKey.RT_CV)?.let { it <= MAX_STEP_UP_RT_CV } ?: true
        val hitOk = input.metrics.valueIfConfident(EmotionMetricKey.HIT_RATE)?.let { it >= MIN_STEP_UP_HIT_RATE } ?: true
        val omissionOk = input.metrics.valueIfConfident(EmotionMetricKey.ABANDONMENT)?.let { it <= MAX_STEP_UP_ABANDONMENT } ?: true
        return rtOk && cvOk && hitOk && omissionOk
    }

    /**
     * Zone mastery on the exercise's own governing metric — see [governingBelowFloor] for why the direction has to
     * travel with the metric. A metric with no honest good/bad direction can never prove mastery either, so
     * difficulty is never raised on a signal the module has explicitly declared not to be a performance statement.
     */
    private fun zoneMastered(input: EmotionAdaptiveInput): Boolean {
        val reading = input.metrics.valueIfConfident(input.governingMetric) ?: return false
        val ceiling = zoneCeiling(input.zone)
        return when (directionOf(input.governingMetric)) {
            MetricDirection.HIGHER_IS_BETTER -> reading >= ceiling
            MetricDirection.LOWER_IS_BETTER -> reading <= 1.0 - ceiling
            MetricDirection.RANGE_IS_BETTER, MetricDirection.INFORMATIONAL_ONLY -> false
        }
    }

    /** The zone's mastery threshold, in "goodness" (1.0 = perfect) so that both directions share one number. */
    private fun zoneCeiling(zone: AdaptiveZone): Double = when (zone) {
        AdaptiveZone.GENERAL -> GENERAL_UP
        AdaptiveZone.TOLERANCE -> TOLERANCE_UP
        AdaptiveZone.COMMISSION -> COMMISSION_UP
    }

    /** The zone's failure threshold, in "goodness" (1.0 = perfect) so that both directions share one number. */
    private fun zoneFloor(zone: AdaptiveZone): Double = when (zone) {
        AdaptiveZone.GENERAL -> GENERAL_DOWN
        AdaptiveZone.TOLERANCE -> TOLERANCE_DOWN
        AdaptiveZone.COMMISSION -> COMMISSION_DOWN
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
        // Ola 0 · S2 — restated in "goodness" (1.0 = perfect) like the other two zones, because the comparison is
        // now mirrored by metric direction rather than hand-written per zone. COMMISSION governs on a
        // lower-is-better rate, so these are the exact complements of the previous 0.10 / 0.25 rate thresholds and
        // the resulting comparisons are byte-for-byte the same decisions, boundaries included.
        private const val COMMISSION_UP = 0.90
        private const val COMMISSION_DOWN = 0.75
        private const val CONSECUTIVE_ERRORS_DOWN = 3

        /**
         * Blocks of stable evidence required before difficulty may move up again — the anti-oscillation guard.
         * Public because the SESSION STRUCTURE has to be long enough to reach it: a session with fewer decisions
         * than this can never evaluate the step-up rule at all, and that reachability is asserted against this
         * constant instead of against a second copy of the number.
         */
        const val COOLDOWN_AFTER_DECISIONS = 2
        private const val SELF_REPORT_STEPUP_MAX = 2

        // MC8u-B §1B speed-precision step-up guard thresholds (only vetoes when the signal is confidently present).
        private const val MAX_STEP_UP_RT_MS = 1500.0 // above this a "correct but very slow" strategy is not rewarded
        private const val MAX_STEP_UP_RT_CV = 0.60 // above this the response rhythm is too unstable to raise difficulty
        private const val MIN_STEP_UP_HIT_RATE = 0.60 // below this GO-omissions are too high (go/no-go disengagement)
        private const val MAX_STEP_UP_ABANDONMENT = 2.0 // more than 2 no-responses in a block = disengaged (choice)
    }
}
