package com.mindos.app.features.mindboost.language.domain.evaluator

import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.RetentionPolicy
import com.mindos.app.features.mindboost.language.domain.model.RubricScore

/**
 * Offline, dev/test [EvaluatorClient]. **Never** transmits anything and **never** certifies
 * transfer evidence.
 *
 * This is the §9.7 degradation path made explicit: for open production with no real Evaluator OS
 * available, the system gives *formative* feedback only — it must not invent an authoritative score
 * or an E2+ claim. So [EvaluationOutput.evidenceEligible] is always false here, and any
 * [EvaluationRequest.targetState] at or above near transfer is routed to [HumanReviewStatus.PENDING].
 * Retention defaults to [RetentionPolicy.NONE] and only rises to abstraction-only when cloud consent
 * was explicitly granted.
 */
class LocalStubEvaluatorClient : EvaluatorClient {

    override suspend fun evaluate(request: EvaluationRequest): EvaluationOutput {
        val score = (request.localHeuristicScore ?: NEUTRAL_SCORE).coerceIn(0, RubricScore.MAX_SCORE)
        val rubricScore = RubricScore(
            score0to100 = score,
            confidence = LOW_CONFIDENCE,
            judgeAgreement = SINGLE_JUDGE_AGREEMENT,
            evidenceEligible = false, // offline open scoring can never support an E2+ claim
            evaluatorVersion = STUB_VERSION,
        )
        val needsReview = request.targetState.isAtLeast(EvidenceState.E2_NEAR_TRANSFER)
        return EvaluationOutput(
            mode = EvaluatorMode.LOCAL,
            rubricVersion = RUBRIC_VERSION,
            rubricScore = rubricScore,
            uncertainty = EvaluatorUncertainty(
                confidence = LOW_CONFIDENCE,
                judgeAgreement = SINGLE_JUDGE_AGREEMENT,
                disagreementUnresolved = false,
            ),
            biasSentinel = BiasSentinelResult(flagged = false),
            humanReviewStatus = if (needsReview) HumanReviewStatus.PENDING else HumanReviewStatus.NOT_REQUIRED,
            retention = if (request.cloudConsentGranted) RetentionPolicy.ABSTRACTION_ONLY else RetentionPolicy.NONE,
            evidenceEligible = false,
        )
    }

    companion object {
        const val STUB_VERSION = ScoringVersionRegistry.LANGUAGE_EVALUATOR_STUB
        const val RUBRIC_VERSION = ScoringVersionRegistry.LANGUAGE_RUBRIC_STUB
        const val NEUTRAL_SCORE = 50
        const val LOW_CONFIDENCE = 0.4
        const val SINGLE_JUDGE_AGREEMENT = 1.0
    }
}
