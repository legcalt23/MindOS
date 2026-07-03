package com.mindos.app.features.mindboost.creativity.domain.exercise

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseDefinition
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId

/**
 * Canonical local definitions of the MVP creativity exercises (`05_exercise_review.md`).
 *
 * Single source of truth for which exercises are reachable ([implemented]) and their
 * deterministic completion rules. Difficulty bounds are per-exercise domain data — hence the
 * localized MagicNumber suppression, consistent with the project's catalog/token convention.
 */
@Suppress("MagicNumber")
object CreativityExerciseCatalog {

    private val ex01 = CreativityExerciseDefinition(
        id = CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
        title = "Suppress the first idea",
        instructions = "List as many uses as you can — skip the first, most obvious one each time.",
        minIdeas = 3,
        targetIdeas = 8,
    )

    private val ex02 = CreativityExerciseDefinition(
        id = CreativityExerciseId.EX02_CATEGORY_ROTATION,
        title = "Category rotation",
        instructions = "Produce ideas for the theme, rotating to a fresh angle with each idea.",
        minIdeas = 4,
        targetIdeas = 10,
    )

    /** Exercises reachable in the local MVP. */
    val implemented: List<CreativityExerciseId> = listOf(
        CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA,
        CreativityExerciseId.EX02_CATEGORY_ROTATION,
    )

    fun definitionFor(id: CreativityExerciseId): CreativityExerciseDefinition = when (id) {
        CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA -> ex01
        CreativityExerciseId.EX02_CATEGORY_ROTATION -> ex02
    }
}
