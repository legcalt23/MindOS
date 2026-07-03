package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.measurement.engine.MeasurementConfidenceBuilder
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 04 / P4.2: Measurement confidence builder + insufficient-confidence gate integration. */
class MeasurementConfidenceBuilderTest {

    private val builder = MeasurementConfidenceBuilder()

    @Test
    fun reliabilityCheckRespectsFloor() {
        assertTrue(builder.checkReliability(0.85))
        assertTrue(!builder.checkReliability(0.50))
    }

    @Test
    fun conditionalSemUsesOverrideThenDefault() {
        assertEquals(40.0, builder.computeConditionalSEM("MID", "MOT_FOC_01", mapOf("MOT_FOC_01:MID" to 40.0)))
        assertEquals(60.0, builder.computeConditionalSEM("HIGH", "MOT_FOC_01"))
    }

    @Test
    fun confidenceIntervalIs95Percent() {
        val ci = builder.computeCI(mu = 1500.0, sigma = 100.0)
        assertEquals(1304.0, ci.lowerBound, 1e-9)
        assertEquals(1696.0, ci.upperBound, 1e-9)
    }

    @Test
    fun goodInputsYieldSufficientDecision() {
        val decision = builder.buildMeasurementConfidence(
            sem = 30.0, reliability = 0.85, sampleSize = 50,
            ledgerApprovedCurrent = true, mu = 1500.0, sigma = 100.0,
        )
        assertEquals(ConfidenceStateEnum.SUFFICIENT, decision.state)
        assertEquals("SUFFICIENT", decision.reasonCode)
        assertTrue(decision.updateMultiplier in 0.5..1.0, "multiplier=${decision.updateMultiplier}")
        assertEquals(30.0, decision.sem.value)
    }

    @Test
    fun eachInsufficiencyConditionIsDetectedWithZeroMultiplier() {
        val cases = mapOf(
            "LEDGER_NOT_APPROVED_CURRENT" to builder.buildMeasurementConfidence(30.0, 0.85, 50, false, 1500.0, 100.0),
            "RELIABILITY_BELOW_MIN" to builder.buildMeasurementConfidence(30.0, 0.40, 50, true, 1500.0, 100.0),
            "SAMPLE_SIZE_BELOW_MIN" to builder.buildMeasurementConfidence(30.0, 0.85, 3, true, 1500.0, 100.0),
            "SEM_ABOVE_MAX" to builder.buildMeasurementConfidence(500.0, 0.85, 50, true, 1500.0, 100.0),
        )
        for ((reason, decision) in cases) {
            assertEquals(ConfidenceStateEnum.INSUFFICIENT, decision.state, "for $reason")
            assertEquals(reason, decision.reasonCode)
            assertEquals(0.0, decision.updateMultiplier)
        }
    }

    @Test
    fun insufficientDecisionIsRejectedByRatingEngineGate() {
        val decision = builder.buildMeasurementConfidence(500.0, 0.85, 50, true, 1500.0, 100.0)
        val engine = BayesianRatingEngine()
        val result = engine.update(
            ArenaTestFixtures.posterior(),
            ArenaTestFixtures.verifiedEvidence(measurementConfidence = decision.state),
        )
        val rejected = result as? RatingUpdateResult.Rejected
            ?: error("Expected Rejected, got $result")
        assertEquals(RatingUpdateRejectionReason.INSUFFICIENT_MEASUREMENT_CONFIDENCE, rejected.reason)
    }
}
