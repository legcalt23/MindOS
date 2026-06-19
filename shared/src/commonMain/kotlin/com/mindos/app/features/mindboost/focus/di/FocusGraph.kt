package com.mindos.app.features.mindboost.focus.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.focus.data.local.LocalFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalSessionHistoryRepository
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.adaptive.DefaultAdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.FocusExerciseGenerators
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricsEngine
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultArchetypeInference
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultOvertrainingGuard
import com.mindos.app.features.mindboost.focus.domain.personalization.DefaultProfileEvolution
import com.mindos.app.features.mindboost.focus.domain.personalization.OvertrainingGuard
import com.mindos.app.features.mindboost.focus.domain.personalization.ProfileEvolution
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
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
    val profileRepository: FocusProfileRepository,
    val historyRepository: SessionHistoryRepository,
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
    val secureStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Focus)
    val profileRepository: FocusProfileRepository =
        remember(secureStore) { LocalFocusProfileRepository(secureStore) }
    val historyRepository: SessionHistoryRepository =
        remember(secureStore) { LocalSessionHistoryRepository(secureStore) }
    return remember(
        metricsEngine, adaptiveEngine, assembler, monotonicClock, profileEvolution,
        overtrainingGuard, profileRepository, historyRepository,
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
            profileRepository = profileRepository,
            historyRepository = historyRepository,
        )
    }
}
