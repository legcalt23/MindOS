package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.features.mindboost.core.lifecycle.AppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.DisabledAppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.rememberAppLifecycleProbe
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.di.focusSessionRecorder
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

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
    val lifecycleProbe = rememberFocusLifecycleProbe()
    return remember(graph, userId, seed, scope, lifecycleProbe) {
        FocusBaselineStateHolder(
            graph = graph,
            userId = userId,
            scope = scope,
            seed = seed,
            lifecycleProbe = lifecycleProbe,
        )
    }
}

/** Composition-scoped factory for [FocusEvaluationStateHolder] (one reserved-probe retest). */
@Composable
fun rememberFocusEvaluationStateHolder(
    graph: FocusGraph,
    userId: String,
): FocusEvaluationStateHolder {
    val scope = rememberCoroutineScope()
    val lifecycleProbe = rememberFocusLifecycleProbe()
    return remember(graph, userId, scope, lifecycleProbe) {
        FocusEvaluationStateHolder(
            graph = graph,
            userId = userId,
            scope = scope,
            lifecycleProbe = lifecycleProbe,
        )
    }
}

/**
 * FOC-P0-03 kill-switch, in ONE place for all three lanes: OFF (the default) hands the holder a TYPED absence
 * (`DISABLED_BY_FLAG`), never a probe that pretends to watch. Only the platform probe can turn a foreground loss
 * into an observation.
 */
@Composable
private fun rememberFocusLifecycleProbe(): AppLifecycleProbe {
    val platformProbe = rememberAppLifecycleProbe()
    return if (FeatureFlagsHub.current.focusLifecycleProbeEnabled) platformProbe else DisabledAppLifecycleProbe
}

/**
 * Composition-scoped factory for [FocusSessionStateHolder].
 *
 * [attemptKey] keys the holder so a restart yields a fresh session; it is NOT a seed. The seed is minted by the
 * engine (via the graph's injected `SeedSource`) at the moment the session is created and persisted, so the UI
 * neither knows nor can choose it — a seed that came from here could name a session the engine never recorded.
 */
@Composable
fun rememberFocusSessionStateHolder(
    graph: FocusGraph,
    exerciseId: ExerciseId,
    userId: String,
    attemptKey: Long,
): FocusSessionStateHolder {
    val scope = rememberCoroutineScope()
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val revocationStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Revocation)
    val recorder = remember(sessionsStore, revocationStore, graph.epochClock, userId) {
        focusSessionRecorder(sessionsStore, revocationStore, graph.epochClock, userId)
    }
    val lifecycleProbe = rememberFocusLifecycleProbe()
    return remember(graph, exerciseId, userId, attemptKey, scope, recorder, lifecycleProbe) {
        FocusSessionStateHolder(
            graph = graph,
            exerciseId = exerciseId,
            userId = userId,
            scope = scope,
            recorder = recorder,
            lifecycleProbe = lifecycleProbe,
        )
    }
}
