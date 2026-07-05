package com.mindos.app.features.mindboost.speed.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecoveryScanner
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
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

/**
 * Production write-ahead recorder for Velocidad over the account-bound `mindboost_sessions` namespace. A pure
 * (non-Compose) factory so the production wiring stays unit-testable. The [SecureSessionRecorder] is total by
 * contract — a storage failure never breaks a training session. Injected into the Velocidad session holder
 * from the exercise screen (MC1b); the holder's own default stays `NoOpSessionRecorder`.
 */
internal fun speedSessionRecorder(sessionsStore: SecureKeyValueStore, clock: EpochClock): SessionRecorder =
    SecureSessionRecorder(SecureSessionRecordStore(sessionsStore), clock)

/**
 * Production orphan-recovery scanner for Velocidad over `mindboost_sessions`. Pure factory (unit-testable);
 * marks STARTED/CHECKPOINT sessions stale by updatedAtMs (heartbeat) as ABANDONED and never touches FINALIZED,
 * scoring, or other blocks. Run once per Velocidad track entry from the root screen (MC1c).
 */
internal fun speedSessionRecovery(sessionsStore: SecureKeyValueStore, clock: EpochClock): SessionRecoveryScanner =
    SessionRecoveryScanner(SecureSessionRecordStore(sessionsStore), clock)
