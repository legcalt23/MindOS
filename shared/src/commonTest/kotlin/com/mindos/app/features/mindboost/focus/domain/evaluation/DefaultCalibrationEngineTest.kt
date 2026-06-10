package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultCalibrationEngineTest {

    private val engine = DefaultCalibrationEngine()
    private val e1 = ExerciseCatalog.E1_VIGIA

    @Test
    fun calibrate_personalizesLapseThreshold_forSlowSpreadResponder() {
        val rts = listOf(540L, 600L, 660L, 700L, 580L, 620L, 660L, 720L, 560L, 640L)
        val thresholds = engine.calibrate(FocusTestFixtures.hits(rts))
        // A slow/spread responder gets a lapse cutoff personalized above the default 500ms.
        assertTrue(thresholds.lapseRtMs > 500, "lapse should personalize upward, was ${thresholds.lapseRtMs}")
        assertNotNull(thresholds.baselineRtCv)
        assertTrue(thresholds.baselineRtCv!! > 0.0)
    }

    @Test
    fun calibrate_fallsBackToDefaults_withTooFewSamples() {
        val thresholds = engine.calibrate(FocusTestFixtures.hits(listOf(300L, 310L)))
        assertEquals(500L, thresholds.lapseRtMs)
        assertNull(thresholds.baselineRtCv)
    }

    @Test
    fun initialDifficulty_isHarder_forHighAccuracy() {
        val samples = FocusTestFixtures.hits(List(20) { 300L }) // all hits → accuracy 1.0
        val difficulty = engine.initialDifficulty(e1, samples)
        assertTrue(difficulty.level(DifficultyAxis.PERCEPTUAL_LOAD).value >= 0.5)
    }

    @Test
    fun initialDifficulty_isEasiest_forLowAccuracy() {
        val samples = List(20) { FocusTestFixtures.trial(it, TrialOutcome.MISS) } // accuracy 0.0
        val difficulty = engine.initialDifficulty(e1, samples)
        assertEquals(0.0, difficulty.level(DifficultyAxis.PERCEPTUAL_LOAD).value)
    }

    @Test
    fun baselineSnapshot_keepsOnlyConfidentMetrics() {
        val metrics = mapOf(
            MetricKey.ACCURACY to com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue(
                MetricKey.ACCURACY, 0.82, n = 50, confident = true,
                unit = com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit.PROPORTION,
            ),
            MetricKey.RT_MEDIAN to com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue.unavailable(
                MetricKey.RT_MEDIAN,
                com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit.MILLISECONDS,
            ),
        )
        val snapshot = engine.baselineSnapshot(
            userId = "user",
            metrics = metrics,
            capturedAtMs = 1_000,
            state = null,
            deviceFingerprint = "dev",
        )
        assertTrue(snapshot.perMetric.containsKey(MetricKey.ACCURACY))
        assertTrue(!snapshot.perMetric.containsKey(MetricKey.RT_MEDIAN))
    }
}
