package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.evaluation.EvaluationResult
import com.mindos.app.features.mindboost.focus.domain.evaluation.MetricChange
import com.mindos.app.features.mindboost.focus.domain.evaluation.RciResult
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransferReportBuilderTest {

    private fun evaluation(vararg changes: MetricChange) =
        EvaluationResult(userId = "user", capturedAtMs = 0, changes = changes.toList())

    @Test
    fun reliableProbeChange_isStrongEvidence() {
        val change = MetricChange(
            metric = MetricKey.RT_MEDIAN, pre = 320.0, post = 270.0,
            rci = RciResult.Value(rci = -2.1, reliable = true),
        )
        val report = TransferReportBuilder.build(evaluation(change))
        assertTrue(report.hasStrongEvidence)
        assertEquals(MetricKey.RT_MEDIAN, report.strongEvidence.single().metric)
    }

    @Test
    fun gatedChange_isPreliminaryOnly() {
        val change = MetricChange(
            metric = MetricKey.ACCURACY, pre = 0.80, post = 0.84, rci = RciResult.NotEnoughData,
        )
        val report = TransferReportBuilder.build(evaluation(change))
        assertFalse(report.hasStrongEvidence)
        assertEquals(1, report.preliminary.size)
    }

    @Test
    fun honestNote_neverClaimsFarTransfer() {
        val strong = TransferReportBuilder.build(
            evaluation(
                MetricChange(MetricKey.RT_CV, 0.30, 0.20, RciResult.Value(-2.5, reliable = true)),
            ),
        ).honestNote.lowercase()
        // Must explicitly disclaim intelligence / daily-life transfer.
        assertTrue("no demuestra" in strong)
        assertTrue("mejora general" in strong || "vida diaria" in strong)
    }

    @Test
    fun preliminaryNote_framesIntraExerciseAsSecondary() {
        val note = TransferReportBuilder.build(
            evaluation(MetricChange(MetricKey.ACCURACY, 0.8, 0.82, RciResult.NotEnoughData)),
        ).honestNote.lowercase()
        assertTrue("secundaria" in note || "no una prueba" in note)
    }
}
