package com.mindos.app.features.mindboost.creativity.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreativityModelsTest {

    private val forbidden = listOf(
        "diagnos", "clinical", "therap", "treatment", "i" + "q", "intelligence", "disorder", "disease",
    )

    @Test
    fun onlyTheTwoAuthorizedExercisesExist() {
        assertEquals(
            listOf(
                CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
                CreativityExerciseId.EX02_CATEGORY_ROTATION,
            ),
            CreativityExerciseId.entries.toList(),
        )
    }

    @Test
    fun validExerciseDefinitionConstructs() {
        val def = CreativityExerciseDefinition(
            id = CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
            title = "Suppress the first idea",
            instructions = "List ideas, skipping the most obvious one.",
            minIdeas = 3,
            targetIdeas = 8,
        )
        assertEquals(CreativityPromptSource.LOCAL_STATIC, def.promptSource)
    }

    @Test
    fun exerciseDefinitionRejectsInvalidIdeaBounds() {
        assertFailsWith<IllegalArgumentException> {
            CreativityExerciseDefinition(
                id = CreativityExerciseId.EX02_CATEGORY_ROTATION,
                title = "x",
                instructions = "y",
                minIdeas = 5,
                targetIdeas = 2,
            )
        }
    }

    @Test
    fun promptRejectsBlankText() {
        assertFailsWith<IllegalArgumentException> {
            CreativityPrompt(id = "p1", exerciseId = CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA, text = "  ")
        }
    }

    @Test
    fun responseNormalizesAndCountsDistinctIdeas() {
        val response = CreativityResponse(
            id = "r1",
            promptId = "p1",
            ideas = listOf(" sky ", "sky", "", "boat"),
            elapsedMonotonicMs = 1_200,
        )
        assertEquals(listOf("sky", "sky", "boat"), response.normalizedIdeas)
        assertEquals(2, response.distinctIdeaCount)
    }

    @Test
    fun rubricResultIsNeverAiDerived() {
        val result = CreativityRubricResult(
            rubricVersion = 1,
            signals = mapOf("fluency" to 3.0, "elaboration" to 1.5),
            summary = CreativityPracticeLabel.STEADY_PRACTICE,
        )
        assertFalse(result.aiDerived)
    }

    @Test
    fun rubricResultRejectsAiDerivedFlag() {
        assertFailsWith<IllegalArgumentException> {
            CreativityRubricResult(
                rubricVersion = 1,
                signals = emptyMap(),
                summary = CreativityPracticeLabel.KEEP_PRACTICING,
                aiDerived = true,
            )
        }
    }

    @Test
    fun practiceLabelsContainNoUnsafeClinicalTerms() {
        for (label in CreativityPracticeLabel.entries) {
            val name = label.name.lowercase()
            assertTrue(
                forbidden.none { name.contains(it) },
                "Practice label '${label.name}' must not contain a clinical or graded term",
            )
        }
    }
}
