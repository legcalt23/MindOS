// Attempt is the flat per-response record (mirrors the §12.6 `language_attempts` row);
// its breadth is the data model, not a smell.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Captured user response for closed-form items. Sealed so scoring can exhaustively branch
 * without an `else`. Free-text responses are intentionally NOT carried as content here —
 * they are referenced by an opaque local ref ([FreeResponseRef]) to keep raw user text out
 * of the evidence/sync path unless privacy consent allows otherwise (v6 §9.7, §13).
 */
@Serializable
sealed interface ResponsePayload {
    /** Single chosen option id. */
    @Serializable
    data class MultipleChoice(val selectedOptionId: String) : ResponsePayload

    /** Chosen option plus the evidence spans the user selected to support it. */
    @Serializable
    data class EvidenceSelection(
        val selectedOptionId: String,
        val selectedEvidenceIds: List<String>,
    ) : ResponsePayload

    /** A user-provided ordering of item ids. */
    @Serializable
    data class Ordering(val orderedIds: List<String>) : ResponsePayload

    /** Opaque reference to a free response stored locally; never the raw text. */
    @Serializable
    data class FreeResponseRef(val localRef: String) : ResponsePayload
}

/**
 * Reasons an attempt must not strongly update the ability profile (v6 §5.6). These are
 * raised during scoring and consumed by the EvidenceEngine; their presence caps or zeroes
 * the evidence weight regardless of correctness.
 */
@Serializable
enum class AttemptFlag {
    REPEATED_ITEM,
    HIGH_PRIOR_KNOWLEDGE,
    LOW_EVALUATOR_CONFIDENCE,
    VARIANT_BIAS_RISK,
    HIGH_FATIGUE,
    INSUFFICIENT_EVIDENCE,
    POSSIBLE_AI_ASSISTANCE,
    LOW_QUALITY_ASR,
    UNSUPPORTED_INFERENCE,
    MISSING_REQUIRED_JUSTIFICATION,
}

/**
 * Rubric score for open production (v6 §9.3, simplified for the spine). Produced by the
 * optional Evaluator OS; the offline core can fill [confidence]/[evidenceEligible] from a
 * local stub. `ClosedFloatingPointRange` is deliberately avoided (not serializable) in
 * favor of flat numeric fields.
 */
@Serializable
data class RubricScore(
    val score0to100: Int,
    val dimensions: Map<String, Int> = emptyMap(),
    val capsApplied: List<String> = emptyList(),
    val criticalErrors: List<ErrorType> = emptyList(),
    val confidence: Double,
    val judgeAgreement: Double,
    val evidenceEligible: Boolean,
    val evaluatorVersion: String,
) {
    init {
        require(score0to100 in 0..MAX_SCORE) { "Rubric score must be 0..$MAX_SCORE." }
        require(confidence in 0.0..1.0) { "Rubric confidence must be 0.0..1.0." }
        require(judgeAgreement in 0.0..1.0) { "Judge agreement must be 0.0..1.0." }
    }

    companion object {
        const val MAX_SCORE = 100
    }
}

/**
 * A single attempt: one response to one task, with the captured + scored fields folded
 * into one record (mirrors the §12.6 `language_attempts` table — the spec's own model).
 * Scored fields are nullable where "not computed / not computable" (never 0 silently):
 * [correctness] null means unscored, not wrong.
 */
@Serializable
data class Attempt(
    val attemptId: AttemptId,
    val sessionId: LanguageSessionId,
    val taskId: TaskId,
    val taskVersion: TaskVersion,
    val bankType: BankType,
    val primaryTrait: TraitCode,
    val operation: OperationType,
    val responseMode: ResponseMode,
    val response: ResponsePayload,
    val correctness: Proportion? = null,
    val latencyMs: LatencyMs? = null,
    val confidence: Proportion? = null,
    val errorTypes: List<ErrorType> = emptyList(),
    val flags: Set<AttemptFlag> = emptySet(),
    val rubricScore: RubricScore? = null,
    val evidenceWeight: Double = 0.0,
    val createdAtEpochMs: Long,
) {
    init {
        require(evidenceWeight >= 0.0) { "evidenceWeight cannot be negative (was $evidenceWeight)." }
    }

    /** True when this attempt carries any reason that forbids a strong profile update (§5.6). */
    val isProfileUpdateBlocked: Boolean get() = flags.isNotEmpty()
}
