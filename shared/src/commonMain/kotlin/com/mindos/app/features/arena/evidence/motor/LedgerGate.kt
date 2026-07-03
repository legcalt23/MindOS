package com.mindos.app.features.arena.evidence.motor

/**
 * Phase 06: Offline, deterministic ledger gate (evidence_ledger_executable_gate.md).
 *
 * Produces the `ledgerApprovedCurrent` signal the Bayesian rating engine consumes. A motor's ledger
 * is approved-current only when its status is approved, ALL required sign-offs are present, and it
 * has not expired. Anything less means the motor cannot affect Competitive Rating and falls to
 * Training/Casual only. Pure: no clock or I/O — the caller supplies [nowMs].
 */
class LedgerGate(
    private val config: LedgerGateConfig = LedgerGateConfig.DEFAULT,
) {

    /** True if the ledger has expired as of [nowMs] (expired => motor falls to Training only). */
    fun checkLedgerExpiry(ledger: MotorEvidenceLedger, nowMs: Long): Boolean =
        nowMs >= ledger.expiryTimestampMs

    /** True if every required sign-off role is present. */
    fun validateSignOffs(ledger: MotorEvidenceLedger): Boolean =
        ledger.signoffs.containsAll(config.requiredSignoffs)

    /** The ledgerApprovedCurrent signal: approved status AND all sign-offs AND not expired. */
    fun isApprovedCurrent(ledger: MotorEvidenceLedger, nowMs: Long): Boolean =
        ledger.statusApproved && validateSignOffs(ledger) && !checkLedgerExpiry(ledger, nowMs)
}
