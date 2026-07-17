// Read-model projections of a sealed CognitiveSessionRun for the history layer (SYSTEM-1 slice 5). The summary
// item is a flat record with many cohesive fields (the exact minimum a serious history screen needs); its width
// is the shape of a view-model, not a smell.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreState

/**
 * SYSTEM-1 slice 5 · A minimized, product-ready SUMMARY of one sealed [CognitiveSessionRun] for a history list.
 *
 * HISTORY ≠ PROFILE. A run appears here even when it is preliminary or ineligible — it is never hidden — but it
 * is marked honestly: [mayCountTowardProfile] / [mayCountTowardCalibration] / [mayCountTowardRanking] and
 * [officialUseDeniedReasons] make explicit whether (and why not) the run may count officially. Measurement
 * honesty is preserved verbatim:
 * [scoreNormalized] is `null` (never a fabricated 0) whenever the score is not a VALID official number, and
 * [accuracy] / [reactionTimeMedianMs] keep their [SessionMetric.Unavailable] semantics — no metric is coerced.
 *
 * This carries NO raw trials and NO per-item content: only session-level aggregates + the eligibility trace.
 */
data class CognitiveSessionHistoryItem(
    val sessionId: String,
    val blockId: ScoreModuleId,
    val exerciseId: String,
    val sessionPurpose: SessionPurpose,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val durationSelectedMs: Long?,
    val durationActualMs: Long,
    val completionStatus: SessionCompletionStatus,
    val scoreState: ScoreState,
    /**
     * The official normalized score in `0.0..1.0`, or `null` when there is no VALID official number — NEVER 0.
     * Copied VERBATIM from the sealed [CognitiveSessionScore.normalized]: the PRODUCER guarantees a non-VALID
     * state carries `null` (charter `null != 0`); this read-model only projects and never coerces/fabricates.
     */
    val scoreNormalized: Double?,
    val preliminary: Boolean,
    val lowConfidence: Boolean,
    val dataQualityCompromised: Boolean,
    /** Accuracy as a [SessionMetric] — an [SessionMetric.Unavailable] stays Unavailable, never becomes 0. */
    val accuracy: SessionMetric,
    /** The principal reaction-time metric (median, ms) as a [SessionMetric] — Unavailable is preserved. */
    val reactionTimeMedianMs: SessionMetric,
    val answerSummary: AnswerSummary,
    val anomalyFlags: List<String>,
    val eligibilityTrace: EligibilityTrace,
    /**
     * Whether this run is ELIGIBLE to count toward the profile / calibration / ranking — a PERMISSION, never a
     * record that an ingestion happened. These project the run's [SessionEligibility] lanes: an ineligible run
     * definitively may NOT count (`false`); an eligible run MAY count but has NOT necessarily been ingested (there
     * is no live ingestor in slice 5). The authoritative "was actually counted" fact belongs to the slice-6
     * ingestor and will carry its own distinct `*Counted` flags — this read-model NEVER claims an ingestion.
     */
    val mayCountTowardProfile: Boolean,
    val mayCountTowardCalibration: Boolean,
    val mayCountTowardRanking: Boolean,
) {
    /**
     * The GLOBAL official-use denial reasons (the UNION across all lanes), or empty when officially eligible —
     * mirrors [eligibilityTrace]. This is NOT a per-lane attribution: e.g. `duration_below_official_threshold`
     * denies only calibration/ranking, never profile. To explain a SPECIFIC lane, read [eligibilityTrace] with
     * the per-lane rule that only calibration/ranking require the duration/protocol threshold.
     */
    val officialUseDeniedReasons: List<String> get() = eligibilityTrace.rejectionReasons
}

/**
 * A SAFE descriptor of a run's exercise-specific payload: its declared [type]/[schemaVersion] and field count
 * only — NEVER the raw opaque `JsonObject`. A UI/diagnostic can show "gonogo_inhibition v1 · 16 fields" without
 * parsing an unvalidated blob (which could break rendering or leak an unexpected shape).
 */
data class ExercisePayloadSummary(
    val type: String,
    val schemaVersion: Int,
    val fieldCount: Int,
)

/**
 * SYSTEM-1 slice 5 · The fuller (but still minimized) DETAIL of one run, for an inspection screen. Adds the full
 * metric set, the difficulty trajectory, the outcome-count breakdown, a SAFE payload summary, and the score
 * contract on top of the [summary]. Still carries NO raw trials.
 */
data class CognitiveSessionDetail(
    val summary: CognitiveSessionHistoryItem,
    val exerciseVersion: String,
    val scoringVersion: String,
    val metrics: CognitiveSessionMetrics,
    val difficulty: DifficultyTrace,
    val trials: TrialAggregate,
    val exercisePayload: ExercisePayloadSummary?,
    val calibrationEffect: CalibrationEffect?,
    val platform: PlatformSnapshot?,
)

/** Projects a sealed run to its history SUMMARY. Pure; preserves `null`/Unavailable semantics verbatim. */
fun CognitiveSessionRun.toHistoryItem(): CognitiveSessionHistoryItem = CognitiveSessionHistoryItem(
    sessionId = sessionId,
    blockId = blockId,
    exerciseId = exerciseId,
    sessionPurpose = sessionPurpose,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    durationSelectedMs = durationSelectedMs,
    durationActualMs = durationActualMs,
    completionStatus = completionStatus,
    scoreState = score.category,
    scoreNormalized = score.normalized,
    preliminary = score.preliminary,
    lowConfidence = score.lowConfidence,
    dataQualityCompromised = eligibility.dataQualityCompromised,
    accuracy = metrics.accuracy,
    reactionTimeMedianMs = metrics.reactionTimeMedianMs,
    answerSummary = answerSummary,
    anomalyFlags = anomalyFlags,
    eligibilityTrace = eligibilityTrace,
    mayCountTowardProfile = eligibility.profileEligible,
    mayCountTowardCalibration = eligibility.calibrationEligible,
    mayCountTowardRanking = eligibility.rankingEligible,
)

/** Projects a sealed run to its DETAIL view. Pure; the exercise payload is mapped to a safe summary, not raw. */
fun CognitiveSessionRun.toDetail(): CognitiveSessionDetail = CognitiveSessionDetail(
    summary = toHistoryItem(),
    exerciseVersion = exerciseVersion,
    scoringVersion = scoringVersion,
    metrics = metrics,
    difficulty = difficulty,
    trials = trials,
    exercisePayload = exerciseMetrics?.let {
        ExercisePayloadSummary(type = it.type, schemaVersion = it.schemaVersion, fieldCount = it.payload.size)
    },
    calibrationEffect = calibrationEffect,
    platform = platform,
)
