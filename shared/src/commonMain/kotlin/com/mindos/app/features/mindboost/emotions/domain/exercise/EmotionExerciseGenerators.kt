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
 *
 * Ola 3 took the menu from six entries to three, and the reason is the same one three times: each of the
 * removed entries was another exercise's host with one constant changed, so it duplicated its twin's load on
 * the user's time without duplicating its evidence.
 *
 * - `EM11ContinuidadGenerator()` (R2) was EM2 with denser planted near-threshold trials — exactly what the
 *   fused exercise's staircase now delivers, and delivers better, because a target error RATE reaches the users
 *   planting never did. Its own 20-trial block could no longer make its governing metric confident, so keeping
 *   it would have kept an entry that cannot adapt.
 * - `EM8AntiImpulsoGenerator()` (R1) was EM1 with a higher no-go share. The fused inhibition exercise moved
 *   from a no-go share to a stop-signal staircase, so "more no-go trials" stopped being a difficulty dial at
 *   all: the staircase sets the demand per trial, for every user, and a second entry offering a fixed higher
 *   share is strictly less adaptive than the one it copies.
 * - `EM10SepPresionGenerator()` was EM3 with a constant time-pressure floor, and the audit's own reading of it
 *   is that adding hurry adds no evidence — its 900 ms floor is not long enough to read a five-to-seven word
 *   phrase, so what it measured under pressure was reading speed. R3's design keeps pressure as a MODE of the
 *   separator, unlocked by holdout accuracy, rather than as a separate menu entry; that mode is not built yet,
 *   which is a declared gap and not a silent one.
 *
 * In all three cases the catalog definition stays; only the runnable path is gone.
 */
object EmotionExerciseGenerators {

    /** Generators wired to the live session/render path, indexed by id. */
    val registry: Map<EmotionExerciseId, EmotionTrialGenerator> = listOf(
        EM1PausaGenerator(),
        EM2RecuperacionGenerator(),
        EM3SeparadorGenerator(),
    ).associateBy { it.exerciseId }
}
