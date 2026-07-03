package com.mindos.app.features.arena.evidence.motor

/**
 * Phase 06: Offline motor status gate (motor_status_gate.md + evidence_ledger_executable_gate.md).
 *
 * - [isMotorEligible]: can the motor affect Competitive Rating at all? (approved-current AND eligible
 *   for at least one rating-affecting context).
 * - [checkMotorStatus]: is the motor allowed in a specific [CompetitiveContext]? Non-rating contexts
 *   (Training/Casual) are always allowed for a known motor — an unapproved/expired motor simply
 *   "falls to Training only". Pure: no clock — the caller supplies [nowMs].
 */
class MotorStatusGate(
    private val registry: MotorLedgerRegistry,
    private val ledgerGate: LedgerGate = LedgerGate(),
) {

    fun isMotorEligible(motorId: String, nowMs: Long): Boolean {
        val ledger = registry.ledgerFor(motorId) ?: return false
        val hasRatingContext = ledger.eligibleContexts.any { it.affectsRating }
        return hasRatingContext && ledgerGate.isApprovedCurrent(ledger, nowMs)
    }

    fun checkMotorStatus(motorId: String, context: CompetitiveContext, nowMs: Long): Boolean {
        val ledger = registry.ledgerFor(motorId) ?: return false
        return when {
            !context.affectsRating -> true
            else -> ledgerGate.isApprovedCurrent(ledger, nowMs) &&
                ledger.eligibleContexts.contains(context)
        }
    }
}
