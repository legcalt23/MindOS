package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId

/**
 * Single registry of the focus exercises wired end-to-end to the live UI. An exercise belongs here ONLY when its
 * generator, its real-time render path (`FocusStimulusField` / `FocusSessionStateHolder`) and its metrics are
 * complete. `ExerciseCatalog.implemented` must equal [wiredExerciseIds]; `ExerciseCatalogConsistencyTest`
 * enforces that, so a finished generator can never be silently forgotten, nor an id exposed without a runnable
 * path.
 *
 * All 13 exercises are wired end-to-end: single-tap (E1 Vigilance, E3 GoNoGo, E7 visual search, E8 dual-task,
 * E9 MOT-probe, E10 RSVP-detection, E12 re-engagement), choice LEFT/RIGHT (E2 Flanker, E5 Stroop/Simon, E6
 * task-switch, E11 anchor, E13 deadline-pressure) and stop-signal (E4 Freno). Adding a generator without its
 * renderer + response path breaks `ExerciseCatalogConsistencyTest` — which is the point.
 *
 * ## Why two maps and not one
 *
 * They are different TYPES, and the type is the barrier — not a naming convention, not a comment. A single
 * `Map<ExerciseId, TrialGenerator>` is exactly what let E2 — already migrated, already versioned — be handed to
 * `FocusFixedBlockRunner` and drawn from an unversioned bare `Long` on the baseline and probe routes: the map's
 * value type said "legacy is fine for any of these", and the compiler agreed. Now [VersionedTrialGenerator] does
 * not extend [TrialGenerator], so a migrated exercise **cannot** be placed in [legacy] — the build fails.
 *
 * The split doubles as the migration ledger: membership of [versioned] IS the definition of "migrated", so the
 * ledger cannot drift from reality the way a documented list would, because it is the wiring itself.
 */
object FocusExerciseGenerators {

    /**
     * Migrated: reproducible form, seed names its own algorithm, routed through `FocusEngine`.
     *
     * **1 of 13.** That is the honest state of the migration and it is not rounded up anywhere.
     */
    val versioned: Map<ExerciseId, VersionedTrialGenerator> = listOf(
        E2FiltroGenerator(),
    ).associateBy { it.exerciseId }

    /**
     * Not yet migrated: unversioned bare-`Long` seed, no reproducible identity, no engine route.
     *
     * **12 of 13.** They are playable and they are not reproducible; both halves are true and neither is hidden.
     * Migrating one means implementing [VersionedTrialGenerator], moving it here to [versioned], and registering
     * its versions in `FocusFormContract` — at which point the compiler removes its legacy route for good.
     */
    val legacy: Map<ExerciseId, TrialGenerator> = listOf(
        E1VigiaGenerator(),
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

    /** Every exercise with a runnable path, by whichever route. What `ExerciseCatalog.implemented` must equal. */
    val wiredExerciseIds: Set<ExerciseId> = versioned.keys + legacy.keys

    init {
        // An exercise on both maps would have a legacy route AND a versioned one — the exact ambiguity the split
        // removes. The type system cannot express "these two maps are disjoint", so it is asserted at class-init:
        // it fails on first touch of the registry, in every test and at app start, rather than in review.
        val both = versioned.keys intersect legacy.keys
        require(both.isEmpty()) {
            "$both are registered as BOTH versioned and legacy. A migrated exercise must be removed from the " +
                "legacy map in the same change, or it keeps an unversioned route the engine cannot see."
        }
    }
}
