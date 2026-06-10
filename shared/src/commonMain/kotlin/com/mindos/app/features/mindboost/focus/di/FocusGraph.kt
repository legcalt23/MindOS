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
import com.mindos.app.features.mindboost.focus.domain.exercise.E1VigiaGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.domain.metrics.DefaultMetricsEngine
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricsEngine
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock

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
    val e1Definition: ExerciseDefinition,
    val e1Generator: TrialGenerator,
    val metricsEngine: MetricsEngine,
    val adaptiveEngine: AdaptiveEngine,
    val assembler: FocusSessionAssembler,
    val monotonicClock: MonotonicClock,
    val epochClock: EpochClock,
    val profileRepository: FocusProfileRepository,
    val historyRepository: SessionHistoryRepository,
)

/** Namespace for the focus feature's encrypted local store (keystore alias + prefs scope). */
private const val FOCUS_NAMESPACE = "mindboost_focus"

/** Production wiring. Stateless engines are created once per feature mount. */
@Composable
fun rememberFocusGraph(): FocusGraph {
    val metricsEngine: MetricsEngine = remember { DefaultMetricsEngine() }
    val adaptiveEngine: AdaptiveEngine = remember { DefaultAdaptiveEngine() }
    val assembler = remember(metricsEngine) { FocusSessionAssembler(metricsEngine) }
    val generator: TrialGenerator = remember { E1VigiaGenerator() }
    val monotonicClock: MonotonicClock = remember { SystemMonotonicClock() }
    val secureStore = rememberSecureKeyValueStore(FOCUS_NAMESPACE)
    val profileRepository: FocusProfileRepository =
        remember(secureStore) { LocalFocusProfileRepository(secureStore) }
    val historyRepository: SessionHistoryRepository =
        remember(secureStore) { LocalSessionHistoryRepository(secureStore) }
    return remember(
        metricsEngine, adaptiveEngine, assembler, generator, monotonicClock,
        profileRepository, historyRepository,
    ) {
        FocusGraph(
            e1Definition = ExerciseCatalog.E1_VIGIA,
            e1Generator = generator,
            metricsEngine = metricsEngine,
            adaptiveEngine = adaptiveEngine,
            assembler = assembler,
            monotonicClock = monotonicClock,
            epochClock = SystemEpochClock,
            profileRepository = profileRepository,
            historyRepository = historyRepository,
        )
    }
}
