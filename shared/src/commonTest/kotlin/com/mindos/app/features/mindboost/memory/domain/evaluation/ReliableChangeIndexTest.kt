package com.mindos.app.features.mindboost.memory.domain.evaluation

import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReliableChangeIndexTest {

    private fun reliability(reliability: Double, sdPre: Double) =
        ReliabilityEstimate(MemoryMetricKey.DELAYED_RECALL_ACCURACY, reliability, sdPre)

    @Test
    fun zeroVarianceOrBadReliability_isNotEnoughData() {
        assertIs<RciResult.NotEnoughData>(
            ReliableChangeIndex.compute(0.5, 0.7, reliability(0.8, sdPre = 0.0)),
        )
        assertIs<RciResult.NotEnoughData>(
            ReliableChangeIndex.compute(0.5, 0.7, reliability(0.0, sdPre = 0.1)),
        )
    }

    @Test
    fun largeChange_isReliable() {
        val r = ReliableChangeIndex.compute(0.40, 0.90, reliability(0.9, sdPre = 0.10))
        val value = assertIs<RciResult.Value>(r)
        assertTrue(value.reliable, "A 0.5 jump with small SD should exceed the 1.96 threshold.")
    }

    @Test
    fun tinyChange_isNotReliable() {
        val r = ReliableChangeIndex.compute(0.50, 0.51, reliability(0.9, sdPre = 0.20))
        val value = assertIs<RciResult.Value>(r)
        assertTrue(!value.reliable, "A negligible change must not be flagged reliable.")
    }
}
