@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.memory.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.di.rememberMemoryGraph
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.presentation.rememberMemoryHomeStateHolder
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

    val homeHolder = rememberMemoryHomeStateHolder(graph)
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
                holder.abort()
                route = MemoryRoute.Home
            })
            MemorySessionScreen(
                holder = holder,
                exerciseId = current.exerciseId,
                onExit = { route = MemoryRoute.Home },
                modifier = modifier,
            )
        }
    }
}

/** Internal navigation of the memory feature. */
private sealed interface MemoryRoute {
    data object Home : MemoryRoute
    data class Session(val exerciseId: MemoryExerciseId, val seed: Long) : MemoryRoute
}
