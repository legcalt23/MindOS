package com.mindos.app.features.mindboost.logic

import com.mindos.app.features.mindboost.logic.domain.content.DefaultLogicContentProvider
import com.mindos.app.features.mindboost.logic.domain.model.JustificationClaim
import com.mindos.app.features.mindboost.logic.domain.model.LogicAttempt
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfidence
import com.mindos.app.features.mindboost.logic.domain.model.LogicItem
import com.mindos.app.features.mindboost.logic.domain.model.LogicResponse
import com.mindos.app.features.mindboost.logic.domain.model.ReasoningScore

object LogicTestFixtures {
    val items: List<LogicItem> = DefaultLogicContentProvider().sessionItems()

    fun correctResponse(
        item: LogicItem,
        confidence: LogicConfidence = LogicConfidence.HIGH,
        hintsUsed: Int = 0,
        timeSpentMs: Long = 5_000,
    ) = LogicResponse(
        selectedAnswerId = item.correctAnswerId,
        selectedPremiseIds = item.requiredPremiseIds,
        reasoningRule = item.validReasoningRules.first(),
        justificationInput = item.expectedJustificationClaim,
        confidence = confidence,
        hintsUsed = hintsUsed,
        timeSpentMs = timeSpentMs,
    )

    @Suppress("LongParameterList") // Test builder exposes independent scoring dimensions.
    fun attempt(
        item: LogicItem = items.first(),
        isCorrect: Boolean = true,
        totalIcr: Double = 90.0,
        justification: Double = 0.9,
        errors: List<com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag> = emptyList(),
        isomorphic: Boolean = item.isIsomorphicProbe,
    ) = LogicAttempt(
        itemId = item.id,
        exerciseType = item.exerciseType,
        targetSkills = item.targetSkills,
        difficulty = item.difficultyLevel,
        surfaceForm = item.surfaceForm,
        logicalStructureId = item.logicalStructureId,
        selectedAnswer = if (isCorrect) item.correctAnswerId else "wrong",
        isCorrect = isCorrect,
        premisesSelected = item.requiredPremiseIds,
        reasoningRule = item.validReasoningRules.first(),
        justificationInput = item.expectedJustificationClaim,
        confidence = LogicConfidence.HIGH,
        hintsUsed = 0,
        timeSpentMs = 5_000,
        scoreBreakdown = ReasoningScore(
            finalAnswerScore = if (isCorrect) 1.0 else 0.0,
            premiseIdentificationScore = 1.0,
            ruleApplicationScore = 1.0,
            justificationScore = justification,
            confidenceCalibrationScore = if (isCorrect) 0.9 else 0.1,
            independenceFromHintsScore = 1.0,
            transferConsistencyScore = if (isomorphic) if (isCorrect) 1.0 else 0.0 else null,
            impulsivityPenalty = 0.0,
            conceptualErrorPenalty = if (errors.isEmpty()) 0.0 else 1.0,
            totalIcr = totalIcr,
        ),
        errorTags = errors,
        transferTag = item.transferLevel,
        isIsomorphicProbe = isomorphic,
        createdAt = 1L,
    )

    fun noJustificationResponse(item: LogicItem) = correctResponse(item).copy(
        selectedPremiseIds = emptyList(),
        justificationInput = JustificationClaim.NOT_PROVIDED,
    )
}
