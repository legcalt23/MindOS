package com.mindos.app.features.mindboost.memory.domain.model

import com.mindos.app.features.mindboost.memory.domain.personalization.MemoryArchetype

/**
 * The evolving, persistable state of a user's memory training. This is the small
 * "save game": the inferred archetype, per-user CME sensitivity, the current
 * per-exercise difficulty, and the active cycle week. Raw trials are NEVER stored
 * (project learning 2026-06-05) — only this state plus session summaries.
 */
data class MemoryProfileState(
    val userId: String,
    val archetype: MemoryArchetype,
    val sensitivity: CmeSensitivity,
    val difficultyByExercise: Map<MemoryExerciseId, MemoryDifficultyVector>,
    val currentCycleWeek: Int,
) {
    init {
        require(userId.isNotBlank()) { "userId cannot be blank." }
        require(currentCycleWeek >= 1) { "currentCycleWeek is 1-based (was $currentCycleWeek)." }
    }

    /** Current difficulty for [exerciseId], or the easiest vector if none stored yet. */
    fun difficultyFor(exerciseId: MemoryExerciseId): MemoryDifficultyVector =
        difficultyByExercise[exerciseId] ?: MemoryDifficultyVector.EASIEST

    /** Returns a copy with [exerciseId]'s difficulty replaced (copy-on-write). */
    fun withDifficulty(exerciseId: MemoryExerciseId, vector: MemoryDifficultyVector): MemoryProfileState =
        copy(difficultyByExercise = difficultyByExercise + (exerciseId to vector))

    companion object {
        /** A fresh profile for a new user (beginner, default sensitivity, week 1). */
        fun initial(userId: String): MemoryProfileState = MemoryProfileState(
            userId = userId,
            archetype = MemoryArchetype.BEGINNER,
            sensitivity = CmeSensitivity.DEFAULT,
            difficultyByExercise = emptyMap(),
            currentCycleWeek = 1,
        )
    }
}
