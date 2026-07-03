package com.mindos.app.features.mindboost.profile.domain.application

import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.orchestration.MindBoostProfileOrchestrator
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import com.mindos.app.features.mindboost.profile.domain.repository.MindBoostProfileRepository
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource
import kotlinx.coroutines.CancellationException

/**
 * Application service tying the profile together: it pulls each module's coarse contribution, folds them
 * with the [MindBoostProfileAggregator], persists the snapshot, and exposes a training recommendation.
 *
 * A single module source failing must NOT break the whole profile — its failure is isolated and the
 * other modules still contribute (cancellation is rethrown, per the repo's structured-concurrency rule).
 * Time is injected via [EpochClock] (never `Clock.System` directly in domain code).
 */
class MindBoostProfileService(
    private val sources: List<CognitiveModuleProfileSource>,
    private val repository: MindBoostProfileRepository,
    private val aggregator: MindBoostProfileAggregator = MindBoostProfileAggregator(),
    private val orchestrator: MindBoostProfileOrchestrator = MindBoostProfileOrchestrator(),
    private val clock: EpochClock = SystemEpochClock,
) {
    /** The last persisted snapshot (instant; survives app restart on Android). Null if never computed. */
    suspend fun cached(userId: String): MindBoostProfile? = repository.load(userId)

    /** Recomputes the profile from live module signals, persists it, and returns it. */
    suspend fun refresh(userId: String): MindBoostProfile {
        val contributions = sources.mapNotNull { safeContribution(it, userId) }
        val profile = aggregator.aggregate(userId, contributions, clock.nowMillis())
        repository.save(profile)
        return profile
    }

    /** A single descriptive training suggestion, limited to the modules wired into this service. */
    fun recommend(profile: MindBoostProfile): TrainingRecommendation =
        orchestrator.recommend(profile, sources.map { it.moduleId }.toSet())

    @Suppress("SwallowedException") // The failing source is isolated; we log a redacted breadcrumb, not the throwable.
    private suspend fun safeContribution(
        source: CognitiveModuleProfileSource,
        userId: String,
    ): ModuleContribution? =
        try {
            source.contribution(userId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            ObservabilityHub.breadcrumb("mbprofile_source_failed:${source.moduleId.name}")
            null
        }
}
