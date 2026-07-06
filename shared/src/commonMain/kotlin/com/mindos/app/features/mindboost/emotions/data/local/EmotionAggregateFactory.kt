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
 * (MC8m — [EmotionTerminalSemantics.hasReliableSessionIdentity]) AND at least [EmotionTerminalSemantics.
 * MIN_SESSION_DURATION_MS] of active training — the PRD "valid session", enforced via [EmotionTerminalSemantics.
 * isValidForAggregate]. A SAFETY_STOP/ABANDONED session, a short (<120s) session, or a legacy/null/corrupt-identity
 * row, never advances calibration (read tolerance ≠ scoring tolerance). Returns null when nothing qualifies, so
 * callers never overwrite an existing aggregate with a fabricated zero (`null ≠ 0`).
 *
 * PRD calibration (MC8n/MC8r): [OFFICIAL_SESSIONS] = 5 valid sessions unlock full sufficiency and the STABLE
 * (official) tier. This is the SCORING authority — NOT `SessionRecord`. A `SessionRecord.status == FINALIZED`
 * (which a SAFETY_STOP also reaches) must NEVER be read as "a completed session for scoring/calibration": the
 * record is a motor-agnostic write-ahead lifecycle log with no score, and the aggregate here is derived only
 * from qualifying summaries.
 */
object EmotionAggregateFactory {
    // MC8n/MC8r · PRD: 5 VALID sessions unlock full sufficiency / official range. STABLE == official calibration.
    private const val OFFICIAL_SESSIONS = 5.0
    private const val RECENT_SAFETY_WINDOW = 5
    private const val SAFETY_LOAD_NORM = 6.0 // ~6 safety events across the recent window ⇒ full safetyLoad (1.0)

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
     * Safety PRESSURE (not calibration): the total safety-event count over the most recent up-to-[RECENT_SAFETY_WINDOW]
     * summaries that have a RELIABLE identity AND a COHERENT terminal ([EmotionTerminalSemantics.targetStatusOrNull]
     * != null) — INCLUDING SAFETY_STOP sessions (a safety stop IS safety pressure). It deliberately does NOT require
     * `countsForAggregate`, so a safety stop still contributes the load it caused; but it DOES exclude
     * corrupt/invalid-identity AND contradictory/unknown-terminal rows, so a corrupt row can never inflate safety.
     * Normalized by [SAFETY_LOAD_NORM] and capped at 1.0.
     */
    fun safetyLoadOf(summaries: List<EmotionSessionSummaryDto>): Double {
        val recentReliable = summaries
            .filter {
                EmotionTerminalSemantics.hasReliableSessionIdentity(it) &&
                    EmotionTerminalSemantics.targetStatusOrNull(it) != null
            }
            .sortedByDescending { it.startedAtMillis }
            .take(RECENT_SAFETY_WINDOW)
        val recentSafety = recentReliable.sumOf { it.safetyEvents.size }
        return (recentSafety.toDouble() / SAFETY_LOAD_NORM).coerceAtMost(1.0)
    }

    /** Coarse calibration tier from the number of VALID completed sessions (MC8n/MC8r · PRD: STABLE at 5). */
    fun tierFor(sessions: Int): ProfileTier = when {
        sessions <= 1 -> ProfileTier.EXPLORATORY
        sessions <= 3 -> ProfileTier.PRELIMINARY
        sessions <= 4 -> ProfileTier.INITIAL_TREND
        else -> ProfileTier.STABLE
    }
}
