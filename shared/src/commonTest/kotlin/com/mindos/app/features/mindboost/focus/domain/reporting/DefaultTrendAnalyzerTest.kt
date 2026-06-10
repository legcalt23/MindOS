package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultTrendAnalyzerTest {

    private val analyzer = DefaultTrendAnalyzer()

    /** Builds sessions newest-first from oldest→newest [values] for one metric. */
    private fun sessions(metric: MetricKey, unit: MetricUnit, vararg values: Double): List<SessionResult> =
        values.mapIndexed { i, v ->
            SessionResult(
                sessionId = SessionId("user:${100 + i}"),
                mode = Mode.TRAINING,
                exerciseFocus = ExerciseId.E1_VIGIA,
                blocks = emptyList(),
                startedAtMs = 0,
                endedAtMs = (100 + i).toLong(),
                sessionMetrics = mapOf(metric to MetricValue(metric, v, 50, true, unit)),
            )
        }.asReversed() // newest-first

    @Test
    fun risingAccuracy_isImproving() {
        val recent = sessions(MetricKey.ACCURACY, MetricUnit.PROPORTION, 0.70, 0.75, 0.80, 0.86)
        assertEquals(TrendDirection.IMPROVING, analyzer.analyze(MetricKey.ACCURACY, recent).direction)
    }

    @Test
    fun risingRtCv_isDeclining_becauseLowerIsBetter() {
        val recent = sessions(MetricKey.RT_CV, MetricUnit.RATIO, 0.20, 0.26, 0.31, 0.37)
        assertEquals(TrendDirection.DECLINING, analyzer.analyze(MetricKey.RT_CV, recent).direction)
    }

    @Test
    fun fallingRtCv_isImproving_becauseLowerIsBetter() {
        val recent = sessions(MetricKey.RT_CV, MetricUnit.RATIO, 0.40, 0.33, 0.27, 0.20)
        assertEquals(TrendDirection.IMPROVING, analyzer.analyze(MetricKey.RT_CV, recent).direction)
    }

    @Test
    fun flatSeries_isStable() {
        val recent = sessions(MetricKey.ACCURACY, MetricUnit.PROPORTION, 0.80, 0.80, 0.80, 0.80)
        assertEquals(TrendDirection.STABLE, analyzer.analyze(MetricKey.ACCURACY, recent).direction)
    }

    @Test
    fun tooFewPoints_isInsufficient() {
        val recent = sessions(MetricKey.ACCURACY, MetricUnit.PROPORTION, 0.80, 0.85)
        assertEquals(TrendDirection.INSUFFICIENT, analyzer.analyze(MetricKey.ACCURACY, recent).direction)
    }
}
