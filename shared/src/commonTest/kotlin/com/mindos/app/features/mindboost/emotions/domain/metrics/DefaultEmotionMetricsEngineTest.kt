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
        val stim = EmotionStimulus(0, EmotionResponseKey.LEFT, EmotionStimulusPayload.TwoAltPerceptual(EmotionResponseKey.LEFT, 0.6, false), 0, 1000)
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
        val slowAccurate = buildList {
            repeat(3) {
                add(choice(EmotionTrialOutcome.CORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 500))
                add(choice(EmotionTrialOutcome.CORRECT, 900)) // post-error: accurate, slow
            }
        }
        val fastFlat = buildList {
            repeat(3) {
                add(choice(EmotionTrialOutcome.CORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 200)) // post-error: wrong, impulsive
            }
        }
        val slow = engine.blockMetrics(slowAccurate).getValue(EmotionMetricKey.POST_ERROR_RECOVERY)
        val fast = engine.blockMetrics(fastFlat).getValue(EmotionMetricKey.POST_ERROR_RECOVERY)
        assertTrue(slow.confident && fast.confident, "both have ≥3 post-error pairs")
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
        assertEquals(1.0, metrics.getValue(EmotionMetricKey.EMOTION_THOUGHT_SEPARATION).value, 1e-9)
    }

    @Test
    fun no_metric_is_nan_and_empty_block_is_empty() {
        assertTrue(engine.blockMetrics(emptyList()).isEmpty())
        val degenerate = List(10) { choice(EmotionTrialOutcome.NO_RESPONSE, null) }
        engine.blockMetrics(degenerate).values.forEach { assertTrue(!it.value.isNaN(), "${it.key} is NaN") }
    }
}
