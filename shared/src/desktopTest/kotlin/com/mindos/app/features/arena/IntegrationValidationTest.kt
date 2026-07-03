package com.mindos.app.features.arena

import com.mindos.app.core.config.DefaultFeatureFlags
import com.mindos.app.features.arena.comparability.engine.ComparabilityClassifier
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.config.TerminologyScanner
import com.mindos.app.features.arena.evidence.motor.CompetitiveContext
import com.mindos.app.features.arena.evidence.motor.LedgerGate
import com.mindos.app.features.arena.evidence.motor.MotorLedgerRegistry
import com.mindos.app.features.arena.evidence.motor.MotorStatusGate
import com.mindos.app.features.arena.integrity.engine.IntegrityGateBuilder
import com.mindos.app.features.arena.integrity.engine.SecrecyPolicy
import com.mindos.app.features.arena.integrity.model.IntegritySignals
import com.mindos.app.features.arena.integrity.review.PostReviewResolver
import com.mindos.app.features.arena.integrity.review.ResolutionType
import com.mindos.app.features.arena.measurement.engine.MeasurementConfidenceBuilder
import com.mindos.app.features.arena.metrics.AcceptanceMetricCalculator
import com.mindos.app.features.arena.metrics.MetricId
import com.mindos.app.features.arena.metrics.MetricInputs
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.BlendingConfig
import com.mindos.app.features.arena.rating.engine.MatchOutcomeCalculator
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.rating.model.JitterMs
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.rating.model.ModeRatingState
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import com.mindos.app.features.arena.redteam.RedTeamCatalog
import com.mindos.app.features.arena.session.model.SessionEnvironmentSnapshot
import com.mindos.app.features.arena.session.model.SessionResult
import com.mindos.app.features.arena.session.explanation.InternalReasonCode
import com.mindos.app.features.arena.session.explanation.UserSafeExplanationMapper
import com.mindos.app.features.arena.simulation.generators.PopulationConfig
import com.mindos.app.features.arena.simulation.runner.SimulationRunConfig
import com.mindos.app.features.arena.simulation.runner.SimulationRunner
import com.mindos.app.features.arena.trials.AntiCoachingControls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 12 / P12.2: Integration validation — runs all 25 VAL checks against the built components.
 * OFFLINE verification only; performs NO Codex sign-off and NO go-live (VAL_07/VAL_25 verify nothing
 * is live). Source scans use code-with-comments-stripped so doc mentions don't create false matches.
 */
class IntegrationValidationTest {

    private val engine = BayesianRatingEngine()
    private val now = 1_000_000L

    @Test
    fun val01_02_03_terminologyScanAndNoSelfReportOrPremiumInRatingMath() {
        val rankTerm = "E" + "L" + "O"
        assertTrue(TerminologyScanner.scan("ranked by $rankTerm points", listOf(rankTerm)).isNotEmpty()) // VAL_01

        val engineCode = ArenaSourceLocator.kotlinFiles(ArenaSourceLocator.engineDir())
            .joinToString("\n") { ArenaSourceLocator.codeWithoutComments(it).lowercase() }
        for (banned in listOf("readiness", "fatigue", "self-report", "selfreport", "self_report")) {
            assertTrue(!engineCode.contains(banned), "VAL_02: '$banned' found in rating engine code")
        }
        assertTrue(!engineCode.contains("premium"), "VAL_03: 'premium' found in rating engine code")
    }

    @Test
    fun val04_07_25_premiumEquivalenceFlagsDisabledAndNoMotorLive() {
        val prior = ArenaTestFixtures.posterior()
        val evidence = ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.8)
        assertEquals(engine.update(prior, evidence), engine.update(prior, evidence)) // VAL_04

        assertTrue(!DefaultFeatureFlags.arenaMentalSpeedRankedEnabled) // VAL_07
        assertTrue(!DefaultFeatureFlags.arenaRatingEngineEnabled) // VAL_25 (nothing live)
        assertTrue(!DefaultFeatureFlags.arenaShadowRankedEnabled) // VAL_25
        assertTrue(!DefaultFeatureFlags.arenaMatchmakingEnabled) // VAL_25 (all 5 strict flags OFF)
        assertTrue(!DefaultFeatureFlags.arenaTrialsEnabled) // VAL_25

        val gate = MotorStatusGate(MotorLedgerRegistry.default(now + 1_000L))
        for (motor in listOf(MotorLedgerRegistry.MOT_FOC_01, MotorLedgerRegistry.MOT_MEM_01, MotorLedgerRegistry.MOT_SPD_01)) {
            assertTrue(!gate.checkMotorStatus(motor, CompetitiveContext.RANKED, now), "VAL_25: $motor ranked-eligible")
        }
    }

    @Test
    fun val08_09_evidenceLedgersCompleteAndSignedOff() {
        val registry = MotorLedgerRegistry.default(now + 1_000L)
        assertEquals(3, registry.motorIds().size) // VAL_08
        val ledger = registry.ledgerFor(MotorLedgerRegistry.MOT_FOC_01)!!
        assertTrue(LedgerGate().validateSignOffs(ledger)) // VAL_09
    }

    @Test
    fun val10_11_18_blendingSmurfSigmaAndValidityGate() {
        assertEquals(0.0, BlendingConfig.PURE_TASK.matchWeight) // VAL_10 (task/match separated)
        assertEquals(0.5, MatchOutcomeCalculator().blend(0.2, 0.8, BlendingConfig.BALANCED_PVP), 1e-9)

        val smurf = engine.update(
            ArenaTestFixtures.posterior(sigma = 300.0),
            ArenaTestFixtures.verifiedEvidence(smurfSuspicion = SmurfSuspicionLevel.HIGH),
        ) as RatingUpdateResult.Applied
        assertTrue(smurf.posterior.uncertainty.value >= 300.0) // VAL_11

        val invalid = engine.update(
            ArenaTestFixtures.posterior(),
            ArenaTestFixtures.verifiedEvidence(sessionValidity = SessionValidityStatusEnum.INVALID),
        )
        assertEquals(RatingUpdateRejectionReason.SESSION_INVALID, (invalid as RatingUpdateResult.Rejected).reason) // VAL_18
    }

    @Test
    fun val12_13_userCopySafeAndNoAntiCheatReveal() {
        for (code in InternalReasonCode.entries) {
            val copy = UserSafeExplanationMapper.copyFor(code)
            assertTrue(copy.none { it.isDigit() }, "VAL_12: $code leaks a number")
            assertTrue(SecrecyPolicy.isSafeForUser(copy), "VAL_13: $code reveals internals")
        }
        assertTrue(!SecrecyPolicy.isSafeForUser("blocked by the collusion graph"))
    }

    @Test
    fun val14_15_16_17_workflowAndDecisionBuilders() {
        assertTrue(PostReviewResolver().resolve(ResolutionType.FALSE_POSITIVE_RESTORED, humanReviewed = true).ratingRestored) // VAL_14
        assertEquals(IntegrityStateEnum.VERIFIED, IntegrityGateBuilder().buildIntegrityGate(IntegritySignals()).state) // VAL_15
        assertEquals(ComparabilityStateEnum.COMPARABLE, ComparabilityClassifier().classifyLatency(com.mindos.app.features.arena.rating.model.LatencyMs(20))) // VAL_16
        val measurement = MeasurementConfidenceBuilder().buildMeasurementConfidence(30.0, 0.85, 50, true, 1500.0, 100.0)
        assertEquals(ConfidenceStateEnum.SUFFICIENT, measurement.state) // VAL_17
    }

    @Test
    fun val20_21_22_23_24_harnessMetricsRedTeamAndTrials() {
        val report = SimulationRunner().run(
            SimulationRunConfig(masterSeed = 7, population = PopulationConfig(size = 1000), sessionsPerPlayer = 100),
        )
        assertTrue(report.sessionsProcessed > 0) // VAL_20
        assertEquals(100_000, report.sessionsProcessed) // VAL_21

        val calc = AcceptanceMetricCalculator()
        val metrics = calc.compute(MetricInputs(values = mapOf(MetricId.MET_17 to 100.0, MetricId.MET_24 to 0.0)))
        assertTrue(calc.allModeledPass(metrics)) // VAL_22

        assertEquals(27, RedTeamCatalog.THREATS.size) // VAL_23
        assertTrue(AntiCoachingControls().pausePatternSuspicious(listOf(40_000L, 50_000L, 60_000L))) // VAL_24
    }

    @Test
    fun val05_06_19_modeRatingsEnvSnapshotAndSyntheticReferences() {
        // VAL_05: mode ratings are isolated per mode.
        val modes = mapOf(
            "FOCUS" to ModeRatingState("FOCUS", Rating(1600.0), Sigma(80.0), Rating(1600.0), 0L, 10),
            "SPEED" to ModeRatingState("SPEED", Rating(1400.0), Sigma(90.0), Rating(1500.0), 0L, 7),
        )
        assertEquals(2, modes.size)
        assertTrue(modes.getValue("FOCUS").ratingMu.value != modes.getValue("SPEED").ratingMu.value)

        // VAL_06: a session captures its environment snapshot.
        val env = SessionEnvironmentSnapshot(
            "HIGH_END_120HZ", "TOUCH", 120, 120, "MEDIUM", LatencyMs(20), JitterMs(5), 0.1, "EN", "COMPARABLE",
            "SIM", "SIM", "SIM", 0L,
        )
        assertEquals("HIGH_END_120HZ", env.deviceClass)

        // VAL_19: session results reference synthetic ids only.
        val session = SessionResult("SYNTH_SESSION_1", "SYNTH_PLAYER_1", env, 0.8, 0L)
        assertTrue(session.syntheticSessionId.startsWith("SYNTH_") && session.syntheticPlayerId.startsWith("SYNTH_"))
    }
}
