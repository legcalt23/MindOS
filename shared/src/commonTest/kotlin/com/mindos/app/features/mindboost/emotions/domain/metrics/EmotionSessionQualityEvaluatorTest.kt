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

    private fun metrics(confident: Boolean): EmotionMetricSet =
        mapOf(EmotionMetricKey.CATEGORY_ACCURACY to EmotionMetricValue(EmotionMetricKey.CATEGORY_ACCURACY, 0.9, 20, confident, EmotionMetricUnit.PROPORTION))

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
}
