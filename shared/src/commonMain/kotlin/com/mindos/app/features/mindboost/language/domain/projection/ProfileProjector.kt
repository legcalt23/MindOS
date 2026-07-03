package com.mindos.app.features.mindboost.language.domain.projection

import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.TraitEstimate
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile

/**
 * Event-sourcing projector (v6 §12.3): rebuilds a [UserLanguageProfile] from the immutable
 * [EvidencePacket] stream. Pure and deterministic — the same packets always yield the same
 * profile, so the profile is never stored as mutable truth and deleting packets fully removes
 * derived ability (§13 granular deletion).
 *
 * Promotion is evidence-gated: only packets with a positive [EvidencePacket.evidenceWeight]
 * contribute to the evidence state and reliability. A high-state packet that was gated to weight
 * 0 (fatigue, repeated item, confounder…) can NOT promote a trait — the honesty contract.
 *
 * θ estimation (mean/SD via IRT) is deliberately a placeholder here (thetaMean = 0): real
 * calibration is a backend/Phase-3 concern. What this projector establishes from local evidence
 * is the **evidence state** and a monotone **reliability** — exactly what claims and adaptation
 * need offline.
 */
object ProfileProjector {

    fun project(
        userIdHash: UserIdHash,
        packets: List<EvidencePacket>,
        nowEpochMs: Long,
    ): UserLanguageProfile {
        val estimates = packets.groupBy { it.trait }
            .mapValues { (trait, traitPackets) -> estimateFor(trait, traitPackets, nowEpochMs) }
        return UserLanguageProfile(userIdHash, estimates, nowEpochMs)
    }

    private fun estimateFor(
        trait: TraitCode,
        packets: List<EvidencePacket>,
        nowEpochMs: Long,
    ): TraitEstimate {
        val gated = packets.filter { it.evidenceWeight > 0.0 }
        val totalWeight = gated.sumOf { it.evidenceWeight }
        val reliability = (totalWeight / RELIABILITY_SATURATION).coerceIn(0.0, 1.0)
        val state = gated.maxByOrNull { it.supportsState.rank }?.supportsState ?: EvidenceState.LOWEST
        val practiceEffectRisk = if (packets.isEmpty()) {
            0.0
        } else {
            packets.count { it.bankType == BankType.TRAINING }.toDouble() / packets.size
        }
        return TraitEstimate(
            trait = trait,
            thetaMean = 0.0,
            thetaSd = 1.0 - reliability,
            reliability = reliability,
            evidenceState = state,
            practiceEffectRisk = practiceEffectRisk,
            confounderRisk = 0.0,
            fairnessRisk = 0.0,
            estimatorVersion = ESTIMATOR_VERSION,
            updatedAtEpochMs = nowEpochMs,
        )
    }

    const val ESTIMATOR_VERSION = ScoringVersionRegistry.LANGUAGE_PROJECTION_ESTIMATOR
    const val RELIABILITY_SATURATION = 10.0
}
