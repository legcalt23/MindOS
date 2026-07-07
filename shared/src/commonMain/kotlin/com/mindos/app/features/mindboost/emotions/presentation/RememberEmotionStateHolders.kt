package com.mindos.app.features.mindboost.emotions.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.di.emotionSessionRecorder
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import kotlin.coroutines.cancellation.CancellationException

/** Composition-scoped factory for the emotions home menu. */
@Composable
fun rememberEmotionHomeStateHolder(): EmotionHomeStateHolder = remember { EmotionHomeStateHolder() }

/**
 * Composition-scoped factory for the read-only history/progress holder (MC8u-B §4). Loads once per (repository,
 * user) from the local encrypted store; a recoverable load failure never crashes module entry (cancellation still
 * propagates).
 */
@Composable
fun rememberEmotionHistory(graph: EmotionGraph, userId: String): EmotionHistoryStateHolder {
    val holder = remember(graph.repository, userId, graph.analytics) {
        EmotionHistoryStateHolder(graph.repository, userId, graph.analytics)
    }
    LaunchedEffect(holder) {
        runCatching { holder.load() }.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }
    return holder
}

/**
 * Composition-scoped factory for [EmotionSessionStateHolder]. The [seed] keys the holder so a fresh
 * seed (e.g. per session) yields a new, deterministic sequence.
 */
@Composable
fun rememberEmotionSessionStateHolder(
    graph: EmotionGraph,
    exerciseId: EmotionExerciseId,
    userId: String,
    seed: Long,
): EmotionSessionStateHolder {
    val scope = rememberCoroutineScope()
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val recorder = remember(sessionsStore, graph.epochClock) {
        emotionSessionRecorder(sessionsStore, graph.epochClock)
    }
    return remember(graph, exerciseId, userId, seed, scope, recorder) {
        EmotionSessionStateHolder(
            graph = graph,
            exerciseId = exerciseId,
            userId = userId,
            scope = scope,
            seed = seed,
            recorder = recorder,
        )
    }
}
