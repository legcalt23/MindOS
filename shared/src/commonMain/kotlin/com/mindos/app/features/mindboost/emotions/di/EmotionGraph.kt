@file:Suppress("MaxLineLength") // Dense DI wiring expressions.

package com.mindos.app.features.mindboost.emotions.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalytics
import com.mindos.app.features.mindboost.emotions.analytics.NoOpEmotionAnalytics
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecoveryScanner
import com.mindos.app.features.mindboost.emotions.data.local.EmotionRepository
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalReconciler
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseDefinition
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionTrialGenerator
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

/**
 * Self-contained dependency graph for MindBoost · Emociones. Mirrors `FocusGraph`/`SpeedGraph`:
 * interfaces (not concretes), factory beside the graph, local-first encrypted persistence
 * ([rememberSecureKeyValueStore]). Swapping storage changes only [rememberEmotionGraph], not call sites.
 */
@Stable
data class EmotionGraph(
    val generators: Map<EmotionExerciseId, EmotionTrialGenerator>,
    val metricsEngine: EmotionMetricsEngine,
    val assembler: EmotionSessionAssembler,
    val adaptiveEngine: EmotionAdaptiveEngine,
    val overloadDetector: OverloadDetector,
    val pauseGovernor: PauseGovernor,
    val feedbackBuilder: EmotionFeedbackBuilder,
    val repository: EmotionRepository,
    val epochClock: EpochClock,
    val monotonicClock: MonotonicClock,
    // MC8u-B+ §I: PII-free analytics sink. Stable no-op default (an `object`, never a per-instance) so adding it does
    // not break the graph's structural equality; production stays no-op until a real analytics backend is wired.
    val analytics: EmotionAnalytics = NoOpEmotionAnalytics,
) {
    fun definitionFor(id: EmotionExerciseId): EmotionExerciseDefinition = EmotionExerciseCatalog.definition(id)

    fun generatorFor(id: EmotionExerciseId): EmotionTrialGenerator =
        requireNotNull(generators[id]) { "No emotion generator registered for $id." }
}

/** Production wiring. Stateless engines are created once per feature mount. */
@Composable
fun rememberEmotionGraph(): EmotionGraph {
    val metricsEngine: EmotionMetricsEngine = remember { DefaultEmotionMetricsEngine() }
    val assembler = remember(metricsEngine) { EmotionSessionAssembler(metricsEngine) }
    val adaptiveEngine: EmotionAdaptiveEngine = remember { DefaultEmotionAdaptiveEngine() }
    val overloadDetector: OverloadDetector = remember { DefaultOverloadDetector() }
    val pauseGovernor = remember { PauseGovernor() }
    val feedbackBuilder = remember { EmotionFeedbackBuilder() }
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Emotions)
    val repository: EmotionRepository = remember(secureStore) { LocalEmotionRepository(secureStore) }
    val monotonicClock: MonotonicClock = remember { SystemMonotonicClock() }
    return remember(metricsEngine, assembler, adaptiveEngine, overloadDetector, pauseGovernor, feedbackBuilder, repository, monotonicClock) {
        EmotionGraph(
            generators = EmotionExerciseGenerators.registry,
            metricsEngine = metricsEngine,
            assembler = assembler,
            adaptiveEngine = adaptiveEngine,
            overloadDetector = overloadDetector,
            pauseGovernor = pauseGovernor,
            feedbackBuilder = feedbackBuilder,
            repository = repository,
            epochClock = SystemEpochClock,
            monotonicClock = monotonicClock,
        )
    }
}

/**
 * Production write-ahead recorder for the Emociones session over the account-bound `mindboost_sessions`
 * namespace. Pure (non-Compose) factory so the wiring stays unit-testable, mirroring the other blocks. The
 * [SecureSessionRecorder] is total by contract — a storage failure never breaks a session — and only lifecycle
 * primitives (never emotions, self-report, safety reason, or scores) are written. Injected into the session
 * holder from `rememberEmotionSessionStateHolder` (MC8b).
 */
internal fun emotionSessionRecorder(sessionsStore: SecureKeyValueStore, clock: EpochClock): SessionRecorder =
    SecureSessionRecorder(SecureSessionRecordStore(sessionsStore), clock)

/**
 * Production orphan-recovery scanner for Emociones over `mindboost_sessions`. Pure factory (unit-testable);
 * marks STARTED/CHECKPOINT sessions stale by updatedAtMs (heartbeat) as ABANDONED and never touches FINALIZED,
 * scoring, or Emociones' own stores. Run once per Emociones entry — block-agnostic, safe, and idempotent.
 */
internal fun emotionSessionRecovery(sessionsStore: SecureKeyValueStore, clock: EpochClock): SessionRecoveryScanner =
    SessionRecoveryScanner(SecureSessionRecordStore(sessionsStore), clock)

/**
 * Durable-terminal reconciler for Emociones (MC8g). Retries pending terminals (never lose a completed session to a
 * save failure) and repairs each Emotions `SessionRecord` to match its canonical summary. Run once per entry,
 * BEFORE [emotionSessionRecovery], so a completed/safety-stop session can never be recovered as ABANDONED.
 */
internal fun emotionTerminalReconciler(
    repository: EmotionRepository,
    sessionsStore: SecureKeyValueStore,
    clock: EpochClock,
    userId: String,
): EmotionTerminalReconciler =
    EmotionTerminalReconciler(repository, SecureSessionRecordStore(sessionsStore), clock, userId)
