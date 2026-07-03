package com.mindos.app.features.mindboost.creativity.domain.scoring

import com.mindos.app.features.mindboost.creativity.domain.exercise.CreativityExerciseCatalog
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPracticeLabel
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CreativityRubricScorerTest {

    // EX01: minIdeas = 3, targetIdeas = 8 -> midpoint = 5.
    private val definition = CreativityExerciseCatalog.definitionFor(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA)

    private fun response(ideas: List<String>) =
        CreativityResponse(id = "r1", promptId = "p1", ideas = ideas, elapsedMonotonicMs = 1_000)

    @Test
    fun fluencyCountsDistinctSubstantiveIdeas() {
        // "cat"/"hat" substantive & distinct → 2; single-char "a"/"b"/"c" (incl. the duplicate) never count.
        val result = CreativityRubricScorer.score(definition, response(listOf("cat", "hat", "a", "b", "b", "c")))
        assertEquals(2.0, result.signals[CreativityRubricScorer.SIGNAL_FLUENCY])
    }

    @Test
    fun singleCharGibberishScoresZeroFluency() {
        // The audit's exact gaming ("a,b,c,d,e") no longer inflates fluency.
        val result = CreativityRubricScorer.score(definition, response(listOf("a", "b", "c", "d", "e")))
        assertEquals(0.0, result.signals[CreativityRubricScorer.SIGNAL_FLUENCY])
    }

    @Test
    fun elaborationIsAverageWordsPerIdea() {
        val result = CreativityRubricScorer.score(definition, response(listOf("big red ball", "sky")))
        assertEquals(2.0, result.signals[CreativityRubricScorer.SIGNAL_ELABORATION])
    }

    @Test
    fun strongLabelAtOrAboveTarget() {
        val result = CreativityRubricScorer.score(definition, response((1..8).map { "idea$it" }))
        assertEquals(CreativityPracticeLabel.STRONG_LOCAL_SESSION, result.summary)
    }

    @Test
    fun steadyLabelAroundMidpoint() {
        val result = CreativityRubricScorer.score(definition, response((1..6).map { "idea$it" }))
        assertEquals(CreativityPracticeLabel.STEADY_PRACTICE, result.summary)
    }

    @Test
    fun keepPracticingJustAboveMinimum() {
        val result = CreativityRubricScorer.score(definition, response(listOf("a", "b", "c")))
        assertEquals(CreativityPracticeLabel.KEEP_PRACTICING, result.summary)
    }

    @Test
    fun resultIsNeverAiDerived() {
        assertFalse(CreativityRubricScorer.score(definition, response(listOf("a", "b", "c"))).aiDerived)
    }
}
