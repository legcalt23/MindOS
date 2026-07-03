@file:Suppress("MagicNumber") // Default difficulty / digest constants are documented simulation defaults.

package com.mindos.app.features.arena.simulation.runner

import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import com.mindos.app.features.arena.simulation.generators.AbuseType
import com.mindos.app.features.arena.simulation.generators.PlayerPopulationGenerator
import com.mindos.app.features.arena.simulation.generators.PopulationConfig
import com.mindos.app.features.arena.simulation.generators.SeedDeriver
import kotlin.math.roundToLong
import kotlin.random.Random

/** Phase 07: Reproducible simulation run configuration (a master seed derives all sub-seeds). */
data class SimulationRunConfig(
    val masterSeed: Long,
    val population: PopulationConfig,
    val sessionsPerPlayer: Int,
    val taskDifficulty: Double = 1500.0,
) {
    init {
        require(sessionsPerPlayer >= 0) { "sessionsPerPlayer must be >= 0" }
    }
}

/** Phase 07: Aggregate, sanitizable simulation report — no raw identifiers, only synthetic aggregates. */
data class SimulationReport(
    val populationSize: Int,
    val sessionsProcessed: Int,
    val updatesApplied: Int,
    val updatesRejected: Int,
    val cleanAppliedRate: Double,
    val abusersDetectedRate: Double,
    val resultDigest: String,
)

/**
 * Phase 07: Offline simulation runner. Generates a synthetic population and runs each player's
 * sessions through the full gate pipeline ([RatingUpdateRunner]), accumulating aggregate metrics.
 * Deterministic for a given master seed; no real UIDs, no I/O. Designed to process 100k+ sessions
 * well under the time budget.
 */
class SimulationRunner(
    private val populationGenerator: PlayerPopulationGenerator = PlayerPopulationGenerator(),
    private val sessionGenerator: SessionGenerator = SessionGenerator(),
    private val updateRunner: RatingUpdateRunner = RatingUpdateRunner(),
) {

    fun run(config: SimulationRunConfig): SimulationReport {
        val random = Random(SeedDeriver.derive(config.masterSeed, SESSION_STREAM))
        val population = populationGenerator
            .generatePopulation(config.population, SeedDeriver.derive(config.masterSeed, POPULATION_STREAM))
        val tally = Tally()
        var muAccumulator = 0L

        for (player in population) {
            var posterior = RatingPosterior(Rating(1500.0), Sigma(350.0))
            repeat(config.sessionsPerPlayer) {
                val session = sessionGenerator.generateSession(
                    player, TaskMotorType.FOCUS_INTERFERENCE_CONTROL, "FOCUS", config.taskDifficulty, random,
                )
                val result = updateRunner.runRatingUpdate(posterior, session, ledgerApprovedCurrent = true)
                posterior = result.newPosterior
                tally.record(player.abuseType, result.update is RatingUpdateResult.Applied)
            }
            muAccumulator += posterior.mean.value.roundToLong()
        }

        return tally.toReport(population.size, digest(muAccumulator, tally))
    }

    private fun digest(muAccumulator: Long, tally: Tally): String {
        var hash = SEED_PRIME
        hash = HASH_MULT * hash + muAccumulator
        hash = HASH_MULT * hash + tally.sessions
        hash = HASH_MULT * hash + tally.applied
        return hash.toString(RADIX_HEX)
    }

    /** Mutable accumulator kept private to the runner; never escapes. */
    private class Tally {
        var sessions = 0L
        var applied = 0L
        var rejected = 0L
        var cleanSessions = 0L
        var cleanApplied = 0L
        var abuseSessions = 0L
        var abuseDetected = 0L

        fun record(abuseType: AbuseType, wasApplied: Boolean) {
            sessions++
            if (wasApplied) applied++ else rejected++
            if (abuseType == AbuseType.CLEAN) {
                cleanSessions++
                if (wasApplied) cleanApplied++
            } else {
                abuseSessions++
                if (!wasApplied) abuseDetected++
            }
        }

        fun toReport(populationSize: Int, resultDigest: String): SimulationReport = SimulationReport(
            populationSize = populationSize,
            sessionsProcessed = sessions.toInt(),
            updatesApplied = applied.toInt(),
            updatesRejected = rejected.toInt(),
            cleanAppliedRate = if (cleanSessions > 0) cleanApplied.toDouble() / cleanSessions else 0.0,
            abusersDetectedRate = if (abuseSessions > 0) abuseDetected.toDouble() / abuseSessions else 0.0,
            resultDigest = resultDigest,
        )
    }

    companion object {
        private const val POPULATION_STREAM = 1L
        private const val SESSION_STREAM = 2L
        private const val SEED_PRIME = 1_125_899_906_842_597L
        private const val HASH_MULT = 31L
        private const val RADIX_HEX = 16
    }
}
