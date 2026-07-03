package com.mindos.app.features.mindboost.creativity.domain.exercise

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPromptSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreativityExerciseCatalogTest {

    @Test
    fun bothMvpExercisesAreImplemented() {
        assertEquals(
            listOf(
                CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
                CreativityExerciseId.EX02_CATEGORY_ROTATION,
            ),
            CreativityExerciseCatalog.implemented,
        )
    }

    @Test
    fun everyImplementedExerciseHasAValidLocalDefinition() {
        for (id in CreativityExerciseCatalog.implemented) {
            val def = CreativityExerciseCatalog.definitionFor(id)
            assertEquals(id, def.id)
            assertEquals(CreativityPromptSource.LOCAL_STATIC, def.promptSource)
            assertTrue(def.targetIdeas >= def.minIdeas)
            assertTrue(def.title.isNotBlank())
            assertTrue(def.instructions.isNotBlank())
        }
    }

    @Test
    fun everyImplementedExerciseHasANonEmptyLocalPromptPool() {
        for (id in CreativityExerciseCatalog.implemented) {
            assertTrue(CreativityPromptCatalog.promptsFor(id).isNotEmpty())
        }
    }
}
