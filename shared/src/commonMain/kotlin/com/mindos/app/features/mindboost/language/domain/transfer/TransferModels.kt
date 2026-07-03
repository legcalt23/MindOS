// The default retention-review intervals (7/30 days, §6.7) ARE domain data of the mission
// contract, not incidental constants — MagicNumber is suppressed for the file, matching the
// convention used by the other MindBoost config/token objects.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.domain.transfer

import com.mindos.app.features.mindboost.language.domain.evaluator.ReviewTrigger
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlinx.serialization.Serializable

/**
 * Transfer Lab model (v6 §10). The moat is not exercises — it is *demonstrating* that a skill is
 * used on new material, an applied task, and after a delay. These types capture the transfer
 * ladder (T0–T6), the functional-transfer mission contract, and the inputs/outputs of the
 * near-transfer verifier. All pure `commonMain`.
 */

/**
 * The transfer ladder (v6 §10.2). Strict order T0 < … < T6; use [rank]/[isAtLeast] for gating.
 * Near transfer is [T2_NEAR] (parallel form, same construct); functional transfer is [T3_FUNCTIONAL].
 */
@Serializable
enum class TransferLevel {
    T0_REPETITION,
    T1_SURFACE_VARIATION,
    T2_NEAR,
    T3_FUNCTIONAL,
    T4_PERSONAL,
    T5_MAINTAINED,
    T6_MULTIDOMAIN,
    ;

    val rank: Int get() = ordinal

    fun isAtLeast(other: TransferLevel): Boolean = rank >= other.rank

    companion object {
        /** Classifies the transfer level of a piece of work from its source characteristics. */
        fun classify(
            bankType: BankType,
            isOwnMaterial: Boolean = false,
            isDelayed: Boolean = false,
            isMultiDomain: Boolean = false,
        ): TransferLevel = when {
            isMultiDomain -> T6_MULTIDOMAIN
            isDelayed || bankType == BankType.RETENTION -> T5_MAINTAINED
            isOwnMaterial -> T4_PERSONAL
            bankType == BankType.TRANSFER_LAB -> T3_FUNCTIONAL
            bankType == BankType.PARALLEL_FORM || bankType == BankType.EVALUATION -> T2_NEAR
            bankType == BankType.TRAINING -> T1_SURFACE_VARIATION
            else -> T0_REPETITION // calibration / repair / gold / red-team carry no transfer claim
        }
    }
}

/** Target user profiles for functional transfer missions (v6 §10.3). */
@Serializable
enum class TransferProfile {
    STUDENT,
    FOUNDER,
    LEADER,
    SALES,
    WRITER,
    BILINGUAL,
    CRITICAL_THINKER,
}

/**
 * Functional transfer mission contract (v6 §10.4 Transfer Mission Passport). This is the
 * "functional transfer contract": a goal-oriented applied task whose successful, evidence-eligible
 * completion supports an [EvidenceState.E3_FUNCTIONAL_TRANSFER] claim. The actual open-response
 * scoring is delegated to the Evaluator OS (separate slice) via [rubricId]; this type carries the
 * contract only.
 */
@Serializable
data class TransferMission(
    val missionId: String,
    val profile: TransferProfile,
    val goal: String,
    val inputType: String,
    val traits: List<TraitCode>,
    val rubricId: String,
    val caps: List<String> = emptyList(),
    val humanReviewTriggers: List<ReviewTrigger> = emptyList(),
    val eligibleState: EvidenceState = EvidenceState.E3_FUNCTIONAL_TRANSFER,
    val retentionReviewDays: List<Int> = listOf(7, 30),
    val allowPrivateMaterial: Boolean = false,
) {
    init {
        require(missionId.isNotBlank()) { "missionId cannot be blank." }
        require(traits.isNotEmpty()) { "A transfer mission must target at least one trait." }
    }
}

/**
 * One verified trial used as transfer evidence. [isStrongUpdateAllowed] comes from the scoring
 * gates — a confounded trial (repeated item, fatigue, variant bias…) can never count as transfer.
 */
@Serializable
data class TransferTrial(
    val bankType: BankType,
    val correctness: Proportion,
    val isStrongUpdateAllowed: Boolean,
    val isOwnMaterial: Boolean = false,
    val isDelayed: Boolean = false,
    val isMultiDomain: Boolean = false,
) {
    /** The transfer level this trial can support, before considering confounds. */
    val level: TransferLevel
        get() = TransferLevel.classify(bankType, isOwnMaterial, isDelayed, isMultiDomain)
}

/** Verdict of a transfer check: whether transfer was demonstrated, at which level, and why. */
@Serializable
data class TransferVerdict(
    val trait: TraitCode,
    val demonstrated: Boolean,
    val level: TransferLevel,
    val accuracy: Proportion?,
    val trialsConsidered: Int,
    val reasons: List<String>,
)
