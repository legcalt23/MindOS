@file:Suppress("MagicNumber", "MaxLineLength") // Accuracy/intensity fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.adaptive

import com.mindos.app.features.mindboost.emotions.domain.catalog.AdaptiveZone
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricSet
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionSafetyState
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadProxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultEmotionAdaptiveEngineTest {

    private val engine = DefaultEmotionAdaptiveEngine()
    private val difficulty = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = 0.4)

    private fun acc(value: Double, confident: Boolean = true) =
        EmotionMetricValue(EmotionMetricKey.ACCURACY, value, if (confident) 20 else 2, confident, EmotionMetricUnit.PROPORTION)

    @Suppress("LongParameterList") // A defaulted test-fixture builder; each knob maps to one engine input.
    private fun input(
        zone: AdaptiveZone = AdaptiveZone.GENERAL,
        governing: EmotionAxis = EmotionAxis.CATEGORY_AMBIGUITY,
        metrics: EmotionMetricSet = mapOf(EmotionMetricKey.ACCURACY to acc(0.82)),
        intensity: Int = 1,
        fired: Set<OverloadProxy> = emptySet(),
        prevFired: Set<OverloadProxy> = emptySet(),
        checkInResolved: Boolean = false,
        consecutiveErrors: Int = 0,
        blocksSinceChange: Int = 5,
    ) = EmotionAdaptiveInput(
        zone = zone,
        governingAxis = governing,
        metrics = metrics,
        currentDifficulty = difficulty,
        intensity = InductionIntensity(intensity),
        safety = EmotionSafetyState(fired, prevFired, checkInResolved),
        consecutiveErrors = consecutiveErrors,
        blocksSinceChange = blocksSinceChange,
    )

    @Test
    fun intensity_5_forces_safety_stop_over_everything() {
        val decision = engine.decide(input(intensity = 5, metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9))))
        assertEquals(AdaptiveAction.SAFETY_STOP, decision.action)
    }

    @Test
    fun intensity_4_reduces_and_lowers_intensity() {
        val decision = engine.decide(input(intensity = 4))
        assertEquals(AdaptiveAction.REDUCE_INTENSITY, decision.action)
        assertEquals(3, decision.nextIntensity.value)
    }

    @Test
    fun fired_proxies_trigger_check_in_before_performance() {
        val decision = engine.decide(input(fired = setOf(OverloadProxy.ERROR_STREAK), metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9))))
        assertEquals(AdaptiveAction.CHECK_IN, decision.action)
    }

    @Test
    fun step_up_when_mastered_and_safe() {
        val decision = engine.decide(input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9))))
        assertEquals(AdaptiveAction.STEP_UP, decision.action)
        assertTrue(decision.nextDifficulty.level(EmotionAxis.CATEGORY_AMBIGUITY) > difficulty.level(EmotionAxis.CATEGORY_AMBIGUITY))
        assertEquals(1, decision.nextIntensity.value, "engine never raises intensity")
    }

    @Test
    fun step_up_blocked_by_escalation_lock_even_if_mastered() {
        val decision = engine.decide(
            input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9)), prevFired = setOf(OverloadProxy.ERROR_STREAK)),
        )
        assertEquals(AdaptiveAction.HOLD, decision.action)
    }

    @Test
    fun step_up_blocked_when_accuracy_unconfident() {
        val decision = engine.decide(input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.95, confident = false))))
        assertEquals(AdaptiveAction.HOLD, decision.action)
    }

    @Test
    fun low_accuracy_steps_down() {
        val decision = engine.decide(input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.5))))
        assertEquals(AdaptiveAction.STEP_DOWN, decision.action)
        assertTrue(decision.nextDifficulty.level(EmotionAxis.CATEGORY_AMBIGUITY) < difficulty.level(EmotionAxis.CATEGORY_AMBIGUITY))
    }

    @Test
    fun hysteresis_holds_before_step_up() {
        val decision = engine.decide(input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9)), blocksSinceChange = 1))
        assertEquals(AdaptiveAction.HOLD, decision.action)
    }

    @Test
    fun intensity_is_never_raised_above_three_on_step_up() {
        val decision = engine.decide(input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9)), intensity = 3))
        assertEquals(AdaptiveAction.STEP_UP, decision.action)
        assertTrue(decision.nextIntensity.value <= 3)
    }
}
