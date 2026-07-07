@file:Suppress("MaxLineLength") // User-facing labels / builder expressions read best on one line.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics

/**
 * MC8u-D §6 — reusable, PII-free calibration-progress model for the MAIN experience (the block screen and/or the
 * global dashboard). Distinct from [EmotionHistoryUiModel] (the menu history card) in that it foregrounds:
 *  - the 0/5 → 5/5 progress toward the official range,
 *  - a PROVISIONAL vs OFFICIAL status label (never an official rank before 5 valid sessions),
 *  - the 0–100 task-performance block score (or "—" while CALIBRATING),
 *  - a coarse note when the last session did NOT count / was low confidence,
 *  - a non-clinical disclaimer.
 *
 * Everything is a text LABEL (never color-only) and derived from counts already gated by the aggregate: low-confidence
 * and anomalous sessions never reach [validSessions] (they are excluded by `isValidForAggregate` before this model is
 * built), so progress can only advance on a genuinely valid session. No raw enum names, no per-person claim.
 */
data class EmotionCalibrationProgressUiModel(
    val title: String,
    val validSessions: Int,
    val target: Int,
    val progressText: String,
    val statusLabel: String,
    val officialRangeUnlocked: Boolean,
    val blockScoreText: String,
    val blockScoreCalibrating: Boolean,
    val lastSessionText: String?,
    val notCountedNote: String?,
    val lowConfidenceNote: String?,
    val nextRecommendationText: String?,
    val disclaimer: String,
)

/** Pure builder for [EmotionCalibrationProgressUiModel]. Reusable by the block screen and a future dashboard. */
object EmotionCalibrationProgressBuilder {

    /**
     * @param validSessions PRD-valid sessions so far (already excludes low-confidence/anomalous/short sessions).
     * @param blockScore the 0–100 task-performance score, or null while CALIBRATING (shown as "—").
     * @param lastSessionCounted whether the most recent session counted (true/false), or null if there is none yet.
     * @param lastSessionLowConfidence whether the most recent session was flagged low confidence.
     * @param nextRecommendation the display name of the next suggested practice (never a raw enum), or null.
     */
    fun build(
        validSessions: Int,
        blockScore: Int?,
        lastSessionCounted: Boolean?,
        lastSessionLowConfidence: Boolean,
        nextRecommendation: String?,
    ): EmotionCalibrationProgressUiModel {
        val target = EmotionStrings.VALID_SESSION_TARGET
        val unlocked = validSessions >= target
        return EmotionCalibrationProgressUiModel(
            title = EmotionStrings.PROGRESS_TITLE,
            validSessions = validSessions.coerceIn(0, target),
            target = target,
            progressText = EmotionStrings.validSessionsProgress(validSessions),
            statusLabel = if (unlocked) EmotionStrings.PROGRESS_OFFICIAL_UNLOCKED else EmotionStrings.PROGRESS_PROVISIONAL,
            officialRangeUnlocked = unlocked,
            blockScoreText = EmotionStrings.PROGRESS_SCORE_PREFIX + (blockScore?.let { "$it/100" } ?: EmotionStrings.PROGRESS_SCORE_CALIBRATING),
            blockScoreCalibrating = blockScore == null,
            lastSessionText = when (lastSessionCounted) {
                true -> EmotionStrings.PROGRESS_LAST_VALID
                false -> EmotionStrings.PROGRESS_LAST_PRACTICE
                null -> null
            },
            notCountedNote = if (lastSessionCounted == false) EmotionStrings.PROGRESS_SESSION_NOT_COUNTED else null,
            lowConfidenceNote = if (lastSessionLowConfidence) EmotionStrings.PROGRESS_LOW_CONFIDENCE else null,
            nextRecommendationText = nextRecommendation?.let { EmotionStrings.PROGRESS_NEXT_PREFIX + it },
            disclaimer = EmotionStrings.PROGRESS_DISCLAIMER,
        )
    }

    /**
     * MC8u-E §4/§6 — build the progress model straight from the loaded history (persisted summaries + derived
     * aggregate), so the block screen and a future dashboard render it without new persistence. `validSessions`
     * already excludes low-confidence/anomalous sessions (aggregate uses `isValidForAggregate`); the last session's
     * counted/low-confidence state and its 0–100 block score come from the most-recent summary.
     */
    fun fromHistory(
        summaries: List<EmotionSessionSummaryDto>,
        aggregate: EmotionAggregateDto?,
        nextRecommendation: String?,
    ): EmotionCalibrationProgressUiModel {
        val latest = summaries.maxByOrNull { it.startedAtMillis }
        val validSessions = aggregate?.sessionsCompleted
            ?: summaries.count { EmotionTerminalSemantics.isValidForAggregate(it) }
        val lastCounted = latest?.let { EmotionTerminalSemantics.isValidForAggregate(it) }
        val lowConfidence = latest?.let { !it.governingConfident || it.anomalyFlags.isNotEmpty() } ?: false
        return build(validSessions, latest?.normalizedScore, lastCounted, lowConfidence, nextRecommendation)
    }
}
