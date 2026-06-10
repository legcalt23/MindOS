package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultMetricsEngineTest {

    private val engine = DefaultMetricsEngine()
    private val e1 = ExerciseCatalog.E1_VIGIA
    private val thresholds = IndividualThresholds()

    @Test
    fun accuracy_rtMedian_lapses_andErrorTallies_areCorrect() {
        val trials = buildList {
            addAll(FocusTestFixtures.hits(listOf(250, 260, 255, 245, 250, 265, 255, 260)))
            add(FocusTestFixtures.trial(8, TrialOutcome.MISS))
            add(FocusTestFixtures.trial(9, TrialOutcome.MISS))
            add(FocusTestFixtures.trial(10, TrialOutcome.CORRECT_REJECTION, isTarget = false))
            add(FocusTestFixtures.trial(11, TrialOutcome.CORRECT_REJECTION, isTarget = false))
            add(FocusTestFixtures.trial(12, TrialOutcome.FALSE_ALARM, rtMs = 300, isTarget = false))
        }

        val metrics = engine.blockMetrics(trials, e1, thresholds)

        // correct = 8 hits + 2 correct rejections = 10; valid trials = 13.
        assertEquals(10.0 / 13.0, metrics.valueOf(MetricKey.ACCURACY), absoluteTolerance = 1e-6)
        // Median of [245,250,250,255,255,260,260,265] = 255.
        assertEquals(255.0, metrics.valueOf(MetricKey.RT_MEDIAN), absoluteTolerance = 0.5)
        assertEquals(0.0, metrics.valueOf(MetricKey.LAPSES))
        assertEquals(1.0, metrics.valueOf(MetricKey.FALSE_ALARMS))
        assertEquals(2.0, metrics.valueOf(MetricKey.OMISSIONS))
        // RT-CV is small for tightly clustered RTs.
        assertTrue(metrics.valueOf(MetricKey.RT_CV) < 0.05)
    }

    @Test
    fun rtMedian_rejectsOutlierViaMad() {
        val trials = FocusTestFixtures.hits(listOf(300, 310, 305, 295, 302, 9000))
        val metrics = engine.blockMetrics(trials, e1, thresholds)
        // The 9000ms outlier is dropped; median of [295,300,302,305,310] = 302, n = 5 (confident).
        assertEquals(302.0, metrics.valueOf(MetricKey.RT_MEDIAN), absoluteTolerance = 0.5)
        assertEquals(5, metrics[MetricKey.RT_MEDIAN]?.n)
    }

    @Test
    fun lapses_useIndividualThreshold() {
        val trials = FocusTestFixtures.hits(listOf(300, 320, 600, 700, 310, 330))
        val metrics = engine.blockMetrics(trials, e1, IndividualThresholds(lapseRtMs = 500))
        assertEquals(2.0, metrics.valueOf(MetricKey.LAPSES))
    }

    @Test
    fun emptyBlock_marksMetricsUnconfident() {
        val metrics = engine.blockMetrics(emptyList(), e1, thresholds)
        assertFalse(metrics[MetricKey.ACCURACY]?.confident ?: true)
        assertFalse(metrics[MetricKey.RT_MEDIAN]?.confident ?: true)
    }

    @Test
    fun iesUnavailable_whenAccuracyZero() {
        val trials = listOf(
            FocusTestFixtures.trial(0, TrialOutcome.MISS),
            FocusTestFixtures.trial(1, TrialOutcome.MISS),
            FocusTestFixtures.trial(2, TrialOutcome.FALSE_ALARM, rtMs = 280, isTarget = false),
        )
        val metrics = engine.blockMetrics(trials, e1, thresholds)
        assertFalse(metrics[MetricKey.IES]?.confident ?: true)
    }
}
