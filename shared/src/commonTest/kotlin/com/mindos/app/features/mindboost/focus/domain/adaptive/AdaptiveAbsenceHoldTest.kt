package com.mindos.app.features.mindboost.focus.domain.adaptive

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Absence must never move a user's difficulty.
 *
 * The defect: `MetricSet.valueOf(key, default = 0.0)` returned `0.0` for a metric the engine could not
 * confidently measure. `DefaultAdaptiveEngine` fed that into `accuracy < accDownThreshold (0.65)`, which is
 * trivially true of `0.0`, so **a block that measured nothing stepped the user's difficulty down as if they had
 * failed it**. The sealed-result work does not close this on its own: the leak is the *default-taking accessor*,
 * not the sentinel behind it.
 *
 * Found by an adversarial psychometric review of the blueprint and verified against the code before the fix.
 */
class AdaptiveAbsenceHoldTest {

    private val engine = DefaultAdaptiveEngine()
    private val e2 = ExerciseCatalog.E2_FILTRO

    // Deliberately mid-ladder, not EASIEST: from the floor a step-down is coerced to a no-op, so the
    // discriminating arms below could not tell "stepped down" from "held" and would pass either way.
    private val start = DifficultyVector.EASIEST.stepUp(e2.governingAxis, 0.4)

    private fun input(metrics: Map<MetricKey, MetricValue>) = AdaptiveInput(
        exercise = e2,
        current = start,
        recentBlock = metrics,
        recentTrials = emptyList(),
        perAxisMastery = emptyMap(),
        fatigue = FatigueSignal(level = FatigueLevel.LOW, sustained = false, emerging = false),
        blocksSinceChange = 99, // past hysteresis, so a Hold here can only come from the branch under test
        config = AdaptiveConfig(),
    )

    private fun accuracy(value: Double, confident: Boolean) = MetricValue(
        key = MetricKey.ACCURACY,
        value = value,
        n = if (confident) 40 else 0,
        confident = confident,
        unit = MetricUnit.RATIO,
    )

    @Test
    fun an_absent_accuracy_holds_and_never_steps_down() {
        val result = engine.decide(input(emptyMap()))

        assertEquals(
            AdaptiveDecision.Hold(HoldReason.METRIC_UNAVAILABLE),
            result.decision,
            "a block with NO accuracy metric must Hold, not step",
        )
        assertEquals(start, result.nextDifficulty, "Hold must preserve the previous difficulty exactly")
    }

    @Test
    fun an_unconfident_accuracy_holds_and_never_steps_down() {
        // The exact shape the engine produces from a too-small sample: a number, flagged not-confident.
        val result = engine.decide(input(mapOf(MetricKey.ACCURACY to accuracy(0.0, confident = false))))

        assertEquals(
            AdaptiveDecision.Hold(HoldReason.METRIC_UNAVAILABLE),
            result.decision,
            "an unconfident accuracy must Hold: insufficient sample is not failure",
        )
        assertEquals(start, result.nextDifficulty, "Hold must preserve the previous difficulty exactly")
    }

    /**
     * The discriminating arm. Without it, "clamp everything to Hold" would pass every test above and silently
     * disable adaptation for every real user.
     */
    @Test
    fun a_measured_low_accuracy_still_steps_down() {
        val result = engine.decide(input(mapOf(MetricKey.ACCURACY to accuracy(0.30, confident = true))))

        assertTrue(
            result.decision is AdaptiveDecision.StepDown,
            "a REAL low accuracy must still step down, was ${result.decision}",
        )
        assertTrue(result.nextDifficulty != start, "a real step-down must actually move difficulty")
    }

    /** The other discriminating arm: the fix must not suppress progression either. */
    @Test
    fun a_measured_high_accuracy_still_steps_up() {
        val result = engine.decide(input(mapOf(MetricKey.ACCURACY to accuracy(0.95, confident = true))))

        assertTrue(
            result.decision is AdaptiveDecision.StepUp,
            "a REAL high accuracy must still step up, was ${result.decision}",
        )
        assertTrue(result.nextDifficulty != start, "a real step-up must actually move difficulty")
    }

    @Test
    fun a_measured_mid_accuracy_holds_for_the_maintenance_band_not_for_absence() {
        val result = engine.decide(input(mapOf(MetricKey.ACCURACY to accuracy(0.75, confident = true))))

        assertEquals(
            AdaptiveDecision.Hold(HoldReason.MAINTENANCE_BAND),
            result.decision,
            "a measured mid-band accuracy must Hold for MAINTENANCE_BAND — the reason is not interchangeable " +
                "with METRIC_UNAVAILABLE, which is exactly the distinction the typed reason exists to keep",
        )
        assertEquals(start, result.nextDifficulty, "maintenance-band Hold preserves difficulty")
    }
}
