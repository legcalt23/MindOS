// Posterior/estimate records carry the full §5.4 / §12.6 field set; wide constructors are
// the data model.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Multiplicative evidence gates (v6 §5.5). Each gate can only *reduce* the weight an
 * attempt contributes to the profile — none can amplify a weak signal. The EvidenceEngine
 * (later slice) applies them; this enum is the closed set they draw from.
 */
@Serializable
enum class EvidenceGate {
    BASE_ITEM_INFORMATION,
    NOVELTY,
    BANK_SEPARATION,
    RELIABILITY,
    CONFOUNDER,
    FATIGUE,
    FAIRNESS,
    EVALUATOR_CONFIDENCE,
    TRANSFER_DISTANCE,
    PRIVACY_PERMISSION,
}

/**
 * An append-only unit of evidence (v6 §12.3 event sourcing). The profile is a *projection*
 * of packets, never a mutated-in-place value — so the full ability history can be replayed
 * and audited. One packet records what a scored attempt contributed to one trait, under
 * which gates, toward which evidence state.
 */
@Serializable
data class EvidencePacket(
    val packetId: String,
    val userIdHash: UserIdHash,
    val sourceAttemptId: AttemptId,
    val trait: TraitCode,
    val supportsState: EvidenceState,
    val bankType: BankType,
    val operation: OperationType,
    val itemInformation: Double,
    val evidenceWeight: Double,
    val gatesApplied: Set<EvidenceGate>,
    val transferDistance: Double,
    val createdAtEpochMs: Long,
) {
    init {
        require(itemInformation >= 0.0) { "itemInformation cannot be negative." }
        require(evidenceWeight >= 0.0) { "evidenceWeight cannot be negative." }
    }
}

/**
 * Per-trait estimate snapshot (mirrors §12.6 `language_trait_estimates`). The lightweight
 * row used for fast reads; the full posterior lives in [AbilityPosterior].
 */
@Serializable
data class TraitEstimate(
    val trait: TraitCode,
    val thetaMean: Double,
    val thetaSd: Double,
    val reliability: Double,
    val evidenceState: EvidenceState,
    val practiceEffectRisk: Double,
    val confounderRisk: Double,
    val fairnessRisk: Double,
    val estimatorVersion: String,
    val updatedAtEpochMs: Long,
) {
    init {
        require(thetaSd >= 0.0) { "thetaSd cannot be negative (was $thetaSd)." }
        require(reliability in 0.0..1.0) { "reliability must be in 0.0..1.0 (was $reliability)." }
    }
}

/**
 * Full Bayesian-flavored ability posterior (v6 §5.4). Credible intervals are stored as flat
 * `low/high` doubles (ranges aren't serializable). All risk/evidence accumulators are
 * carried so the ClaimEngine and UX can show uncertainty honestly.
 */
@Serializable
data class AbilityPosterior(
    val trait: TraitCode,
    val thetaMean: Double,
    val thetaSd: Double,
    val credibleInterval90Low: Double,
    val credibleInterval90High: Double,
    val reliability: Double,
    val evidenceState: EvidenceState,
    val nearTransferEvidence: Double,
    val functionalTransferEvidence: Double,
    val maintenanceEvidence: Double,
    val practiceEffectRisk: Double,
    val confounderRisk: Double,
    val fairnessRisk: Double,
    val estimatorVersion: String,
    val lastUpdatedAtEpochMs: Long,
) {
    init {
        require(thetaSd >= 0.0) { "thetaSd cannot be negative (was $thetaSd)." }
        require(credibleInterval90Low <= credibleInterval90High) {
            "Credible interval low ($credibleInterval90Low) must be <= high ($credibleInterval90High)."
        }
        require(reliability in 0.0..1.0) { "reliability must be in 0.0..1.0 (was $reliability)." }
    }
}

/**
 * Aggregate language profile for one user: the per-trait projection plus the timestamp of
 * the last evidence applied. Built by the EvidenceEngine from the packet stream; consumed
 * by adaptation, claims and UX. Trait coverage is sparse (only measured traits appear).
 */
@Serializable
data class UserLanguageProfile(
    val userIdHash: UserIdHash,
    val traitEstimates: Map<TraitCode, TraitEstimate> = emptyMap(),
    val updatedAtEpochMs: Long,
) {
    /** Evidence state for [trait], or the safe [EvidenceState.LOWEST] if not yet measured. */
    fun evidenceStateOf(trait: TraitCode): EvidenceState =
        traitEstimates[trait]?.evidenceState ?: EvidenceState.LOWEST

    /** True when [trait] has any estimate beyond the calibrating floor. */
    fun hasMeasurableEvidence(trait: TraitCode): Boolean =
        traitEstimates[trait]?.evidenceState?.isAtLeast(EvidenceState.E1_TRAINED_IMPROVEMENT) == true
}
