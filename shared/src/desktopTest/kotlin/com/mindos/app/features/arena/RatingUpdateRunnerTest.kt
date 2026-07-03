package com.mindos.app.features.arena

import com.mindos.app.features.arena.comparability.model.DeviceClass
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.simulation.generators.AbuseType
import com.mindos.app.features.arena.simulation.generators.SyntheticPlayer
import com.mindos.app.features.arena.simulation.runner.RatingUpdateRunner
import com.mindos.app.features.arena.simulation.runner.SessionGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 07 / P7.2: Session generator + rating-update runner across the full gate pipeline. */
class RatingUpdateRunnerTest {

    private val runner = RatingUpdateRunner()
    private val prior = ArenaTestFixtures.posterior(mu = 1500.0, sigma = 300.0)

    private fun player(abuse: AbuseType, device: DeviceClass = DeviceClass.HIGH_END_120HZ) =
        SyntheticPlayer(
            id = "P1",
            skillByBlock = mapOf("FOCUS" to 1650.0),
            deviceClass = device,
            abuseType = abuse,
            languageGroup = "EN",
            accessibilityClass = "COMPARABLE",
        )

    private fun session(abuse: AbuseType, device: DeviceClass = DeviceClass.HIGH_END_120HZ, seed: Long = 5) =
        SessionGenerator().generateSession(
            player(abuse, device), TaskMotorType.FOCUS_INTERFERENCE_CONTROL, "FOCUS", 1500.0, Random(seed),
        )

    @Test
    fun cleanSessionAppliesRatingUpdate() {
        val result = runner.runRatingUpdate(prior, session(AbuseType.CLEAN), ledgerApprovedCurrent = true)
        assertTrue(result.update is RatingUpdateResult.Applied, "state=${result.integrityState} comp=${result.comparability}")
        assertEquals(IntegrityStateEnum.VERIFIED, result.integrityState)
    }

    @Test
    fun colluderIsInvalidatedAndRejected() {
        val result = runner.runRatingUpdate(prior, session(AbuseType.COLLUDER), ledgerApprovedCurrent = true)
        assertTrue(result.update is RatingUpdateResult.Rejected)
        assertEquals(IntegrityStateEnum.INVALIDATED, result.integrityState)
    }

    @Test
    fun boosterIsHeldAndRejected() {
        val result = runner.runRatingUpdate(prior, session(AbuseType.BOOSTER), ledgerApprovedCurrent = true)
        assertTrue(result.update is RatingUpdateResult.Rejected)
        assertEquals(IntegrityStateEnum.HOLD, result.integrityState)
    }

    @Test
    fun lagSwitcherIsNotComparableAndRejected() {
        val result = runner.runRatingUpdate(prior, session(AbuseType.LAG_SWITCHER), ledgerApprovedCurrent = true)
        assertEquals(ComparabilityStateEnum.NOT_COMPARABLE, result.comparability)
        assertTrue(result.update is RatingUpdateResult.Rejected)
    }

    @Test
    fun smurfAppliesButKeepsSigmaHigh() {
        val result = runner.runRatingUpdate(prior, session(AbuseType.SMURF), ledgerApprovedCurrent = true)
        assertTrue(result.update is RatingUpdateResult.Applied)
        assertTrue(result.newPosterior.uncertainty.value >= prior.uncertainty.value, "sigma must not collapse")
    }

    @Test
    fun unapprovedLedgerRejectsEvenCleanSession() {
        val result = runner.runRatingUpdate(prior, session(AbuseType.CLEAN), ledgerApprovedCurrent = false)
        assertTrue(result.update is RatingUpdateResult.Rejected)
        assertEquals(prior, result.newPosterior)
    }

    @Test
    fun runnerIsReproducible() {
        val s = session(AbuseType.CLEAN)
        assertEquals(
            runner.runRatingUpdate(prior, s, true),
            runner.runRatingUpdate(prior, s, true),
        )
    }
}
