package com.mindos.app.features.arena

import com.mindos.app.features.arena.metrics.AcceptanceMetricCalculator
import com.mindos.app.features.arena.metrics.MetricId
import com.mindos.app.features.arena.metrics.MetricInputs
import com.mindos.app.features.arena.metrics.MetricStatus
import com.mindos.app.features.arena.metrics.MetricsDashboardExporter
import com.mindos.app.features.arena.rating.engine.NonCompensatoryRank
import com.mindos.app.features.arena.rating.engine.SpecialistBadgeAssigner
import com.mindos.app.features.arena.rating.model.BlockRatingState
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import com.mindos.app.features.arena.rating.rollback.RollbackEngine
import com.mindos.app.features.arena.sanitization.DataSanitizer
import com.mindos.app.features.arena.sanitization.SyntheticIdMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 09 / P9.2: Metric computation from built features + sanitized dashboard. */
class AcceptanceMetricCalculatorTest {

    private val calculator = AcceptanceMetricCalculator()
    private val notModeled = setOf(
        MetricId.MET_14, MetricId.MET_15, MetricId.MET_16,
        MetricId.MET_18, MetricId.MET_19, MetricId.MET_20, MetricId.MET_21,
    )

    /** Real built-feature computations for a few metrics. */
    private fun rollbackConsistencyPercent(): Double {
        val engine = RollbackEngine()
        val consistent = (1..100).count { i ->
            val r = engine.rollback("S$i", originalDelta = i.toDouble(), syntheticPlayerId = "P$i", 0L, 0L)
            val restored = engine.applyRollback(ArenaTestFixtures.playerRatingState(mu = 1500.0 + i), r.invertedDelta)
            restored.globalRatingMu.value == 1500.0
        }
        return consistent.toDouble()
    }

    private fun unifocalReachingElitePercent(): Double {
        val rank = NonCompensatoryRank()
        val reachedElite = (1..50).count {
            val blocks = mapOf(
                "A" to BlockRatingState("A", Rating(1900.0), Sigma(50.0), 0L),
                "Weak" to BlockRatingState("Weak", Rating(1000.0), Sigma(50.0), 0L),
            )
            !rank.calculateGlobalRank(blocks).eliteBlocked
        }
        return reachedElite.toDouble() / 50 * 100
    }

    private fun badgePrecisionPercent(): Double {
        val blocks = mapOf(
            "Hi" to BlockRatingState("Hi", Rating(1800.0), Sigma(50.0), 0L),
            "Lo" to BlockRatingState("Lo", Rating(1400.0), Sigma(50.0), 0L),
        )
        val badged = SpecialistBadgeAssigner().assignBadges(blocks).map { it.blockId }.toSet()
        return if (badged == setOf("Hi")) 100.0 else 0.0
    }

    private fun passingInputs(): MetricInputs = MetricInputs(
        values = mapOf(
            MetricId.MET_01 to 25.0, MetricId.MET_02 to 0.5, MetricId.MET_03 to 0.4, MetricId.MET_04 to 2.0,
            MetricId.MET_05 to 97.0, MetricId.MET_06 to 99.5, MetricId.MET_07 to 0.2, MetricId.MET_08 to 0.5,
            MetricId.MET_09 to 12.0, MetricId.MET_10 to 18.0, MetricId.MET_11 to 0.02, MetricId.MET_12 to 0.0,
            MetricId.MET_13 to 0.5, MetricId.MET_17 to rollbackConsistencyPercent(),
            MetricId.MET_22 to badgePrecisionPercent(), MetricId.MET_23 to unifocalReachingElitePercent(),
            MetricId.MET_24 to 0.0,
        ),
        notModeled = notModeled,
    )

    @Test
    fun computesAll24MetricsAndFlagsNotModeled() {
        val metrics = calculator.compute(passingInputs())
        assertEquals(24, metrics.size)
        for (id in notModeled) {
            val m = metrics.first { it.id == id }
            assertEquals(MetricStatus.NOT_MODELED, m.status)
            assertEquals(null, m.value)
        }
    }

    @Test
    fun builtFeatureMetricsPassTheirThresholds() {
        val byId = calculator.compute(passingInputs()).associateBy { it.id }
        assertEquals(MetricStatus.PASS, byId.getValue(MetricId.MET_17).status) // rollback 100%
        assertEquals(MetricStatus.PASS, byId.getValue(MetricId.MET_22).status) // badge precision 100%
        assertEquals(MetricStatus.PASS, byId.getValue(MetricId.MET_23).status) // non-compensatory 0%
        assertEquals(MetricStatus.PASS, byId.getValue(MetricId.MET_24).status) // premium margin 0
        assertTrue(calculator.allModeledPass(calculator.compute(passingInputs())))
    }

    @Test
    fun aFailingValueIsReportedAndFailsTheGate() {
        val inputs = passingInputs().copy(values = passingInputs().values + (MetricId.MET_08 to 15.0))
        val metrics = calculator.compute(inputs)
        assertEquals(MetricStatus.FAILURE, metrics.first { it.id == MetricId.MET_08 }.status)
        assertTrue(!calculator.allModeledPass(metrics))
    }

    @Test
    fun dashboardRendersAll24AndIsIdentifierFree() {
        val output = MetricsDashboardExporter(DataSanitizer(SyntheticIdMapper(seed = 1)))
            .export(calculator.compute(passingInputs()))
        assertTrue(output.content.contains("MET_01"))
        assertTrue(output.content.contains("MET_24"))
        assertTrue(!Regex("[A-Za-z0-9]{20,}").containsMatchIn(output.content), "identifier-like token leaked")
    }
}
