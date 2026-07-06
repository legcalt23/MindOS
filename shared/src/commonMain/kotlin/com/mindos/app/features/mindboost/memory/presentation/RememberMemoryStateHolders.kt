package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.di.deferredRecallSessionRecorder
import com.mindos.app.features.mindboost.memory.di.mainMemorySessionRecorder
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

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
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val recorder = remember(sessionsStore, graph.epochClock) {
        mainMemorySessionRecorder(sessionsStore, graph.epochClock)
    }
    return remember(graph, userId, seed, scope, recorder) {
        MemorySessionStateHolder(graph = graph, userId = userId, scope = scope, seed = seed, recorder = recorder)
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
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val recorder = remember(sessionsStore, graph.epochClock) {
        deferredRecallSessionRecorder(sessionsStore, graph.epochClock)
    }
    return remember(graph, userId, seed, scope, recorder) {
        DeferredRecallSessionStateHolder(
            graph = graph, userId = userId, scope = scope, seed = seed, recorder = recorder,
        )
    }
}
