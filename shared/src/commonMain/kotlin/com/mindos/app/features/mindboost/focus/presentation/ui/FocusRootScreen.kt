@file:Suppress("FunctionNaming")

package com.mindos.app.features.mindboost.focus.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.di.rememberFocusGraph
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.rememberFocusHomeStateHolder
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

    val homeHolder = rememberFocusHomeStateHolder(graph)
    LaunchedEffect(userId, route) {
        if (route is FocusRoute.Home) homeHolder.load(userId, displayName)
    }

    when (val current = route) {
        is FocusRoute.Home -> {
            PlatformBackHandler(enabled = true, onBack = onExit)
            FocusHomeScreen(
                state = homeHolder.uiState,
                onStart = { template ->
                    sessionSeed = graph.epochClock.nowMillis()
                    route = FocusRoute.Session(template, sessionSeed)
                },
                onBack = onExit,
                modifier = modifier,
            )
        }

        is FocusRoute.Session -> {
            val holder = rememberFocusSessionStateHolder(
                graph = graph,
                userId = userId,
                seed = current.seed,
            )
            LaunchedEffect(holder, current.template) { holder.prepare(current.template) }
            PlatformBackHandler(enabled = true, onBack = {
                holder.abort()
                route = FocusRoute.Home
            })
            FocusSessionScreen(
                holder = holder,
                onExit = { route = FocusRoute.Home },
                onRestart = {
                    sessionSeed = graph.epochClock.nowMillis()
                    route = FocusRoute.Session(current.template, sessionSeed)
                },
                modifier = modifier,
            )
        }
    }
}

/** Internal navigation of the focus feature. */
private sealed interface FocusRoute {
    data object Home : FocusRoute
    data class Session(val template: SessionTemplate, val seed: Long) : FocusRoute
}
