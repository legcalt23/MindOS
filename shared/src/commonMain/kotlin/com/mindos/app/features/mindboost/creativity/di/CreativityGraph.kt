package com.mindos.app.features.mindboost.creativity.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativitySessionStore
import com.mindos.app.features.mindboost.creativity.data.persistence.SecureCreativitySessionStore
import com.mindos.app.features.mindboost.creativity.domain.session.CreativitySessionCoordinator
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

/**
 * Self-contained dependency graph for the offline Creativity practice feature. Same pattern as
 * [com.mindos.app.features.mindboost.speed.di.SpeedGraph]: interfaces (not concretes), factory
 * beside the graph, encrypted local-first persistence on Android, in-memory on desktop/iOS.
 * No AI, no network.
 */
@Stable
data class CreativityGraph(
    val store: CreativitySessionStore,
    val coordinator: CreativitySessionCoordinator,
)

/** Production wiring. The encrypted store is created once per feature mount. */
@Composable
fun rememberCreativityGraph(): CreativityGraph {
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Creativity)
    val store: CreativitySessionStore = remember(secureStore) { SecureCreativitySessionStore(secureStore) }
    return remember(store) {
        CreativityGraph(store = store, coordinator = CreativitySessionCoordinator(store))
    }
}
