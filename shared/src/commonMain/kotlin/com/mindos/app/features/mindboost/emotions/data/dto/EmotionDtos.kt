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

/**
 * Ola 4 — the result of ONE held-out probe administered in this session. Recorded separately from every training
 * statistic on purpose: it is the only evidence in the module that survives the objection "you just got better at
 * the items you practised".
 *
 * [servedItemIds] is what burns those items for this user (a probe item answered once is no longer held out), so
 * the union of this field across summaries IS the served set the next draw excludes. Content is authored bank
 * phrases — the same neutral strings the exposure guard already stores — never user text.
 */
@Serializable
data class HoldoutProbeDto(
    val servedItemIds: List<String>,
    val correct: Int,
    val total: Int,
)

/**
 * Ola 3 · R3 · P4 — one session's contribution to the robustness index: how the user classified in CALM and how
 * the same user classified UNDER PRESSURE, as counts.
 *
 * Persisted per session because the index is a POOLED reading and cannot be anything else. The ratified floor for
 * a pressure comparison is 20 per cell, and a session's calm cell is one block — ten trials — so no single
 * session can produce the number; what a session can produce is its share of it. See
 * `SeparatorRobustnessIndex` for the arithmetic that settled this, including why growing the block would not
 * have changed the answer.
 *
 * Both totals count every classification trial PRESENTED, omissions included: under a tightened window a timeout
 * is how pressure usually bites, and excluding those would remove the cost where it is largest.
 */
@Serializable
data class PressureCellsDto(
    val calmCorrect: Int,
    val calmTotal: Int,
    val pressureCorrect: Int,
    val pressureTotal: Int,
)

/**
 * Ola 1 · N1 — one session's calibration evidence, as COUNTS rather than as a list of trials.
 *
 * This shape is not a compression convenience, it is what makes the layer storable at all: the module persists
 * session SUMMARIES and never raw trials, and a per-trial confidence judgment is trial-level data. Collapsing to
 * (level, chanceLevel) → (n, correct) is a genuine summary and it is also SUFFICIENT — inside one bucket every
 * judgment carries the same stated probability, so Brier, signed overconfidence and its interval, resolution and
 * the per-level accuracies are all exactly recoverable from these counts. Nothing is approximated.
 */
@Serializable
data class CalibrationBucketDto(
    val level: String,
    val chanceLevel: Double,
    val n: Int,
    val correct: Int,
)

/** Ola 1 · N1 — one block's forecast and what actually happened. */
@Serializable
data class BlockForecastDto(
    val predictedCorrect: Int,
    val blockSize: Int,
    val actualCorrect: Int,
)

/** Ola 1 · N1 — the calibration evidence a single session contributed. Absent for hosts the layer cannot serve. */
@Serializable
data class CalibrationSummaryDto(
    val buckets: List<CalibrationBucketDto> = emptyList(),
    val forecasts: List<BlockForecastDto> = emptyList(),
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
    // Ola 4: the held-out probe served in this session, if any. Additive with a legacy default of null — a row
    // written before the probe existed truthfully reports "no probe was served", never a fabricated zero-score one.
    // Deliberately NOT part of metricsSummary: probe trials never enter a training metric, a block score, the
    // adaptive decision, or the calibration count.
    val holdoutProbe: HoldoutProbeDto? = null,
    /**
     * Ola 3 · R3 · P4 — this session's calm and under-pressure classification counts. Additive with a legacy
     * default of null, and null is load-bearing in three different ways that all read the same at the aggregate:
     * the row predates the field, the exercise was not the separator, or pressure never unlocked. None of them
     * is a session that scored zero under pressure, which is what a fabricated all-zero row would claim.
     */
    val pressureCells: PressureCellsDto? = null,
    /**
     * Ola 1 · N1 — the transversal calibration layer's contribution. Additive with a legacy default of null, and
     * null is load-bearing: rows written before the layer existed, and rows from hosts that cannot carry a
     * judgment, are ABSENT rather than a session that scored zero calibration.
     */
    val calibration: CalibrationSummaryDto? = null,
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

/**
 * Ola 3 · R3 · P4 — the pooled robustness reading, or the reason there is none. Exactly one side is populated:
 * a measured reading carries its interval and direction, an unavailable one carries [gap].
 *
 * [gap] exists rather than letting absence speak for itself because the reasons are different facts about the
 * user — pressure never served, or served and still short of the floor — and a reader that could not tell them
 * apart would show "not applicable" to someone who is four sessions away from a number.
 */
@Serializable
data class RobustnessDto(
    val index: Double? = null,
    val intervalLow: Double? = null,
    val intervalHigh: Double? = null,
    val verdict: String? = null,
    val gap: String? = null,
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
    // Ola 3 · R3 · P4. An all-null default means "this row predates the index" — deliberately NOT a stored gap,
    // because a legacy row never ran the derivation and cannot report its outcome. The aggregate is rebuilt from
    // the summaries on the next completion, so the empty reading is replaced by a derived one rather than aged.
    val robustness: RobustnessDto = RobustnessDto(),
    val confidence: Double,
    val trend: String,
    val updatedAtMillis: Long,
)
