package com.mindos.app.features.arena

import com.mindos.app.features.arena.evidence.motor.CompetitiveContext
import com.mindos.app.features.arena.evidence.motor.LedgerGate
import com.mindos.app.features.arena.evidence.motor.LedgerSignoffRole
import com.mindos.app.features.arena.evidence.motor.MotorEvidenceLedger
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 06 / P6.1: Ledger gate — approved-current from status + all sign-offs + non-expiry. */
class LedgerGateTest {

    private val gate = LedgerGate()
    private val now = 1_000_000L

    private fun ledger(
        statusApproved: Boolean = true,
        signoffs: Set<LedgerSignoffRole> = LedgerSignoffRole.entries.toSet(),
        expiryTimestampMs: Long = now + 1_000L,
    ) = MotorEvidenceLedger(
        motorId = "MOT_FOC_01",
        ledgerVersion = "v1",
        statusApproved = statusApproved,
        eligibleContexts = setOf(CompetitiveContext.TRAINING, CompetitiveContext.SHADOW),
        signoffs = signoffs,
        expiryTimestampMs = expiryTimestampMs,
        approvedBySignoffReference = "SIGNOFF_REF_1",
    )

    @Test
    fun fullyApprovedNonExpiredLedgerIsApprovedCurrent() {
        assertTrue(gate.isApprovedCurrent(ledger(), now))
    }

    @Test
    fun missingAnySignOffBlocksApproval() {
        val incomplete = LedgerSignoffRole.entries.toSet() - LedgerSignoffRole.PRIVACY
        assertTrue(!gate.validateSignOffs(ledger(signoffs = incomplete)))
        assertTrue(!gate.isApprovedCurrent(ledger(signoffs = incomplete), now))
    }

    @Test
    fun expiredLedgerIsNotApprovedCurrent() {
        val expired = ledger(expiryTimestampMs = now - 1L)
        assertTrue(gate.checkLedgerExpiry(expired, now))
        assertTrue(!gate.isApprovedCurrent(expired, now))
    }

    @Test
    fun unapprovedStatusIsNotApprovedCurrent() {
        assertTrue(!gate.isApprovedCurrent(ledger(statusApproved = false), now))
    }

    @Test
    fun blankMotorIdIsRejected() {
        assertFailsWith<IllegalArgumentException> { ledger().copy(motorId = " ") }
    }
}
