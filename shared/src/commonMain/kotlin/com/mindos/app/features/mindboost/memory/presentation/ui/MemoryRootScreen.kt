@file:Suppress("FunctionNaming", "LongMethod")

package com.mindos.app.features.mindboost.memory.presentation.ui

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
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.di.deferredRecallSessionRecovery
import com.mindos.app.features.mindboost.memory.di.rememberMemoryGraph
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import com.mindos.app.features.mindboost.memory.presentation.rememberDeferredRecallSessionStateHolder
import com.mindos.app.features.mindboost.memory.presentation.rememberMemoryHomeStateHolder
import com.mindos.app.features.mindboost.memory.presentation.rememberMemoryProgressStateHolder
import com.mindos.app.features.mindboost.memory.presentation.rememberMemorySessionStateHolder

/**
 * Entry point for MindBoost · Memoria. Owns the feature's lightweight internal
 * navigation (home ⇄ live session) and the [MemoryGraph]. This is the single wiring
 * point the MindBoost hub mounts — keeping the feature self-contained.
 */
@Composable
fun MemoryRootScreen(
    userId: String,
    displayName: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    graph: MemoryGraph = rememberMemoryGraph(),
) {
    var route by remember { mutableStateOf<MemoryRoute>(MemoryRoute.Home) }

    // Recover write-ahead deferred-recall sessions abandoned in a prior process, once per Memoria track entry
    // (before any review starts): stale STARTED/CHECKPOINT → ABANDONED, never touching FINALIZED or scoring.
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val revocationStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Revocation)
    LaunchedEffect(Unit) {
        deferredRecallSessionRecovery(sessionsStore, revocationStore, graph.epochClock, userId).recover()
    }
    // Root-level scope that survives a route change, so an explicit deferred-recall abort can finish marking the
    // SessionRecord ABANDONED before we navigate away (the DeferredReview composable's own scope would be cancelled).
    val reviewExitScope = rememberCoroutineScope()

    val homeHolder = rememberMemoryHomeStateHolder(graph)
    val progressHolder = rememberMemoryProgressStateHolder(graph)
    LaunchedEffect(userId, route) {
        if (route is MemoryRoute.Home) homeHolder.load(userId, displayName)
    }

    when (val current = route) {
        is MemoryRoute.Home -> {
            PlatformBackHandler(enabled = true, onBack = onExit)
            MemoryHomeScreen(
                state = homeHolder.uiState,
                onStart = { exerciseId ->
                    route = MemoryRoute.Session(exerciseId, graph.epochClock.nowMillis())
                },
                onReview = { route = MemoryRoute.DeferredReview(graph.epochClock.nowMillis()) },
                onProgress = { route = MemoryRoute.Progress },
                onBack = onExit,
                modifier = modifier,
            )
        }

        is MemoryRoute.Session -> {
            val holder = rememberMemorySessionStateHolder(
                graph = graph,
                userId = userId,
                seed = current.seed,
            )
            LaunchedEffect(holder, current.exerciseId) { holder.prepare(current.exerciseId) }
            PlatformBackHandler(enabled = true, onBack = {
                // Await the canonical ABANDONED transition before navigating away (MC5a).
                reviewExitScope.launch {
                    holder.abortAndAwait()
                    route = MemoryRoute.Home
                }
            })
            MemorySessionScreen(
                holder = holder,
                exerciseId = current.exerciseId,
                onExit = { route = MemoryRoute.Home },
                modifier = modifier,
            )
        }

        is MemoryRoute.DeferredReview -> {
            val holder = rememberDeferredRecallSessionStateHolder(
                graph = graph,
                userId = userId,
                seed = current.seed,
            )
            LaunchedEffect(holder) { holder.start() }
            PlatformBackHandler(enabled = true, onBack = {
                // Await the canonical ABANDONED transition before navigating away (MC4d).
                reviewExitScope.launch {
                    holder.abortAndAwait()
                    route = MemoryRoute.Home
                }
            })
            DeferredRecallScreen(
                holder = holder,
                onExit = { route = MemoryRoute.Home },
                modifier = modifier,
            )
        }

        is MemoryRoute.Progress -> {
            LaunchedEffect(Unit) { progressHolder.load(userId) }
            PlatformBackHandler(enabled = true, onBack = { route = MemoryRoute.Home })
            MemoryProgressScreen(
                state = progressHolder.uiState,
                onBack = { route = MemoryRoute.Home },
                modifier = modifier,
            )
        }
    }
}

/** Internal navigation of the memory feature. */
private sealed interface MemoryRoute {
    data object Home : MemoryRoute
    data object Progress : MemoryRoute
    data class Session(val exerciseId: MemoryExerciseId, val seed: Long) : MemoryRoute
    data class DeferredReview(val seed: Long) : MemoryRoute
}
