package com.mindos.app.features.mindboost.logic.domain.scoring

import com.mindos.app.features.mindboost.logic.domain.model.DistractorType
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicItem
import com.mindos.app.features.mindboost.logic.domain.model.LogicResponse
import com.mindos.app.features.mindboost.logic.domain.model.ReasoningScore

data class ScoringResult(
    val isCorrect: Boolean,
    val score: ReasoningScore,
    val errorTags: List<LogicErrorTag>,
)

fun interface ReasoningScorer {
    fun score(item: LogicItem, response: LogicResponse, transferConsistencyScore: Double?): ScoringResult
}

/** Process-first ICR scorer. Speed can only subtract through impulsivity; it never adds points. */
class DefaultReasoningScorer : ReasoningScorer {

    override fun score(
        item: LogicItem,
        response: LogicResponse,
        transferConsistencyScore: Double?,
    ): ScoringResult {
        val selected = item.answerOptions.firstOrNull { it.id == response.selectedAnswerId }
        val isCorrect = selected?.isCorrect == true
        val premiseScore = overlapScore(item.requiredPremiseIds, response.selectedPremiseIds)
        val ruleScore = if (response.reasoningRule in item.validReasoningRules) 1.0 else 0.0
        val claimScore = if (response.justificationInput == item.expectedJustificationClaim) 1.0 else 0.0
        val justificationScore = (premiseScore + ruleScore + claimScore) / JUSTIFICATION_PARTS
        val confidenceScore = if (isCorrect) {
            response.confidence.probability
        } else {
            1.0 - response.confidence.probability
        }
        val independenceScore = (1.0 - response.hintsUsed * HINT_COST).coerceIn(0.0, 1.0)
        val impulsive = !isCorrect && response.timeSpentMs < item.minimumReasoningTimeMs
        val conceptualError = selected?.distractorType != null

        val weightedBase = FINAL_ANSWER_WEIGHT * (if (isCorrect) 1.0 else 0.0) +
            RULE_WEIGHT * ruleScore +
            PREMISE_WEIGHT * premiseScore +
            JUSTIFICATION_WEIGHT * justificationScore +
            CONFIDENCE_WEIGHT * confidenceScore +
            INDEPENDENCE_WEIGHT * independenceScore
        // The parentheses around each `if` are load-bearing: without them the `else` branch swallows
        // the rest of the sum, so the second penalty is either lost or multiplied into the first.
        val penaltyPoints = MAX_SCORE * (
            IMPULSIVITY_WEIGHT * (if (impulsive) 1.0 else 0.0) +
                CONCEPTUAL_ERROR_WEIGHT * (if (conceptualError) 1.0 else 0.0)
            )
        val total = (MAX_SCORE * weightedBase - penaltyPoints).coerceIn(0.0, MAX_SCORE)
        val errors = buildList {
            selected?.distractorType?.toErrorTag()?.let(::add)
            if (impulsive) add(LogicErrorTag.IMPULSIVE_RESPONSE)
            if (!isCorrect && response.confidence.probability >= HIGH_CONFIDENCE_THRESHOLD) {
                add(LogicErrorTag.OVERCONFIDENT_ERROR)
            }
        }.distinct()

        return ScoringResult(
            isCorrect = isCorrect,
            score = ReasoningScore(
                finalAnswerScore = if (isCorrect) 1.0 else 0.0,
                premiseIdentificationScore = premiseScore,
                ruleApplicationScore = ruleScore,
                justificationScore = justificationScore,
                confidenceCalibrationScore = confidenceScore,
                independenceFromHintsScore = independenceScore,
                transferConsistencyScore = transferConsistencyScore,
                impulsivityPenalty = if (impulsive) 1.0 else 0.0,
                conceptualErrorPenalty = if (conceptualError) 1.0 else 0.0,
                totalIcr = total,
            ),
            errorTags = errors,
        )
    }

    private fun overlapScore(required: List<String>, selected: List<String>): Double {
        if (required.isEmpty()) return if (selected.isEmpty()) 1.0 else 0.0
        val requiredSet = required.toSet()
        val selectedSet = selected.toSet()
        val union = requiredSet union selectedSet
        return if (union.isEmpty()) 1.0 else (requiredSet intersect selectedSet).size.toDouble() / union.size
    }

    private fun DistractorType.toErrorTag(): LogicErrorTag = LogicErrorTag.valueOf(name)

    private companion object {
        const val MAX_SCORE = 100.0
        const val FINAL_ANSWER_WEIGHT = 0.35
        const val RULE_WEIGHT = 0.20
        const val PREMISE_WEIGHT = 0.15
        const val JUSTIFICATION_WEIGHT = 0.15
        const val CONFIDENCE_WEIGHT = 0.10
        const val INDEPENDENCE_WEIGHT = 0.05
        const val IMPULSIVITY_WEIGHT = 0.10
        const val CONCEPTUAL_ERROR_WEIGHT = 0.15
        const val HINT_COST = 0.25
        const val JUSTIFICATION_PARTS = 3.0
        const val HIGH_CONFIDENCE_THRESHOLD = 0.85
    }
}

object CounterexampleValidator {
    fun invalidates(case: com.mindos.app.features.mindboost.logic.domain.model.CounterexampleCase): Boolean =
        case.premisesRemainTrue && case.conclusionBecomesFalse
}
