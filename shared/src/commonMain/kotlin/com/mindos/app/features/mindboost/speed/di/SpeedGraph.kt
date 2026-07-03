package com.mindos.app.features.mindboost.speed.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.speed.data.persistence.SecureSpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.MetricThresholds
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

/**
 * Grafo de dependencias autocontenido del feature Velocidad Mental. Mismo patrón que
 * [com.mindos.app.features.mindboost.focus.di.FocusGraph]: interfaces (no concretas),
 * factory junto al grafo, persistencia local-first cifrada en Android
 * ([rememberSecureKeyValueStore]) y fallback in-memory en desktop/iOS.
 */
@Stable
data class SpeedGraph(
    val store: SpeedMeasurementStore,
    val clock: EpochClock,
    val thresholds: MetricThresholds,
)

/** Wiring de producción. El store cifrado se crea una vez por montaje del feature. */
@Composable
fun rememberSpeedGraph(): SpeedGraph {
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Speed)
    val store: SpeedMeasurementStore = remember(secureStore) { SecureSpeedMeasurementStore(secureStore) }
    return remember(store) {
        SpeedGraph(
            store = store,
            clock = SystemEpochClock,
            thresholds = MetricThresholds.DEFAULT,
        )
    }
}
