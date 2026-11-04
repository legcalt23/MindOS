package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The evaluation service is the honest gate (06_ §5–§6): without trusted reliability it must NEVER report
 * a reliable change — only a preliminary raw delta. Once reliability and enough administrations exist,
 * a large change does fire RCI, proving the pipeline is wired end-to-end (not dead code).
 */
class FocusEvaluationServiceTest {

    private val baseline = BaselineSnapshot(
        userId = "u1",
        perMetric = mapOf(MetricKey.RT_MEDIAN to 400.0),
        capturedAtMs = 1_000L,
    )

    @Test
    fun withoutReliability_changeIsPreliminary_neverReliable() {
        val service = FocusEvaluationService() // default estimator → EMPTY source, always gated.
        val retest = FocusTestFixtures.metricSet(MetricKey.RT_MEDIAN to 300.0)

        val result = service.evaluate(
            baseline,
            retest,
            priorAdministrations = 99,
            capturedAtMs = 2_000L,
            repeatsEarlierForm = false,
        )

        val change = result.changes.single { it.metric == MetricKey.RT_MEDIAN }
        assertEquals(RciResult.NotEnoughData, change.rci)
        assertEquals(-100.0, change.rawDelta, "Raw delta is still shown, just not as a reliable change.")
        assertFalse(result.hasReliableChange)
        val report = service.transferReport(result)
        assertFalse(report.hasStrongEvidence)
        assertTrue(report.preliminary.any { it.metric == MetricKey.RT_MEDIAN })
    }

    @Test
    fun belowMinAdministrations_staysGated_evenWithReliability() {
        val service = FocusEvaluationService(
            ProbeReliabilityEstimator(
                source = { mapOf(MetricKey.RT_MEDIAN to ReliabilityEstimate(MetricKey.RT_MEDIAN, 0.8, 50.0)) },
            ),
        )
        val retest = FocusTestFixtures.metricSet(MetricKey.RT_MEDIAN to 300.0)

        // Only 2 prior administrations (< MIN_ADMINISTRATIONS) → reliability not trusted yet.
        val result = service.evaluate(
            baseline,
            retest,
            priorAdministrations = 2,
            capturedAtMs = 2_000L,
            repeatsEarlierForm = false,
        )

        assertEquals(RciResult.NotEnoughData, result.changes.single().rci)
        assertFalse(result.hasReliableChange)
    }

    @Test
    fun withReliabilityAndEnoughAdministrations_largeChangeIsReliable() {
        val service = FocusEvaluationService(
            ProbeReliabilityEstimator(
                source = { mapOf(MetricKey.RT_MEDIAN to ReliabilityEstimate(MetricKey.RT_MEDIAN, 0.8, 50.0)) },
            ),
        )
        // pre=400, post=300, sdPre=50, r=0.8 → |RCI|≈3.16 > 1.96 → reliable.
        val retest = FocusTestFixtures.metricSet(MetricKey.RT_MEDIAN to 300.0)

        val result = service.evaluate(
            baseline,
            retest,
            priorAdministrations = ProbeReliabilityEstimator.MIN_ADMINISTRATIONS,
            capturedAtMs = 2_000L,
            repeatsEarlierForm = false,
        )

        val rci = result.changes.single().rci
        assertTrue(rci is RciResult.Value && rci.reliable, "Large change with reliability must be reliable.")
        assertTrue(result.hasReliableChange)
        assertTrue(service.transferReport(result).hasStrongEvidence)
    }

    /**
     * MB-FOCUS-0122 (R13). The SAME retest that is reliable on fresh material above is not evidence of change
     * when it re-serves a form the user has already been given: memory of the sequence is confounded with
     * change, so the reliability estimate does not apply and the RCI stays gated. And the fact is tri-state:
     * a row whose form identity is not known (`null`) is withheld too — unknown is never assumed fresh.
     */
    @Test
    fun aRepeatedOrUnidentifiedForm_isNeverReliable_evenWithReliabilityAndEnoughAdministrations() {
        val service = FocusEvaluationService(
            ProbeReliabilityEstimator(
                source = { mapOf(MetricKey.RT_MEDIAN to ReliabilityEstimate(MetricKey.RT_MEDIAN, 0.8, 50.0)) },
            ),
        )
        val retest = FocusTestFixtures.metricSet(MetricKey.RT_MEDIAN to 300.0)
        fun evaluate(repeatsEarlierForm: Boolean?) = service.evaluate(
            baseline,
            retest,
            priorAdministrations = ProbeReliabilityEstimator.MIN_ADMINISTRATIONS + 5,
            capturedAtMs = 2_000L,
            repeatsEarlierForm = repeatsEarlierForm,
        )

        // Anti-vacuity: with fresh material this very comparison IS reliable, so the two denials below are
        // attributable to the form and to nothing else.
        assertTrue(evaluate(repeatsEarlierForm = false).hasReliableChange, "fresh material must still be reliable")

        for (fact in listOf(true, null)) {
            val result = evaluate(repeatsEarlierForm = fact)
            assertEquals(RciResult.NotEnoughData, result.changes.single().rci, "repeatsEarlierForm=$fact")
            assertFalse(result.hasReliableChange, "repeatsEarlierForm=$fact must never carry a reliable change")
            assertEquals(-100.0, result.changes.single().rawDelta, "the raw delta is still shown, just not as reliable")
            assertFalse(service.transferReport(result).hasStrongEvidence)
        }
    }
}
