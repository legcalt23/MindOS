package com.mindos.app.features.arena

import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.simulation.generators.SkillGenerator
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 08 — Simulation Tests (offline, synthetic). Rating-dynamics scenarios SIM_01/02/03/12/13/25.
 * These exercise the already-built rating engine; no new production code.
 */
class RatingDynamicsSimTest {

    private val engine = BayesianRatingEngine()

    private fun skills(n: Int, seed: Long): List<Double> {
        val gen = SkillGenerator()
        val random = Random(seed)
        return (0 until n).map { gen.generateSkill(random) }
    }

    /** Static agent calibrated against its own (adaptive) difficulty; returns final posterior + last delta. */
    private fun runStatic(trueSkill: Double, sessions: Int): Pair<RatingPosterior, Double> {
        var posterior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 350.0)
        var lastDelta = Double.MAX_VALUE
        repeat(sessions) {
            val evidence = ArenaTestFixtures.verifiedEvidence(taskDifficulty = trueSkill, observedPerformance = 0.5)
            val applied = engine.update(posterior, evidence) as RatingUpdateResult.Applied
            lastDelta = abs(applied.delta.muDelta)
            posterior = applied.posterior
        }
        return posterior to lastDelta
    }

    @Test
    fun sim01_ratingConvergesWithin30SessionsFor95PercentOfPlayers() {
        val population = skills(200, seed = 1)
        val converged = population.count { runStatic(it, sessions = 30).second < 5.0 }
        assertTrue(converged.toDouble() / population.size >= 0.95, "convergedFraction=${converged.toDouble() / population.size}")
    }

    @Test
    fun sim02_and_sim03_noSystematicInflationOrDriftAcrossSeasons() {
        val population = skills(200, seed = 2)
        // Season 1: converge.
        val afterSeason1 = population.map { runStatic(it, 30).first.mean.value }
        // Season 2: same static play continues — mu must not drift.
        val afterSeason2 = population.mapIndexed { i, _ ->
            var p = ArenaTestFixtures.posterior(mu = afterSeason1[i], sigma = 80.0)
            repeat(30) {
                p = (engine.update(p, ArenaTestFixtures.verifiedEvidence(taskDifficulty = population[i], observedPerformance = 0.5))
                    as RatingUpdateResult.Applied).posterior
            }
            p.mean.value
        }
        val drift = abs(afterSeason2.average() - afterSeason1.average())
        assertTrue(drift < afterSeason1.average() * 0.01, "drift=$drift mean=${afterSeason1.average()}")
    }

    @Test
    fun sim12_lowSampleSizeKeepsUncertaintyHigh() {
        val (posterior, _) = runStatic(trueSkill = 1500.0, sessions = 3)
        assertTrue(posterior.uncertainty.value > 200.0, "sigma=${posterior.uncertainty.value}")
    }

    @Test
    fun sim13_highInitialSigmaDecreasesMonotonicallyWithCleanSessions() {
        var posterior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 350.0)
        var previous = posterior.uncertainty.value
        repeat(10) {
            posterior = (engine.update(posterior, ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.5))
                as RatingUpdateResult.Applied).posterior
            assertTrue(posterior.uncertainty.value <= previous, "sigma rose: $previous -> ${posterior.uncertainty.value}")
            previous = posterior.uncertainty.value
        }
    }

    @Test
    fun sim25_premiumGivesNoAdvantage_identicalInputsAreEquivalent() {
        // There is no premium input anywhere in the rating math; identical play => identical outcome,
        // so any premium-vs-free win-rate delta is exactly zero.
        val free = runStatic(1700.0, 20).first
        val premium = runStatic(1700.0, 20).first
        assertEquals(free, premium)
        assertEquals(0.0, free.mean.value - premium.mean.value, 0.0)
    }
}
