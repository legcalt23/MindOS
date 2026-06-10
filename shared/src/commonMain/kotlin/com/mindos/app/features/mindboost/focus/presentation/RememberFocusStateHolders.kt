package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.focus.di.FocusGraph

/** Composition-scoped factory for [FocusHomeStateHolder]. */
@Composable
fun rememberFocusHomeStateHolder(graph: FocusGraph): FocusHomeStateHolder =
    remember(graph) {
        FocusHomeStateHolder(
            profileRepository = graph.profileRepository,
            historyRepository = graph.historyRepository,
        )
    }

/**
 * Composition-scoped factory for [FocusSessionStateHolder]. The [seed] keys the
 * holder so a fresh seed (e.g. per session) yields a new, deterministic sequence.
 */
@Composable
fun rememberFocusSessionStateHolder(
    graph: FocusGraph,
    userId: String,
    seed: Long,
): FocusSessionStateHolder {
    val scope = rememberCoroutineScope()
    return remember(graph, userId, seed, scope) {
        FocusSessionStateHolder(
            graph = graph,
            userId = userId,
            scope = scope,
            seed = seed,
        )
    }
}
