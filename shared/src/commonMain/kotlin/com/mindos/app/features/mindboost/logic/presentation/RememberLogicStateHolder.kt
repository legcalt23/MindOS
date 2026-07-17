package com.mindos.app.features.mindboost.logic.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.logic.di.LogicGraph
import com.mindos.app.features.mindboost.logic.di.logicSessionRecorder
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

@Composable
fun rememberLogicStateHolder(
    graph: LogicGraph,
    userId: String,
    displayName: String,
): LogicStateHolder {
    val scope = rememberCoroutineScope()
    // MC2b: activate the real write-ahead recorder in production. Writes run on `scope` (the same composable
    // scope that already persists the Lógica session); the sessions store is the account-bound
    // `mindboost_sessions` namespace covered by the account-deletion eraser.
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val revocationStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Revocation)
    val recorder = remember(sessionsStore, revocationStore, graph.epochClock, userId) {
        logicSessionRecorder(sessionsStore, revocationStore, graph.epochClock, userId)
    }
    return remember(graph, userId, displayName, scope, recorder) {
        LogicStateHolder(graph, userId, displayName, scope, recorder)
    }
}

