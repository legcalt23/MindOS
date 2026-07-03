package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId

/** Composition-scoped factory for [FocusHomeStateHolder]. */
@Composable
fun rememberFocusHomeStateHolder(graph: FocusGraph): FocusHomeStateHolder =
    remember(graph) {
        FocusHomeStateHolder(
            profileRepository = graph.profileRepository,
            historyRepository = graph.historyRepository,
            overtrainingGuard = graph.overtrainingGuard,
        )
    }

/** Composition-scoped factory for [FocusProgressStateHolder]. */
@Composable
fun rememberFocusProgressStateHolder(graph: FocusGraph): FocusProgressStateHolder =
    remember(graph) {
        FocusProgressStateHolder(
            historyRepository = graph.historyRepository,
            baselineRepository = graph.baselineRepository,
            probeBankRepository = graph.probeBankRepository,
            evaluationService = graph.evaluationService,
        )
    }

/**
 * Composition-scoped factory for [FocusBaselineStateHolder]. The [seed] keys the holder so a fresh
 * seed (e.g. per attempt) yields a new, deterministic calibration sequence.
 */
@Composable
fun rememberFocusBaselineStateHolder(
    graph: FocusGraph,
    userId: String,
    seed: Long,
): FocusBaselineStateHolder {
    val scope = rememberCoroutineScope()
    return remember(graph, userId, seed, scope) {
        FocusBaselineStateHolder(graph = graph, userId = userId, scope = scope, seed = seed)
    }
}

/** Composition-scoped factory for [FocusEvaluationStateHolder] (one reserved-probe retest). */
@Composable
fun rememberFocusEvaluationStateHolder(
    graph: FocusGraph,
    userId: String,
): FocusEvaluationStateHolder {
    val scope = rememberCoroutineScope()
    return remember(graph, userId, scope) {
        FocusEvaluationStateHolder(graph = graph, userId = userId, scope = scope)
    }
}

/**
 * Composition-scoped factory for [FocusSessionStateHolder]. The [seed] keys the
 * holder so a fresh seed (e.g. per session) yields a new, deterministic sequence.
 */
@Composable
fun rememberFocusSessionStateHolder(
    graph: FocusGraph,
    exerciseId: ExerciseId,
    userId: String,
    seed: Long,
): FocusSessionStateHolder {
    val scope = rememberCoroutineScope()
    return remember(graph, exerciseId, userId, seed, scope) {
        FocusSessionStateHolder(
            graph = graph,
            exerciseId = exerciseId,
            userId = userId,
            scope = scope,
            seed = seed,
        )
    }
}
