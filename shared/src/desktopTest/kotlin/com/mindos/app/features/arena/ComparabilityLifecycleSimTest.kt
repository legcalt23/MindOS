package com.mindos.app.features.arena

import com.mindos.app.features.arena.comparability.engine.ComparabilityClassifier
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.hardware.IllegalMatchException
import com.mindos.app.features.arena.matchmaking.MatchmakingPoolState
import com.mindos.app.features.arena.matchmaking.SessionInit
import com.mindos.app.features.arena.rating.decay.RankDecay
import com.mindos.app.features.arena.rating.decay.SeasonConfig
import com.mindos.app.features.arena.rating.decay.SeasonReset
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.NonCompensatoryRank
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.rating.engine.SpecialistBadgeAssigner
import com.mindos.app.features.arena.rating.model.BlockRatingState
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import com.mindos.app.features.arena.rating.rollback.RollbackEngine
import com.mindos.app.features.arena.integrity.review.PostReviewResolver
import com.mindos.app.features.arena.integrity.review.ResolutionType
import com.mindos.app.features.arena.simulation.generators.AbuseType
import com.mindos.app.features.arena.simulation.generators.SyntheticPlayer
import com.mindos.app.features.arena.simulation.runner.RatingUpdateRunner
import com.mindos.app.features.arena.simulation.runner.SessionGenerator
import com.mindos.app.features.arena.comparability.model.DeviceClass
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Phase 08 — Simulation Tests (offline, synthetic). Comparability/device + lifecycle + ops scenarios
 * SIM_09/10/14/15/16/17/18/19/20/21/22/23. Exercise already-built engines; no new production code.
 */
class ComparabilityLifecycleSimTest {

    private val engine = BayesianRatingEngine()
    private val prior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 300.0)

    @Test
    fun sim09_mentalSpeedCannotBeRankedAcrossHardwareClasses() {
        val pool = MatchmakingPoolState(TaskMotorType.MENTAL_SPEED_CHOICE_REACTION)
        val android = SessionInit("S1", "P1", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120)
        val ios = SessionInit("S2", "P2", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.IOS_120)
        assertFailsWith<IllegalMatchException> { pool.requireMatch(android, ios) }
        assertTrue(pool.canMatch(android, android.copy(syntheticSessionId = "S3", syntheticPlayerId = "P3")))
    }

    @Test
    fun sim10_languageGroupDoesNotAffectRating() {
        fun run(language: String) = RatingUpdateRunner().runRatingUpdate(
            prior,
            SessionGenerator().generateSession(
                SyntheticPlayer("P", mapOf("FOCUS" to 1650.0), DeviceClass.HIGH_END_120HZ, AbuseType.CLEAN, language, "COMPARABLE"),
                TaskMotorType.FOCUS_INTERFERENCE_CONTROL, "FOCUS", 1500.0, Random(5),
            ),
            ledgerApprovedCurrent = true,
        )
        assertEquals(run("EN").newPosterior, run("ES").newPosterior)
    }

    @Test
    fun sim16_notComparableSessionsAreRejected() {
        assertEquals(ComparabilityStateEnum.NOT_COMPARABLE, ComparabilityClassifier().classifyLatency(LatencyMs(200)))
        val result = engine.update(prior, ArenaTestFixtures.verifiedEvidence(comparability = ComparabilityStateEnum.NOT_COMPARABLE))
        assertEquals(RatingUpdateRejectionReason.NOT_COMPARABLE, (result as RatingUpdateResult.Rejected).reason)
    }

    @Test
    fun sim17_partiallyComparableSessionsGetAReducedUpdate() {
        val full = engine.update(prior, ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.9, comparabilityMultiplier = 1.0))
        val partial = engine.update(
            prior,
            ArenaTestFixtures.verifiedEvidence(
                observedPerformance = 0.9,
                comparability = ComparabilityStateEnum.PARTIALLY_COMPARABLE,
                comparabilityMultiplier = 0.5,
            ),
        )
        val fullDelta = abs((full as RatingUpdateResult.Applied).delta.muDelta)
        val partialDelta = abs((partial as RatingUpdateResult.Applied).delta.muDelta)
        assertTrue(partialDelta < fullDelta, "partial=$partialDelta full=$fullDelta")
    }

    @Test
    fun sim14_falsePositiveResolutionRestoresRating() {
        val outcome = PostReviewResolver().resolve(ResolutionType.FALSE_POSITIVE_RESTORED, humanReviewed = true)
        assertTrue(outcome.ratingRestored)
    }

    @Test
    fun sim15_rollbackInvertsAPreviousUpdate() {
        val result = RollbackEngine().rollback("SESSION_1", originalDelta = 25.0, syntheticPlayerId = "P1", 1L, 2L)
        assertEquals(-25.0, result.invertedDelta, 1e-9)
    }

    @Test
    fun sim18_severeResolutionsRequireHumanReview() {
        assertFailsWith<IllegalStateException> {
            PostReviewResolver().resolve(ResolutionType.INVALIDATED_CONFIRMED, humanReviewed = false)
        }
    }

    @Test
    fun sim19_seasonResetCompressesButPreservesOrdering() {
        val reset = SeasonReset()
        val config = SeasonConfig(seasonId = "S2")
        val strong = reset.applySeasonReset(ArenaTestFixtures.playerRatingState(mu = 1900.0), config).globalRatingMu.value
        val weak = reset.applySeasonReset(ArenaTestFixtures.playerRatingState(mu = 1100.0), config).globalRatingMu.value
        assertTrue(strong > weak) // ordering preserved
        assertTrue((strong - weak) < (1900.0 - 1100.0)) // distribution compressed
    }

    @Test
    fun sim20_inactivityDecayInflatesSigmaNotMu() {
        val player = ArenaTestFixtures.playerRatingState(mu = 1600.0, sigma = 100.0)
        val decayed = RankDecay().applyRankDecay(player, daysSinceLastSession = 20)
        assertTrue(decayed.globalRatingSigma.value > 100.0)
        assertEquals(1600.0, decayed.globalRatingMu.value, 1e-9)
    }

    @Test
    fun sim21_specialistBadgeDoesNotElevateGlobalRank() {
        val blocks = mapOf(
            "Speed" to BlockRatingState("Speed", Rating(1800.0), Sigma(50.0), 0L),
            "Memory" to BlockRatingState("Memory", Rating(1300.0), Sigma(50.0), 0L),
        )
        assertEquals(listOf("Speed"), SpecialistBadgeAssigner().assignBadges(blocks).map { it.blockId })
        assertTrue(NonCompensatoryRank().calculateGlobalRank(blocks).globalMu < 1800.0)
    }

    @Test
    fun sim22_weakBlockBlocksElite() {
        val blocks = mapOf(
            "A" to BlockRatingState("A", Rating(1900.0), Sigma(50.0), 0L),
            "Weak" to BlockRatingState("Weak", Rating(1000.0), Sigma(50.0), 0L),
        )
        assertTrue(NonCompensatoryRank().calculateGlobalRank(blocks).eliteBlocked)
    }

    @Test
    fun sim23_unapprovedLedgerBlocksRating_itemPoolLeakageNotModeled() {
        // Protected item-pool leakage itself is NOT modeled offline (no item-exposure subsystem).
        // The available structural control is the ledger gate: an unapproved motor cannot affect rating.
        val result = engine.update(prior, ArenaTestFixtures.verifiedEvidence(ledgerApprovedCurrent = false))
        assertEquals(RatingUpdateRejectionReason.LEDGER_NOT_APPROVED_CURRENT, (result as RatingUpdateResult.Rejected).reason)
    }
}
