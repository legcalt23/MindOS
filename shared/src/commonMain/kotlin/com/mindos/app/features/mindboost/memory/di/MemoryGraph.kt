package com.mindos.app.features.mindboost.memory.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.memory.data.local.LocalDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalDeferredResultStore
import com.mindos.app.features.mindboost.memory.data.local.LocalPairsResultStore
import com.mindos.app.features.mindboost.memory.data.local.LocalRecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalSpanResultStore
import com.mindos.app.features.mindboost.memory.domain.adaptive.DefaultMemoryAdaptiveEngine
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveEngine
import com.mindos.app.features.mindboost.memory.domain.antiplateau.InterventionPlanner
import com.mindos.app.features.mindboost.memory.domain.antiplateau.PlateauDetector
import com.mindos.app.features.mindboost.memory.domain.content.MemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallScheduler
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex1SpanGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex3ListGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex4PairsGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.MemoryTrialGenerator
import com.mindos.app.features.mindboost.memory.domain.metrics.DefaultMemoryMetricsEngine
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricsEngine
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.personalization.ArchetypeInference
import com.mindos.app.features.mindboost.memory.domain.personalization.DefaultArchetypeInference
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredRecallRepository
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.MemoryProfileRepository
import com.mindos.app.features.mindboost.memory.domain.repository.MemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.domain.repository.PairsResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.RecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.domain.repository.SpanResultStore
import com.mindos.app.features.mindboost.memory.domain.reporting.MemoryFitnessCalculator
import com.mindos.app.features.mindboost.memory.domain.session.DefaultMemoryRecoveryPolicy
import com.mindos.app.features.mindboost.memory.domain.session.MemoryRecoveryPolicy
import com.mindos.app.features.mindboost.memory.domain.session.MemorySessionAssembler
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

/**
 * Self-contained dependency graph for MindBoost · Memoria (mirrors the project's
 * AppGraph / FocusGraph pattern: interfaces where they exist, factory beside the
 * graph, scoped to this feature so the root graph is not inflated).
 *
 * Persistence is local-first and encrypted on Android via the shared
 * [rememberSecureKeyValueStore]; desktop/iOS fall back to in-memory. Swapping
 * storage changes only [rememberMemoryGraph], not call sites. Tests build a
 * [MemoryGraph] directly with fakes — never [rememberMemoryGraph].
 */
@Stable
data class MemoryGraph(
    val content: MemoryContent,
    val generators: Map<MemoryExerciseId, MemoryTrialGenerator>,
    val metricsEngine: MemoryMetricsEngine,
    val adaptiveEngine: MemoryAdaptiveEngine,
    val assembler: MemorySessionAssembler,
    val recoveryPolicy: MemoryRecoveryPolicy,
    val deferredScheduler: DeferredRecallScheduler,
    val archetypeInference: ArchetypeInference,
    val plateauDetector: PlateauDetector,
    val interventionPlanner: InterventionPlanner,
    val fitnessCalculator: MemoryFitnessCalculator,
    val profileRepository: MemoryProfileRepository,
    val sessionHistoryRepository: MemorySessionHistoryRepository,
    val deferredRecallRepository: DeferredRecallRepository,
    val recentItemRegistryRepository: RecentItemRegistryRepository,
    val spanResultStore: SpanResultStore,
    val pairsResultStore: PairsResultStore,
    val deferredResultStore: DeferredResultStore,
    val epochClock: EpochClock,
) {
    /** The trial generator for [id], or null if that exercise is not implemented yet. */
    fun generatorFor(id: MemoryExerciseId): MemoryTrialGenerator? = generators[id]
}

/** The V1 implemented trial generators, keyed by exercise (EX1/EX3/EX4). */
private fun implementedGenerators(): Map<MemoryExerciseId, MemoryTrialGenerator> = mapOf(
    MemoryExerciseId.EX1_SPAN_COMPLEJO to Ex1SpanGenerator(),
    MemoryExerciseId.EX3_LISTA_ESTRATEGIA to Ex3ListGenerator(),
    MemoryExerciseId.EX4_PARES_ASOCIADOS to Ex4PairsGenerator(),
)

/** Production wiring. Stateless engines are created once per feature mount. */
@Composable
fun rememberMemoryGraph(): MemoryGraph {
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Memory)
    return remember(secureStore) {
        MemoryGraph(
            content = defaultMemoryContent(),
            generators = implementedGenerators(),
            metricsEngine = DefaultMemoryMetricsEngine(),
            adaptiveEngine = DefaultMemoryAdaptiveEngine(),
            assembler = MemorySessionAssembler(),
            recoveryPolicy = DefaultMemoryRecoveryPolicy(),
            deferredScheduler = DeferredRecallScheduler(),
            archetypeInference = DefaultArchetypeInference(),
            plateauDetector = PlateauDetector(),
            interventionPlanner = InterventionPlanner(),
            fitnessCalculator = MemoryFitnessCalculator(),
            profileRepository = LocalMemoryProfileRepository(secureStore),
            sessionHistoryRepository = LocalMemorySessionHistoryRepository(secureStore),
            deferredRecallRepository = LocalDeferredRecallRepository(secureStore),
            recentItemRegistryRepository = LocalRecentItemRegistryRepository(secureStore),
            spanResultStore = LocalSpanResultStore(secureStore),
            pairsResultStore = LocalPairsResultStore(secureStore),
            deferredResultStore = LocalDeferredResultStore(secureStore),
            epochClock = SystemEpochClock,
        )
    }
}

/**
 * Production write-ahead recorder for Memoria-diferido over the account-bound `mindboost_sessions` namespace. A
 * pure (non-Compose) factory so the wiring stays unit-testable, mirroring `logicSessionRecorder` /
 * `creativitySessionRecorder`. The [SecureSessionRecorder] is total by contract — a storage failure never breaks
 * a recall session — and only lifecycle primitives (never words, lures, or selections) are written. Injected
 * into the deferred-recall session holder from `rememberDeferredRecallSessionStateHolder` (MC4b).
 */
internal fun deferredRecallSessionRecorder(sessionsStore: SecureKeyValueStore, clock: EpochClock): SessionRecorder =
    SecureSessionRecorder(SecureSessionRecordStore(sessionsStore), clock)
