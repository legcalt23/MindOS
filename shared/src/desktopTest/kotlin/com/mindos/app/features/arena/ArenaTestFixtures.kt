package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.ActivityStateEnum
import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.MinorStateEnum
import com.mindos.app.features.arena.config.OsFamily
import com.mindos.app.features.arena.config.RefreshRateTier
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.config.VerificationStateEnum
import com.mindos.app.features.arena.hardware.HardwareClass
import com.mindos.app.features.arena.rating.engine.RatingPosterior
import com.mindos.app.features.arena.rating.engine.TrialEvidence
import com.mindos.app.features.arena.rating.model.Confidence
import com.mindos.app.features.arena.rating.model.LatencyMs
import com.mindos.app.features.arena.rating.model.MatchQuality
import com.mindos.app.features.arena.rating.model.MeasurementQuality
import com.mindos.app.features.arena.rating.model.PlayerDefaultProfile
import com.mindos.app.features.arena.rating.model.PlayerRatingState
import com.mindos.app.features.arena.rating.model.Rating
import com.mindos.app.features.arena.rating.model.Sigma

/** Shared, synthetic-only fixtures for arena tests. No real identifiers anywhere. */
object ArenaTestFixtures {

    val ANDROID_120 = HardwareClass(OsFamily.ANDROID, RefreshRateTier.HZ_120)
    val IOS_120 = HardwareClass(OsFamily.IOS, RefreshRateTier.HZ_120)
    val ANDROID_60 = HardwareClass(OsFamily.ANDROID, RefreshRateTier.HZ_60)

    fun posterior(mu: Double = 1500.0, sigma: Double = 350.0): RatingPosterior =
        RatingPosterior(Rating(mu), Sigma(sigma))

    @Suppress("LongParameterList") // Mirrors the auditable evidence record; defaults keep call sites tiny.
    fun verifiedEvidence(
        motorType: TaskMotorType = TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
        taskDifficulty: Double = 1500.0,
        observedPerformance: Double = 0.75,
        integrityState: IntegrityStateEnum = IntegrityStateEnum.VERIFIED,
        ledgerApprovedCurrent: Boolean = true,
        comparability: ComparabilityStateEnum = ComparabilityStateEnum.COMPARABLE,
        measurementConfidence: ConfidenceStateEnum = ConfidenceStateEnum.SUFFICIENT,
        sessionValidity: SessionValidityStatusEnum = SessionValidityStatusEnum.VALID,
        measurementMultiplier: Double = 1.0,
        comparabilityMultiplier: Double = 1.0,
        modeScalingFactor: Double = 1.0,
        smurfSuspicion: SmurfSuspicionLevel = SmurfSuspicionLevel.NONE,
        hardwareClass: HardwareClass? = ANDROID_120,
    ): TrialEvidence = TrialEvidence(
        motorType = motorType,
        taskDifficulty = Rating(taskDifficulty),
        observedPerformance = observedPerformance,
        accuracy = Confidence(observedPerformance),
        latency = LatencyMs(320L),
        integrityState = integrityState,
        ledgerApprovedCurrent = ledgerApprovedCurrent,
        comparability = comparability,
        measurementConfidence = measurementConfidence,
        sessionValidity = sessionValidity,
        measurementMultiplier = MeasurementQuality(measurementMultiplier),
        comparabilityMultiplier = MatchQuality(comparabilityMultiplier),
        modeScalingFactor = modeScalingFactor,
        smurfSuspicion = smurfSuspicion,
        hardwareClass = hardwareClass,
    )

    fun playerRatingState(
        mu: Double = 1450.0,
        sigma: Double = 200.0,
        integrityState: IntegrityStateEnum = IntegrityStateEnum.VERIFIED,
    ): PlayerRatingState = PlayerRatingState(
        playerIdPseudonymized = "SYNTH_PLAYER_FIXTURE",
        globalRatingMu = Rating(mu),
        globalRatingSigma = Sigma(sigma),
        blockRatings = emptyMap(),
        modeRatings = emptyMap(),
        provisionalState = true,
        activityState = ActivityStateEnum.ACTIVE,
        verificationState = VerificationStateEnum.VERIFIED,
        integrityState = integrityState,
        minorState = MinorStateEnum.ADULT,
        regionState = "EU",
        defaultProfile = PlayerDefaultProfile(0L, Rating(1500.0), Sigma(350.0)),
        lastRatingUpdateTimestampMs = 0L,
        seasonId = "SEASON_1",
    )
}
