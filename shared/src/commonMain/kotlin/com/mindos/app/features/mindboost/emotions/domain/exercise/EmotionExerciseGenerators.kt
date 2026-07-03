package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId

/**
 * Single registry of the emotion exercises wired end-to-end to the live UI. An exercise belongs here
 * ONLY when its generator, real-time render path and metrics are complete.
 * `EmotionExerciseCatalog.implemented` is derived from these keys; the consistency test pins the
 * relationship so a finished generator can never be silently forgotten, nor an id exposed without a
 * runnable path.
 *
 * Ola 0: empty. Ola 1 step 1 adds `EM1PausaGenerator()`, `EM2RecuperacionGenerator()`,
 * `EM3SeparadorGenerator()` here.
 */
object EmotionExerciseGenerators {

    /** Generators wired to the live session/render path, indexed by id. */
    val registry: Map<EmotionExerciseId, EmotionTrialGenerator> = listOf(
        EM1PausaGenerator(),
        EM2RecuperacionGenerator(),
        EM3SeparadorGenerator(),
        EM8AntiImpulsoGenerator(),
        EM10SepPresionGenerator(),
        EM11ContinuidadGenerator(),
    ).associateBy { it.exerciseId }
}
