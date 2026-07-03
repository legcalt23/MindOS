package com.mindos.app.features.mindboost.creativity.domain.exercise

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPromptSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreativityExerciseEngineTest {

    @Test
    fun sameSeedYieldsSamePrompt() {
        val a = CreativityExerciseEngine.nextPrompt(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, seed = 42)
        val b = CreativityExerciseEngine.nextPrompt(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, seed = 42)
        assertEquals(a, b)
    }

    @Test
    fun promptComesFromTheLocalPoolWithLocalOrigin() {
        val prompt = CreativityExerciseEngine.nextPrompt(CreativityExerciseId.EX02_CATEGORY_ROTATION, seed = 7)
        assertEquals(CreativityExerciseId.EX02_CATEGORY_ROTATION, prompt.exerciseId)
        assertEquals(CreativityPromptSource.LOCAL_STATIC, prompt.origin)
        assertTrue(
            CreativityPromptCatalog.promptsFor(CreativityExerciseId.EX02_CATEGORY_ROTATION).contains(prompt.text),
        )
    }

    @Test
    fun differentSeedsCanSelectDifferentPrompts() {
        val texts = (0L until 20L)
            .map { CreativityExerciseEngine.nextPrompt(CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, it).text }
            .toSet()
        assertTrue(texts.size >= 2, "Deterministic selection should span more than one prompt across seeds")
    }
}
