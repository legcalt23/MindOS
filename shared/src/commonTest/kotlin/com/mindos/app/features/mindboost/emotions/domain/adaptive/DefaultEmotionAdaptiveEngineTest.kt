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

    private fun mv(key: EmotionMetricKey, value: Double, confident: Boolean = true) =
        EmotionMetricValue(key, value, if (confident) 20 else 2, confident, EmotionMetricUnit.INDEX)

    @Suppress("LongParameterList") // A defaulted test-fixture builder; each knob maps to one engine input.
    private fun input(
        zone: AdaptiveZone = AdaptiveZone.GENERAL,
        governing: EmotionAxis = EmotionAxis.CATEGORY_AMBIGUITY,
        // Ola 0 · S2 — the zone now compares the exercise's OWN governing metric. ACCURACY is kept as the fixture
        // default precisely so every staircase test below keeps the exact semantics it had before this wave: those
        // tests are about the ORDERED RULES (safety first, hysteresis, the speed-precision guard), not about which
        // key a zone happens to read, and silently repointing them would have emptied them instead of adapting them.
        governingMetric: EmotionMetricKey = EmotionMetricKey.ACCURACY,
        metrics: EmotionMetricSet = mapOf(EmotionMetricKey.ACCURACY to acc(0.82)),
        intensity: Int = 1,
        fired: Set<OverloadProxy> = emptySet(),
        prevFired: Set<OverloadProxy> = emptySet(),
        checkInResolved: Boolean = false,
        consecutiveErrors: Int = 0,
        // The smallest value at which the step-up rule is evaluated at all, and — unlike the 5 this used to default
        // to — one a real session can actually reach: the counter is reset on every difficulty change and a session
        // has a bounded number of blocks, so a fixture above the threshold was testing a configuration production
        // could never produce.
        blocksSinceChange: Int = DefaultEmotionAdaptiveEngine.COOLDOWN_AFTER_DECISIONS,
    ) = EmotionAdaptiveInput(
        zone = zone,
        governingAxis = governing,
        governingMetric = governingMetric,
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

    // ---- MC8u-B §1B speed-precision step-up guard --------------------------------------------------------------

    @Test
    fun perfect_accuracy_with_excessive_rt_does_not_step_up() {
        val decision = engine.decide(
            input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9), EmotionMetricKey.RT_MEDIAN to mv(EmotionMetricKey.RT_MEDIAN, 2000.0))),
        )
        assertEquals(AdaptiveAction.HOLD, decision.action, "high accuracy at an excessive RT is not rewarded with a step-up")
    }

    @Test
    fun speed_precision_guard_blocks_trivial_slow_strategy() {
        val decision = engine.decide(
            input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.95), EmotionMetricKey.RT_MEDIAN to mv(EmotionMetricKey.RT_MEDIAN, 3000.0))),
        )
        assertTrue(decision.action != AdaptiveAction.STEP_UP)
    }

    @Test
    fun reasonable_accuracy_and_rt_can_step_up() {
        val decision = engine.decide(
            input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9), EmotionMetricKey.RT_MEDIAN to mv(EmotionMetricKey.RT_MEDIAN, 800.0))),
        )
        assertEquals(AdaptiveAction.STEP_UP, decision.action)
    }

    @Test
    fun high_speed_low_accuracy_does_not_step_up() {
        val decision = engine.decide(
            input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.5), EmotionMetricKey.RT_MEDIAN to mv(EmotionMetricKey.RT_MEDIAN, 300.0))),
        )
        assertTrue(decision.action != AdaptiveAction.STEP_UP)
    }

    @Test
    fun high_omission_rate_blocks_step_up() {
        val decision = engine.decide(
            input(metrics = mapOf(EmotionMetricKey.ACCURACY to acc(0.9), EmotionMetricKey.ABANDONMENT to mv(EmotionMetricKey.ABANDONMENT, 3.0))),
        )
        assertEquals(AdaptiveAction.HOLD, decision.action)
    }

    @Test
    fun low_hit_rate_blocks_difficulty_step_up() {
        // The commission reading is CONFIDENT and mastered, so zone mastery is satisfied and the hit rate is the
        // only thing left that can block the step-up. With an unconfident reading (as this fixture used to have)
        // mastery failed on its own and the assertion held no matter what the hit-rate guard did.
        val decision = engine.decide(
            input(
                zone = AdaptiveZone.COMMISSION,
                governing = EmotionAxis.INHIBITORY_DEMAND,
                governingMetric = EmotionMetricKey.COMMISSION_RATE,
                metrics = mapOf(
                    EmotionMetricKey.COMMISSION_RATE to mv(EmotionMetricKey.COMMISSION_RATE, 0.05),
                    EmotionMetricKey.HIT_RATE to mv(EmotionMetricKey.HIT_RATE, 0.2),
                ),
            ),
        )
        assertEquals(AdaptiveAction.HOLD, decision.action, "a low hit rate can never earn a step-up, however clean the inhibition")
    }

    @Test
    fun a_mastered_commission_block_with_a_healthy_hit_rate_does_step_up() {
        // The discriminating half of the test above: without this, tightening the guard until nothing can ever
        // step up would look like a passing suite.
        val decision = engine.decide(
            input(
                zone = AdaptiveZone.COMMISSION,
                governing = EmotionAxis.INHIBITORY_DEMAND,
                governingMetric = EmotionMetricKey.COMMISSION_RATE,
                metrics = mapOf(
                    EmotionMetricKey.COMMISSION_RATE to mv(EmotionMetricKey.COMMISSION_RATE, 0.05),
                    EmotionMetricKey.HIT_RATE to mv(EmotionMetricKey.HIT_RATE, 0.9),
                ),
            ),
        )
        assertEquals(AdaptiveAction.STEP_UP, decision.action)
    }

    // ---- Ola 0 · S2: the zone reads the exercise's governing metric, carrying its direction ---------------------

    @Test
    fun a_zone_adapts_on_the_governing_metric_its_paradigm_actually_emits() {
        // The classification paradigm emits CATEGORY_ACCURACY and never ACCURACY. Before this wave the GENERAL zone
        // compared a hard-coded ACCURACY key, found nothing, and fell through to its absent-value default on every
        // single block — so no classification exercise could ever step up or step down on performance.
        val mastered = engine.decide(
            input(
                governingMetric = EmotionMetricKey.CATEGORY_ACCURACY,
                metrics = mapOf(EmotionMetricKey.CATEGORY_ACCURACY to mv(EmotionMetricKey.CATEGORY_ACCURACY, 0.9)),
            ),
        )
        assertEquals(AdaptiveAction.STEP_UP, mastered.action, "a mastered classification block must be able to rise")

        val failing = engine.decide(
            input(
                governingMetric = EmotionMetricKey.CATEGORY_ACCURACY,
                metrics = mapOf(EmotionMetricKey.CATEGORY_ACCURACY to mv(EmotionMetricKey.CATEGORY_ACCURACY, 0.5)),
            ),
        )
        assertEquals(AdaptiveAction.STEP_DOWN, failing.action, "a failing classification block must be able to fall")
    }

    @Test
    fun a_lower_is_better_governing_metric_is_not_read_upside_down() {
        // The whole point of routing through `directionOf`. A bare `>= ceiling` would call the WORST reading
        // mastery and raise difficulty for the most unstable user, and `< floor` would never fire for them at all.
        val unstable = engine.decide(
            input(
                governingMetric = EmotionMetricKey.RT_CV,
                metrics = mapOf(EmotionMetricKey.RT_CV to mv(EmotionMetricKey.RT_CV, 0.90)),
            ),
        )
        assertEquals(AdaptiveAction.STEP_DOWN, unstable.action, "a very high response-rhythm variability is failure, not mastery")

        val steady = engine.decide(
            input(
                governingMetric = EmotionMetricKey.RT_CV,
                metrics = mapOf(EmotionMetricKey.RT_CV to mv(EmotionMetricKey.RT_CV, 0.05)),
            ),
        )
        assertEquals(AdaptiveAction.STEP_UP, steady.action, "a very steady response rhythm is mastery on a lower-is-better metric")
    }

    @Test
    fun a_metric_with_no_honest_direction_can_neither_raise_nor_lower_difficulty() {
        // `directionOf` classifies AFFECTIVE_INTERFERENCE_COST as INFORMATIONAL_ONLY: the module displays it but
        // has explicitly ruled it out as a strong/weak performance statement. Gating difficulty on it either way
        // would promote a signal the module refuses to phrase as performance into the mastery decision.
        val extreme = engine.decide(
            input(
                governingMetric = EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
                metrics = mapOf(EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST to mv(EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST, 1.0)),
            ),
        )
        assertEquals(AdaptiveAction.HOLD, extreme.action)

        val zero = engine.decide(
            input(
                governingMetric = EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST,
                metrics = mapOf(EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST to mv(EmotionMetricKey.AFFECTIVE_INTERFERENCE_COST, 0.0)),
            ),
        )
        assertEquals(AdaptiveAction.HOLD, zero.action)
    }

    @Test
    fun the_commission_failure_floor_stays_inclusive() {
        // Restating the commission thresholds in "goodness" must not move the boundary by a hair: 2 false alarms in
        // 8 no-go trials is exactly 0.25, a value real blocks produce, and it counted as failure before this wave.
        val decision = engine.decide(
            input(
                zone = AdaptiveZone.COMMISSION,
                governing = EmotionAxis.INHIBITORY_DEMAND,
                governingMetric = EmotionMetricKey.COMMISSION_RATE,
                metrics = mapOf(EmotionMetricKey.COMMISSION_RATE to mv(EmotionMetricKey.COMMISSION_RATE, 0.25)),
            ),
        )
        assertEquals(AdaptiveAction.STEP_DOWN, decision.action, "a commission rate exactly ON the floor is still a failure")
    }
}
