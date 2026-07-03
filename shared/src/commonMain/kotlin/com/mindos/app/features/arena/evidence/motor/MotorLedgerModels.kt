@file:Suppress("MagicNumber") // The ~6-month default expiry period (ms) is a documented named default.

package com.mindos.app.features.arena.evidence.motor

/**
 * Phase 06: Competitive contexts a motor may be eligible for. RANKED / TOURNAMENT remain unreachable
 * in this offline scope (public ranked is forbidden until Phase 12 sign-off).
 */
enum class CompetitiveContext(val affectsRating: Boolean) {
    TRAINING(false),
    CASUAL(false),
    SHADOW(true),
    RANKED(true),
    TOURNAMENT(true),
}

/**
 * Phase 06: The eight required ledger sign-off roles (ledger_signoff_logic.md). A missing sign-off
 * means the ledger is not approved.
 */
enum class LedgerSignoffRole {
    PRODUCT,
    SCIENCE,
    DATA,
    ENGINEERING,
    INTEGRITY,
    FAIRNESS,
    PRIVACY,
    UX,
}

/**
 * Phase 06: Focused motor evidence ledger consumed by the gates. A pragmatic subset of the full
 * v1.1.1 schema (ledger_schema.md) — the fields the executable gates actually read. Synthetic only.
 */
data class MotorEvidenceLedger(
    val motorId: String,
    val ledgerVersion: String,
    val statusApproved: Boolean,
    val eligibleContexts: Set<CompetitiveContext>,
    val signoffs: Set<LedgerSignoffRole>,
    val expiryTimestampMs: Long,
    val approvedBySignoffReference: String,
) {
    init {
        require(motorId.isNotBlank()) { "motorId must not be blank" }
        require(ledgerVersion.isNotBlank()) { "ledgerVersion must not be blank" }
        require(expiryTimestampMs >= 0) { "expiryTimestampMs must be >= 0" }
    }
}

/**
 * Phase 06: Ledger gate configuration. By default ALL eight sign-off roles are required.
 */
data class LedgerGateConfig(
    val configVersion: String = "LEDGER_GATE_CONFIG_V1.0",
    val requiredSignoffs: Set<LedgerSignoffRole> = LedgerSignoffRole.entries.toSet(),
    /** ~6 months in ms; used when minting/revalidating ledgers. */
    val expiryPeriodMs: Long = 15_778_800_000L,
) {
    init {
        require(requiredSignoffs.isNotEmpty()) { "requiredSignoffs must not be empty" }
        require(expiryPeriodMs > 0) { "expiryPeriodMs must be > 0" }
    }

    companion object {
        val DEFAULT: LedgerGateConfig = LedgerGateConfig()
    }
}
