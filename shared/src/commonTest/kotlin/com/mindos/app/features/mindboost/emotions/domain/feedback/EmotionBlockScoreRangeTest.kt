@file:Suppress("MagicNumber") // The values ARE the boundary cases under test.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricUnit
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Ola 3 · R1 — `MB-EMO-0014`: the 0-100 mapping assumed every governing metric is a proportion, and clamped
 * anything else into range before publishing it.
 *
 * That assumption was already false when it was written (d' runs roughly -4..+4) and R1 makes it false twice
 * over (SSRT is a latency). Under the clamp a strong d' of 3.6 published as a flawless 100 and a weak 0.9 as a
 * confident 90 — a fabricated number derived from a real measurement, which is the expensive direction of the
 * charter's `null != 0`. The mapping now refuses what it cannot honestly map.
 */
class EmotionBlockScoreRangeTest {

    private fun metric(value: Double, key: EmotionMetricKey = EmotionMetricKey.CATEGORY_ACCURACY) =
        EmotionMetricValue(key = key, value = value, n = 40, confident = true, unit = EmotionMetricUnit.PROPORTION)

    @Test
    fun a_reading_outside_the_unit_interval_has_no_score_instead_of_a_clamped_one() {
        assertNull(
            EmotionBlockScore.of(metric(3.6, EmotionMetricKey.CATEGORY_DPRIME_MIN), MetricDirection.HIGHER_IS_BETTER),
            "a d' of 3.6 used to publish as a perfect 100",
        )
        assertNull(
            EmotionBlockScore.of(metric(-1.2, EmotionMetricKey.CATEGORY_DPRIME_MIN), MetricDirection.HIGHER_IS_BETTER),
            "a negative d' used to publish as a 0, which reads as a measured floor rather than an unmappable value",
        )
        assertNull(
            EmotionBlockScore.of(metric(284.0, EmotionMetricKey.SSRT), MetricDirection.LOWER_IS_BETTER),
            "a latency in milliseconds has no 0-100 mapping at all",
        )
    }

    /**
     * The discriminating half. Refusing everything would pass the test above and silently delete the score from
     * every exercise that legitimately has one, so the proportions must map exactly as they always did —
     * boundaries included, since 0.0 and 1.0 are inside the interval and are NOT the clamp doing the work.
     */
    @Test
    fun a_proportion_still_maps_exactly_as_before() {
        assertEquals(90, EmotionBlockScore.of(metric(0.9), MetricDirection.HIGHER_IS_BETTER))
        assertEquals(10, EmotionBlockScore.of(metric(0.9), MetricDirection.LOWER_IS_BETTER))
        assertEquals(100, EmotionBlockScore.of(metric(1.0), MetricDirection.HIGHER_IS_BETTER))
        assertEquals(0, EmotionBlockScore.of(metric(0.0), MetricDirection.HIGHER_IS_BETTER))
        assertEquals(100, EmotionBlockScore.of(metric(0.0), MetricDirection.LOWER_IS_BETTER))
    }

    @Test
    fun an_unconfident_or_directionless_reading_still_has_no_score() {
        assertNull(
            EmotionBlockScore.of(
                EmotionMetricValue(
                    EmotionMetricKey.CATEGORY_ACCURACY,
                    0.9,
                    40,
                    confident = false,
                    EmotionMetricUnit.PROPORTION,
                ),
                MetricDirection.HIGHER_IS_BETTER,
            ),
        )
        assertNull(EmotionBlockScore.of(metric(0.9), MetricDirection.INFORMATIONAL_ONLY))
        assertNull(EmotionBlockScore.of(metric(0.9), MetricDirection.RANGE_IS_BETTER))
        assertNull(EmotionBlockScore.of(null, MetricDirection.HIGHER_IS_BETTER))
    }
}
