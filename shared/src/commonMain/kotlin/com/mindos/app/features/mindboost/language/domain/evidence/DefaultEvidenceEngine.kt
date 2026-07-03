package com.mindos.app.features.mindboost.language.domain.evidence

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState

/**
 * Default [EvidenceEngine]. Pure and offline. Resolves the supported state by the strict policy
 * above and emits a packet only when there is promotable, weight-bearing evidence.
 */
class DefaultEvidenceEngine : EvidenceEngine {

    override fun evaluate(input: EvidenceInput): EvidenceDecision {
        val state = if (input.isStrongUpdateAllowed) resolveState(input) else null
        if (state == null) {
            val reason = if (!input.isStrongUpdateAllowed) {
                "Evento confundido o sin actualización fuerte: no promociona (§5.6)."
            } else {
                "El banco/contexto no aporta evidencia promocionable."
            }
            return EvidenceDecision(null, null, reason)
        }
        val packet = EvidencePacket(
            packetId = input.packetId,
            userIdHash = input.userIdHash,
            sourceAttemptId = input.sourceAttemptId,
            trait = input.trait,
            supportsState = state,
            bankType = input.bankType,
            operation = input.operation,
            itemInformation = input.itemInformation,
            evidenceWeight = input.evidenceWeight,
            gatesApplied = input.gatesApplied,
            transferDistance = input.transferDistance,
            createdAtEpochMs = input.createdAtEpochMs,
        )
        return EvidenceDecision(packet, state, reasonFor(state))
    }

    private fun resolveState(input: EvidenceInput): EvidenceState? = when {
        input.isDelayedRetest && input.priorState.isAtLeast(EvidenceState.E3_FUNCTIONAL_TRANSFER) ->
            EvidenceState.E4_MAINTAINED
        input.bankType == BankType.TRANSFER_LAB && input.evaluatorEvidenceEligible ->
            EvidenceState.E3_FUNCTIONAL_TRANSFER
        input.nearTransferDemonstrated -> EvidenceState.E2_NEAR_TRANSFER
        input.bankType in E1_PROGRESS_BANKS -> EvidenceState.E1_TRAINED_IMPROVEMENT
        else -> null
    }

    private fun reasonFor(state: EvidenceState): String = when (state) {
        EvidenceState.E0_CALIBRATING -> "Calibrando."
        EvidenceState.E1_TRAINED_IMPROVEMENT -> "Mejora en familias practicadas (evidencia entrenada)."
        EvidenceState.E2_NEAR_TRANSFER -> "Transferencia cercana verificada en material nuevo."
        EvidenceState.E3_FUNCTIONAL_TRANSFER -> "Transferencia funcional certificada por el evaluador."
        EvidenceState.E4_MAINTAINED -> "Mejora mantenida tras retest diferido."
        EvidenceState.E5_ROBUST_GENERALIZATION -> "Generalización robusta."
    }

    companion object {
        // Banks whose clean attempts bear in-template (E1) improvement evidence. GOLD_SET / RED_TEAM
        // never compute progress (§8.3); PARALLEL_FORM / TRANSFER_LAB / RETENTION promote only via the
        // transfer/maintenance branches above.
        private val E1_PROGRESS_BANKS = setOf(
            BankType.TRAINING, BankType.REPAIR, BankType.CALIBRATION, BankType.EVALUATION,
        )
    }
}
