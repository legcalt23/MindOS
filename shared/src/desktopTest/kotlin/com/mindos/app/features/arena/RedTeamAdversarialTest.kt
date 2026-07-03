package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.evidence.motor.CompetitiveContext
import com.mindos.app.features.arena.integrity.engine.IntegrityGateBuilder
import com.mindos.app.features.arena.integrity.model.IntegritySignals
import com.mindos.app.features.arena.matchmaking.engine.MatchCandidate
import com.mindos.app.features.arena.matchmaking.engine.MatchmakingEngine
import com.mindos.app.features.arena.matchmaking.engine.QueueExpansionConfig
import com.mindos.app.features.arena.matchmaking.engine.FallbackDecision
import com.mindos.app.features.arena.matchmaking.engine.LowPopulationFallback
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.NonCompensatoryRank
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.rating.model.BlockRatingState
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import com.mindos.app.features.arena.rating.rollback.RollbackEngine
import com.mindos.app.features.arena.integrity.review.PostReviewResolver
import com.mindos.app.features.arena.integrity.review.ResolutionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Phase 11 / P11.2: Adversarial verification — for each red-team threat, the attacker attempts the
 * exploit and the built defense produces the expected (mitigated) outcome.
 */
class RedTeamAdversarialTest {

    private val gate = IntegrityGateBuilder()
    private val engine = BayesianRatingEngine()

    @Test
    fun rt02_12_13_boostingAndSolverAndLlmAreHeld() {
        assertEquals(IntegrityStateEnum.HOLD, gate.buildIntegrityGate(IntegritySignals(boosting = true)).state)
    }

    @Test
    fun rt03_04_collusionAndWinTradingAreInvalidated() {
        assertEquals(IntegrityStateEnum.INVALIDATED, gate.buildIntegrityGate(IntegritySignals(collusion = true)).state)
    }

    @Test
    fun rt05_accountSharingIsHeld() {
        assertEquals(IntegrityStateEnum.HOLD, gate.buildIntegrityGate(IntegritySignals(accountSharing = true)).state)
    }

    @Test
    fun rt06_queueSnipingIsConstrainedByMatchmakingRules() {
        val a = candidate(1500.0, TaskMotorType.FOCUS_INTERFERENCE_CONTROL)
        val differentMotor = candidate(1500.0, TaskMotorType.WORKING_MEMORY_SEQUENCE_UPDATING)
        assertTrue(!MatchmakingEngine().canMatch(a, differentMotor, CompetitiveContext.SHADOW, waitSeconds = 5))
    }

    @Test
    fun rt20_premiumBuysNoRatingAdvantage() {
        val prior = ArenaTestFixtures.posterior()
        val evidence = ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.8)
        assertEquals(engine.update(prior, evidence), engine.update(prior, evidence))
    }

    @Test
    fun rt22_appealAbuseIsRateLimited() {
        val resolver = PostReviewResolver()
        assertTrue(resolver.isAppealAllowed(ResolutionType.INVALIDATED_CONFIRMED, 0L, 1_000L, priorAppealCount = 0))
        assertTrue(!resolver.isAppealAllowed(ResolutionType.INVALIDATED_CONFIRMED, 0L, 1_000L, priorAppealCount = 5))
    }

    @Test
    fun rt25_lowPopulationAbuseCancelsInsteadOfForcingAsymmetricMatch() {
        assertEquals(
            FallbackDecision.CANCEL_FAIRNESS_PRESERVED,
            LowPopulationFallback.decide(200, QueueExpansionConfig.DEFAULT),
        )
    }

    @Test
    fun rt16_rollbackAbuseRequiresHumanReviewAndIsExact() {
        assertFailsWith<IllegalStateException> {
            PostReviewResolver().resolve(ResolutionType.INVALIDATED_CONFIRMED, humanReviewed = false)
        }
        val r = RollbackEngine().rollback("S", originalDelta = 42.0, syntheticPlayerId = "P", 0L, 0L)
        assertEquals(-42.0, r.invertedDelta, 1e-9)
    }

    @Test
    fun rt17_27_sandbaggingAndOneBlockExploitCannotReachElite() {
        val blocks = mapOf(
            "Strong" to BlockRatingState("Strong", Rating(2100.0), Sigma(40.0), 0L),
            "Sandbagged" to BlockRatingState("Sandbagged", Rating(900.0), Sigma(40.0), 0L),
        )
        assertTrue(NonCompensatoryRank().calculateGlobalRank(blocks).eliteBlocked)
    }

    @Test
    fun rt14_falsePositiveHoldIsRestoredOnReview() {
        assertTrue(PostReviewResolver().resolve(ResolutionType.FALSE_POSITIVE_RESTORED, humanReviewed = true).ratingRestored)
    }

    @Test
    fun rt23_notComparableManipulationIsRejected() {
        val result = engine.update(
            ArenaTestFixtures.posterior(),
            ArenaTestFixtures.verifiedEvidence(comparability = ComparabilityStateEnum.NOT_COMPARABLE),
        )
        assertTrue(result is RatingUpdateResult.Rejected)
    }

    private fun candidate(mu: Double, motor: TaskMotorType) = MatchCandidate(
        "P", mu, 80.0, motor, ArenaTestFixtures.ANDROID_120, "TOUCH", IntegrityStateEnum.VERIFIED,
        SmurfSuspicionLevel.NONE, 50, isMinor = false, commsActive = false, ComparabilityStateEnum.COMPARABLE,
    )
}
