package com.mindos.app.features.arena

import com.mindos.app.features.arena.evidence.motor.BlockingCondition
import com.mindos.app.features.arena.evidence.motor.MotorEligibilityChecker
import com.mindos.app.features.arena.evidence.motor.MotorLedgerRegistry
import com.mindos.app.features.arena.evidence.motor.RevalidationEvaluator
import com.mindos.app.features.arena.evidence.motor.RevalidationSignals
import com.mindos.app.features.arena.evidence.motor.RevalidationTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 06 / P6.3: Eligibility aggregation, blocking conditions, and revalidation triggers. */
class MotorEligibilityCheckerTest {

    private val now = 1_000_000L
    private val checker = MotorEligibilityChecker(MotorLedgerRegistry.default(now + 10_000L))

    @Test
    fun shadowCandidateMotorHasNoBlockingConditions() {
        val result = checker.checkEligibility(MotorLedgerRegistry.MOT_FOC_01, now)
        assertTrue(result.eligible)
        assertTrue(result.approvedCurrent)
        assertTrue(result.blockingConditions.isEmpty())
    }

    @Test
    fun trainingOnlyMotorIsBlockedByNoRatingContext() {
        val result = checker.checkEligibility(MotorLedgerRegistry.MOT_SPD_01, now)
        assertTrue(!result.eligible)
        assertTrue(result.approvedCurrent)
        assertEquals(listOf(BlockingCondition.NO_RATING_CONTEXT), result.blockingConditions)
    }

    @Test
    fun expiredLedgerReportsExpiredBlockingCondition() {
        val expired = MotorEligibilityChecker(MotorLedgerRegistry.default(now - 1L))
        val result = expired.checkEligibility(MotorLedgerRegistry.MOT_FOC_01, now)
        assertTrue(!result.eligible)
        assertTrue(!result.approvedCurrent)
        assertTrue(result.blockingConditions.contains(BlockingCondition.EXPIRED))
    }

    @Test
    fun unknownMotorReportsUnknownBlockingCondition() {
        val result = checker.checkEligibility("MOT_UNKNOWN", now)
        assertEquals(listOf(BlockingCondition.UNKNOWN_MOTOR), result.blockingConditions)
        assertTrue(!result.eligible)
    }

    @Test
    fun revalidationFiresOnDriftAboveThresholdAndEachSignal() {
        val evaluator = RevalidationEvaluator()
        assertTrue(!evaluator.requiresRevalidation(RevalidationSignals()))
        assertTrue(!evaluator.requiresRevalidation(RevalidationSignals(seasonalMuDriftFraction = 0.04)))
        assertTrue(
            evaluator.triggered(RevalidationSignals(seasonalMuDriftFraction = 0.06))
                .contains(RevalidationTrigger.SEASONAL_MU_DRIFT),
        )
        val all = evaluator.triggered(
            RevalidationSignals(
                seasonalMuDriftFraction = 0.2,
                newDeviceClassIntroduced = true,
                difAnomalyDetected = true,
                itemPoolContaminated = true,
                solverBreakthrough = true,
            ),
        )
        assertEquals(RevalidationTrigger.entries.toSet(), all)
    }
}
