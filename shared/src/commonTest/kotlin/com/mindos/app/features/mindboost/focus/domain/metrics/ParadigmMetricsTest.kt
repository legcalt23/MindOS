package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialMarkers
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParadigmMetricsTest {

    private val engine = DefaultMetricsEngine()
    private val thresholds = IndividualThresholds()

    @Test
    fun switchCost_isRtDifferenceBetweenSwitchAndRepeat() {
        fun t(i: Int, rt: Long, switch: Boolean) = FocusTestFixtures.trial(
            i, TrialOutcome.HIT, rtMs = rt,
            markers = TrialMarkers(isSwitch = switch), exerciseId = ExerciseId.E6_CAMBIO,
        )
        val trials = buildList {
            repeat(4) { add(t(it, 400, switch = true)) }
            repeat(4) { add(t(10 + it, 320, switch = false)) }
        }
        val metrics = engine.blockMetrics(trials, ExerciseCatalog.E6_CAMBIO, thresholds)
        assertEquals(80.0, metrics.valueOf(MetricKey.SWITCH_COST), absoluteTolerance = 1.0)
    }

    @Test
    fun setSizeSlope_isPositiveWhenRtGrowsWithSetSize() {
        fun t(i: Int, rt: Long, size: Int) = FocusTestFixtures.trial(
            i, TrialOutcome.HIT, rtMs = rt,
            markers = TrialMarkers(setSize = size), exerciseId = ExerciseId.E7_RASTREO,
        )
        val trials = buildList {
            listOf(4, 8, 16).forEach { size ->
                repeat(3) { add(t(size * 10 + it, 280L + size * 8L, size)) }
            }
        }
        val metrics = engine.blockMetrics(trials, ExerciseCatalog.E7_RASTREO, thresholds)
        assertTrue(metrics.valueOf(MetricKey.SET_SIZE_SLOPE) > 0.0)
    }

    @Test
    fun dualTaskCost_isAccuracyDropFromSingleToDual() {
        fun t(i: Int, o: TrialOutcome, dual: Boolean) = FocusTestFixtures.trial(
            i, o, markers = TrialMarkers(dualTask = dual), exerciseId = ExerciseId.E8_DUO,
        )
        val trials = buildList {
            repeat(5) { add(t(it, TrialOutcome.HIT, dual = false)) }
            repeat(3) { add(t(10 + it, TrialOutcome.HIT, dual = true)) }
            repeat(2) { add(t(20 + it, TrialOutcome.MISS, dual = true)) }
        }
        val metrics = engine.blockMetrics(trials, ExerciseCatalog.E8_DUO, thresholds)
        // single accuracy 1.0; dual 3/5 = 0.6 → cost 0.4.
        assertEquals(0.4, metrics.valueOf(MetricKey.DUAL_TASK_COST), absoluteTolerance = 0.01)
    }

    @Test
    fun distractionResistance_isAccuracyRatioUnderIntrusion() {
        fun t(i: Int, o: TrialOutcome, intrusion: Boolean) = FocusTestFixtures.trial(
            i, o, markers = TrialMarkers(duringIntrusion = intrusion), exerciseId = ExerciseId.E11_ANCLA,
        )
        val trials = buildList {
            repeat(5) { add(t(it, TrialOutcome.HIT, intrusion = false)) }
            repeat(4) { add(t(10 + it, TrialOutcome.HIT, intrusion = true)) }
            add(t(20, TrialOutcome.MISS, intrusion = true))
        }
        val metrics = engine.blockMetrics(trials, ExerciseCatalog.E11_ANCLA, thresholds)
        // baseline accuracy 1.0; intrusion 4/5 = 0.8 → resistance 0.8.
        assertEquals(0.8, metrics.valueOf(MetricKey.DISTRACTION_RESISTANCE_INDEX), absoluteTolerance = 0.01)
    }

    @Test
    fun pressureDelta_isAccuracyDropUnderDeadline() {
        fun t(i: Int, o: TrialOutcome, pressure: Boolean) = FocusTestFixtures.trial(
            i, o, markers = TrialMarkers(underPressure = pressure), exerciseId = ExerciseId.E13_PRESION,
        )
        val trials = buildList {
            repeat(5) { add(t(it, TrialOutcome.HIT, pressure = false)) }
            repeat(7) { add(t(10 + it, TrialOutcome.HIT, pressure = true)) }
            repeat(3) { add(t(20 + it, TrialOutcome.MISS, pressure = true)) }
        }
        val metrics = engine.blockMetrics(trials, ExerciseCatalog.E13_PRESION, thresholds)
        // calm accuracy 1.0; pressure 7/10 = 0.7 → delta 0.3.
        assertEquals(0.3, metrics.valueOf(MetricKey.PRESSURE_DELTA), absoluteTolerance = 0.01)
    }
}
