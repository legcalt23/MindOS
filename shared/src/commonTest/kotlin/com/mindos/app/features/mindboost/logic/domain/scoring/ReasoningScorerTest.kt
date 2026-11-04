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
    fun impulsivityAndConceptualErrorPenaltiesAreAdditive() {
        val item = LogicTestFixtures.items.first()
        val wrong = item.answerOptions.first { !it.isCorrect && it.distractorType != null }
        // The SAME option with its distractor label stripped. Isolating the conceptual-error term this
        // way keeps every other input fixed - identity, position, correctness, and the whole
        // justification base - so the only thing that can move the score is the penalty block.
        val unlabelled = item.copy(
            answerOptions = item.answerOptions.map {
                if (it.id == wrong.id) it.copy(distractorType = null) else it
            },
        )
        fun icrOf(conceptual: Boolean, impulsive: Boolean): Double {
            val target = if (conceptual) item else unlabelled
            val response = LogicTestFixtures.correctResponse(target).copy(
                selectedAnswerId = wrong.id,
                timeSpentMs = if (impulsive) {
                    item.minimumReasoningTimeMs - 1
                } else {
                    item.minimumReasoningTimeMs + SLOW_MARGIN_MS
                },
            )
            return scorer.score(target, response, null).score.totalIcr
        }

        val neither = icrOf(conceptual = false, impulsive = false)
        val onlyImpulsive = icrOf(conceptual = false, impulsive = true)
        val onlyConceptual = icrOf(conceptual = true, impulsive = false)
        val both = icrOf(conceptual = true, impulsive = true)
        val impulsivityCost = neither - onlyImpulsive
        val conceptualCost = neither - onlyConceptual

        // Anti-vacuity: additivity is trivially true if either term is worth nothing, and the floor
        // clamp would hide the difference if any cell bottomed out at zero.
        assertTrue(impulsivityCost > 0.0, "impulsivity must cost something; measured $impulsivityCost")
        assertTrue(conceptualCost > 0.0, "a conceptual error must cost something; measured $conceptualCost")
        assertTrue(both > 0.0, "no cell may hit the 0 clamp, or the deltas are clipped; both=$both")

        // The invariant. Deliberately weight-independent: re-weighting the scorer is a product
        // decision, whereas losing a penalty term to operator precedence is a defect.
        assertEquals(
            impulsivityCost + conceptualCost,
            neither - both,
            PENALTY_TOLERANCE,
            "the two penalties must add up; separately $impulsivityCost + $conceptualCost, together ${neither - both}",
        )
        // Same fact stated the way it reaches the user: a conceptual error still costs something on
        // top of an impulsive answer. Under an absorbing `else` these two scores are identical.
        assertTrue(both < onlyImpulsive, "conceptual error must still cost on an impulsive answer")
    }

    @Test
    fun correctCounterexampleKeepsPremisesAndFalsifiesConclusion() {
        val item = LogicTestFixtures.items.first { it.exerciseType == LogicExerciseType.COUNTEREXAMPLE }
        val case = item.answerOptions.first { it.isCorrect }.counterexampleCase

        assertTrue(case != null && CounterexampleValidator.invalidates(case))
    }

    private companion object {
        const val SLOW_MARGIN_MS = 1_000L
        const val PENALTY_TOLERANCE = 1e-9
    }
}

