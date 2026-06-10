@file:Suppress("TooManyFunctions") // A mapper file is a collection of small toDto/toDomain pairs.

package com.mindos.app.features.mindboost.focus.data.mapper

import com.mindos.app.features.mindboost.focus.data.dto.BaselineSnapshotDto
import com.mindos.app.features.mindboost.focus.data.dto.BlockSummaryDto
import com.mindos.app.features.mindboost.focus.data.dto.DifficultyVectorDto
import com.mindos.app.features.mindboost.focus.data.dto.IndividualThresholdsDto
import com.mindos.app.features.mindboost.focus.data.dto.MetricValueDto
import com.mindos.app.features.mindboost.focus.data.dto.SessionSummaryDto
import com.mindos.app.features.mindboost.focus.data.dto.SubjectiveStateDto
import com.mindos.app.features.mindboost.focus.data.dto.UserCognitiveProfileDto
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile

/**
 * DTO ⇄ domain mappers for local persistence. Sessions map to SUMMARIES: the
 * per-trial list is intentionally dropped on the way to storage and comes back as
 * an empty list — only metrics/difficulty are needed for history and trends.
 */

// ── DifficultyVector ──────────────────────────────────────────────────────────

fun DifficultyVector.toDto(): DifficultyVectorDto = DifficultyVectorDto(
    perceptualLoad = perceptualLoad.value,
    interference = interference.value,
    temporalUncertainty = temporalUncertainty.value,
    inhibitoryDemand = inhibitoryDemand.value,
    dualTaskLoad = dualTaskLoad.value,
    timeOnTask = timeOnTask.value,
    pressure = pressure.value,
    distractorSalience = distractorSalience.value,
    familiarityPenalty = familiarityPenalty.value,
)

fun DifficultyVectorDto.toDomain(): DifficultyVector = DifficultyVector(
    perceptualLoad = AxisLevel.clamped(perceptualLoad),
    interference = AxisLevel.clamped(interference),
    temporalUncertainty = AxisLevel.clamped(temporalUncertainty),
    inhibitoryDemand = AxisLevel.clamped(inhibitoryDemand),
    dualTaskLoad = AxisLevel.clamped(dualTaskLoad),
    timeOnTask = AxisLevel.clamped(timeOnTask),
    pressure = AxisLevel.clamped(pressure),
    distractorSalience = AxisLevel.clamped(distractorSalience),
    familiarityPenalty = AxisLevel.clamped(familiarityPenalty),
)

// ── Thresholds / subjective state ─────────────────────────────────────────────

fun IndividualThresholds.toDto(): IndividualThresholdsDto =
    IndividualThresholdsDto(lapseRtMs, anticipationFloorMs, baselineRtCv)

fun IndividualThresholdsDto.toDomain(): IndividualThresholds =
    IndividualThresholds(lapseRtMs, anticipationFloorMs, baselineRtCv)

fun SubjectiveState.toDto(): SubjectiveStateDto =
    SubjectiveStateDto(sleepHours, caffeine, mood1to5, localHour)

fun SubjectiveStateDto.toDomain(): SubjectiveState =
    SubjectiveState(sleepHours, caffeine, mood1to5, localHour)

// ── Metrics ───────────────────────────────────────────────────────────────────

fun MetricValue.toDto(): MetricValueDto =
    MetricValueDto(key = key.name, value = value, n = n, confident = confident, unit = unit.name)

fun MetricValueDto.toDomain(): MetricValue = MetricValue(
    key = MetricKey.valueOf(key),
    value = value,
    n = n,
    confident = confident,
    unit = MetricUnit.valueOf(unit),
)

fun MetricSet.toDto(): Map<String, MetricValueDto> = entries.associate { it.key.name to it.value.toDto() }

fun Map<String, MetricValueDto>.toMetricSet(): MetricSet =
    entries.associate { MetricKey.valueOf(it.key) to it.value.toDomain() }

// ── Baseline ──────────────────────────────────────────────────────────────────

fun BaselineSnapshot.toDto(): BaselineSnapshotDto = BaselineSnapshotDto(
    userId = userId,
    perMetric = perMetric.entries.associate { it.key.name to it.value },
    capturedAtMs = capturedAtMs,
    state = state?.toDto(),
    deviceFingerprint = deviceFingerprint,
)

fun BaselineSnapshotDto.toDomain(): BaselineSnapshot = BaselineSnapshot(
    userId = userId,
    perMetric = perMetric.entries.associate { MetricKey.valueOf(it.key) to it.value },
    capturedAtMs = capturedAtMs,
    state = state?.toDomain(),
    deviceFingerprint = deviceFingerprint,
)

// ── Session (summary only — trials dropped) ───────────────────────────────────

fun BlockResult.toSummaryDto(): BlockSummaryDto = BlockSummaryDto(
    blockId = blockId.value,
    exerciseId = exerciseId.name,
    mode = mode.name,
    difficultyStart = difficultyStart.toDto(),
    difficultyEnd = difficultyEnd.toDto(),
    metrics = metrics.toDto(),
    trialCount = trials.size,
)

fun BlockSummaryDto.toDomain(): BlockResult = BlockResult(
    blockId = BlockId(blockId),
    exerciseId = ExerciseId.valueOf(exerciseId),
    mode = Mode.valueOf(mode),
    trials = emptyList(), // raw trials are never persisted; metrics carry the signal.
    difficultyStart = difficultyStart.toDomain(),
    difficultyEnd = difficultyEnd.toDomain(),
    metrics = metrics.toMetricSet(),
)

fun SessionResult.toSummaryDto(): SessionSummaryDto = SessionSummaryDto(
    sessionId = sessionId.value,
    mode = mode.name,
    exerciseFocus = exerciseFocus.name,
    blocks = blocks.map { it.toSummaryDto() },
    startedAtMs = startedAtMs,
    endedAtMs = endedAtMs,
    subjectiveState = subjectiveState?.toDto(),
    sessionMetrics = sessionMetrics.toDto(),
)

fun SessionSummaryDto.toDomain(): SessionResult = SessionResult(
    sessionId = SessionId(sessionId),
    mode = Mode.valueOf(mode),
    exerciseFocus = ExerciseId.valueOf(exerciseFocus),
    blocks = blocks.map { it.toDomain() },
    startedAtMs = startedAtMs,
    endedAtMs = endedAtMs,
    subjectiveState = subjectiveState?.toDomain(),
    sessionMetrics = sessionMetrics.toMetricSet(),
)

// ── Profile ───────────────────────────────────────────────────────────────────

fun UserCognitiveProfile.toDto(): UserCognitiveProfileDto = UserCognitiveProfileDto(
    userId = userId,
    archetype = archetype.name,
    archetypeConfidence = archetypeConfidence,
    thresholds = thresholds.toDto(),
    baseline = baseline?.toDto(),
    startingDifficulty = startingDifficulty.entries.associate { it.key.name to it.value.toDto() },
    cycleWeek = cycleWeek,
    totalSessions = totalSessions,
    updatedAtMs = updatedAtMs,
)

fun UserCognitiveProfileDto.toDomain(): UserCognitiveProfile = UserCognitiveProfile(
    userId = userId,
    archetype = UserArchetype.valueOf(archetype),
    archetypeConfidence = archetypeConfidence,
    thresholds = thresholds.toDomain(),
    baseline = baseline?.toDomain(),
    startingDifficulty = startingDifficulty.entries.associate {
        ExerciseId.valueOf(it.key) to it.value.toDomain()
    },
    cycleWeek = cycleWeek,
    totalSessions = totalSessions,
    updatedAtMs = updatedAtMs,
)
