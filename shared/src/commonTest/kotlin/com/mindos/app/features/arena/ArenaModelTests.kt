package com.mindos.app.features.arena

import com.mindos.app.core.config.DefaultFeatureFlags
import com.mindos.app.features.arena.audit.model.RatingMutationAuditLog
import com.mindos.app.features.arena.config.ActivityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.MinorStateEnum
import com.mindos.app.features.arena.config.VerificationStateEnum
import com.mindos.app.features.arena.rating.model.Confidence
import com.mindos.app.features.arena.rating.model.ConfidenceInterval
import com.mindos.app.features.arena.rating.model.JitterMs
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.rating.model.PlayerDefaultProfile
import com.mindos.app.features.arena.rating.model.PlayerRatingState
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArenaModelTests {

    @Test
    fun testFeatureFlagsDefaultToOff() {
        // Validation: Feature flags must default to OFF for arena features
        assertEquals(false, DefaultFeatureFlags.arenaRatingEngineEnabled)
        assertEquals(false, DefaultFeatureFlags.arenaShadowRankedEnabled)
        assertEquals(false, DefaultFeatureFlags.arenaMentalSpeedRankedEnabled)
        assertEquals(false, DefaultFeatureFlags.arenaMatchmakingEnabled)
        assertEquals(false, DefaultFeatureFlags.arenaTrialsEnabled)
    }

    @Test
    fun testValueObjectValidation() {
        // Validation: Critical numeric concepts use value objects and enforce invariants
        
        // Rating
        val r = Rating(1500.0)
        assertEquals(1500.0, r.value)
        assertFailsWith<IllegalArgumentException> { Rating(Double.NaN) }

        // Sigma
        val s = Sigma(350.0)
        assertEquals(350.0, s.value)
        assertFailsWith<IllegalArgumentException> { Sigma(-1.0) }
        assertFailsWith<IllegalArgumentException> { Sigma(Double.NaN) }

        // Confidence
        val c = Confidence(0.95)
        assertEquals(0.95, c.value)
        assertFailsWith<IllegalArgumentException> { Confidence(-0.1) }
        assertFailsWith<IllegalArgumentException> { Confidence(1.1) }

        // Latency
        assertFailsWith<IllegalArgumentException> { LatencyMs(-5L) }
        
        // Jitter
        assertFailsWith<IllegalArgumentException> { JitterMs(-1L) }
        
        // Confidence Interval
        val ci = ConfidenceInterval(0.5, 0.8)
        assertEquals(0.5, ci.lowerBound)
        assertFailsWith<IllegalArgumentException> { ConfidenceInterval(0.8, 0.5) }
    }

    @Test
    fun testSerializationContracts() {
        // Validation: Data models must be serializable
        val ratingState = PlayerRatingState(
            playerIdPseudonymized = "SYNTH_PLAYER_123",
            globalRatingMu = Rating(1500.0),
            globalRatingSigma = Sigma(350.0),
            blockRatings = emptyMap(),
            modeRatings = emptyMap(),
            provisionalState = true,
            activityState = ActivityStateEnum.ACTIVE,
            verificationState = VerificationStateEnum.UNVERIFIED,
            integrityState = IntegrityStateEnum.OPEN,
            minorState = MinorStateEnum.ADULT,
            regionState = "EU",
            defaultProfile = PlayerDefaultProfile(123456789L, Rating(1500.0), Sigma(350.0)),
            lastRatingUpdateTimestampMs = 123456789L,
            seasonId = "SEASON_1"
        )

        val json = Json.encodeToString(ratingState)
        assertTrue(json.contains("SYNTH_PLAYER_123"))
        assertTrue(json.contains("1500.0")) // Inlined Rating
    }

    @Test
    fun testAuditLogBaselineConstruction() {
        // Validation: Base audit log construction enforces the base fields
        val auditLog = RatingMutationAuditLog(
            syntheticId = "AUDIT_123",
            syntheticReferenceId = "SESSION_456",
            timestampMs = 123456789L,
            configVersion = "BLEND_CONFIG_V1.0",
            decisionState = "OPEN",
            reasonCode = "STANDARD_UPDATE",
            sourceEngine = "RatingCoreEngine",
            previousRatingMu = 1500.0,
            newRatingMu = 1520.0,
            previousRatingSigma = 350.0,
            newRatingSigma = 340.0
        )
        
        assertEquals("AUDIT_123", auditLog.syntheticId)
        assertEquals("SESSION_456", auditLog.syntheticReferenceId)
        assertEquals("BLEND_CONFIG_V1.0", auditLog.configVersion)
    }
}
