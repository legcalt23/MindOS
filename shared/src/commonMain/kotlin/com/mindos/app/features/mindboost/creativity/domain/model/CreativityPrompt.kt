package com.mindos.app.features.mindboost.creativity.domain.model

import kotlinx.serialization.Serializable

/**
 * Where a prompt comes from. The local MVP only ever uses local, deterministic sources —
 * never an external model. Modeled as an enum so non-local origins are impossible to set.
 */
@Serializable
enum class CreativityPromptSource {
    LOCAL_STATIC,
}

/**
 * A single local prompt / stimulus for a creativity practice exercise.
 *
 * Deterministic and offline: selected from local static assets or local code, never
 * produced by AI or fetched over the network.
 */
@Serializable
data class CreativityPrompt(
    val id: String,
    val exerciseId: CreativityExerciseId,
    val text: String,
    val origin: CreativityPromptSource = CreativityPromptSource.LOCAL_STATIC,
) {
    init {
        require(id.isNotBlank()) { "Prompt id must not be blank" }
        require(text.isNotBlank()) { "Prompt text must not be blank" }
        require(origin == CreativityPromptSource.LOCAL_STATIC) {
            "Creativity prompts must originate locally (no AI/network source)"
        }
    }
}
