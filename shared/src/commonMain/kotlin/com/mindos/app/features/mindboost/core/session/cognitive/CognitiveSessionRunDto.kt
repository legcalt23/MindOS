// Versioned on-disk DTOs for a CognitiveSessionRun plus their pure domain<->DTO mappers. Flat records with many
// cohesive fields (LongParameterList) and one small mapping function per nested type (TooManyFunctions) are the
// idiomatic shape of a serialization layer, not a smell.
@file:Suppress("LongParameterList", "TooManyFunctions")

package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Versioned on-disk shape of a [CognitiveSessionRun]. A dedicated DTO (not the domain model) carries
 * [schemaVersion] so migrations stay explicit; enums persist as their `name` and are FAIL-CLOSED on read — an
 * unknown enum name (or a schemaVersion newer than supported) makes `toDomain`/`decodeRun` throw, so the row is
 * treated as corrupt, never silently accepted. Property names are the stable wire keys (same convention as
 * `SessionRecordDto`). Decoding also re-runs the domain invariants, so any tampered/invalid persisted value
 * fails to decode and is treated as corrupt — never silently repaired.
 */
@Serializable
internal data class CognitiveSessionRunDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val sessionId: String,
    val blockId: String,
    val exerciseId: String,
    val exerciseVersion: String,
    val scoringVersion: String,
    val sessionPurpose: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val durationSelectedMs: Long?,
    val durationActualMs: Long,
    val completionStatus: String,
    val finalizationFailure: String?,
    val difficulty: DifficultyTraceDto,
    val trials: TrialAggregateDto,
    val answerSummary: AnswerSummaryDto,
    val metrics: CognitiveSessionMetricsDto,
    val exerciseMetrics: ExerciseSpecificMetricPayloadDto?,
    val score: CognitiveSessionScoreDto,
    val eligibility: SessionEligibilityDto,
    val eligibilityTrace: EligibilityTraceDto,
    val calibrationEffect: CalibrationEffectDto?,
    val anomalyFlags: List<String>,
    val platform: PlatformSnapshotDto?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
internal data class DifficultyBlockDto(
    val blockIndex: Int,
    val start: Double?,
    val end: Double?,
    val maxReached: Double?,
)

@Serializable
internal data class DifficultyTraceDto(
    val start: Double?,
    val end: Double?,
    val maxReached: Double?,
    val interleaved: Boolean,
    val perBlock: List<DifficultyBlockDto>,
)

@Serializable
internal data class OutcomeCountDto(val kind: String, val count: Int)

@Serializable
internal data class TrialAggregateDto(val planned: Int, val completed: Int, val outcomes: List<OutcomeCountDto>)

@Serializable
internal data class AnswerSummaryDto(val total: Int, val correct: Int, val incorrect: Int, val omissions: Int)

/** Flattened [SessionMetric]: exactly one of [value] (Measured) or [unavailableReason] (Unavailable) is set. */
@Serializable
internal data class SessionMetricDto(val value: Double?, val unavailableReason: String?)

@Serializable
internal data class CognitiveSessionMetricsDto(
    val accuracy: SessionMetricDto,
    val reactionTimeMedianMs: SessionMetricDto,
    val reactionTimeMeanMs: SessionMetricDto,
    val reactionTimeMinMs: SessionMetricDto,
    val reactionTimeMaxMs: SessionMetricDto,
    val reactionTimeVariabilityCv: SessionMetricDto,
)

@Serializable
internal data class ExerciseSpecificMetricPayloadDto(val type: String, val schemaVersion: Int, val payload: JsonObject)

@Serializable
internal data class CognitiveSessionScoreDto(
    val normalized: Double?,
    val category: String,
    val scoringVersion: String,
    val preliminary: Boolean,
    val lowConfidence: Boolean,
)

@Serializable
internal data class TimingIntegritySnapshotDto(
    val clockSource: String,
    val monotonicAvailable: Boolean,
    val backgroundedDuringSession: Boolean,
    val pauseResumeCount: Int,
    val missedTickFlags: Int,
    val suspectedInputLatency: Boolean,
    val integrityStatus: String,
)

@Serializable
internal data class SessionEligibilityDto(
    val profileEligible: Boolean,
    val calibrationEligible: Boolean,
    val rankingEligible: Boolean,
    val timingIntegrity: TimingIntegritySnapshotDto,
    val dataQualityCompromised: Boolean,
)

@Serializable
internal data class EligibilityTraceDto(
    val integrityEligible: Boolean,
    val writeAheadEligible: Boolean,
    val durationEligible: Boolean,
    val purposeEligible: Boolean,
    val dataQualityEligible: Boolean,
    val officialUseEligible: Boolean,
    val rejectionReasons: List<String>,
)

@Serializable
internal data class CalibrationEffectDto(
    val countedTowardCalibration: Boolean,
    val validSessionIndexAtCompletion: Int?,
    val calibrationRuleVersion: String,
)

@Serializable
internal data class PlatformSnapshotDto(
    val osName: String,
    val osVersion: String,
    val appVersion: String,
    val deviceTier: String?,
)

// ---- domain -> DTO ----

internal fun CognitiveSessionRun.toDto(): CognitiveSessionRunDto = CognitiveSessionRunDto(
    sessionId = sessionId,
    blockId = blockId.name,
    exerciseId = exerciseId,
    exerciseVersion = exerciseVersion,
    scoringVersion = scoringVersion,
    sessionPurpose = sessionPurpose.name,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    durationSelectedMs = durationSelectedMs,
    durationActualMs = durationActualMs,
    completionStatus = completionStatus.name,
    finalizationFailure = finalizationFailure?.name,
    difficulty = difficulty.toDto(),
    trials = trials.toDto(),
    answerSummary = with(answerSummary) { AnswerSummaryDto(total, correct, incorrect, omissions) },
    metrics = metrics.toDto(),
    exerciseMetrics = exerciseMetrics?.let { ExerciseSpecificMetricPayloadDto(it.type, it.schemaVersion, it.payload) },
    score = score.toDto(),
    eligibility = eligibility.toDto(),
    eligibilityTrace = eligibilityTrace.toDto(),
    calibrationEffect = calibrationEffect?.let {
        CalibrationEffectDto(it.countedTowardCalibration, it.validSessionIndexAtCompletion, it.calibrationRuleVersion)
    },
    anomalyFlags = anomalyFlags,
    platform = platform?.let { PlatformSnapshotDto(it.osName, it.osVersion, it.appVersion, it.deviceTier) },
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun DifficultyTrace.toDto(): DifficultyTraceDto = DifficultyTraceDto(
    start = start,
    end = end,
    maxReached = maxReached,
    interleaved = interleaved,
    perBlock = perBlock.map { DifficultyBlockDto(it.blockIndex, it.start, it.end, it.maxReached) },
)

private fun TrialAggregate.toDto(): TrialAggregateDto =
    TrialAggregateDto(planned, completed, outcomes.map { OutcomeCountDto(it.kind.name, it.count) })

private fun CognitiveSessionMetrics.toDto(): CognitiveSessionMetricsDto = CognitiveSessionMetricsDto(
    accuracy = accuracy.toDto(),
    reactionTimeMedianMs = reactionTimeMedianMs.toDto(),
    reactionTimeMeanMs = reactionTimeMeanMs.toDto(),
    reactionTimeMinMs = reactionTimeMinMs.toDto(),
    reactionTimeMaxMs = reactionTimeMaxMs.toDto(),
    reactionTimeVariabilityCv = reactionTimeVariabilityCv.toDto(),
)

private fun SessionMetric.toDto(): SessionMetricDto = when (this) {
    is SessionMetric.Measured -> SessionMetricDto(value = value, unavailableReason = null)
    is SessionMetric.Unavailable -> SessionMetricDto(value = null, unavailableReason = reason.name)
}

private fun CognitiveSessionScore.toDto(): CognitiveSessionScoreDto =
    CognitiveSessionScoreDto(normalized, category.name, scoringVersion, preliminary, lowConfidence)

private fun SessionEligibility.toDto(): SessionEligibilityDto = SessionEligibilityDto(
    profileEligible = profileEligible,
    calibrationEligible = calibrationEligible,
    rankingEligible = rankingEligible,
    timingIntegrity = with(timingIntegrity) {
        TimingIntegritySnapshotDto(
            clockSource = clockSource.name,
            monotonicAvailable = monotonicAvailable,
            backgroundedDuringSession = backgroundedDuringSession,
            pauseResumeCount = pauseResumeCount,
            missedTickFlags = missedTickFlags,
            suspectedInputLatency = suspectedInputLatency,
            integrityStatus = integrityStatus.name,
        )
    },
    dataQualityCompromised = dataQualityCompromised,
)

private fun EligibilityTrace.toDto(): EligibilityTraceDto = EligibilityTraceDto(
    integrityEligible = integrityEligible,
    writeAheadEligible = writeAheadEligible,
    durationEligible = durationEligible,
    purposeEligible = purposeEligible,
    dataQualityEligible = dataQualityEligible,
    officialUseEligible = officialUseEligible,
    rejectionReasons = rejectionReasons,
)

// ---- DTO -> domain (re-runs domain invariants; throws on any invalid/unknown value) ----

internal fun CognitiveSessionRunDto.toDomain(): CognitiveSessionRun = CognitiveSessionRun(
    sessionId = sessionId,
    blockId = ScoreModuleId.valueOf(blockId),
    exerciseId = exerciseId,
    exerciseVersion = exerciseVersion,
    scoringVersion = scoringVersion,
    sessionPurpose = SessionPurpose.valueOf(sessionPurpose),
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    durationSelectedMs = durationSelectedMs,
    durationActualMs = durationActualMs,
    completionStatus = SessionCompletionStatus.valueOf(completionStatus),
    finalizationFailure = finalizationFailure?.let { FinalizationFailureReason.valueOf(it) },
    difficulty = difficulty.toDomain(),
    trials = trials.toDomain(),
    answerSummary = with(answerSummary) { AnswerSummary(total, correct, incorrect, omissions) },
    metrics = metrics.toDomain(),
    exerciseMetrics = exerciseMetrics?.let { ExerciseSpecificMetricPayload(it.type, it.schemaVersion, it.payload) },
    score = score.toDomain(),
    eligibility = eligibility.toDomain(),
    eligibilityTrace = eligibilityTrace.toDomain(),
    calibrationEffect = calibrationEffect?.let {
        CalibrationEffect(it.countedTowardCalibration, it.validSessionIndexAtCompletion, it.calibrationRuleVersion)
    },
    anomalyFlags = anomalyFlags,
    platform = platform?.let { PlatformSnapshot(it.osName, it.osVersion, it.appVersion, it.deviceTier) },
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun DifficultyTraceDto.toDomain(): DifficultyTrace = DifficultyTrace(
    start = start,
    end = end,
    maxReached = maxReached,
    interleaved = interleaved,
    perBlock = perBlock.map { DifficultyBlock(it.blockIndex, it.start, it.end, it.maxReached) },
)

private fun TrialAggregateDto.toDomain(): TrialAggregate =
    TrialAggregate(planned, completed, outcomes.map { OutcomeCount(TrialOutcomeKind.valueOf(it.kind), it.count) })

private fun CognitiveSessionMetricsDto.toDomain(): CognitiveSessionMetrics = CognitiveSessionMetrics(
    accuracy = accuracy.toDomain(),
    reactionTimeMedianMs = reactionTimeMedianMs.toDomain(),
    reactionTimeMeanMs = reactionTimeMeanMs.toDomain(),
    reactionTimeMinMs = reactionTimeMinMs.toDomain(),
    reactionTimeMaxMs = reactionTimeMaxMs.toDomain(),
    reactionTimeVariabilityCv = reactionTimeVariabilityCv.toDomain(),
)

private fun SessionMetricDto.toDomain(): SessionMetric = when {
    value != null && unavailableReason == null -> SessionMetric.Measured(value)
    value == null && unavailableReason != null ->
        SessionMetric.Unavailable(MetricUnavailableReason.valueOf(unavailableReason))
    else -> error("SessionMetricDto must set exactly one of value / unavailableReason.")
}

private fun CognitiveSessionScoreDto.toDomain(): CognitiveSessionScore =
    CognitiveSessionScore(normalized, ScoreState.valueOf(category), scoringVersion, preliminary, lowConfidence)

private fun SessionEligibilityDto.toDomain(): SessionEligibility = SessionEligibility(
    profileEligible = profileEligible,
    calibrationEligible = calibrationEligible,
    rankingEligible = rankingEligible,
    timingIntegrity = with(timingIntegrity) {
        TimingIntegritySnapshot(
            clockSource = ClockSource.valueOf(clockSource),
            monotonicAvailable = monotonicAvailable,
            backgroundedDuringSession = backgroundedDuringSession,
            pauseResumeCount = pauseResumeCount,
            missedTickFlags = missedTickFlags,
            suspectedInputLatency = suspectedInputLatency,
            integrityStatus = TimingIntegrityStatus.valueOf(integrityStatus),
        )
    },
    dataQualityCompromised = dataQualityCompromised,
)

private fun EligibilityTraceDto.toDomain(): EligibilityTrace = EligibilityTrace(
    integrityEligible = integrityEligible,
    writeAheadEligible = writeAheadEligible,
    durationEligible = durationEligible,
    purposeEligible = purposeEligible,
    dataQualityEligible = dataQualityEligible,
    officialUseEligible = officialUseEligible,
    rejectionReasons = rejectionReasons,
)

// ---- encode / decode (top-level so the store class stays under the function threshold) ----

/** The Json used for run persistence. Deterministic output (fixed field order) so re-encoding is stable. */
internal val cognitiveRunJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun encodeRun(json: Json, run: CognitiveSessionRun): String =
    json.encodeToString(CognitiveSessionRunDto.serializer(), run.toDto())

/** Decodes a raw run. Throws (parse / unknown enum / newer schema / invalid invariant) so the caller can treat
 *  any failure as corrupt — it NEVER fabricates or repairs. */
internal fun decodeRun(json: Json, raw: String): CognitiveSessionRun {
    val dto = json.decodeFromString(CognitiveSessionRunDto.serializer(), raw)
    require(dto.schemaVersion <= CognitiveSessionRunDto.SCHEMA_VERSION) {
        "CognitiveSessionRun schemaVersion ${dto.schemaVersion} is newer than supported."
    }
    return dto.toDomain()
}
