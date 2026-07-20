package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Test fixtures for [CognitiveSessionRun] and friends. Every default produces a VALID, cleanly-completed run so
 * a test can override exactly the one field it wants to exercise. Lives in desktopTest (JVM), which detekt does
 * not scan, so a wide defaulted factory needs no suppression.
 */
internal object CognitiveSessionRunFixtures {

    fun cleanTiming(): TimingIntegritySnapshot = TimingIntegritySnapshot(
        clockSource = ClockSource.FRAME_MONOTONIC,
        monotonicAvailable = true,
        backgroundedDuringSession = false,
        pauseResumeCount = 0,
        missedTickFlags = 0,
        suspectedInputLatency = false,
        integrityStatus = TimingIntegrityStatus.CLEAN,
    )

    fun validMetrics(): CognitiveSessionMetrics = CognitiveSessionMetrics(
        accuracy = SessionMetric.Measured(0.9),
        reactionTimeMedianMs = SessionMetric.Measured(400.0),
        reactionTimeMeanMs = SessionMetric.Measured(420.0),
        reactionTimeMinMs = SessionMetric.Measured(250.0),
        reactionTimeMaxMs = SessionMetric.Measured(800.0),
        reactionTimeVariabilityCv = SessionMetric.Measured(0.2),
    )

    fun validScore(): CognitiveSessionScore = CognitiveSessionScore(
        normalized = 0.8,
        category = ScoreState.VALID,
        scoringVersion = "1.1.0",
        preliminary = false,
        lowConfidence = false,
    )

    fun validEligibility(): SessionEligibility = SessionEligibility(
        profileEligible = true,
        calibrationEligible = true,
        rankingEligible = true,
        timingIntegrity = cleanTiming(),
        dataQualityCompromised = false,
    )

    /** An all-pass diagnostic trace with no rejection reasons — the companion of [validEligibility]. */
    fun validTrace(): EligibilityTrace = EligibilityTrace(
        integrityEligible = true,
        writeAheadEligible = true,
        durationEligible = true,
        purposeEligible = true,
        dataQualityEligible = true,
        officialUseEligible = true,
        rejectionReasons = emptyList(),
    )

    fun exercisePayload(): ExerciseSpecificMetricPayload = ExerciseSpecificMetricPayload(
        type = "gonogo_inhibition",
        schemaVersion = 1,
        payload = sampleJson(),
    )

    fun sampleJson(): JsonObject = buildJsonObject {
        put("commissions", 2)
        put("omissions", 1)
    }

    @Suppress("LongParameterList")
    fun validRun(
        sessionId: String = "sess_0123456789abcdef",
        blockId: ScoreModuleId = ScoreModuleId.SPEED,
        exerciseId: String = "e8",
        exerciseVersion: String = "1.0.0",
        scoringVersion: String = "1.1.0",
        sessionPurpose: SessionPurpose = SessionPurpose.TRAINING_FREE,
        startedAtEpochMs: Long = 1_000L,
        completedAtEpochMs: Long? = 121_000L,
        durationSelectedMs: Long? = null,
        durationActualMs: Long = 120_000L,
        completionStatus: SessionCompletionStatus = SessionCompletionStatus.COMPLETED,
        finalizationFailure: FinalizationFailureReason? = null,
        difficulty: DifficultyTrace = DifficultyTrace(0.5, 0.6, 0.7, interleaved = false, perBlock = emptyList()),
        trials: TrialAggregate = TrialAggregate(
            planned = 20,
            completed = 20,
            outcomes = listOf(
                OutcomeCount(TrialOutcomeKind.ACTIVE_CORRECT, 15),
                OutcomeCount(TrialOutcomeKind.CORRECT_INHIBITION, 5),
            ),
        ),
        answerSummary: AnswerSummary = AnswerSummary(total = 20, correct = 18, incorrect = 1, omissions = 1),
        metrics: CognitiveSessionMetrics = validMetrics(),
        exerciseMetrics: ExerciseSpecificMetricPayload? = null,
        score: CognitiveSessionScore = validScore(),
        eligibility: SessionEligibility = validEligibility(),
        eligibilityTrace: EligibilityTrace = validTrace(),
        calibrationEffect: CalibrationEffect? = null,
        anomalyFlags: List<String> = emptyList(),
        platform: PlatformSnapshot? = null,
        createdAtEpochMs: Long = 121_000L,
        updatedAtEpochMs: Long = 121_000L,
    ): CognitiveSessionRun = CognitiveSessionRun(
        sessionId = sessionId,
        blockId = blockId,
        exerciseId = exerciseId,
        exerciseVersion = exerciseVersion,
        scoringVersion = scoringVersion,
        sessionPurpose = sessionPurpose,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        durationSelectedMs = durationSelectedMs,
        durationActualMs = durationActualMs,
        completionStatus = completionStatus,
        finalizationFailure = finalizationFailure,
        difficulty = difficulty,
        trials = trials,
        answerSummary = answerSummary,
        metrics = metrics,
        exerciseMetrics = exerciseMetrics,
        score = score,
        eligibility = eligibility,
        eligibilityTrace = eligibilityTrace,
        calibrationEffect = calibrationEffect,
        anomalyFlags = anomalyFlags,
        platform = platform,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
    )

    @Suppress("LongParameterList")
    fun validInputs(
        // ADR-FOCUS-PURPOSE: one purpose field, in the single authority's vocabulary. Defaulted official-ready
        // for the same reason as the kill-switch defaults below — so a generic-gate test flips exactly the one
        // input it exercises.
        //
        // CALIBRATION specifically, because it is the ONLY member that qualifies for all three lanes at once:
        // MEASUREMENT feeds the profile but is absent from the calibration/ranking allowlists, and TRAINING_*
        // is the mirror image. Any other default would make "a fully official-ready run" un-expressible.
        purpose: SessionMeasurementPurpose = SessionMeasurementPurpose.CALIBRATION,
        lineageComplete: Boolean = true,
        completionStatus: SessionCompletionStatus = SessionCompletionStatus.COMPLETED,
        durationActualMs: Long = 120_000L,
        approvedProtocol: ApprovedProtocol? = null,
        completedTrials: Int = 20,
        stimuliShown: Int = 20,
        dataQualityCompromised: Boolean = false,
        timing: TimingIntegritySnapshot = cleanTiming(),
        writeAheadPresent: Boolean = true,
        // Measurement-quality kill-switch defaults: OFFICIAL-READY (final, VALID, confident, authoritative
        // timebase, anti-gaming-covered), so a generic-gate test flips exactly the one input it exercises.
        measurementPreliminary: Boolean = false,
        scoreCategoryValid: Boolean = true,
        lowConfidence: Boolean = false,
        authoritativeTimebaseSatisfied: Boolean = true,
        antiGamingCoverageSatisfied: Boolean = true,
    ): EligibilityInputs = EligibilityInputs(
        purpose = purpose,
        lineageComplete = lineageComplete,
        completionStatus = completionStatus,
        durationActualMs = durationActualMs,
        approvedProtocol = approvedProtocol,
        completedTrials = completedTrials,
        stimuliShown = stimuliShown,
        dataQualityCompromised = dataQualityCompromised,
        timing = timing,
        writeAheadPresent = writeAheadPresent,
        measurementPreliminary = measurementPreliminary,
        scoreCategoryValid = scoreCategoryValid,
        lowConfidence = lowConfidence,
        authoritativeTimebaseSatisfied = authoritativeTimebaseSatisfied,
        antiGamingCoverageSatisfied = antiGamingCoverageSatisfied,
    )
}
