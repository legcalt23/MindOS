package com.mindos.app.features.mindboost.language.domain.evidence

import com.mindos.app.features.mindboost.language.domain.model.AttemptId
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceGate
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import kotlinx.serialization.Serializable

/**
 * Advanced evidence promotion policy (v6 §5.2 ladder, §5.5 gates, §10 transfer). Turns one scored
 * evidence event into an [EvidencePacket] tagged with the *highest evidence state it honestly
 * supports* — or nothing when the event carries no promotable evidence.
 *
 * The honesty contract (the whole point of the module):
 *  - a confounded event (no strong update allowed) NEVER promotes — it yields no packet;
 *  - near transfer (E2) requires a demonstrated verdict on unseen material (bank separation);
 *  - functional transfer (E3) requires an evidence-eligible evaluator output on a Transfer Lab task;
 *  - maintenance (E4) requires a delayed retest once the trait already reached E3.
 *
 * The [com.mindos.app.features.mindboost.language.domain.projection.ProfileProjector] then folds the
 * packet stream — only weight-bearing packets move the profile — so promotion stays event-sourced.
 */
fun interface EvidenceEngine {
    fun evaluate(input: EvidenceInput): EvidenceDecision
}

/** One scored evidence event plus the promotion signals needed to place it on the ladder. */
@Serializable
data class EvidenceInput(
    val packetId: String,
    val userIdHash: UserIdHash,
    val sourceAttemptId: AttemptId,
    val trait: TraitCode,
    val bankType: BankType,
    val operation: OperationType,
    val itemInformation: Double,
    val transferDistance: Double,
    val createdAtEpochMs: Long,
    val isStrongUpdateAllowed: Boolean,
    val evidenceWeight: Double,
    val gatesApplied: Set<EvidenceGate> = emptySet(),
    val priorState: EvidenceState = EvidenceState.LOWEST,
    val nearTransferDemonstrated: Boolean = false,
    val evaluatorEvidenceEligible: Boolean = false,
    val isDelayedRetest: Boolean = false,
)

/** The promotion outcome: a [packet] (null when no evidence is promotable) and a claim-safe reason. */
@Serializable
data class EvidenceDecision(
    val packet: EvidencePacket?,
    val supportsState: EvidenceState?,
    val reason: String,
)
