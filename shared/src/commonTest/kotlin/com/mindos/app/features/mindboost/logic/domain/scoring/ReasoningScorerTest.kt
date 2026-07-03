package com.mindos.app.features.mindboost.logic.domain.scoring

import com.mindos.app.features.mindboost.logic.LogicTestFixtures
import com.mindos.app.features.mindboost.logic.domain.model.DistractorType
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfidence
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicExerciseType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReasoningScorerTest {
    private val scorer = DefaultReasoningScorer()

    @Test
    fun icrRewardsCorrectProcessWithoutUsingSpeedAsBonus() {
        val item = LogicTestFixtures.items.first()
        val result = scorer.score(item, LogicTestFixtures.correctResponse(item), null)

        assertTrue(result.isCorrect)
        assertEquals(1.0, result.score.finalAnswerScore)
        assertEquals(1.0, result.score.premiseIdentificationScore)
        assertEquals(1.0, result.score.ruleApplicationScore)
        assertTrue(result.score.totalIcr > 95.0)
    }

    @Test
    fun highConfidenceWrongAnswerIsPenalizedMoreThanLowConfidenceWrongAnswer() {
        val item = LogicTestFixtures.items.first()
        val wrong = item.answerOptions.first { !it.isCorrect }.id
        val base = LogicTestFixtures.correctResponse(item).copy(selectedAnswerId = wrong, timeSpentMs = 5_000)

        val high = scorer.score(item, base.copy(confidence = LogicConfidence.HIGH), null)
        val low = scorer.score(item, base.copy(confidence = LogicConfidence.LOW), null)

        assertTrue(high.score.confidenceCalibrationScore < low.score.confidenceCalibrationScore)
        assertTrue(high.score.totalIcr < low.score.totalIcr)
        assertTrue(LogicErrorTag.OVERCONFIDENT_ERROR in high.errorTags)
    }

    @Test
    fun correctAnswerWithoutJustificationNeverReceivesMaximumScore() {
        val item = LogicTestFixtures.items.first()
        val result = scorer.score(item, LogicTestFixtures.noJustificationResponse(item), null)

        assertTrue(result.isCorrect)
        assertTrue(result.score.justificationScore < 0.5)
        assertTrue(result.score.totalIcr < 90.0)
    }

    @Test
    fun detectsAffirmingConsequentConditionalError() {
        val item = LogicTestFixtures.items.first { it.exerciseType == LogicExerciseType.CONDITIONAL_TEST }
        val option = item.answerOptions.first { it.distractorType == DistractorType.AFFIRMING_CONSEQUENT }
        val result = scorer.score(
            item,
            LogicTestFixtures.correctResponse(item).copy(selectedAnswerId = option.id),
            null,
        )

        assertTrue(LogicErrorTag.AFFIRMING_CONSEQUENT in result.errorTags)
    }

    @Test
    fun detectsDenyingAntecedentConditionalError() {
        val item = LogicTestFixtures.items.first { it.exerciseType == LogicExerciseType.CONDITIONAL_TEST }
        val option = item.answerOptions.first { it.distractorType == DistractorType.DENYING_ANTECEDENT }
        val result = scorer.score(
            item,
            LogicTestFixtures.correctResponse(item).copy(selectedAnswerId = option.id),
            null,
        )

        assertTrue(LogicErrorTag.DENYING_ANTECEDENT in result.errorTags)
    }

    @Test
    fun correctCounterexampleKeepsPremisesAndFalsifiesConclusion() {
        val item = LogicTestFixtures.items.first { it.exerciseType == LogicExerciseType.COUNTEREXAMPLE }
        val case = item.answerOptions.first { it.isCorrect }.counterexampleCase

        assertTrue(case != null && CounterexampleValidator.invalidates(case))
    }
}

