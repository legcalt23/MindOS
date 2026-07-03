package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.memory.di.MemoryGraph

/** Composition-scoped factory for [MemoryHomeStateHolder]. */
@Composable
fun rememberMemoryHomeStateHolder(graph: MemoryGraph): MemoryHomeStateHolder =
    remember(graph) {
        MemoryHomeStateHolder(
            profileRepository = graph.profileRepository,
            deferredRecallRepository = graph.deferredRecallRepository,
            deferredScheduler = graph.deferredScheduler,
            content = graph.content,
            epochClock = graph.epochClock,
        )
    }

/**
 * Composition-scoped factory for [MemorySessionStateHolder]. The [seed] keys the
 * holder so a fresh seed (per session) yields a new, deterministic trial.
 */
@Composable
fun rememberMemorySessionStateHolder(
    graph: MemoryGraph,
    userId: String,
    seed: Long,
): MemorySessionStateHolder {
    val scope = rememberCoroutineScope()
    return remember(graph, userId, seed, scope) {
        MemorySessionStateHolder(graph = graph, userId = userId, scope = scope, seed = seed)
    }
}

/**
 * Composition-scoped factory for [DeferredRecallSessionStateHolder] — the between-session 24h
 * delayed-recall review. The [seed] keys the holder so a fresh seed (per entry) yields a new,
 * deterministic candidate ordering.
 */
@Composable
fun rememberDeferredRecallSessionStateHolder(
    graph: MemoryGraph,
    userId: String,
    seed: Long,
): DeferredRecallSessionStateHolder {
    val scope = rememberCoroutineScope()
    return remember(graph, userId, seed, scope) {
        DeferredRecallSessionStateHolder(graph = graph, userId = userId, scope = scope, seed = seed)
    }
}
