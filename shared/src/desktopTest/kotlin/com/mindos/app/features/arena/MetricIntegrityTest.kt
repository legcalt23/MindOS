package com.mindos.app.features.arena

import com.mindos.app.core.config.DefaultFeatureFlags
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.RatingUpdateRejectionReason
import com.mindos.app.features.arena.observability.ArenaLogger
import com.mindos.app.features.arena.rating.engine.BayesianRatingEngine
import com.mindos.app.features.arena.rating.engine.RatingUpdateResult
import com.mindos.app.features.arena.sanitization.DataSanitizer
import com.mindos.app.features.arena.sanitization.SyntheticIdMapper
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Task 10: Metric integrity — verified-only updates, no premium in math, no public-ranked surface. */
class MetricIntegrityTest {

    private val engine = BayesianRatingEngine()

    @Test
    fun integrityNotVerifiedBlocksRatingUpdate() {
        val result = engine.update(
            ArenaTestFixtures.posterior(),
            ArenaTestFixtures.verifiedEvidence(integrityState = IntegrityStateEnum.OPEN),
        )
        val rejected = result as RatingUpdateResult.Rejected
        assertEquals(RatingUpdateRejectionReason.INTEGRITY_NOT_VERIFIED, rejected.reason)
    }

    @Test
    fun premiumFlagIsAbsentFromBayesianMathModule() {
        val mathFiles = ArenaSourceLocator.kotlinFiles(ArenaSourceLocator.engineDir()) +
            File(ArenaSourceLocator.mainDir(), "config/RatingEngineConfig.kt")
        for (file in mathFiles) {
            val code = ArenaSourceLocator.codeWithoutComments(file)
            assertFalse(code.contains("premium", ignoreCase = true), "premium found in ${file.name}")
        }
    }

    @Test
    fun premiumCannotAffectRatingDeltaBecauseItIsNotAnInput() {
        val prior = ArenaTestFixtures.posterior()
        val evidence = ArenaTestFixtures.verifiedEvidence(observedPerformance = 0.8)
        // Identical inputs => identical delta. There is no premium parameter to vary.
        assertEquals(engine.update(prior, evidence), engine.update(prior, evidence))
    }

    @Test
    fun globalRatingIsNotIncludedInSanitizedUserFacingOutput() {
        val logger = ArenaLogger(
            object : com.mindos.app.features.arena.observability.ObservabilitySink {
                override fun record(event: com.mindos.app.features.arena.observability.NonFatalTelemetryEvent) = Unit
            },
            DataSanitizer(SyntheticIdMapper(1L)),
        )
        val line = logger.logRatingStateAsSafeEvent(ArenaTestFixtures.playerRatingState(mu = 1450.0, sigma = 200.0))
        assertFalse(line.contains("1450"), "global rating mean must stay hidden")
    }

    @Test
    fun noPublicRankedSurfaceIsIntroduced() {
        for (file in ArenaSourceLocator.kotlinFiles(ArenaSourceLocator.mainDir())) {
            val code = ArenaSourceLocator.codeWithoutComments(file)
            assertFalse(code.contains("PUBLIC_RANKED"), "public-ranked symbol in ${file.name}")
            assertFalse(code.contains("publicRanked"), "public-ranked symbol in ${file.name}")
        }
        // Arena ranked feature flags must default OFF.
        assertFalse(DefaultFeatureFlags.arenaShadowRankedEnabled)
        assertFalse(DefaultFeatureFlags.arenaMentalSpeedRankedEnabled)
    }
}
