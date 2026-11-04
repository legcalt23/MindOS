@file:Suppress("MagicNumber", "MaxLineLength") // RT/accuracy fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultEmotionMetricsEngineTest {

    private val engine = DefaultEmotionMetricsEngine()

    private fun goNoGo(outcome: EmotionTrialOutcome, rt: Long?, frameOpen: Boolean, congruent: Boolean = true): ClassifiedEmotionTrial {
        val responseKey = if (outcome == EmotionTrialOutcome.HIT || outcome == EmotionTrialOutcome.FALSE_ALARM) {
            EmotionResponseKey.PRIMARY
        } else {
            EmotionResponseKey.NONE
        }
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.AffectiveGoNoGo(frameOpen, "espera", congruent), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, responseKey), outcome)
    }

    private fun choice(outcome: EmotionTrialOutcome, rt: Long?): ClassifiedEmotionTrial {
        val key = when (outcome) {
            EmotionTrialOutcome.CORRECT -> EmotionResponseKey.LEFT
            EmotionTrialOutcome.INCORRECT -> EmotionResponseKey.RIGHT
            else -> EmotionResponseKey.NONE
        }
        val stim = EmotionStimulus(0, EmotionResponseKey.LEFT, EmotionStimulusPayload.TwoAltPerceptual(EmotionResponseKey.LEFT, 0.6), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, key), outcome)
    }

    private fun classif(truth: EmotionCategory, chosen: EmotionCategory?): ClassifiedEmotionTrial {
        val outcome = when (chosen) {
            null -> EmotionTrialOutcome.NO_RESPONSE
            truth -> EmotionTrialOutcome.CORRECT
            else -> EmotionTrialOutcome.INCORRECT
        }
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.Classification("x", truth, false), 0, 1000)
        val key = if (chosen == null) EmotionResponseKey.NONE else EmotionResponseKey.CATEGORY_A
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, 600, key, chosen), outcome)
    }

    @Test
    fun commission_rate_is_over_the_no_go_denominator() {
        val trials = List(2) { goNoGo(EmotionTrialOutcome.FALSE_ALARM, 300, frameOpen = false) } +
            List(6) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, frameOpen = false) } +
            List(12) { goNoGo(EmotionTrialOutcome.HIT, 500, frameOpen = true) }
        val metrics = engine.blockMetrics(trials)
        val commission = metrics.getValue(EmotionMetricKey.COMMISSION_RATE)
        assertEquals(0.25, commission.value, 1e-9)
        assertTrue(commission.confident, "8 no-go ≥ MIN should be confident")
    }

    @Test
    fun commission_unconfident_when_too_few_no_go() {
        val trials = List(1) { goNoGo(EmotionTrialOutcome.FALSE_ALARM, 300, frameOpen = false) } +
            List(10) { goNoGo(EmotionTrialOutcome.HIT, 500, frameOpen = true) }
        assertTrue(!engine.blockMetrics(trials).getValue(EmotionMetricKey.COMMISSION_RATE).confident)
    }

    @Test
    fun post_error_recovery_slow_accurate_beats_fast_flat() {
        // Ola 3 · R2 raised the pair floor from 3 to 8, so the fixture grows to 8 repeats. The assertion is
        // untouched: what changed is how much evidence the engine demands before it will state it.
        val slowAccurate = buildList {
            repeat(8) {
                add(choice(EmotionTrialOutcome.CORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 500))
                add(choice(EmotionTrialOutcome.CORRECT, 900)) // post-error: accurate, slow
            }
        }
        val fastFlat = buildList {
            repeat(8) {
                add(choice(EmotionTrialOutcome.CORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 200)) // post-error: wrong, impulsive
            }
        }
        val slow = engine.blockMetrics(slowAccurate).getValue(EmotionMetricKey.POST_ERROR_RECOVERY)
        val fast = engine.blockMetrics(fastFlat).getValue(EmotionMetricKey.POST_ERROR_RECOVERY)
        assertTrue(slow.confident && fast.confident, "both have ≥8 post-error pairs")
        assertTrue(slow.value >= fast.value, "slow-accurate recovery (${slow.value}) must beat fast-flat (${fast.value})")
        assertTrue(slow.value > 0.5 && fast.value < 0.5)
    }

    @Test
    fun post_error_recovery_unavailable_with_too_few_errors() {
        val trials = List(12) { choice(EmotionTrialOutcome.CORRECT, 500) }
        assertTrue(!engine.blockMetrics(trials).getValue(EmotionMetricKey.POST_ERROR_RECOVERY).confident)
    }

    @Test
    fun classification_accuracy_and_separation_are_chance_adjusted() {
        // 8 internal/thought trials all correct → separation = (1 - 0.25)/(0.75) = 1.0
        val trials = List(4) { classif(EmotionCategory.INTERNAL_STATE, EmotionCategory.INTERNAL_STATE) } +
            List(4) { classif(EmotionCategory.THOUGHT, EmotionCategory.THOUGHT) } +
            List(2) { classif(EmotionCategory.FACT, EmotionCategory.IMPULSE) }
        val metrics = engine.blockMetrics(trials)
        assertEquals(0.8, metrics.getValue(EmotionMetricKey.CATEGORY_ACCURACY).value, 1e-9)
        assertEquals(1.0, metrics.getValue(EmotionMetricKey.STATE_THOUGHT_ACCURACY).value, 1e-9)
    }

    @Test
    fun the_block_reports_the_weakest_categorys_discrimination_and_not_only_its_accuracy() {
        // Ola 2 · R3 · P3. The wiring test the module was missing: a metric computed but never emitted protects
        // nothing. 6 items per category, every one right except that urges are called facts — accuracy stays at
        // 75 %, which reads as a good session, while the weakest boundary is not being made at all.
        val trials = EmotionCategory.entries.flatMap { truth ->
            val chosen = if (truth == EmotionCategory.IMPULSE) EmotionCategory.FACT else truth
            List(6) { classif(truth, chosen) }
        }
        val metrics = engine.blockMetrics(trials)
        assertEquals(0.75, metrics.getValue(EmotionMetricKey.CATEGORY_ACCURACY).value, 1e-9)
        assertEquals(
            0.0,
            metrics.getValue(EmotionMetricKey.CATEGORY_DPRIME_MIN).value,
            1e-9,
            "the missed boundary must reach the metric set, not stop at the calculator",
        )
    }

    @Test
    fun a_block_too_short_to_estimate_discrimination_reports_no_number() {
        val trials = EmotionCategory.entries.flatMap { truth -> List(3) { classif(truth, truth) } }
        val dPrime = engine.blockMetrics(trials).getValue(EmotionMetricKey.CATEGORY_DPRIME_MIN)
        assertTrue(!dPrime.confident, "12 answered trials are below the estimate's floor")
        assertEquals(12, dPrime.n, "the refusal must still say how much evidence there was")
    }

    // MC8q/MC8r: ABANDONMENT is gated by MIN_TOTAL like every other metric (was an inline total>0 flag).
    @Test
    fun abandonment_is_not_confident_below_min_sample() {
        val trials = List(4) { choice(EmotionTrialOutcome.CORRECT, 500) } + List(1) { choice(EmotionTrialOutcome.NO_RESPONSE, null) }
        val abandonment = engine.blockMetrics(trials).getValue(EmotionMetricKey.ABANDONMENT)
        assertEquals(1.0, abandonment.value, 1e-9)
        assertTrue(!abandonment.confident, "a 5-trial block is below MIN_TOTAL, so ABANDONMENT is not confident")
    }

    @Test
    fun abandonment_is_confident_at_min_sample() {
        val trials = List(7) { choice(EmotionTrialOutcome.CORRECT, 500) } + List(1) { choice(EmotionTrialOutcome.NO_RESPONSE, null) }
        assertTrue(engine.blockMetrics(trials).getValue(EmotionMetricKey.ABANDONMENT).confident, "8 trials ≥ MIN_TOTAL is confident")
    }

    // MC8q/MC8r: ANTICIPATIONS confidence gates on the HIT sample the count is drawn from (hits ≥ MIN_RT), both ways.
    @Test
    fun anticipations_confidence_gates_on_hit_sample() {
        val fewHits = List(6) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, frameOpen = false) } +
            List(3) { goNoGo(EmotionTrialOutcome.HIT, 200, frameOpen = true) } // fast (anticipatory) hits, but too few
        assertTrue(!engine.blockMetrics(fewHits).getValue(EmotionMetricKey.ANTICIPATIONS).confident, "3 hits < MIN_RT → not confident")
        val enoughHits = List(6) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, frameOpen = false) } +
            List(8) { goNoGo(EmotionTrialOutcome.HIT, 200, frameOpen = true) } // 8 hits ≥ MIN_RT
        assertTrue(engine.blockMetrics(enoughHits).getValue(EmotionMetricKey.ANTICIPATIONS).confident, "8 hits ≥ MIN_RT → confident")
    }

    @Test
    fun no_metric_is_nan_and_empty_block_is_empty() {
        assertTrue(engine.blockMetrics(emptyList()).isEmpty())
        val degenerate = List(10) { choice(EmotionTrialOutcome.NO_RESPONSE, null) }
        engine.blockMetrics(degenerate).values.forEach { assertTrue(!it.value.isNaN(), "${it.key} is NaN") }
    }
}
