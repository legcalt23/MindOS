@file:Suppress("MagicNumber") // Synthetic latency/duration and digest constants are documented below.

package com.mindos.app.features.arena.simulation

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.evidence.ledger.SimulationRunId
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.rating.engine.TrialEvidence
import com.mindos.app.features.arena.rating.model.Confidence
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.rating.model.MatchQuality
import com.mindos.app.features.arena.rating.model.MeasurementQuality
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Phase 07: Offline synthetic simulation harness.
 *
 * Generates a synthetic population, runs many seeded rating updates, and returns aggregate,
 * sanitized metrics. Pure and deterministic given the seed — no Firebase, analytics, network, or
 * real UIDs. Designed to process 100k+ matches well under the 5-minute budget.
 */
class SimulationHarness(
    private val engine: BayesianRatingEngine = BayesianRatingEngine(),
) {

    fun run(config: SimulationConfig): SimulationResult {
        val random = Random(config.seed.value)
        val runId = SimulationRunId.random(random)
        val profiles = SyntheticProfileGenerator(random)
            .generate(config.populationSize, config.motorType, config.hardwareClass)
        val posteriors = HashMap<String, RatingPosterior>(profiles.size)
        for (profile in profiles) posteriors[profile.id.value] = profile.initialPosterior

        var applied = 0
        var rejected = 0
        repeat(config.matchCount) {
            val profile = profiles[random.nextInt(profiles.size)]
            if (applyMatch(profile, config, posteriors)) applied++ else rejected++
        }

        return SimulationResult(
            runId = runId,
            matchesProcessed = config.matchCount,
            ratingUpdatesApplied = applied,
            ratingUpdatesRejected = rejected,
            averageMatchDurationMs = AVERAGE_MATCH_DURATION_MS,
            finalPosteriorDigest = digest(profiles, posteriors),
            syntheticProfileIds = profiles.map { it.id.value },
        )
    }

    private fun applyMatch(
        profile: SyntheticProfile,
        config: SimulationConfig,
        posteriors: MutableMap<String, RatingPosterior>,
    ): Boolean {
        val prior = posteriors.getValue(profile.id.value)
        val observed = engine.expectedTaskPerformance(
            profile.trueSkill.value,
            config.taskDifficulty.value,
        )
        val result = engine.update(prior, buildEvidence(observed, config))
        return when (result) {
            is RatingUpdateResult.Applied -> {
                posteriors[profile.id.value] = result.posterior
                true
            }
            is RatingUpdateResult.Rejected -> false
        }
    }

    private fun buildEvidence(observed: Double, config: SimulationConfig): TrialEvidence {
        val score = observed.coerceIn(0.0, 1.0)
        return TrialEvidence(
            motorType = config.motorType,
            taskDifficulty = config.taskDifficulty,
            observedPerformance = score,
            accuracy = Confidence(score),
            latency = LatencyMs(SYNTHETIC_LATENCY_MS),
            integrityState = IntegrityStateEnum.VERIFIED,
            ledgerApprovedCurrent = true,
            comparability = ComparabilityStateEnum.COMPARABLE,
            measurementConfidence = ConfidenceStateEnum.SUFFICIENT,
            sessionValidity = SessionValidityStatusEnum.VALID,
            measurementMultiplier = MeasurementQuality(1.0),
            comparabilityMultiplier = MatchQuality(1.0),
            modeScalingFactor = 1.0,
            smurfSuspicion = SmurfSuspicionLevel.NONE,
            hardwareClass = config.hardwareClass,
        )
    }

    private fun digest(
        profiles: List<SyntheticProfile>,
        posteriors: Map<String, RatingPosterior>,
    ): String {
        var hash = SEED_PRIME
        for (profile in profiles) {
            hash = HASH_MULT * hash + posteriors.getValue(profile.id.value).mean.value.roundToLong()
        }
        return hash.toString(RADIX_HEX)
    }

    companion object {
        private const val AVERAGE_MATCH_DURATION_MS = 45_000L
        private const val SYNTHETIC_LATENCY_MS = 350L
        private const val SEED_PRIME = 1_125_899_906_842_597L
        private const val HASH_MULT = 31L
        private const val RADIX_HEX = 16
    }
}
