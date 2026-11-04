package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReliableChangeIndexTest {

    @Test
    fun rci_matchesWorkedExample_andFlagsReliableChange() {
        // sdPre=40, reliability=0.8 → SEM=40*sqrt(0.2)=17.889, SEdiff=sqrt(2)*SEM=25.298.
        // RCI=(250-300)/25.298 = -1.976 → |RCI|>1.96 → reliable.
        val estimate = ReliabilityEstimate(MetricKey.RT_MEDIAN, reliability = 0.8, sdPre = 40.0)
        val result = ReliableChangeIndex.compute(pre = 300.0, post = 250.0, reliability = estimate)
        val value = assertIs<RciResult.Value>(result)
        assertEquals(-1.976, value.rci, absoluteTolerance = 0.01)
        assertTrue(value.reliable)
    }

    @Test
    fun rci_isGated_whenReliabilityIsZero() {
        val estimate = ReliabilityEstimate(MetricKey.RT_MEDIAN, reliability = 0.0, sdPre = 40.0)
        assertIs<RciResult.NotEnoughData>(
            ReliableChangeIndex.compute(pre = 300.0, post = 250.0, reliability = estimate),
        )
    }

    @Test
    fun rci_isGated_whenSdPreIsZero() {
        val estimate = ReliabilityEstimate(MetricKey.RT_MEDIAN, reliability = 0.8, sdPre = 0.0)
        assertIs<RciResult.NotEnoughData>(
            ReliableChangeIndex.compute(pre = 300.0, post = 250.0, reliability = estimate),
        )
    }

    @Test
    fun smallChange_isNotReliable() {
        val estimate = ReliabilityEstimate(MetricKey.RT_MEDIAN, reliability = 0.8, sdPre = 40.0)
        val value = assertIs<RciResult.Value>(
            ReliableChangeIndex.compute(pre = 300.0, post = 295.0, reliability = estimate),
        )
        assertTrue(!value.reliable)
    }

    @Test
    fun comparator_gatesMetricsWithoutReliability() {
        val baseline = BaselineSnapshot(
            userId = "user",
            perMetric = mapOf(MetricKey.ACCURACY to 0.80, MetricKey.RT_MEDIAN to 320.0),
            capturedAtMs = 0,
        )
        // `measurementSufficient = true` is what the engine itself stamps at n = 50 (central floor 20), so the
        // fixture states a fact it was previously leaving out — it is not weakened to keep passing. What this
        // test discriminates is untouched: reliability present vs absent, with everything else held equal.
        val retest = mapOf(
            MetricKey.ACCURACY to MetricValue(
                MetricKey.ACCURACY, 0.88, 50, true, MetricUnit.PROPORTION, measurementSufficient = true,
            ),
            MetricKey.RT_MEDIAN to MetricValue(
                MetricKey.RT_MEDIAN, 250.0, 50, true, MetricUnit.MILLISECONDS, measurementSufficient = true,
            ),
        )
        // Reliability only known for RT_MEDIAN; ACCURACY must be gated.
        val reliabilities = mapOf(
            MetricKey.RT_MEDIAN to ReliabilityEstimate(MetricKey.RT_MEDIAN, 0.8, 40.0),
        )
        val result = EvaluationComparator.compare("user", baseline, retest, reliabilities, capturedAtMs = 10)

        val accuracyChange = result.changes.single { it.metric == MetricKey.ACCURACY }
        assertIs<RciResult.NotEnoughData>(accuracyChange.rci)

        val rtChange = result.changes.single { it.metric == MetricKey.RT_MEDIAN }
        assertIs<RciResult.Value>(rtChange.rci)
        assertTrue(result.hasReliableChange)
    }
}
