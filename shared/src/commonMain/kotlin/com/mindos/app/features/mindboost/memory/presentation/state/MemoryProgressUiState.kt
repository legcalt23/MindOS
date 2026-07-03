package com.mindos.app.features.mindboost.memory.presentation.state

/** Read-only progress view for Memoria: sessions recorded per implemented exercise. */
sealed interface MemoryProgressUiState {
    data object Loading : MemoryProgressUiState

    data class Ready(val exercises: List<MemoryExerciseProgress>) : MemoryProgressUiState
}

data class MemoryExerciseProgress(
    val name: String,
    /**
     * Sessions/trials RECORDED for this exercise — honestly "registradas", not "válidas". Validity toward the
     * ≥20-valid-trial profile gate lives in the scorers; this count never claims those trials are all valid.
     */
    val recordedCount: Int,
)
