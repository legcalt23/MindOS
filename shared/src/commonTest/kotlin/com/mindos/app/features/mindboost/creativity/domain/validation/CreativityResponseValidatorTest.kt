package com.mindos.app.features.mindboost.creativity.domain.validation

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseDefinition
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreativityResponseValidatorTest {

    private val definition = CreativityExerciseDefinition(
        id = CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
        title = "Suppress the first idea",
        instructions = "List ideas, skipping the most obvious one.",
        minIdeas = 3,
        targetIdeas = 8,
    )

    private fun response(ideas: List<String>, elapsed: Long = 5_000) =
        CreativityResponse(id = "r1", promptId = "p1", ideas = ideas, elapsedMonotonicMs = elapsed)

    @Test
    fun acceptsEnoughDistinctIdeas() {
        val result = CreativityResponseValidator.validate(definition, response(listOf("a", "b", "c", "d")))
        assertTrue(result.isValid)
    }

    @Test
    fun rejectsEmptyResponse() {
        val result = CreativityResponseValidator.validate(definition, response(listOf("   ", "")))
        assertEquals(
            CreativityValidationResult.Invalid(CreativityValidationError.EMPTY_RESPONSE),
            result,
        )
    }

    @Test
    fun rejectsTooFewDistinctIdeas() {
        // 4 raw ideas but only 2 distinct after normalization → below minIdeas (3).
        val result = CreativityResponseValidator.validate(definition, response(listOf("a", "a", "b", " b ")))
        assertEquals(
            CreativityValidationResult.Invalid(CreativityValidationError.BELOW_MIN_IDEAS),
            result,
        )
    }

    @Test
    fun rejectsNonMonotonicTiming() {
        val result = CreativityResponseValidator.validate(definition, response(listOf("a", "b", "c"), elapsed = -1))
        assertEquals(
            CreativityValidationResult.Invalid(CreativityValidationError.TIMING_NOT_MONOTONIC),
            result,
        )
    }
}
