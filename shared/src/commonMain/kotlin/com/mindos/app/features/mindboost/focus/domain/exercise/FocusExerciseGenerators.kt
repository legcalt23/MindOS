package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId

/**
 * Single registry of the focus exercises wired end-to-end to the live UI. An exercise belongs
 * here ONLY when its generator, its real-time render path (`FocusStimulusField` /
 * `FocusSessionStateHolder`) and its metrics are complete. `ExerciseCatalog.implemented` must
 * equal this map's keys; `ExerciseCatalogConsistencyTest` enforces that, so a finished generator
 * can never be silently forgotten, nor an id exposed without a runnable path.
 *
 * All 13 exercises are wired end-to-end: single-tap (E1 Vigilance, E3 GoNoGo, E7 visual search,
 * E8 dual-task, E9 MOT-probe, E10 RSVP-detection, E12 re-engagement), choice LEFT/RIGHT (E2 Flanker,
 * E5 Stroop/Simon, E6 task-switch, E11 anchor, E13 deadline-pressure) and stop-signal (E4 Freno).
 * Adding a generator here without its renderer + response path breaks
 * `ExerciseCatalogConsistencyTest` — which is the point.
 */
object FocusExerciseGenerators {

    /** The exercises whose generator is wired to the live session/render path, indexed by id. */
    val registry: Map<ExerciseId, TrialGenerator> = listOf(
        E1VigiaGenerator(),
        E2FiltroGenerator(),
        E3GatilloGenerator(),
        E4FrenoGenerator(),
        E5ConflictoGenerator(),
        E6CambioGenerator(),
        E7RastreoGenerator(),
        E8DuoGenerator(),
        E9EnjambreGenerator(),
        E10FlujoGenerator(),
        E11AnclaGenerator(),
        E12ReengancheGenerator(),
        E13PresionGenerator(),
    ).associateBy { it.exerciseId }
}
