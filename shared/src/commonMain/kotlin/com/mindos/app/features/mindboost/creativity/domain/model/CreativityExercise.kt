package com.mindos.app.features.mindboost.creativity.domain.model

import kotlinx.serialization.Serializable

/**
 * The exercises authorized for the local creativity MVP (`05_exercise_review.md`):
 * divergent-thinking practice with intra-personal scoring only.
 *
 * - [EX01_SUPPRESS_FIRST_IDEA]: list ideas while suppressing the first/obvious one.
 * - [EX02_CATEGORY_ROTATION]: produce ideas across rotating categories.
 */
@Serializable
enum class CreativityExerciseId {
    EX01_SUPPRESS_FIRST_IDEA,
    EX02_CATEGORY_ROTATION,
}

/**
 * Static, local definition of a creativity practice exercise.
 *
 * Practice with local self-assessment — not a diagnosis, not a graded ability test, not a clinical
 * tool. Prompts are always [CreativityPromptSource.LOCAL_STATIC]; no AI generation. The
 * completion rule ([minIdeas]) is a deterministic count, never an AI judgement.
 */
@Serializable
data class CreativityExerciseDefinition(
    val id: CreativityExerciseId,
    val title: String,
    val instructions: String,
    val promptSource: CreativityPromptSource = CreativityPromptSource.LOCAL_STATIC,
    /** Minimum distinct ideas required for a complete attempt (deterministic rule). */
    val minIdeas: Int,
    /** Soft target shown to the user; never gates completion. */
    val targetIdeas: Int,
) {
    init {
        require(title.isNotBlank()) { "Exercise title must not be blank" }
        require(instructions.isNotBlank()) { "Exercise instructions must not be blank" }
        require(minIdeas >= 1) { "minIdeas must be >= 1" }
        require(targetIdeas >= minIdeas) { "targetIdeas must be >= minIdeas" }
        require(promptSource == CreativityPromptSource.LOCAL_STATIC) {
            "Creativity MVP prompts must be local/static (no AI generation)"
        }
    }
}
