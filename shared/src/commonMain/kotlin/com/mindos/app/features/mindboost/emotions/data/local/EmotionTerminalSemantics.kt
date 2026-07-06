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

    /** The single keyspace delimiter policy — shared by the repository's key building and the reconciler's write
     * barrier — so an id that would corrupt/collide storage keys is rejected IDENTICALLY everywhere (MC8l). The
     * [SESSION_ID_REGEX] below EXCLUDES this delimiter, so a valid id can never break the keyspace. */
    const val KEY_DELIMITER = ":"

    /**
     * The accepted character set for a session identity (MC8m): opaque ASCII — letters, digits, `_`, `-`, length
     * 1..128. This matches the production generator (`sess_` + 32 hex) and is safe for local keys AND future
     * cloud/Firestore document ids. It deliberately EXCLUDES the [KEY_DELIMITER] (`:`), `/`, `.`, whitespace,
     * control and non-ASCII characters — anything that could corrupt a keyspace or a document path.
     */
    private val SESSION_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,128}$")

    /**
     * Whether [sessionId] may serve as a keyspace identity. Only a valid id may be written to a key, act as a
     * canonical dedupe identity, or be reconciled into the write-ahead `SessionRecordStore`. An invalid/blank id is
     * treated as legacy/corrupt: tolerated on READ, never on WRITE.
     */
    fun isValidSessionId(sessionId: String): Boolean = SESSION_ID_REGEX.matches(sessionId)

    /**
     * Whether [summary] has a RELIABLE identity for scoring/calibration eligibility (MC8m): a non-null, valid
     * sessionId. Read tolerance ≠ scoring tolerance — a legacy/null/corrupt row may be READ (never crash) but must
     * NOT unlock calibration/ranges. There is deliberately NO grandfathering of null/invalid ids for the aggregate.
     */
    fun hasReliableSessionIdentity(summary: EmotionSessionSummaryDto): Boolean =
        summary.sessionId != null && isValidSessionId(summary.sessionId)

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
