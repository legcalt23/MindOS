package com.mindos.app.features.mindboost.logic.di

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
import com.mindos.app.features.mindboost.logic.data.local.LocalLogicTrainingRepository
import com.mindos.app.features.mindboost.logic.domain.adaptive.DefaultLogicDifficultyAdapter
import com.mindos.app.features.mindboost.logic.domain.adaptive.DifficultyAdapter
import com.mindos.app.features.mindboost.logic.domain.content.DefaultLogicContentProvider
import com.mindos.app.features.mindboost.logic.domain.content.LogicContentProvider
import com.mindos.app.features.mindboost.logic.domain.feedback.DefaultLogicFeedbackEngine
import com.mindos.app.features.mindboost.logic.domain.feedback.FeedbackEngine
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import com.mindos.app.features.mindboost.logic.domain.scoring.DefaultReasoningScorer
import com.mindos.app.features.mindboost.logic.domain.scoring.ReasoningScorer
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

@Stable
data class LogicGraph(
    val contentProvider: LogicContentProvider,
    val scorer: ReasoningScorer,
    val difficultyAdapter: DifficultyAdapter,
    val feedbackEngine: FeedbackEngine,
    val repository: LogicTrainingRepository,
    val epochClock: EpochClock,
)

@Composable
fun rememberLogicGraph(): LogicGraph {
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Logic)
    return remember(secureStore) {
        LogicGraph(
            contentProvider = DefaultLogicContentProvider(),
            scorer = DefaultReasoningScorer(),
            difficultyAdapter = DefaultLogicDifficultyAdapter(),
            feedbackEngine = DefaultLogicFeedbackEngine(),
            repository = LocalLogicTrainingRepository(secureStore),
            epochClock = SystemEpochClock,
        )
    }
}

/**
 * Production write-ahead recorder for Lógica over the account-bound `mindboost_sessions` namespace. A pure
 * (non-Compose) factory so the production wiring stays unit-testable. The [SecureSessionRecorder] is total by
 * contract — a storage failure never breaks a training session. Injected into the Lógica session holder from
 * `rememberLogicStateHolder` (MC2b); the holder's own default stays `NoOpSessionRecorder`.
 *
 * [subject] MUST be the current authenticated user's id (SYSTEM-1 slice 6-R3, §14 fold-in): `mindboost_sessions`
 * is one physical namespace shared by every locally-authenticated account on the device, so binding [subject]
 * at construction is what keeps this recorder's writes scoped to ITS OWN user.
 */
internal fun logicSessionRecorder(
    sessionsStore: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    clock: EpochClock,
    subject: String,
): SessionRecorder = SecureSessionRecorder(
    SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = subject),
    clock,
)

/**
 * Production orphan-recovery scanner for Lógica over `mindboost_sessions`. Pure factory (unit-testable); marks
 * STARTED/CHECKPOINT sessions stale by updatedAtMs (heartbeat) as ABANDONED and never touches FINALIZED,
 * scoring, or other blocks. Run once per Lógica track entry from the root screen (MC2c).
 *
 * [subject] MUST be the current authenticated user's id — see [logicSessionRecorder]'s KDoc. Binding it here is
 * what makes the scanner see (and only ever mark ABANDONED) THIS user's own orphaned rows.
 */
internal fun logicSessionRecovery(
    sessionsStore: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    clock: EpochClock,
    subject: String,
): SessionRecoveryScanner = SessionRecoveryScanner(
    SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = subject),
    clock,
)
