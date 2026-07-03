package com.mindos.app.features.arena

import com.mindos.app.features.arena.metrics.MetricCatalog
import com.mindos.app.features.arena.metrics.MetricEvaluator
import com.mindos.app.features.arena.metrics.MetricId
import com.mindos.app.features.arena.metrics.MetricStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 09 / P9.1: Metric catalog completeness + threshold evaluation. */
class AcceptanceMetricsTest {

    @Test
    fun catalogDefinesAllTwentyFourMetricsWithNoMissingThresholds() {
        assertEquals(24, MetricId.entries.size)
        assertEquals(24, MetricCatalog.DEFINITIONS.size)
        assertTrue(MetricId.entries.all { MetricCatalog.DEFINITIONS.containsKey(it) })
    }

    @Test
    fun lowerIsBetterBands() {
        val t = MetricCatalog.definitionFor(MetricId.MET_01).threshold // <30 / >40 / >60
        assertEquals(MetricStatus.PASS, MetricEvaluator.evaluate(20.0, t))
        assertEquals(MetricStatus.WARNING, MetricEvaluator.evaluate(35.0, t))
        assertEquals(MetricStatus.FAILURE, MetricEvaluator.evaluate(70.0, t))
    }

    @Test
    fun higherIsBetterBands() {
        val t = MetricCatalog.definitionFor(MetricId.MET_05).threshold // >95 / <90 / <80
        assertEquals(MetricStatus.PASS, MetricEvaluator.evaluate(97.0, t))
        assertEquals(MetricStatus.WARNING, MetricEvaluator.evaluate(92.0, t))
        assertEquals(MetricStatus.FAILURE, MetricEvaluator.evaluate(70.0, t))
    }

    @Test
    fun withinMarginBands() {
        val t = MetricCatalog.definitionFor(MetricId.MET_24).threshold // ±0.5 / ±1.0 / ±2.0
        assertEquals(MetricStatus.PASS, MetricEvaluator.evaluate(0.3, t))
        assertEquals(MetricStatus.PASS, MetricEvaluator.evaluate(-0.4, t))
        assertEquals(MetricStatus.WARNING, MetricEvaluator.evaluate(0.8, t))
        assertEquals(MetricStatus.FAILURE, MetricEvaluator.evaluate(1.5, t))
    }

    @Test
    fun exactHundredPercentTargetsPassOnlyAtHundred() {
        val t = MetricCatalog.definitionFor(MetricId.MET_17).threshold // Rollback consistency = 100%
        assertEquals(MetricStatus.PASS, MetricEvaluator.evaluate(100.0, t))
        assertEquals(MetricStatus.FAILURE, MetricEvaluator.evaluate(99.9, t))
    }
}
