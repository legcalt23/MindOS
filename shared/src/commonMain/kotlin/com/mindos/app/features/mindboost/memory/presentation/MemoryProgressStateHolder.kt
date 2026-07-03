package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.PairsResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.SpanResultStore
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryExerciseProgress
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryProgressUiState

/**
 * Read-only progress holder for Memoria, mirroring the FocusProgressScreen pattern. Shows how many sessions
 * are RECORDED per implemented exercise (EX1 span, EX3 deferred, EX4 pairs) — honestly framed as
 * "registradas", NOT as valid trials toward the ≥20-valid profile gate (validity lives in the scorers).
 * Reuses the module's existing result stores; adds no new persistence.
 */
class MemoryProgressStateHolder(
    private val spanStore: SpanResultStore,
    private val pairsStore: PairsResultStore,
    private val deferredStore: DeferredResultStore,
) {
    var uiState: MemoryProgressUiState by mutableStateOf(MemoryProgressUiState.Loading)
        private set

    suspend fun load(userId: String) {
        val span = spanStore.load(userId).trials.size
        val pairs = pairsStore.load(userId).trials.size
        val deferred = deferredStore.load(userId).outcomes.size
        uiState = MemoryProgressUiState.Ready(
            exercises = listOf(
                MemoryExerciseProgress(MemoryExerciseCatalog.EX1_SPAN_COMPLEJO.displayName, span),
                MemoryExerciseProgress(MemoryExerciseCatalog.EX3_LISTA_ESTRATEGIA.displayName, deferred),
                MemoryExerciseProgress(MemoryExerciseCatalog.EX4_PARES_ASOCIADOS.displayName, pairs),
            ),
        )
    }
}

@Composable
fun rememberMemoryProgressStateHolder(graph: MemoryGraph): MemoryProgressStateHolder =
    remember(graph) {
        MemoryProgressStateHolder(graph.spanResultStore, graph.pairsResultStore, graph.deferredResultStore)
    }
