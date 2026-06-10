package com.mindos.app.features.mindboost.memory.domain.adaptive

import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricSet
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricUnit
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryPerformanceTest {

    private val config = MemoryAdaptiveConfig()

    private fun metric(key: MemoryMetricKey, value: Double, unit: MemoryMetricUnit) =
        MemoryMetricValue(key, value, n = 10, confident = true, unit = unit)

    private fun block(vararg pairs: Pair<MemoryMetricKey, MemoryMetricValue>): MemoryMetricSet = pairs.toMap()

    @Test
    fun highAccuracyNoErrors_yieldsHighPerformance() {
        val b = block(
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY to
                metric(MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY, 0.95, MemoryMetricUnit.PROPORTION),
        )
        val p = MemoryPerformance.of(b, confidenceBias = 0.0, config = config)
        // 0.4*0.95 + 0.4*0.95 (delayed falls back to immediate) + 0.2*0.5 (neutral speed) = 0.86
        assertEquals(0.86, p, absoluteTolerance = 1e-9)
    }

    @Test
    fun intrusionsAndBiasReducePerformance() {
        val clean = block(
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY to
                metric(MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY, 0.8, MemoryMetricUnit.PROPORTION),
        )
        val noisy = block(
            MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY to
                metric(MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY, 0.8, MemoryMetricUnit.PROPORTION),
            MemoryMetricKey.INTRUSION_RATE to
                metric(MemoryMetricKey.INTRUSION_RATE, 0.5, MemoryMetricUnit.PROPORTION),
        )
        assertTrue(
            MemoryPerformance.of(noisy, 0.0, config) < MemoryPerformance.of(clean, 0.0, config),
            "Intrusions must lower performance.",
        )
        assertTrue(
            MemoryPerformance.of(clean, confidenceBias = 0.6, config = config) <
                MemoryPerformance.of(clean, confidenceBias = 0.0, config = config),
            "Confidence bias must lower performance.",
        )
    }

    @Test
    fun speedNorm_isNeutralWithoutRt() {
        assertEquals(0.5, MemoryPerformance.speedNorm(emptyMap(), config), 1e-9)
    }

    @Test
    fun speedNorm_fasterIsHigher() {
        val fast = block(
            MemoryMetricKey.RETRIEVAL_SPEED to
                metric(MemoryMetricKey.RETRIEVAL_SPEED, 500.0, MemoryMetricUnit.MILLISECONDS),
        )
        val slow = block(
            MemoryMetricKey.RETRIEVAL_SPEED to
                metric(MemoryMetricKey.RETRIEVAL_SPEED, 3500.0, MemoryMetricUnit.MILLISECONDS),
        )
        assertTrue(MemoryPerformance.speedNorm(fast, config) > MemoryPerformance.speedNorm(slow, config))
    }

    @Test
    fun zoneOf_mapsBoundaries() {
        assertEquals(MemoryZone.FRUSTRATION, zoneOf(0.30, config))
        assertEquals(MemoryZone.SUBOPTIMAL_LOW, zoneOf(0.50, config))
        assertEquals(MemoryZone.OPTIMAL, zoneOf(0.70, config))
        assertEquals(MemoryZone.SUBOPTIMAL_HIGH, zoneOf(0.85, config))
        assertEquals(MemoryZone.TOO_EASY, zoneOf(0.95, config))
    }
}
