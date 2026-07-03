package com.mindos.app.features.arena

import com.mindos.app.features.arena.evidence.motor.CompetitiveContext
import com.mindos.app.features.arena.evidence.motor.MotorLedgerRegistry
import com.mindos.app.features.arena.evidence.motor.MotorStatusGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 06 / P6.2: Motor registry (3 MVP ledgers) + status / eligibility gates. */
class MotorStatusGateTest {

    private val now = 1_000_000L
    private val futureExpiry = now + 10_000L
    private val registry = MotorLedgerRegistry.default(futureExpiry)
    private val gate = MotorStatusGate(registry)

    @Test
    fun registrySeedsThreeMvpMotors() {
        assertEquals(
            setOf(
                MotorLedgerRegistry.MOT_FOC_01,
                MotorLedgerRegistry.MOT_MEM_01,
                MotorLedgerRegistry.MOT_SPD_01,
            ),
            registry.motorIds(),
        )
    }

    @Test
    fun shadowCandidateMotorsAreRatingEligibleInShadow() {
        assertTrue(gate.isMotorEligible(MotorLedgerRegistry.MOT_FOC_01, now))
        assertTrue(gate.checkMotorStatus(MotorLedgerRegistry.MOT_FOC_01, CompetitiveContext.SHADOW, now))
        assertTrue(gate.isMotorEligible(MotorLedgerRegistry.MOT_MEM_01, now))
    }

    @Test
    fun mentalSpeedIsTrainingOnlyAndBlockedFromRating() {
        // SPD-01 has no rating-affecting eligible context -> not rating eligible, blocked in Shadow.
        assertTrue(!gate.isMotorEligible(MotorLedgerRegistry.MOT_SPD_01, now))
        assertTrue(!gate.checkMotorStatus(MotorLedgerRegistry.MOT_SPD_01, CompetitiveContext.SHADOW, now))
        // ...but it is always allowed in Training.
        assertTrue(gate.checkMotorStatus(MotorLedgerRegistry.MOT_SPD_01, CompetitiveContext.TRAINING, now))
    }

    @Test
    fun rankedAndTournamentAreNeverEligible() {
        assertTrue(!gate.checkMotorStatus(MotorLedgerRegistry.MOT_FOC_01, CompetitiveContext.RANKED, now))
        assertTrue(!gate.checkMotorStatus(MotorLedgerRegistry.MOT_FOC_01, CompetitiveContext.TOURNAMENT, now))
    }

    @Test
    fun expiredLedgerFallsToTrainingOnly() {
        val expiredGate = MotorStatusGate(MotorLedgerRegistry.default(now - 1L))
        assertTrue(!expiredGate.isMotorEligible(MotorLedgerRegistry.MOT_FOC_01, now))
        assertTrue(!expiredGate.checkMotorStatus(MotorLedgerRegistry.MOT_FOC_01, CompetitiveContext.SHADOW, now))
        assertTrue(expiredGate.checkMotorStatus(MotorLedgerRegistry.MOT_FOC_01, CompetitiveContext.TRAINING, now))
    }

    @Test
    fun unknownMotorIsNeverEligible() {
        assertTrue(!gate.isMotorEligible("MOT_UNKNOWN", now))
        assertTrue(!gate.checkMotorStatus("MOT_UNKNOWN", CompetitiveContext.TRAINING, now))
    }
}
