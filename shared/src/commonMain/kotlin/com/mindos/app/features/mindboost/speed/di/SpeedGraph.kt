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
import com.mindos.app.features.mindboost.speed.data.persistence.ExerciseTelemetryStore
import com.mindos.app.features.mindboost.speed.data.persistence.SecureExerciseTelemetryStore
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
    val telemetryStore: ExerciseTelemetryStore,
    val clock: EpochClock,
    val thresholds: MetricThresholds,
)

/** Wiring de producción. Los stores cifrados se crean una vez por montaje del feature (mismo namespace `Speed`). */
@Composable
fun rememberSpeedGraph(): SpeedGraph {
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Speed)
    val store: SpeedMeasurementStore = remember(secureStore) { SecureSpeedMeasurementStore(secureStore) }
    // MC9-C-R2: E5 telemetry summaries live in the SAME `mindboost_speed` namespace (prefix `tele:`), so they are
    // already covered by the account-deletion eraser; no new namespace and no TrialRecord migration.
    val telemetryStore: ExerciseTelemetryStore = remember(secureStore) { SecureExerciseTelemetryStore(secureStore) }
    return remember(store, telemetryStore) {
        SpeedGraph(
            store = store,
            telemetryStore = telemetryStore,
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
