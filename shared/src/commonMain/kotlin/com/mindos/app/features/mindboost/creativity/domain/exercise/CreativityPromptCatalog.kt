package com.mindos.app.features.mindboost.creativity.domain.exercise

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId

/**
 * Static, local prompt pools for each exercise. Deterministic and offline — these are the
 * ONLY source of prompts in the MVP (no AI generation, no network). EX01 uses everyday
 * objects for alternative-uses; EX02 uses seed themes for category rotation.
 */
object CreativityPromptCatalog {

    private val ex01Suppress: List<String> = listOf(
        "a brick",
        "a paperclip",
        "a wooden spoon",
        "an empty bottle",
        "a rubber band",
        "a cardboard box",
        "a bath towel",
        "a coffee mug",
    )

    private val ex02Rotation: List<String> = listOf(
        "uses for water",
        "things that are round",
        "ways to travel",
        "things found in a kitchen",
        "uses for light",
        "things that grow",
    )

    fun promptsFor(exerciseId: CreativityExerciseId): List<String> = when (exerciseId) {
        CreativityExerciseId.EX01_SUPPRESS_FIRST_IDEA -> ex01Suppress
        CreativityExerciseId.EX02_CATEGORY_ROTATION -> ex02Rotation
    }
}
