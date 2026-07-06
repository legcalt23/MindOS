@file:Suppress("MagicNumber") // Calibration thresholds/norms ARE the data of this pure formula file.

package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.emotions.domain.model.TransferConfidence

/**
 * The SINGLE, pure authority for the emotion aggregate (the calibration profile) derived from persisted
 * summaries. Both the live terminal (holder) and the durable-terminal reconciler build the aggregate through
 * here (MC8h), so a completed session recovered by pending-retry advances the aggregate EXACTLY as a live
 * completion would — never a second, drifting formula.
 *
 * HONEST calibration semantics (rigor charter): a session counts ONLY when it is a real completion
 * (endReason-authoritative via [EmotionTerminalSemantics.countsForAggregate]) AND has a RELIABLE identity
 * (MC8m — [EmotionTerminalSemantics.hasReliableSessionIdentity]). A SAFETY_STOP/ABANDONED session, or a
 * legacy/null/corrupt-identity row, never advances calibration (read tolerance ≠ scoring tolerance). Returns
 * null when nothing qualifies, so callers never overwrite an existing aggregate with a fabricated zero (`null ≠ 0`).
 *
 * This is the SCORING authority — NOT `SessionRecord`. A `SessionRecord.status == FINALIZED` (which a SAFETY_STOP
 * also reaches) must NEVER be read as "a completed session for scoring/calibration": the record is a motor-agnostic
 * write-ahead lifecycle log with no score, and the aggregate here is derived only from qualifying summaries.
 */
object EmotionAggregateFactory {
    private const val STABLE_SESSIONS = 8.0
    private const val SAFETY_LOAD_NORM = 6.0

    /** The aggregate implied by [summaries] at [now], or null if no qualifying completed session exists yet. */
    fun fromSummaries(summaries: List<EmotionSessionSummaryDto>, now: Long): EmotionAggregateDto? {
        val completed = summaries.filter {
            EmotionTerminalSemantics.countsForAggregate(it) && EmotionTerminalSemantics.hasReliableSessionIdentity(it)
        }
        if (completed.isEmpty()) return null
        val sessions = completed.size
        val sufficiency = (sessions.toDouble() / STABLE_SESSIONS).coerceAtMost(1.0)
        val recentSafety = completed.maxByOrNull { it.startedAtMillis }?.safetyEvents?.size ?: 0
        return EmotionAggregateDto(
            sessionsCompleted = sessions,
            tier = tierFor(sessions).name,
            dataSufficiency = sufficiency,
            safetyLoad = (recentSafety.toDouble() / SAFETY_LOAD_NORM).coerceAtMost(1.0),
            transferConfidence = TransferConfidence.NOT_DEMONSTRATED.name,
            confidence = sufficiency,
            trend = ProfileTrend.UNKNOWN.name,
            updatedAtMillis = now,
        )
    }

    /** Coarse calibration tier from the number of completed sessions. */
    fun tierFor(sessions: Int): ProfileTier = when {
        sessions <= 1 -> ProfileTier.EXPLORATORY
        sessions <= 3 -> ProfileTier.PRELIMINARY
        sessions <= 7 -> ProfileTier.INITIAL_TREND
        else -> ProfileTier.STABLE
    }
}
