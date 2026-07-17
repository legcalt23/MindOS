package com.mindos.app.features.mindboost.creativity.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecoveryScanner
import com.mindos.app.features.mindboost.creativity.data.persistence.CreativitySessionStore
import com.mindos.app.features.mindboost.creativity.data.persistence.SecureCreativitySessionStore
import com.mindos.app.features.mindboost.creativity.domain.session.CreativitySessionCoordinator
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
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

/**
 * Production write-ahead recorder for Creatividad over the account-bound `mindboost_sessions` namespace. A pure
 * (non-Compose) factory so the wiring stays unit-testable, mirroring `speedSessionRecorder`/`logicSessionRecorder`.
 * The [SecureSessionRecorder] is total by contract — a storage failure never breaks a creativity round — and
 * only lifecycle primitives (never idea text or prompts) are written. Injected into
 * [com.mindos.app.features.mindboost.creativity.presentation.CreativitySessionStateHolder] from the root screen.
 *
 * [subject] MUST be the current authenticated user's id (SYSTEM-1 slice 6-R3, §14 fold-in): `mindboost_sessions`
 * is one physical namespace shared by every locally-authenticated account on the device, so binding [subject]
 * at construction is what keeps this recorder's writes scoped to ITS OWN user.
 */
internal fun creativitySessionRecorder(
    sessionsStore: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    clock: EpochClock,
    subject: String,
): SessionRecorder = SecureSessionRecorder(
    SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = subject),
    clock,
)

/**
 * Production orphan-recovery scanner for Creatividad over `mindboost_sessions`. Pure factory (unit-testable);
 * marks STARTED/CHECKPOINT sessions stale by updatedAtMs (heartbeat) as ABANDONED and never touches FINALIZED,
 * scoring, creative content, or other blocks. Run once per Creatividad root entry (MC3c). The scan is
 * block-agnostic over all mindboost_sessions — safe and idempotent, mirroring the Velocidad/Lógica recovery.
 *
 * [subject] MUST be the current authenticated user's id — see [creativitySessionRecorder]'s KDoc. Binding it
 * here is what makes the scanner see (and only ever mark ABANDONED) THIS user's own orphaned rows.
 */
internal fun creativitySessionRecovery(
    sessionsStore: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    clock: EpochClock,
    subject: String,
): SessionRecoveryScanner = SessionRecoveryScanner(
    SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = subject),
    clock,
)
