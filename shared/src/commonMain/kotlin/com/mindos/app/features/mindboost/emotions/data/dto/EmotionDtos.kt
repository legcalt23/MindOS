@file:Suppress("MaxLineLength") // Persistence-contract KDoc/comments read best whole.

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
    // MC8u-B+ §B/§E/§K: per-session quality + versioning (schemaVersion NOT bumped — these are additive with legacy
    // defaults so old rows deserialize unchanged and never lose earned progress). `governingConfident` defaults TRUE
    // and `scoringVersion` defaults LEGACY(0) so a pre-gate row keeps counting; a NEW row stamps scoringVersion=SCORING
    // and its real confidence/anomaly, and the calibration gate (isValidForAggregate) enforces quality only on new rows.
    val governingConfident: Boolean = true,
    val anomalyFlags: List<String> = emptyList(),
    val scoringVersion: Int = EmotionScoringVersions.LEGACY,
    val exerciseVersion: Int = EmotionScoringVersions.LEGACY,
    // MC8u-C §2/§3: explicit session-contract fields (PRD requires them first-class, not silently re-derived from
    // metricsSummary). All additive with legacy defaults. `normalizedScore` is the 0–100 MindBoost TRAINING-PERFORMANCE
    // block score (null = CALIBRATING; task performance, never an emotional/clinical reading — see EmotionBlockScore).
    // NotApplicableByCharter for Emociones (see EmotionSessionContract): rawScore/scoreDelta/xpEarned (no cognitive
    // per-session score beyond the task-performance block score); durationSelected/blockId/maxLevelReached (fixed
    // 2-block structure, single governing axis); platform/syncStatus (local-only, no sync yet).
    val difficultyStart: Double = 0.0,
    val totalAnswers: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val omissions: Int = 0,
    val accuracy: Double = 0.0,
    val averageReactionTimeMs: Double = 0.0,
    val bestReactionTimeMs: Double = 0.0,
    val worstReactionTimeMs: Double = 0.0,
    val reactionTimeVariability: Double = 0.0,
    val normalizedScore: Int? = null,
    // MC8u-D §2/§6: sync posture + device platform are now first-class (not NotApplicableByCharter). Additive with
    // charter-honest defaults: local-first storage means a fresh row is LOCAL_ONLY (durable on-device, no cloud copy)
    // and the app injects the real platform. A future cloud-sync layer flips `syncStatus` (see EmotionSyncContract).
    val platform: String = EmotionPlatform.UNKNOWN,
    val syncStatus: String = EmotionSyncStatus.LOCAL_ONLY,
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
