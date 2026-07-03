package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import kotlin.math.abs
import kotlin.math.log10
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class BayesianRatingEngineTest {

    private val engine = BayesianRatingEngine()

    @Test
    fun verifiedEvidenceUpdatesRating() {
        val prior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 350.0)
        val result = engine.update(prior, ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.9))
        val applied = result as? RatingUpdateResult.Applied ?: fail("Expected Applied, got $result")
        assertTrue(applied.posterior.mean.value > prior.mean.value, "rating should rise for strong perf")
        assertNotEquals(prior, applied.posterior)
    }

    @Test
    fun unverifiedEvidenceDoesNotUpdateRating() {
        val prior = ArenaTestFixtures.posterior()
        for (state in IntegrityStateEnum.entries.filter { it != IntegrityStateEnum.VERIFIED }) {
            val result = engine.update(prior, ArenaTestFixtures.verifiedEvidence(integrityState = state))
            val rejected = result as? RatingUpdateResult.Rejected ?: fail("Expected Rejected for $state")
            assertEquals(RatingUpdateRejectionReason.INTEGRITY_NOT_VERIFIED, rejected.reason)
        }
    }

    @Test
    fun premiumCannotInfluenceBecauseItIsNotAnInput() {
        // The engine has no premium parameter; identical inputs must yield identical results.
        val prior = ArenaTestFixtures.posterior()
        val evidence = ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.66)
        assertEquals(engine.update(prior, evidence), engine.update(prior, evidence))
    }

    @Test
    fun staticSkillAgentConvergesWithin50Trials() {
        var posterior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 350.0)
        val evidence = ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.75, taskDifficulty = 1500.0)
        var lastDelta = Double.MAX_VALUE
        repeat(50) {
            val applied = engine.update(posterior, evidence) as RatingUpdateResult.Applied
            lastDelta = abs(applied.delta.muDelta)
            posterior = applied.posterior
        }
        val fixedPoint = 1500.0 - 400.0 * log10((1.0 - 0.75) / 0.75)
        assertTrue(abs(posterior.mean.value - fixedPoint) < 60.0, "mu=${posterior.mean.value}, target=$fixedPoint")
        assertTrue(posterior.uncertainty.value < 100.0, "sigma should shrink, was ${posterior.uncertainty.value}")
        assertTrue(lastDelta < 2.0, "should be stabilized, last delta=$lastDelta")
    }

    @Test
    fun ratingStateIsImmutableAfterUpdate() {
        val prior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 350.0)
        engine.update(prior, ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.95))
        assertEquals(1500.0, prior.mean.value)
        assertEquals(350.0, prior.uncertainty.value)
    }

    @Test
    fun expectedTaskPerformanceIsMonotonicInSkill() {
        val weak = engine.expectedTaskPerformance(1300.0, 1500.0)
        val strong = engine.expectedTaskPerformance(1700.0, 1500.0)
        assertTrue(weak < strong)
        assertEquals(0.5, engine.expectedTaskPerformance(1500.0, 1500.0), 1e-9)
    }

    @Test
    fun taskPerformanceDeltaIsObservedMinusExpected() {
        assertEquals(0.25, engine.taskPerformanceDelta(0.75, 0.50), 1e-9)
    }

    @Test
    fun sigmaShrinksOnValidUpdateButIsKeptHighWhenSmurfSuspicionIsHigh() {
        val prior = ArenaTestFixtures.posterior(sigma = 300.0)
        val normal = engine.update(prior, ArenaTestFixtures.verifiedEvidence()) as RatingUpdateResult.Applied
        assertTrue(normal.posterior.uncertainty.value < 300.0, "sigma should shrink on a clean update")

        val smurf = engine.update(
            prior,
            ArenaTestFixtures.verifiedEvidence(smurfSuspicion = SmurfSuspicionLevel.HIGH),
        ) as RatingUpdateResult.Applied
        assertTrue(smurf.posterior.uncertainty.value >= 300.0, "sigma must not collapse under smurf suspicion")
    }

    @Test
    fun eachHardGateRejectsWithItsTypedReason() {
        val prior = ArenaTestFixtures.posterior()
        val cases = mapOf(
            RatingUpdateRejectionReason.LEDGER_NOT_APPROVED_CURRENT to
                ArenaTestFixtures.verifiedEvidence(ledgerApprovedCurrent = false),
            RatingUpdateRejectionReason.NOT_COMPARABLE to
                ArenaTestFixtures.verifiedEvidence(comparability = ComparabilityStateEnum.NOT_COMPARABLE),
            RatingUpdateRejectionReason.INSUFFICIENT_MEASUREMENT_CONFIDENCE to
                ArenaTestFixtures.verifiedEvidence(measurementConfidence = ConfidenceStateEnum.INSUFFICIENT),
            RatingUpdateRejectionReason.SESSION_INVALID to
                ArenaTestFixtures.verifiedEvidence(sessionValidity = SessionValidityStatusEnum.INVALID),
        )
        for ((reason, evidence) in cases) {
            val result = engine.update(prior, evidence)
            val rejected = result as? RatingUpdateResult.Rejected ?: fail("Expected Rejected for $reason")
            assertEquals(reason, rejected.reason)
        }
    }
}
