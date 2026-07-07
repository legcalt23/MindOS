@file:Suppress("MagicNumber", "MaxLineLength") // RT/accuracy fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8u-B+ §B/§E: the per-session quality signal that gates calibration. */
class EmotionSessionQualityEvaluatorTest {

    private fun trial(rt: Long): ClassifiedEmotionTrial {
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.Classification("x", EmotionCategory.INTERNAL_STATE, false), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, rt, EmotionResponseKey.CATEGORY_A), EmotionTrialOutcome.CORRECT)
    }

    private fun noResponseTrial(): ClassifiedEmotionTrial {
        val stim = EmotionStimulus(0, null, EmotionStimulusPayload.Classification("x", EmotionCategory.INTERNAL_STATE, false), 0, 1000)
        return ClassifiedEmotionTrial(RecordedEmotionTrial(stim, null, EmotionResponseKey.NONE), EmotionTrialOutcome.NO_RESPONSE)
    }

    private fun metrics(confident: Boolean, key: EmotionMetricKey = EmotionMetricKey.CATEGORY_ACCURACY, value: Double = 0.9): EmotionMetricSet =
        mapOf(key to EmotionMetricValue(key, value, 20, confident, EmotionMetricUnit.PROPORTION))

    @Test
    fun confident_governing_with_normal_timing_is_clean() {
        val q = EmotionSessionQualityEvaluator.evaluate(List(10) { trial(600) }, metrics(confident = true), EmotionMetricKey.CATEGORY_ACCURACY)
        assertTrue(q.governingConfident)
        assertTrue(q.anomalyFlags.isEmpty())
    }

    @Test
    fun unconfident_governing_flags_low_confidence() {
        val q = EmotionSessionQualityEvaluator.evaluate(List(10) { trial(600) }, metrics(confident = false), EmotionMetricKey.CATEGORY_ACCURACY)
        assertFalse(q.governingConfident)
        assertTrue(q.anomalyFlags.contains(EmotionSessionQualityEvaluator.ANOMALY_LOW_CONFIDENCE))
    }

    @Test
    fun impossible_timing_is_flagged_even_when_governing_confident() {
        val q = EmotionSessionQualityEvaluator.evaluate(List(10) { trial(50) }, metrics(confident = true), EmotionMetricKey.CATEGORY_ACCURACY)
        assertTrue(q.anomalyFlags.contains(EmotionSessionQualityEvaluator.ANOMALY_IMPOSSIBLE_TIMING))
    }

    // ---- MC8u-C §8 engagement floor ----------------------------------------------------------------------------

    @Test
    fun no_response_session_is_flagged_insufficient_engagement() {
        val q = EmotionSessionQualityEvaluator.evaluate(List(20) { noResponseTrial() }, metrics(confident = true), EmotionMetricKey.CATEGORY_ACCURACY)
        assertTrue(q.anomalyFlags.contains(EmotionSessionQualityEvaluator.ANOMALY_INSUFFICIENT_ENGAGEMENT))
    }

    @Test
    fun too_few_responses_is_flagged() {
        val trials = List(3) { trial(600) } + List(7) { noResponseTrial() } // 30% engaged
        val q = EmotionSessionQualityEvaluator.evaluate(trials, metrics(confident = true), EmotionMetricKey.CATEGORY_ACCURACY)
        assertTrue(q.anomalyFlags.contains(EmotionSessionQualityEvaluator.ANOMALY_INSUFFICIENT_ENGAGEMENT))
    }

    @Test
    fun enough_responses_even_slow_is_engaged() {
        val trials = List(12) { trial(1400) } + List(3) { noResponseTrial() } // 80% engaged, slow but honest
        val q = EmotionSessionQualityEvaluator.evaluate(trials, metrics(confident = true), EmotionMetricKey.CATEGORY_ACCURACY)
        assertFalse(q.anomalyFlags.contains(EmotionSessionQualityEvaluator.ANOMALY_INSUFFICIENT_ENGAGEMENT))
        assertTrue(q.anomalyFlags.isEmpty(), "an engaged, confident, normally-timed session is clean: ${q.anomalyFlags}")
    }

    @Test
    fun low_precision_but_engaged_session_can_count() {
        // A confident, engaged reading of LOW performance is still valid calibration — low score, not invalid.
        val q = EmotionSessionQualityEvaluator.evaluate(List(15) { trial(600) }, metrics(confident = true, value = 0.35), EmotionMetricKey.CATEGORY_ACCURACY)
        assertTrue(q.governingConfident)
        assertTrue(q.anomalyFlags.isEmpty())
    }

    @Test
    fun go_no_go_is_exempt_from_engagement_floor() {
        // Withholding is a correct response in go/no-go; its engagement is enforced by the commission hit-rate floor.
        val q = EmotionSessionQualityEvaluator.evaluate(List(20) { noResponseTrial() }, metrics(confident = true, key = EmotionMetricKey.COMMISSION_RATE), EmotionMetricKey.COMMISSION_RATE)
        assertFalse(q.anomalyFlags.contains(EmotionSessionQualityEvaluator.ANOMALY_INSUFFICIENT_ENGAGEMENT))
    }
}
