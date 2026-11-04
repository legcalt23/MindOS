package com.mindos.app.features.mindboost.focus.domain.engine

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId

/**
 * Every version that decides what form an exercise produces, for ONE exercise.
 *
 * Each is load-bearing: change any of them and the same seed can yield a different presented form, so each is
 * part of the reproducible identity and of `formId`.
 */
data class FocusFormVersions(
    /** The drawn sequence. Bump when the generator consumes different draws or maps them differently. */
    val generatorVersion: Int,
    /** The exercise's task definition (what the user is asked to do). */
    val exerciseVersion: Int,
    /** The difficulty → concrete-parameters mapping (e.g. `E2DifficultyMapping`). */
    val difficultyMappingVersion: Int,
    /** How responses map to keys/sides. */
    val responseMappingVersion: Int,
    /** The item/content bank the form draws from. */
    val contentVersion: Int,
)

/**
 * THE canonical source of form versions for MindBoost · Enfoque.
 *
 * Before this existed the same number lived in four places — the generator's constant, the generator's
 * property, the engine's literal pin, and the caller's request — so "the version" was whatever the last editor
 * remembered to change. This is now the single authority: the generator and the engine both VALIDATE against
 * it, and no caller (UI, state holder) may name a version at all.
 *
 * The generator still declares its own version independently rather than reading it from here. That is
 * deliberate: a generator that derived its version from this table would agree with it by construction, and the
 * check would be vacuous. Two independent declarations that must match is a real check —
 * `ProductionFocusGraphWiringTest` is where they are held together.
 */
object FocusFormContract {

    /**
     * The version of the engine's own session-construction contract. Bump when the engine changes how it
     * resolves or assembles a session in a way that could alter the presented form.
     */
    const val ENGINE_CONTRACT_VERSION = 1

    /**
     * Exercises wired to [FocusEngine], with the versions each is authorized at. Absent = not wired: the engine
     * fails closed rather than inventing a version for it. The twelve unmigrated exercises are absent by design.
     */
    private val FORMS: Map<ExerciseId, FocusFormVersions> = mapOf(
        // E2 is generatorVersion 3 since its congruency split became a quota instead of a per-trial coin flip,
        // so a given seed lays the conditions out differently than it did at 2. (2 was the move off
        // `kotlin.random.Random` — XorWow, an algorithm the stdlib does not guarantee across versions — to the
        // pinned splitmix64.) Each step yields a different form for the same seed, so they must never be
        // conflated.
        ExerciseId.E2_FILTRO to FocusFormVersions(
            generatorVersion = 3,
            exerciseVersion = 1,
            difficultyMappingVersion = 1,
            responseMappingVersion = 1,
            contentVersion = 1,
        ),
    )

    /** The authorized versions for [exerciseId], or null when it is not wired to the engine. */
    fun versionsFor(exerciseId: ExerciseId): FocusFormVersions? = FORMS[exerciseId]

    /** The exercises this contract authorizes — the engine's wired set. */
    fun wiredExercises(): Set<ExerciseId> = FORMS.keys
}
