package com.mindos.app.features.mindboost.focus.data.dto

import kotlinx.serialization.Serializable

/**
 * Persistence DTOs for MindBoost local-first storage. Versioned with
 * [schemaVersion] so the format can evolve without losing history. Enum-keyed
 * domain maps are flattened to `Map<String, …>` (enum `.name`) for stable JSON.
 *
 * IMPORTANT: sessions persist SUMMARIES only — raw per-trial data is never stored
 * (10_data_persistence_plan.md). [SessionSummaryDto] therefore carries block
 * metrics and difficulty, not the trial list.
 */
object FocusSchema {
    const val CURRENT = 1
}

@Serializable
data class DifficultyVectorDto(
    val perceptualLoad: Double = 0.0,
    val interference: Double = 0.0,
    val temporalUncertainty: Double = 0.0,
    val inhibitoryDemand: Double = 0.0,
    val dualTaskLoad: Double = 0.0,
    val timeOnTask: Double = 0.0,
    val pressure: Double = 0.0,
    val distractorSalience: Double = 0.0,
    val familiarityPenalty: Double = 0.0,
)

@Serializable
data class IndividualThresholdsDto(
    val lapseRtMs: Long = 500,
    val anticipationFloorMs: Long = 150,
    val baselineRtCv: Double? = null,
)

@Serializable
data class SubjectiveStateDto(
    val sleepHours: Double? = null,
    val caffeine: Boolean? = null,
    val mood1to5: Int? = null,
    val localHour: Int? = null,
)

@Serializable
data class MetricValueDto(
    val key: String,
    val value: Double,
    val n: Int,
    val confident: Boolean,
    val unit: String,
)

@Serializable
data class BlockSummaryDto(
    val blockId: String,
    val exerciseId: String,
    val mode: String,
    val difficultyStart: DifficultyVectorDto,
    val difficultyEnd: DifficultyVectorDto,
    val metrics: Map<String, MetricValueDto>,
    val trialCount: Int,
)

@Serializable
data class SessionSummaryDto(
    val sessionId: String,
    val mode: String,
    val exerciseFocus: String,
    val blocks: List<BlockSummaryDto>,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val subjectiveState: SubjectiveStateDto? = null,
    val sessionMetrics: Map<String, MetricValueDto>,
    val schemaVersion: Int = FocusSchema.CURRENT,
)

@Serializable
data class BaselineSnapshotDto(
    val userId: String,
    val perMetric: Map<String, Double>,
    val capturedAtMs: Long,
    val state: SubjectiveStateDto? = null,
    val deviceFingerprint: String? = null,
    val schemaVersion: Int = FocusSchema.CURRENT,
)

@Serializable
data class UserCognitiveProfileDto(
    val userId: String,
    val archetype: String,
    val archetypeConfidence: Double,
    val thresholds: IndividualThresholdsDto,
    val baseline: BaselineSnapshotDto? = null,
    val startingDifficulty: Map<String, DifficultyVectorDto>,
    val cycleWeek: Int,
    val totalSessions: Int,
    val updatedAtMs: Long,
    val schemaVersion: Int = FocusSchema.CURRENT,
)
