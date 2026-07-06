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
    // MC8n · PRD: 5 VALID sessions unlock full sufficiency / official range. STABLE == official calibration.
    private const val OFFICIAL_SESSIONS = 5.0
    private const val SAFETY_WINDOW = 5
    private const val PER_SESSION_SAFETY_NORM = 3.0 // ~3 safety events per recent session ⇒ full safetyLoad (1.0)

    /** The aggregate implied by [summaries] at [now], or null if no VALID completed session exists yet. */
    fun fromSummaries(summaries: List<EmotionSessionSummaryDto>, now: Long): EmotionAggregateDto? {
        val completed = summaries.filter { EmotionTerminalSemantics.isValidForAggregate(it) }
        if (completed.isEmpty()) return null
        val sessions = completed.size
        val sufficiency = (sessions.toDouble() / OFFICIAL_SESSIONS).coerceAtMost(1.0)
        return EmotionAggregateDto(
            sessionsCompleted = sessions,
            tier = tierFor(sessions).name,
            dataSufficiency = sufficiency,
            safetyLoad = safetyLoadOf(summaries),
            transferConfidence = TransferConfidence.NOT_DEMONSTRATED.name,
            confidence = sufficiency,
            trend = ProfileTrend.UNKNOWN.name,
            updatedAtMillis = now,
        )
    }

    /**
     * Safety PRESSURE (not calibration): the average safety-event count over the most recent up-to-[SAFETY_WINDOW]
     * RELIABLE-identity summaries — INCLUDING SAFETY_STOP sessions (a safety stop IS safety pressure). Corrupt/
     * invalid-identity rows are excluded. Capped at 1.0. This deliberately does NOT require `countsForAggregate`, so
     * a session that ended in a safety stop still contributes to the load it caused.
     */
    fun safetyLoadOf(summaries: List<EmotionSessionSummaryDto>): Double {
        val recent = summaries
            .filter { EmotionTerminalSemantics.hasReliableSessionIdentity(it) }
            .sortedByDescending { it.startedAtMillis }
            .take(SAFETY_WINDOW)
        if (recent.isEmpty()) return 0.0
        val avgEvents = recent.sumOf { it.safetyEvents.size }.toDouble() / recent.size
        return (avgEvents / PER_SESSION_SAFETY_NORM).coerceAtMost(1.0)
    }

    /** Coarse calibration tier from the number of VALID completed sessions (MC8n · PRD: STABLE at 5). */
    fun tierFor(sessions: Int): ProfileTier = when {
        sessions <= 1 -> ProfileTier.EXPLORATORY
        sessions <= 2 -> ProfileTier.PRELIMINARY
        sessions <= 4 -> ProfileTier.INITIAL_TREND
        else -> ProfileTier.STABLE
    }
}
