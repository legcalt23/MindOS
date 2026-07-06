package com.mindos.app.features.mindboost.emotions.data.dto

import kotlinx.serialization.Serializable

/**
 * Persistence schema versions. Both the per-session summary and the aggregate carry their OWN version
 * so each can evolve independently; reads go through `readVersioned` (quarantine on a future version).
 */
object EmotionSchema {
    const val SESSION_SUMMARY = 1
    const val AGGREGATE = 1
    const val PENDING_TERMINAL = 1
}

/**
 * Persisted DTOs. Enum-like fields are stored as Strings (decoupled from domain enums + forward-compat
 * with `ignoreUnknownKeys`). Only SUMMARIES are ever written — never raw trials, free text, or
 * identifiers. `encodeDefaults = true` guarantees `schemaVersion` is always present.
 */
@Serializable
data class SelfReportDto(
    val valence: Int,
    val arousal: Int,
    val intensity: Int,
    val label: String? = null,
)

@Serializable
data class MetricSummaryDto(
    val key: String,
    val value: Double,
    val n: Int,
    val confident: Boolean,
    val unit: String,
)

@Serializable
data class DifficultySummaryDto(
    val governingAxis: String,
    val level: Double,
    val intensity: Int,
)

@Serializable
data class SafetyEventDto(
    val type: String,
    val atOffsetMs: Long,
    val reportedIntensity: Int? = null,
    val dominantProxy: String? = null,
)

@Serializable
data class EmotionSessionSummaryDto(
    val schemaVersion: Int = EmotionSchema.SESSION_SUMMARY,
    val exerciseId: String,
    val startedAtMillis: Long,
    val durationMs: Long,
    val difficultySummary: DifficultySummaryDto,
    val metricsSummary: List<MetricSummaryDto>,
    val selfReportBefore: SelfReportDto? = null,
    val selfReportAfter: SelfReportDto? = null,
    val safetyEvents: List<SafetyEventDto> = emptyList(),
    val completed: Boolean,
    val abandoned: Boolean,
    // MC8g: links the summary to its write-ahead SessionRecord (recovery reconciliation) and states the terminal
    // reason explicitly so downstream never has to infer safety-stop from events. Nullable for old rows.
    val sessionId: String? = null,
    val endReason: String? = null, // COMPLETED | SAFETY_STOP | ABANDONED
)

/**
 * A durable "pending terminal" (MC8g): written BEFORE attempting the summary save so a COMPLETED/SAFETY_STOP
 * whose save fails is never lost — recovery retries [summary] until it persists. Deleted once the summary is
 * durable. Keyed by the opaque session id (from [EmotionSessionSummaryDto.sessionId]).
 *
 * MC8h: [attemptCount] starts at 1 (the live terminal's own save) and is incremented by the reconciler on every
 * FAILED retry; [lastAttemptAtMillis] records when that retry ran. These are real resilience telemetry (not
 * decorative), so a persistently failing terminal is observable rather than silently retried forever.
 */
@Serializable
data class EmotionPendingTerminalDto(
    val schemaVersion: Int = EmotionSchema.PENDING_TERMINAL,
    val summary: EmotionSessionSummaryDto,
    val attemptCount: Int,
    val createdAtMillis: Long,
    val lastAttemptAtMillis: Long? = null,
)

@Serializable
data class EmotionAggregateDto(
    val schemaVersion: Int = EmotionSchema.AGGREGATE,
    val sessionsCompleted: Int,
    val scores: List<MetricSummaryDto> = emptyList(),
    val tier: String,
    val dataSufficiency: Double,
    val safetyLoad: Double,
    val transferConfidence: String,
    val confidence: Double,
    val trend: String,
    val updatedAtMillis: Long,
)
