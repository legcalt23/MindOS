package com.mindos.app.features.mindboost.logic.domain.feedback

import com.mindos.app.features.mindboost.logic.LogicTestFixtures
import com.mindos.app.features.mindboost.logic.domain.model.DistractorType
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicExerciseType
import com.mindos.app.features.mindboost.logic.domain.scoring.DefaultReasoningScorer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogicFeedbackEngineTest {
    private val scorer = DefaultReasoningScorer()
    private val engine = DefaultLogicFeedbackEngine()

    @Test
    fun falseContradictionProducesSpecificFeedbackWithoutRevealingSolution() {
        val item = LogicTestFixtures.items.first { it.exerciseType == LogicExerciseType.CONTRADICTION_DETECTION }
        val option = item.answerOptions.first { it.distractorType == DistractorType.FALSE_CONTRADICTION }
        val response = LogicTestFixtures.correctResponse(item).copy(selectedAnswerId = option.id)
        val feedback = engine.feedback(item, response, scorer.score(item, response, null))

        assertEquals(LogicErrorTag.FALSE_CONTRADICTION, feedback.errorTag)
        assertTrue(feedback.diagnostic.contains("contradicción"))
        assertFalse(feedback.revealSolution)
    }

    @Test
    fun feedbackNamesAffirmingConsequentPattern() {
        val item = LogicTestFixtures.items.first { it.exerciseType == LogicExerciseType.CONDITIONAL_TEST }
        val option = item.answerOptions.first { it.distractorType == DistractorType.AFFIRMING_CONSEQUENT }
        val response = LogicTestFixtures.correctResponse(item).copy(selectedAnswerId = option.id)
        val feedback = engine.feedback(item, response, scorer.score(item, response, null))

        assertEquals(LogicErrorTag.AFFIRMING_CONSEQUENT, feedback.errorTag)
        assertFalse(feedback.revealSolution)
    }
}

