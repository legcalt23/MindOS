package com.mindos.app.features.mindboost.focus.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.core.runtime.PlatformSeedSource
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.session.SecureSessionRecordStore
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecoveryScanner
import com.mindos.app.features.mindboost.focus.data.local.LocalBaselineRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalProbeBankRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalSessionHistoryRepository
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.engine.DefaultFocusEngine
import com.mindos.app.features.mindboost.focus.domain.engine.FocusEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineAssembler
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocol
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocols
import com.mindos.app.features.mindboost.focus.domain.evaluation.CalibrationEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.DefaultCalibrationEngine
import com.mindos.app.features.mindboost.focus.domain.evaluation.FocusEvaluationService
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.exercise.VersionedTrialGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
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
    /**
     * UNMIGRATED generators wired to the live UI, indexed by id (see [FocusExerciseGenerators.legacy]).
     *
     * Cannot contain a migrated exercise: [VersionedTrialGenerator] is not a [TrialGenerator], so the build
     * fails. That is what keeps E2 off the bare-`Long` path everywhere at once, rather than at the call sites
     * someone remembered to check.
     */
    val generators: Map<ExerciseId, TrialGenerator>,
    /** MIGRATED generators (see [FocusExerciseGenerators.versioned]). The only ones the engine can build. */
    val versionedGenerators: Map<ExerciseId, VersionedTrialGenerator>,
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
    /**
     * Mints session seeds. Injected here rather than called as a platform singleton from Compose, so a test can
     * pin a deterministic sequence — and so the UI cannot mint an identity the engine never sees.
     */
    val seedSource: SeedSource = PlatformSeedSource,
    /**
     * The versioned session authority (ENG-01). Defaults to an engine over THIS graph's own
     * [versionedGenerators] and [seedSource], so a graph can never hand the holder an engine that builds forms
     * from a different registry, or mints from a different source, than the one it exposes. Substitutable so a
     * test can spy on it.
     */
    val focusEngine: FocusEngine = DefaultFocusEngine(versionedGenerators, seedSource),
) {
    /** Catalogue definition for [id] (single source of truth: [ExerciseCatalog]). */
    fun definitionFor(id: ExerciseId): ExerciseDefinition = ExerciseCatalog.definition(id)

    /** Registered legacy generator for [id]; fails fast if an unmigrated id is exposed without a generator. */
    fun generatorFor(id: ExerciseId): TrialGenerator =
        requireNotNull(generators[id]) { "No legacy focus generator registered for $id." }

    /**
     * The form for one FIXED-difficulty block (baseline / evaluation probe) — the single place those two routes
     * resolve a generator.
     *
     * It exists because the baseline and probe routes used to call `generator.generate(difficulty, spec, seed)`
     * on a `TrialGenerator` resolved from one map, which silently took E2 down the unversioned path on exactly
     * the two measurement-relevant routes. Here the branch is on the registry an exercise is actually in, and
     * since E2 cannot type-check into [generators], its versioned branch is not a convention — it is the only
     * branch that compiles for it.
     *
     * Honest scope: this gives the fixed-block routes a *versioned* draw. It does not give them a persisted
     * identity, and their seed is still a wall-clock value the caller mints and never stores — so baseline and
     * probe forms remain non-reproducible after the fact. That gap is real, pre-existing, and declared; closing
     * it is the same work as `createSession`, applied to those routes, and is not in this slice.
     */
    fun fixedBlockForm(
        id: ExerciseId,
        difficulty: DifficultyVector,
        spec: BlockSpec,
        seed: SessionSeed,
    ): List<Stimulus> {
        versionedGenerators[id]?.let { return it.generate(difficulty, spec, seed) }
        @Suppress("DEPRECATION") // The 12 unmigrated exercises have no other path; migrating one removes this.
        return generatorFor(id).generate(difficulty, spec, seed.value)
    }
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
            generators = FocusExerciseGenerators.legacy,
            versionedGenerators = FocusExerciseGenerators.versioned,
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
 *
 * [subject] MUST be the current authenticated user's id (SYSTEM-1 slice 6-R3, §14 fold-in): `mindboost_sessions`
 * is one physical namespace shared by every locally-authenticated account on the device, so binding [subject]
 * at construction is what keeps this recorder's writes scoped to ITS OWN user.
 */
internal fun focusSessionRecorder(
    sessionsStore: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    clock: EpochClock,
    subject: String,
): SessionRecorder = SecureSessionRecorder(
    SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = subject),
    clock,
)

/**
 * Production orphan-recovery scanner for Enfoque over `mindboost_sessions`. Pure factory (unit-testable); marks
 * STARTED/CHECKPOINT sessions stale by updatedAtMs (heartbeat) as ABANDONED and never touches FINALIZED,
 * scoring, or Enfoque's own stores. Run once per Enfoque entry — block-agnostic, safe, and idempotent.
 *
 * [subject] MUST be the current authenticated user's id — see [focusSessionRecorder]'s KDoc. Binding it here is
 * what makes the scanner see (and only ever mark ABANDONED) THIS user's own orphaned rows.
 */
internal fun focusSessionRecovery(
    sessionsStore: SecureKeyValueStore,
    revocationStore: SecureKeyValueStore,
    clock: EpochClock,
    subject: String,
): SessionRecoveryScanner = SessionRecoveryScanner(
    SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = subject),
    clock,
)
