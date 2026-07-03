package com.mindos.app.features.mindboost.emotions.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId

/** Composition-scoped factory for the emotions home menu. */
@Composable
fun rememberEmotionHomeStateHolder(): EmotionHomeStateHolder = remember { EmotionHomeStateHolder() }

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
    return remember(graph, exerciseId, userId, seed, scope) {
        EmotionSessionStateHolder(graph = graph, exerciseId = exerciseId, userId = userId, scope = scope, seed = seed)
    }
}
