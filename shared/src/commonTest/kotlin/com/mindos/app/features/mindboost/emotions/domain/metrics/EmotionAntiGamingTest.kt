@file:Suppress("MagicNumber", "MaxLineLength") // RT/accuracy/trial-count fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC8u-B §1 anti-gaming: a high SCORE must never be produced without honest engagement. Covers the commission
 * hit-rate floor (§1A), the post-error baseline gate (§1C), the granularity denominator (§1D), and the coarse
 * random/spam/impossible-RT guard (§1E). Multi-alternative helpers map each chosen category to its REAL response key
 * (unlike a single-key fixture) so the same-answer-spam guard is exercised faithfully.
 */
class EmotionAntiGamingTest {

    private val engine = DefaultEmotionMetricsEngine()

    private fun goNoGo(outcome: EmotionTrialOutcome, rt: Long?, frameOpen: Boolean): ClassifiedEmotionTrial {
        val key = if (outcome == EmotionTrialOutcome.HIT || outcome == EmotionTrialOutcome.FALSE_ALARM) {
            EmotionResponseKey.PRIMARY
        } else {
            EmotionResponseKey.NONE
        }
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.AffectiveGoNoGo(frameOpen, "espera", true), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, key), outcome)
    }

    private fun choice(outcome: EmotionTrialOutcome, rt: Long?, key: EmotionResponseKey = defaultKey(outcome)): ClassifiedEmotionTrial {
        val stim = EmotionStimulus(0, EmotionResponseKey.LEFT, EmotionStimulusPayload.TwoAltPerceptual(EmotionResponseKey.LEFT, 0.6, false), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, key), outcome)
    }

    private fun defaultKey(outcome: EmotionTrialOutcome): EmotionResponseKey = when (outcome) {
        EmotionTrialOutcome.CORRECT -> EmotionResponseKey.LEFT
        EmotionTrialOutcome.INCORRECT -> EmotionResponseKey.RIGHT
        else -> EmotionResponseKey.NONE
    }

    private fun keyFor(category: EmotionCategory?): EmotionResponseKey = when (category) {
        EmotionCategory.INTERNAL_STATE -> EmotionResponseKey.CATEGORY_A
        EmotionCategory.THOUGHT -> EmotionResponseKey.CATEGORY_B
        EmotionCategory.IMPULSE -> EmotionResponseKey.CATEGORY_C
        EmotionCategory.FACT -> EmotionResponseKey.CATEGORY_D
        null -> EmotionResponseKey.NONE
    }

    /** Classification trial where the chosen category maps to its REAL button key (faithful to the live holder). */
    private fun classif(truth: EmotionCategory, chosen: EmotionCategory?, rt: Long = 600): ClassifiedEmotionTrial {
        val outcome = when (chosen) {
            null -> EmotionTrialOutcome.NO_RESPONSE
            truth -> EmotionTrialOutcome.CORRECT
            else -> EmotionTrialOutcome.INCORRECT
        }
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.Classification("x", truth, false), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, keyFor(chosen), chosen), outcome)
    }

    // ---- §1A commission hit-rate floor -------------------------------------------------------------------------

    @Test
    fun commission_rate_zero_with_low_hit_rate_is_not_confident() {
        val trials = List(6) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, false) } +
            List(8) { goNoGo(EmotionTrialOutcome.MISS, null, true) } // never responded to GO trials
        val commission = engine.blockMetrics(trials).getValue(EmotionMetricKey.COMMISSION_RATE)
        assertEquals(0.0, commission.value, 1e-9)
        assertFalse(commission.confident, "commission 0 with hit rate 0 must not read as perfect inhibition")
    }

    @Test
    fun no_response_strategy_does_not_look_like_perfect_inhibition() {
        val trials = List(8) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, false) } +
            List(8) { goNoGo(EmotionTrialOutcome.MISS, null, true) }
        assertFalse(engine.blockMetrics(trials).getValue(EmotionMetricKey.COMMISSION_RATE).confident)
    }

    @Test
    fun commission_rate_can_be_confident_when_hit_rate_and_sample_are_sufficient() {
        val trials = List(2) { goNoGo(EmotionTrialOutcome.FALSE_ALARM, 300, false) } +
            List(4) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, false) } +
            List(8) { goNoGo(EmotionTrialOutcome.HIT, 500, true) }
        val commission = engine.blockMetrics(trials).getValue(EmotionMetricKey.COMMISSION_RATE)
        assertEquals(2.0 / 6.0, commission.value, 1e-9)
        assertTrue(commission.confident, "engaged block (hit rate 1.0, 6 no-go, 10 responses) must be confident")
    }

    @Test
    fun commission_metric_explains_low_confidence_when_hit_rate_too_low() {
        val trials = List(6) { goNoGo(EmotionTrialOutcome.CORRECT_REJECTION, null, false) } +
            List(2) { goNoGo(EmotionTrialOutcome.HIT, 500, true) } +
            List(6) { goNoGo(EmotionTrialOutcome.MISS, null, true) } // hit rate = 2/8 = 0.25
        val metrics = engine.blockMetrics(trials)
        assertFalse(metrics.getValue(EmotionMetricKey.COMMISSION_RATE).confident)
        val hitRate = metrics.getValue(EmotionMetricKey.HIT_RATE)
        assertEquals(0.25, hitRate.value, 1e-9)
        assertTrue(hitRate.value < 0.5, "the low HIT_RATE is why commission was withheld — it is emitted to explain it")
    }

    // ---- §1C post-error recovery baseline gate -----------------------------------------------------------------

    @Test
    fun post_error_recovery_not_confident_below_min_sample() {
        val trials = List(6) { choice(EmotionTrialOutcome.CORRECT, 500) } + choice(EmotionTrialOutcome.INCORRECT, 500) +
            List(5) { choice(EmotionTrialOutcome.CORRECT, 500) } // only 1 post-error case
        assertFalse(engine.blockMetrics(trials).getValue(EmotionMetricKey.POST_ERROR_RECOVERY).confident)
    }

    @Test
    fun post_error_recovery_unavailable_without_baseline() {
        val trials = List(12) { choice(EmotionTrialOutcome.INCORRECT, 500) } // all errors → no post-correct baseline
        assertFalse(engine.blockMetrics(trials).getValue(EmotionMetricKey.POST_ERROR_RECOVERY).confident, "no baseline → withheld")
    }

    @Test
    fun post_error_recovery_confident_with_enough_cases() {
        val trials = buildList {
            repeat(3) {
                add(choice(EmotionTrialOutcome.CORRECT, 500))
                add(choice(EmotionTrialOutcome.INCORRECT, 500))
                add(choice(EmotionTrialOutcome.CORRECT, 800))
            }
        }
        assertTrue(engine.blockMetrics(trials).getValue(EmotionMetricKey.POST_ERROR_RECOVERY).confident, "3 post-error + 5 post-correct is enough")
    }

    @Test
    fun post_error_recovery_does_not_infer_from_single_error() {
        val trials = List(5) { choice(EmotionTrialOutcome.CORRECT, 500) } + choice(EmotionTrialOutcome.INCORRECT, 500) +
            List(6) { choice(EmotionTrialOutcome.CORRECT, 500) }
        assertFalse(engine.blockMetrics(trials).getValue(EmotionMetricKey.POST_ERROR_RECOVERY).confident)
    }

    // ---- §1D granularity denominator ---------------------------------------------------------------------------

    @Test
    fun granularity_index_uses_distinct_presented_categories() {
        val trials = List(3) { classif(EmotionCategory.INTERNAL_STATE, EmotionCategory.INTERNAL_STATE) } +
            List(3) { classif(EmotionCategory.THOUGHT, EmotionCategory.THOUGHT) } +
            List(3) { classif(EmotionCategory.IMPULSE, EmotionCategory.IMPULSE) }
        val granularity = engine.blockMetrics(trials).getValue(EmotionMetricKey.GRANULARITY_INDEX)
        assertEquals(1.0, granularity.value, 1e-9) // 3 distinct correct / 3 presented, NOT 3/4
        assertTrue(granularity.confident)
    }

    @Test
    fun granularity_index_does_not_penalize_unpresented_categories() {
        val trials = List(5) { classif(EmotionCategory.INTERNAL_STATE, EmotionCategory.INTERNAL_STATE) } +
            List(5) { classif(EmotionCategory.THOUGHT, EmotionCategory.THOUGHT) }
        assertEquals(1.0, engine.blockMetrics(trials).getValue(EmotionMetricKey.GRANULARITY_INDEX).value, 1e-9) // 2/2, not 2/4
    }

    @Test
    fun granularity_index_is_not_confident_when_too_few_categories_presented() {
        val trials = List(5) { classif(EmotionCategory.INTERNAL_STATE, EmotionCategory.INTERNAL_STATE) } +
            List(5) { classif(EmotionCategory.THOUGHT, EmotionCategory.THOUGHT) }
        assertFalse(engine.blockMetrics(trials).getValue(EmotionMetricKey.GRANULARITY_INDEX).confident, "only 2 categories presented")
    }

    @Test
    fun granularity_index_handles_repeated_categories_without_inflating_score() {
        // All four categories presented, but the user always taps the same category → only INTERNAL_STATE is correct.
        val trials = EmotionCategory.entries.flatMap { truth ->
            List(3) { classif(truth, EmotionCategory.INTERNAL_STATE) }
        }
        assertEquals(0.25, engine.blockMetrics(trials).getValue(EmotionMetricKey.GRANULARITY_INDEX).value, 1e-9) // 1 distinct / 4 presented
    }

    // ---- §1E random / spam / impossible RT ---------------------------------------------------------------------

    @Test
    fun same_answer_spam_does_not_produce_high_confidence() {
        val trials = List(12) { classif(EmotionCategory.INTERNAL_STATE, EmotionCategory.INTERNAL_STATE) } // always CATEGORY_A
        val accuracy = engine.blockMetrics(trials).getValue(EmotionMetricKey.CATEGORY_ACCURACY)
        assertEquals(1.0, accuracy.value, 1e-9)
        assertFalse(accuracy.confident, "a same-answer spam streak must not earn a confident perfect score")
    }

    @Test
    fun impossible_reaction_times_are_not_confident() {
        // Alternating keys (no same-key spam) but every response is physiologically impossible (50 ms).
        val keys = listOf(EmotionResponseKey.LEFT, EmotionResponseKey.RIGHT)
        val trials = (0 until 10).map { choice(EmotionTrialOutcome.CORRECT, 50, keys[it % 2]) }
        assertFalse(engine.blockMetrics(trials).getValue(EmotionMetricKey.ACCURACY).confident, "impossible RTs are not a real decision")
    }

    @Test
    fun random_answers_cannot_produce_high_score() {
        val trials = List(5) { choice(EmotionTrialOutcome.CORRECT, 200) } + List(5) { choice(EmotionTrialOutcome.INCORRECT, 200) }
        val accuracy = engine.blockMetrics(trials).getValue(EmotionMetricKey.ACCURACY)
        assertEquals(0.5, accuracy.value, 1e-9)
        assertFalse(accuracy.confident, "near-chance accuracy at fast RT is guessing, not discrimination")
    }
}
