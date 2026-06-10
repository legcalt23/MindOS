package com.mindos.app.features.mindboost.memory.presentation.state

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/** A selectable exercise card on the memory home. */
data class MemoryExerciseCardUi(
    val exerciseId: MemoryExerciseId,
    val name: String,
    val description: String,
)

/** Top-level state of the MindBoost · Memoria home screen. */
sealed interface MemoryHomeUiState {
    data object Loading : MemoryHomeUiState

    data class Ready(
        val displayName: String,
        val archetypeName: String,
        val cycleWeek: Int,
        val dueDeferredCount: Int,
        val exercises: List<MemoryExerciseCardUi>,
    ) : MemoryHomeUiState
}

/** One selectable recall option (id = stable content id, text = what the user sees). */
data class RecallOptionUi(val id: String, val text: String)

/** Honest finished-session summary for the UI. */
data class MemorySessionResultUi(
    val exerciseName: String,
    val immediateAccuracyPercent: Int?,
    val hits: Int,
    val studied: Int,
    val honestNote: String,
)

/** Top-level state of a memory session run (turn-based: study → recall → finished). */
sealed interface MemorySessionUiState {
    data class Intro(
        val exerciseName: String,
        val exerciseDescription: String,
    ) : MemorySessionUiState

    /** Present the study material; the user studies it, then continues. */
    data class Study(
        val exerciseName: String,
        val instruction: String,
        val items: List<String>,
    ) : MemorySessionUiState

    /** Collect recall by selection; [candidates] mixes studied items with distractors. */
    data class Recall(
        val exerciseName: String,
        val instruction: String,
        val candidates: List<RecallOptionUi>,
    ) : MemorySessionUiState

    data class Finished(val result: MemorySessionResultUi) : MemorySessionUiState

    data class Aborted(val reason: String) : MemorySessionUiState
}
