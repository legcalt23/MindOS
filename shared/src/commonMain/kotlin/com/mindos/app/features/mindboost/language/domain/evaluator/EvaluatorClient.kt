package com.mindos.app.features.mindboost.language.domain.evaluator

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.RetentionPolicy
import com.mindos.app.features.mindboost.language.domain.model.RubricScore
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlinx.serialization.Serializable

/**
 * Evaluator OS contracts (v6 §9). The evaluator scores open production "not because it knows" but
 * because it sits inside a system of rubrics, caps, uncertainty, bias sentinels, human review and
 * retention. This is an *optional* layer: the offline core never depends on it.
 *
 * PRIVACY/SECURITY (v6 §9.2 privacy filter, §10.5): the request carries only an opaque
 * [EvaluationRequest.responseRef] — never raw user text — and the [EvaluationRequest.cloudConsentGranted]
 * flag. Implementations that leave the device MUST refuse without that consent; the default
 * implementation ([LocalStubEvaluatorClient]) is offline and never transmits anything.
 */
interface EvaluatorClient {
    suspend fun evaluate(request: EvaluationRequest): EvaluationOutput
}

/** Where evaluation runs (v6 §9.7 degradation). */
@Serializable
enum class EvaluatorMode { LOCAL, HYBRID, REMOTE }

/** Human-review lifecycle for an evaluation (v6 §9.6). */
@Serializable
enum class HumanReviewStatus { NOT_REQUIRED, PENDING, IN_REVIEW, COMPLETED, ESCALATED }

/** Why a case was routed to human review (v6 §9.6 triggers). */
@Serializable
enum class ReviewTrigger {
    LOW_CONFIDENCE_HIGH_STATE,
    JUDGE_DISAGREEMENT,
    DIALECT_BIAS,
    USER_DISPUTE,
    SENSITIVE_MATERIAL,
    BENCHMARK_REGRESSION,
}

/** Bias sentinel verdict (v6 §9.2). [flagged] means the score must not be trusted as-is. */
@Serializable
data class BiasSentinelResult(
    val flagged: Boolean,
    val reasons: List<String> = emptyList(),
)

/** Evaluator uncertainty (v6 §9.3, §9.5). High [disagreementUnresolved] forces conservative use. */
@Serializable
data class EvaluatorUncertainty(
    val confidence: Double,
    val judgeAgreement: Double,
    val disagreementUnresolved: Boolean,
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be 0.0..1.0." }
        require(judgeAgreement in 0.0..1.0) { "judgeAgreement must be 0.0..1.0." }
    }
}

/**
 * A request to evaluate one open response. [responseRef] is an opaque local handle resolved by the
 * privacy layer — the raw text never enters this contract. [targetState] is the evidence state the
 * result would support (used to decide whether human review is required). [localHeuristicScore] is an
 * optional, locally-computed 0..100 score the offline stub may surface as *formative* feedback.
 */
@Serializable
data class EvaluationRequest(
    val taskId: TaskId,
    val rubricId: String,
    val trait: TraitCode,
    val operation: OperationType,
    val responseRef: String,
    val targetState: EvidenceState,
    val cloudConsentGranted: Boolean = false,
    val localHeuristicScore: Int? = null,
)

/**
 * The full evaluation envelope (v6 §9.3 RubricScoreV6 wrapped with OS metadata). [evidenceEligible]
 * is the single boolean the EvidenceEngine keys on; it is true only when the score is trustworthy,
 * unbiased, not pending review, and the mode can certify the [EvaluationRequest.targetState].
 */
@Serializable
data class EvaluationOutput(
    val mode: EvaluatorMode,
    val rubricVersion: String,
    val rubricScore: RubricScore,
    val uncertainty: EvaluatorUncertainty,
    val biasSentinel: BiasSentinelResult,
    val humanReviewStatus: HumanReviewStatus,
    val retention: RetentionPolicy,
    val evidenceEligible: Boolean,
)
