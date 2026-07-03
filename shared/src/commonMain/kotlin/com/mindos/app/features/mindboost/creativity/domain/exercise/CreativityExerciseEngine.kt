package com.mindos.app.features.mindboost.creativity.domain.exercise

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityExerciseId
import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPrompt
import kotlin.random.Random

/**
 * Deterministic, offline selection of a local prompt for an exercise.
 *
 * The same `(exerciseId, seed)` always yields the same prompt — no AI, no network, no
 * wall-clock. The caller supplies [seed] (e.g. derived from the monotonic session start) so
 * selection is reproducible and unit-testable. Prompts are always drawn from the local
 * [CreativityPromptCatalog].
 */
object CreativityExerciseEngine {

    fun nextPrompt(exerciseId: CreativityExerciseId, seed: Long): CreativityPrompt {
        val pool = CreativityPromptCatalog.promptsFor(exerciseId)
        val index = Random(seed).nextInt(pool.size)
        return CreativityPrompt(
            id = "${exerciseId.name}#$index",
            exerciseId = exerciseId,
            text = pool[index],
        )
    }
}
