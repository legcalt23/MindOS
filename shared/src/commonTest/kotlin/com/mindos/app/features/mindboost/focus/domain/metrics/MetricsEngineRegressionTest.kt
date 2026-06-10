package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Snapshot regression: locks the engine's output for a fixed dataset so any change
 * that silently alters a metric is caught. If a value here changes, it must be a
 * deliberate, reviewed change to the metric definition.
 */
class MetricsEngineRegressionTest {

    private val engine = DefaultMetricsEngine()

    @Test
    fun fixedBlock_producesExpectedMetrics() {
        val trials = buildList {
            // 5 hits, evenly spaced RTs (median 340, all below the 500ms lapse threshold).
            listOf(300L, 320L, 340L, 360L, 380L).forEachIndexed { i, rt ->
                add(FocusTestFixtures.trial(i, TrialOutcome.HIT, rtMs = rt))
            }
            add(FocusTestFixtures.trial(10, TrialOutcome.MISS))
            add(FocusTestFixtures.trial(11, TrialOutcome.MISS))
            add(FocusTestFixtures.trial(12, TrialOutcome.CORRECT_REJECTION, isTarget = false))
            add(FocusTestFixtures.trial(13, TrialOutcome.CORRECT_REJECTION, isTarget = false))
            add(FocusTestFixtures.trial(14, TrialOutcome.CORRECT_REJECTION, isTarget = false))
            add(FocusTestFixtures.trial(15, TrialOutcome.FALSE_ALARM, rtMs = 280, isTarget = false))
        }
        val metrics = engine.blockMetrics(trials, ExerciseCatalog.E1_VIGIA, IndividualThresholds())

        // correct = 5 hits + 3 correct rejections = 8; valid trials = 11.
        assertEquals(8.0 / 11.0, metrics.valueOf(MetricKey.ACCURACY), absoluteTolerance = 1e-6)
        assertEquals(340.0, metrics.valueOf(MetricKey.RT_MEDIAN), absoluteTolerance = 0.5)
        // SD([300..380]) = sqrt(1000) ≈ 31.62; CV = 31.62 / 340 ≈ 0.093.
        assertEquals(0.093, metrics.valueOf(MetricKey.RT_CV), absoluteTolerance = 0.005)
        assertEquals(0.0, metrics.valueOf(MetricKey.LAPSES))
        assertEquals(1.0, metrics.valueOf(MetricKey.FALSE_ALARMS))
        assertEquals(2.0, metrics.valueOf(MetricKey.OMISSIONS))
    }
}
