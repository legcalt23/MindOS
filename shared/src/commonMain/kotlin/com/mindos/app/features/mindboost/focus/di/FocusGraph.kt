package com.mindos.app.features.mindboost.focus.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.focus.data.local.LocalBaselineRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalProbeBankRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalSessionHistoryRepository
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineAssembler
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocol
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocols
import com.mindos.app.features.mindboost.focus.domain.evaluation.CalibrationEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.DefaultCalibrationEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricsEngine
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultArchetypeInference
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultOvertrainingGuard
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultPersonalizedSessionScorer
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultProfileEvolution
import com.mindos.app.features.mindboost.focus.domain.personalization.OvertrainingGuard
import com.mindos.app.features.mindboost.focus.domain.personalization.PersonalizedSessionScorer
import com.mindos.app.features.mindboost.focus.domain.personalization.ProfileEvolution
import com.mindos.app.features.mindboost.focus.domain.repository.BaselineRepository
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.ProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

/**
 * Self-contained dependency graph for the MindBoost · Enfoque feature. Mirrors
 * the project's [com.mindos.app.core.di.AppGraph] pattern (interfaces, not
 * concretes; factory beside the graph) but stays scoped to this feature so the
 * root graph is not inflated.
 *
 * Persistence is local-first and encrypted on Android ([rememberSecureKeyValueStore]);
 * desktop/iOS fall back to an in-memory store. Swapping storage changes only
 * [rememberFocusGraph], not call sites.
 */
@Stable
data class FocusGraph(
    /** Generators wired to the live UI, indexed by id (see [FocusExerciseGenerators]). */
    val generators: Map<ExerciseId, TrialGenerator>,
    val metricsEngine: MetricsEngine,
    val adaptiveEngine: AdaptiveEngine,
    val assembler: FocusSessionAssembler,
    val monotonicClock: MonotonicClock,
    val epochClock: EpochClock,
    val profileEvolution: ProfileEvolution,
    val overtrainingGuard: OvertrainingGuard,
    val personalizedScorer: PersonalizedSessionScorer,
    val calibrationEngine: CalibrationEngine,
    val baselineProtocol: BaselineProtocol,
    val baselineAssembler: BaselineAssembler,
    val evaluationService: FocusEvaluationService,
    val profileRepository: FocusProfileRepository,
    val historyRepository: SessionHistoryRepository,
    val baselineRepository: BaselineRepository,
    val probeBankRepository: ProbeBankRepository,
) {
    /** Catalogue definition for [id] (single source of truth: [ExerciseCatalog]). */
    fun definitionFor(id: ExerciseId): ExerciseDefinition = ExerciseCatalog.definition(id)

    /** Registered generator for [id]; fails fast if an id is exposed without a generator. */
    fun generatorFor(id: ExerciseId): TrialGenerator =
        requireNotNull(generators[id]) { "No focus generator registered for $id." }
}

/** Production wiring. Stateless engines are created once per feature mount. */
@Composable
fun rememberFocusGraph(): FocusGraph {
    val metricsEngine: MetricsEngine = remember { DefaultMetricsEngine() }
    val adaptiveEngine: AdaptiveEngine = remember { DefaultAdaptiveEngine() }
    val assembler = remember(metricsEngine) { FocusSessionAssembler(metricsEngine) }
    val monotonicClock: MonotonicClock = remember { SystemMonotonicClock() }
    val profileEvolution: ProfileEvolution =
        remember { DefaultProfileEvolution(DefaultArchetypeInference(), SystemEpochClock) }
    val overtrainingGuard: OvertrainingGuard = remember { DefaultOvertrainingGuard() }
    val personalizedScorer: PersonalizedSessionScorer = remember { DefaultPersonalizedSessionScorer() }
    val calibrationEngine: CalibrationEngine = remember { DefaultCalibrationEngine() }
    val baselineAssembler = remember(calibrationEngine) { BaselineAssembler(calibrationEngine) }
    val evaluationService = remember { FocusEvaluationService() }
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Focus)
    val profileRepository: FocusProfileRepository =
        remember(secureStore) { LocalFocusProfileRepository(secureStore) }
    val historyRepository: SessionHistoryRepository =
        remember(secureStore) { LocalSessionHistoryRepository(secureStore) }
    // Baseline snapshot + reserved-probe runs persist encrypted, kept apart from training history.
    val baselineRepository: BaselineRepository =
        remember(secureStore) { LocalBaselineRepository(secureStore) }
    val probeBankRepository: ProbeBankRepository =
        remember(secureStore) { LocalProbeBankRepository(secureStore) }
    return remember(
        metricsEngine, adaptiveEngine, assembler, monotonicClock, profileEvolution,
        overtrainingGuard, personalizedScorer, calibrationEngine, baselineAssembler, evaluationService,
        profileRepository, historyRepository, baselineRepository, probeBankRepository,
    ) {
        FocusGraph(
            generators = FocusExerciseGenerators.registry,
            metricsEngine = metricsEngine,
            adaptiveEngine = adaptiveEngine,
            assembler = assembler,
            monotonicClock = monotonicClock,
            epochClock = SystemEpochClock,
            profileEvolution = profileEvolution,
            overtrainingGuard = overtrainingGuard,
            personalizedScorer = personalizedScorer,
            calibrationEngine = calibrationEngine,
            baselineProtocol = BaselineProtocols.INITIAL,
            baselineAssembler = baselineAssembler,
            evaluationService = evaluationService,
            profileRepository = profileRepository,
            historyRepository = historyRepository,
            baselineRepository = baselineRepository,
            probeBankRepository = probeBankRepository,
        )
    }
}

/**
 * Production write-ahead recorder for the Enfoque session over the account-bound `mindboost_sessions`
 * namespace. Pure (non-Compose) factory so the wiring stays unit-testable, mirroring the other blocks. The
 * [SecureSessionRecorder] is total by contract — a storage failure never breaks a session — and only lifecycle
 * primitives (never stimuli, trials, RTs, or scores) are written. Injected into the session holder from
 * `rememberFocusSessionStateHolder` (MC7b).
 */
internal fun focusSessionRecorder(sessionsStore: SecureKeyValueStore, clock: EpochClock): SessionRecorder =
    SecureSessionRecorder(SecureSessionRecordStore(sessionsStore), clock)
