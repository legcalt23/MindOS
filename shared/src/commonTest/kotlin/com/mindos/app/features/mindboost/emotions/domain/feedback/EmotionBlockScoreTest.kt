@file:Suppress("MagicNumber", "MaxLineLength") // Score fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** MC8u-C §2: the 0–100 training-performance block score — honest, direction-normalized, null (CALIBRATING) when unsure. */
class EmotionBlockScoreTest {

    private fun metric(key: EmotionMetricKey, value: Double, confident: Boolean) =
        EmotionMetricValue(key, value, 20, confident, EmotionMetricUnit.PROPORTION)

    @Test
    fun higher_is_better_confident_maps_directly() {
        val score = EmotionBlockScore.of(metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.85, true), MetricDirection.HIGHER_IS_BETTER)
        assertEquals(85, score)
    }

    @Test
    fun lower_is_better_confident_inverts() {
        val score = EmotionBlockScore.of(metric(EmotionMetricKey.COMMISSION_RATE, 0.10, true), MetricDirection.LOWER_IS_BETTER)
        assertEquals(90, score)
    }

    @Test
    fun not_confident_is_calibrating_null() {
        assertNull(EmotionBlockScore.of(metric(EmotionMetricKey.CATEGORY_ACCURACY, 0.9, false), MetricDirection.HIGHER_IS_BETTER))
    }

    @Test
    fun null_metric_is_null() {
        assertNull(EmotionBlockScore.of(null, MetricDirection.HIGHER_IS_BETTER))
    }

    @Test
    fun range_or_informational_metric_has_no_honest_score() {
        assertNull(EmotionBlockScore.of(metric(EmotionMetricKey.RT_MEDIAN, 600.0, true), MetricDirection.RANGE_IS_BETTER))
        assertNull(EmotionBlockScore.of(metric(EmotionMetricKey.POST_ERROR_SLOWING, 50.0, true), MetricDirection.INFORMATIONAL_ONLY))
    }
}
