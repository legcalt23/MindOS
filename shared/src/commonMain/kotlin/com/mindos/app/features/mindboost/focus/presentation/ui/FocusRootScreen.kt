@file:Suppress("FunctionNaming", "LongMethod")

package com.mindos.app.features.mindboost.focus.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.di.focusSessionRecovery
import com.mindos.app.features.mindboost.focus.di.rememberFocusGraph
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.rememberFocusBaselineStateHolder
import com.mindos.app.features.mindboost.focus.presentation.rememberFocusEvaluationStateHolder
import com.mindos.app.features.mindboost.focus.presentation.rememberFocusHomeStateHolder
import com.mindos.app.features.mindboost.focus.presentation.rememberFocusProgressStateHolder
import com.mindos.app.features.mindboost.focus.presentation.rememberFocusSessionStateHolder

/**
 * Entry point for MindBoost · Enfoque. Owns the feature's lightweight internal
 * navigation (home ⇄ live session) and the [FocusGraph]. This is the single
 * wiring point the app's HomeScreen mounts — keeping the feature self-contained.
 */
@Composable
fun FocusRootScreen(
    userId: String,
    displayName: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    graph: FocusGraph = rememberFocusGraph(),
) {
    var route by remember { mutableStateOf<FocusRoute>(FocusRoute.Home) }
    var sessionSeed by remember { mutableStateOf(graph.epochClock.nowMillis()) }
    // Root-level scope that survives a route change, so an explicit session abort can finish marking the
    // SessionRecord ABANDONED before we navigate away (the Session composable's own scope would be cancelled).
    val sessionExitScope = rememberCoroutineScope()
    // MC7c: recover write-ahead sessions abandoned in a prior process, once per Enfoque entry (block-agnostic
    // over mindboost_sessions; never touches FINALIZED, scoring, or Enfoque's own stores).
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    LaunchedEffect(Unit) { focusSessionRecovery(sessionsStore, graph.epochClock).recover() }

    val homeHolder = rememberFocusHomeStateHolder(graph)
    LaunchedEffect(userId, route) {
        if (route is FocusRoute.Home) homeHolder.load(userId, displayName)
    }

    when (val current = route) {
        is FocusRoute.Home -> {
            PlatformBackHandler(enabled = true, onBack = onExit)
            FocusHomeScreen(
                state = homeHolder.uiState,
                onStart = { exerciseId, template ->
                    sessionSeed = graph.epochClock.nowMillis()
                    route = FocusRoute.Session(exerciseId, template, sessionSeed)
                },
                onStartBaseline = {
                    sessionSeed = graph.epochClock.nowMillis()
                    route = FocusRoute.Baseline(sessionSeed)
                },
                onEvaluate = { route = FocusRoute.Evaluation },
                onProgress = { route = FocusRoute.Progress },
                onBack = onExit,
                modifier = modifier,
            )
        }

        is FocusRoute.Baseline -> {
            val baselineHolder = rememberFocusBaselineStateHolder(graph, userId, current.seed)
            PlatformBackHandler(enabled = true, onBack = {
                baselineHolder.abort()
                route = FocusRoute.Home
            })
            FocusBaselineScreen(
                holder = baselineHolder,
                onExit = { route = FocusRoute.Home },
                modifier = modifier,
            )
        }

        is FocusRoute.Evaluation -> {
            val evaluationHolder = rememberFocusEvaluationStateHolder(graph, userId)
            LaunchedEffect(evaluationHolder) { evaluationHolder.prepare() }
            PlatformBackHandler(enabled = true, onBack = {
                evaluationHolder.abort()
                route = FocusRoute.Home
            })
            FocusEvaluationScreen(
                holder = evaluationHolder,
                onExit = { route = FocusRoute.Home },
                modifier = modifier,
            )
        }

        is FocusRoute.Progress -> {
            val progressHolder = rememberFocusProgressStateHolder(graph)
            LaunchedEffect(progressHolder, userId) { progressHolder.load(userId) }
            PlatformBackHandler(enabled = true, onBack = { route = FocusRoute.Home })
            FocusProgressScreen(
                state = progressHolder.uiState,
                onBack = { route = FocusRoute.Home },
                modifier = modifier,
            )
        }

        is FocusRoute.Session -> {
            val holder = rememberFocusSessionStateHolder(
                graph = graph,
                exerciseId = current.exerciseId,
                userId = userId,
                seed = current.seed,
            )
            LaunchedEffect(holder, current.template) { holder.prepare(current.template) }
            // Both explicit exits (system back + in-screen abort) await the canonical ABANDONED before nav (MC7a).
            val abortSession: () -> Unit = {
                sessionExitScope.launch {
                    holder.abortAndAwait()
                    route = FocusRoute.Home
                }
            }
            PlatformBackHandler(enabled = true, onBack = abortSession)
            FocusSessionScreen(
                holder = holder,
                onExit = { route = FocusRoute.Home },
                onRestart = {
                    sessionSeed = graph.epochClock.nowMillis()
                    route = FocusRoute.Session(current.exerciseId, current.template, sessionSeed)
                },
                onAbort = abortSession,
                modifier = modifier,
            )
        }
    }
}

/** Internal navigation of the focus feature. */
private sealed interface FocusRoute {
    data object Home : FocusRoute
    data object Progress : FocusRoute
    data object Evaluation : FocusRoute
    data class Baseline(val seed: Long) : FocusRoute
    data class Session(
        val exerciseId: ExerciseId,
        val template: SessionTemplate,
        val seed: Long,
    ) : FocusRoute
}
