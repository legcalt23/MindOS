package com.mindos.app.features.mindboost.emotions.data.local

import com.mindos.app.features.mindboost.core.session.SessionStatus
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto

/**
 * The single pure authority for emotion terminal semantics (MC8i). `endReason` is the PRIMARY source of truth;
 * the `completed`/`abandoned` booleans are only a LEGACY fallback for rows written before `endReason` existed.
 * Centralized so the repository (dedupe), the reconciler (record target), and the aggregate factory
 * (calibration eligibility) all agree — no contradictory boolean-vs-endReason interpretations.
 */
object EmotionTerminalSemantics {
    const val COMPLETED = "COMPLETED"
    const val SAFETY_STOP = "SAFETY_STOP"
    const val ABANDONED = "ABANDONED"

    /** Canonical identity of a session: its opaque sessionId, or a legacy marker derived from its startedAt. */
    fun identityOf(summary: EmotionSessionSummaryDto): String =
        summary.sessionId ?: "legacy:${summary.startedAtMillis}"

    /** The write-ahead record status this terminal implies. `endReason` governs; booleans are legacy fallback. */
    fun targetStatus(summary: EmotionSessionSummaryDto): SessionStatus =
        when (summary.endReason) {
            ABANDONED -> SessionStatus.ABANDONED
            COMPLETED, SAFETY_STOP -> SessionStatus.FINALIZED
            else -> if (summary.abandoned) SessionStatus.ABANDONED else SessionStatus.FINALIZED
        }

    /**
     * Whether this session counts toward calibration: ONLY a real completion. `endReason == COMPLETED` is
     * authoritative; a legacy row (no endReason) counts only when `completed && !abandoned`. A SAFETY_STOP or an
     * ABANDONED session never advances the aggregate.
     */
    fun countsForAggregate(summary: EmotionSessionSummaryDto): Boolean =
        summary.endReason == COMPLETED ||
            (summary.endReason == null && summary.completed && !summary.abandoned)
}
