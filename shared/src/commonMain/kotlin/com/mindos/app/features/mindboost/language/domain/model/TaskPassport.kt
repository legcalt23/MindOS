// TaskPassport and its metadata blocks are declarative content descriptors; their wide
// constructors are the idiomatic shape of the data model, not a code smell.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Task Passport (v6 §7.2): the mandatory, declarative contract every content item must
 * carry before it can enter the system. It is *plain data on purpose* — it does NOT
 * self-validate in its constructor. A structurally-buildable-but-invalid passport is
 * exactly what the `ContentValidator` (next slice) must catch and report with actionable
 * errors. Strong invariants live in value objects ([TaskId], [TaskVersion]); semantic
 * completeness is the validator's job.
 *
 * Mirrors the YAML passport in §7.2 field-for-field so content authored in YAML maps 1:1.
 */
@Serializable
data class TaskPassport(
    val taskId: TaskId,
    val version: TaskVersion,
    val status: QaStatus,
    val moduleId: String,
    val primaryTrait: TraitCode,
    val secondaryTraits: List<TraitCode> = emptyList(),
    val operation: OperationType,
    val responseMode: ResponseMode,
    val dose: CognitiveDose,
    val stimulus: StimulusProfile,
    val scoring: ScoringSpec,
    val feedback: FeedbackSpec,
    val evidence: EvidenceTarget,
    val fairness: FairnessMetadata,
    val privacy: PrivacyMetadata,
    val qa: QaMetadata,
)

/** Stimulus description (v6 §7.2 `stimulus`). Confounder controls live here. */
@Serializable
data class StimulusProfile(
    val source: StimulusSource,
    val words: Int,
    val domain: String,
    val priorKnowledgeDependency: DependencyLevel,
    val culturalDependency: DependencyLevel,
)

/** Scoring contract (v6 §7.2 `scoring`). [caps] are critical score caps (§9.4). */
@Serializable
data class ScoringSpec(
    val scoringRule: ScoringRule,
    val caps: List<String> = emptyList(),
)

/** Feedback / repair contract (v6 §7.2 `feedback`). */
@Serializable
data class FeedbackSpec(
    val errorPatterns: List<ErrorType> = emptyList(),
    val repairPlanId: String? = null,
)

/**
 * Evidence target (v6 §7.2 `evidence`). Declares which states this item can contribute to,
 * which bank it belongs to, and — for transfer — the parallel-form group it shares.
 */
@Serializable
data class EvidenceTarget(
    val eligibleStates: List<EvidenceState>,
    val bankType: BankType,
    val parallelFormGroup: String? = null,
)

/** Fairness metadata (v6 §7.2 `fairness`, §8.5 localization). */
@Serializable
data class FairnessMetadata(
    val variantsValidated: List<String> = emptyList(),
    val difStatus: String,
)

/** Privacy metadata (v6 §7.2 `privacy`, §13). */
@Serializable
data class PrivacyMetadata(
    val privacyClass: PrivacyClass,
)

/** QA gating metadata (v6 §7.2 `qa`, §8.1 pipeline). */
@Serializable
data class QaMetadata(
    val expertReviewRequired: Boolean,
    val pilotRequired: Boolean,
)
