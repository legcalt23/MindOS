package com.mindos.app.features.mindboost.logic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.logic.di.LogicGraph

@Composable
fun rememberLogicStateHolder(
    graph: LogicGraph,
    userId: String,
    displayName: String,
): LogicStateHolder {
    val scope = rememberCoroutineScope()
    return remember(graph, userId, displayName, scope) {
        LogicStateHolder(graph, userId, displayName, scope)
    }
}

