package com.mindos.app.features.arena.evidence.motor

/**
 * Phase 06: Registry of the three MVP motor evidence ledgers, seeded with their documented v1.1.1
 * statuses (mot_*_evidence_ledger.md):
 * - MOT_FOC_01 (Focus / Interference Control): Offline Simulation + Shadow Candidate.
 * - MOT_MEM_01 (Working Memory / Sequence Updating): Training + Shadow Candidate.
 * - MOT_SPD_01 (Mental Speed / Choice Reaction): Training + Offline Simulation ONLY — blocked for
 *   live Shadow Ranked until device-class norms are validated.
 *
 * None are eligible for RANKED / TOURNAMENT — public ranked stays forbidden in this offline scope.
 */
class MotorLedgerRegistry(
    private val ledgers: Map<String, MotorEvidenceLedger>,
) {
    fun ledgerFor(motorId: String): MotorEvidenceLedger? = ledgers[motorId]

    fun motorIds(): Set<String> = ledgers.keys

    companion object {
        const val MOT_FOC_01 = "MOT_FOC_01"
        const val MOT_MEM_01 = "MOT_MEM_01"
        const val MOT_SPD_01 = "MOT_SPD_01"

        private val SHADOW_CANDIDATE = setOf(
            CompetitiveContext.TRAINING,
            CompetitiveContext.CASUAL,
            CompetitiveContext.SHADOW,
        )
        private val TRAINING_ONLY = setOf(
            CompetitiveContext.TRAINING,
            CompetitiveContext.CASUAL,
        )

        /** Builds the default registry; [expiryTimestampMs] is supplied so the registry stays pure. */
        fun default(expiryTimestampMs: Long): MotorLedgerRegistry = MotorLedgerRegistry(
            mapOf(
                MOT_FOC_01 to seed(MOT_FOC_01, SHADOW_CANDIDATE, expiryTimestampMs),
                MOT_MEM_01 to seed(MOT_MEM_01, SHADOW_CANDIDATE, expiryTimestampMs),
                MOT_SPD_01 to seed(MOT_SPD_01, TRAINING_ONLY, expiryTimestampMs),
            ),
        )

        private fun seed(
            motorId: String,
            contexts: Set<CompetitiveContext>,
            expiry: Long,
        ): MotorEvidenceLedger = MotorEvidenceLedger(
            motorId = motorId,
            ledgerVersion = "v1.1.1",
            statusApproved = true,
            eligibleContexts = contexts,
            signoffs = LedgerSignoffRole.entries.toSet(),
            expiryTimestampMs = expiry,
            approvedBySignoffReference = "MODULE_III_V1.1.1_$motorId",
        )
    }
}
