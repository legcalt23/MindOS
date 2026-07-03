package com.mindos.app.features.arena.evidence.model

import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Evidence Ledger State
 * Represents the signed-off state of a motor required for valid execution.
 */
@Serializable
data class EvidenceLedgerState(
    val motorId: String,
    val ledgerVersion: String,
    val isApprovedCurrent: Boolean,
    val expiryTimestampMs: Long,
    val approvedBySignoffReference: String
)
