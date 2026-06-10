package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultFocusScoreCalculatorTest {

    private val calculator = DefaultFocusScoreCalculator()

    private fun metric(
        key: MetricKey,
        value: Double,
        n: Int,
        confident: Boolean = true,
        unit: MetricUnit = MetricUnit.INDEX,
    ) = MetricValue(key, value, n, confident, unit)

    @Test
    fun noConfidentMetrics_yieldsNoScore() {
        val score = calculator.compute(emptyMap())
        assertNull(score.value)
        assertFalse(score.confident)
    }

    @Test
    fun confidentMetrics_produceScoreWithinBand() {
        val metrics = mapOf(
            MetricKey.SUSTAINED_FOCUS_INDEX to metric(MetricKey.SUSTAINED_FOCUS_INDEX, 80.0, 60),
            MetricKey.ACCURACY to metric(MetricKey.ACCURACY, 0.90, 60, unit = MetricUnit.PROPORTION),
        )
        val score = calculator.compute(metrics)
        // Components: 80 and 90 → average 85.
        assertEquals(85.0, score.value!!, absoluteTolerance = 0.01)
        assertTrue(score.confident)
        assertTrue(score.value!! in score.lowerBound!!..score.upperBound!!)
    }

    @Test
    fun lessData_widensTheUncertaintyBand() {
        fun bandWidth(n: Int): Double {
            val metrics = mapOf(
                MetricKey.SUSTAINED_FOCUS_INDEX to metric(MetricKey.SUSTAINED_FOCUS_INDEX, 70.0, n),
                MetricKey.ACCURACY to metric(MetricKey.ACCURACY, 0.80, n, unit = MetricUnit.PROPORTION),
            )
            val s = calculator.compute(metrics)
            return s.upperBound!! - s.lowerBound!!
        }
        assertTrue(bandWidth(10) > bandWidth(60))
    }

    @Test
    fun singleComponent_isNotConfident() {
        val score = calculator.compute(
            mapOf(MetricKey.ACCURACY to metric(MetricKey.ACCURACY, 0.9, 60, unit = MetricUnit.PROPORTION)),
        )
        assertFalse(score.confident) // needs at least two components
    }
}
