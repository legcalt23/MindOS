package com.mindos.app.features.arena

import com.mindos.app.features.arena.comparability.model.DeviceClass
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.simulation.generators.AbuseType
import com.mindos.app.features.arena.simulation.generators.SyntheticPlayer
import com.mindos.app.features.arena.simulation.runner.RatingUpdateRunner
import com.mindos.app.features.arena.simulation.runner.SessionGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 08 — Simulation Tests (offline, synthetic). Integrity-abuse scenarios SIM_04..08, SIM_11, SIM_24.
 * Exercise the already-built integrity gate + full-gate runner; no new production code.
 */
class IntegrityAbuseSimTest {

    private val runner = RatingUpdateRunner()
    private val engine = BayesianRatingEngine()
    private val prior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 300.0)

    private fun session(abuse: AbuseType, device: DeviceClass = DeviceClass.HIGH_END_120HZ) =
        SessionGenerator().generateSession(
            SyntheticPlayer("P", mapOf("FOCUS" to 1650.0), device, abuse, "EN", "COMPARABLE"),
            TaskMotorType.FOCUS_INTERFERENCE_CONTROL, "FOCUS", 1500.0, Random(5),
        )

    private fun run(abuse: AbuseType) = runner.runRatingUpdate(prior, session(abuse), ledgerApprovedCurrent = true)

    @Test
    fun sim04_smurfAppliesButSigmaIsKeptHighForRecalibration() {
        val result = run(AbuseType.SMURF)
        assertTrue(result.update is RatingUpdateResult.Applied)
        assertTrue(result.newPosterior.uncertainty.value >= prior.uncertainty.value)
    }

    @Test
    fun sim05_boostingIsHeldAndBlocked() {
        assertEquals(IntegrityStateEnum.HOLD, run(AbuseType.BOOSTER).integrityState)
        assertTrue(run(AbuseType.BOOSTER).update is RatingUpdateResult.Rejected)
    }

    @Test
    fun sim06_collusionIsInvalidatedAndBlocked() {
        assertEquals(IntegrityStateEnum.INVALIDATED, run(AbuseType.COLLUDER).integrityState)
        assertTrue(run(AbuseType.COLLUDER).update is RatingUpdateResult.Rejected)
    }

    @Test
    fun sim07_accountSharingIsHeldAndBlocked() {
        assertEquals(IntegrityStateEnum.HOLD, run(AbuseType.ACCOUNT_SHARER).integrityState)
        assertTrue(run(AbuseType.ACCOUNT_SHARER).update is RatingUpdateResult.Rejected)
    }

    @Test
    fun sim08_latencyManipulationMakesSessionNotComparable() {
        val result = run(AbuseType.LAG_SWITCHER)
        assertEquals(ComparabilityStateEnum.NOT_COMPARABLE, result.comparability)
        assertTrue(result.update is RatingUpdateResult.Rejected)
    }

    @Test
    fun sim24_and_sim11_farmingLowConfidenceSessionsYieldsNoRatingGain() {
        // Trial / item farming = grinding low-quality (insufficient-confidence) sessions. The
        // insufficient-confidence gate rejects them, so the rating cannot be farmed upward.
        // NOTE: item-pool exposure / protected-pool leakage itself is NOT modeled offline (see risks).
        var posterior = prior
        repeat(50) {
            val result = engine.update(
                posterior,
                ArenaTestFixtures.verifiedEvidence(
                    measurementConfidence = ConfidenceStateEnum.INSUFFICIENT,
                    observedPerformance = 0.99,
                ),
            )
            assertTrue(result is RatingUpdateResult.Rejected)
            if (result is RatingUpdateResult.Applied) posterior = result.posterior
        }
        assertEquals(prior, posterior)
    }
}
