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

    /** PRD minimum for a session to count toward calibration (MC8n): a session shorter than this is engagement, not
     * measurable training. Duration below this NEVER advances the aggregate, regardless of terminal reason. */
    const val MIN_SESSION_DURATION_MS = 120_000L

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

    /** The same opaque-id policy applies to [userId] (MC8n): a Firebase/app UID is opaque ASCII. Rejects `:`/`/`/`.`/
     * whitespace/control/unicode/overlong so it can never break the per-user keyspace. */
    fun isValidUserId(userId: String): Boolean = SESSION_ID_REGEX.matches(userId)

    /**
     * Whether [summary] has a RELIABLE identity for scoring/calibration eligibility (MC8m): a non-null, valid
     * sessionId. Read tolerance ≠ scoring tolerance — a legacy/null/corrupt row may be READ (never crash) but must
     * NOT unlock calibration/ranges. There is deliberately NO grandfathering of null/invalid ids for the aggregate.
     */
    fun hasReliableSessionIdentity(summary: EmotionSessionSummaryDto): Boolean =
        summary.sessionId != null && isValidSessionId(summary.sessionId)

    /** Whether [endReason] is one of the three canonical terminals (MC8n). Null/`"GARBAGE"` are NOT known. */
    fun isKnownEndReason(endReason: String?): Boolean =
        endReason == COMPLETED || endReason == SAFETY_STOP || endReason == ABANDONED

    /**
     * Whether [summary] is a COHERENT terminal acceptable to WRITE (MC8n · tightened MC8p). A new row must carry a
     * canonical `endReason` whose `completed`/`abandoned` booleans EXACTLY match its meaning — the positive triple,
     * not merely the absence of a direct contradiction:
     *  - `COMPLETED`   ⇒ `completed == true  && abandoned == false`
     *  - `SAFETY_STOP` ⇒ `completed == false && abandoned == false`
     *  - `ABANDONED`   ⇒ `completed == false && abandoned == true`
     * Everything else — an unknown `endReason`, a `null` `endReason` (legacy, tolerated on READ only), or any
     * incoherent triple (e.g. `COMPLETED` with `completed=false`, `SAFETY_STOP` with `completed=true`, `ABANDONED`
     * with `abandoned=false`) — is rejected. Read tolerance is not write tolerance.
     */
    fun hasValidTerminalSemantics(summary: EmotionSessionSummaryDto): Boolean =
        when (summary.endReason) {
            COMPLETED -> summary.completed && !summary.abandoned
            SAFETY_STOP -> !summary.completed && !summary.abandoned
            ABANDONED -> !summary.completed && summary.abandoned
            else -> false
        }

    /**
     * The write-ahead record status this terminal implies, or NULL when the row is ambiguous/corrupt and must be
     * SKIPPED by reconciliation (MC8n · tightened MC8p). A KNOWN `endReason` reconciles ONLY when it is a COHERENT
     * terminal ([hasValidTerminalSemantics]) — a contradictory known row (e.g. COMPLETED+abandoned) yields null. A
     * null `endReason` (legacy) reconciles ONLY as an unambiguous XOR terminal (`completed` xor `abandoned`);
     * anything else (unknown reason, null with both/neither boolean) yields null. "Read tolerant, write safe": a
     * corrupt row is never finalized into the write-ahead store.
     */
    fun targetStatusOrNull(summary: EmotionSessionSummaryDto): SessionStatus? =
        when {
            // Legacy (null endReason): reconcile ONLY as an unambiguous XOR terminal.
            summary.endReason == null -> when {
                summary.completed && !summary.abandoned -> SessionStatus.FINALIZED
                summary.abandoned && !summary.completed -> SessionStatus.ABANDONED
                else -> null
            }
            // MC8p: a KNOWN endReason must be a COHERENT terminal — a contradictory row (e.g. COMPLETED+abandoned,
            // SAFETY_STOP+completed, ABANDONED+!abandoned) is SKIPPED, never finalized. Read tolerance ≠ reconcile
            // tolerance: a corrupt row can be read without a crash but must not enter the write-ahead store.
            !hasValidTerminalSemantics(summary) -> null
            summary.endReason == ABANDONED -> SessionStatus.ABANDONED
            else -> SessionStatus.FINALIZED // COMPLETED or SAFETY_STOP — already proven coherent above.
        }

    /**
     * Whether this session counts toward calibration: ONLY a real, COHERENT completion (MC8n · tightened MC8p). A
     * `COMPLETED` row counts ONLY when its booleans are coherent ([hasValidTerminalSemantics]) — a corrupt
     * `COMPLETED + abandoned=true` or `COMPLETED + completed=false` row NEVER counts. A legacy row (no endReason)
     * counts only when `completed && !abandoned`. A SAFETY_STOP or an ABANDONED session never advances the aggregate.
     * (This ALSO gates a FINALIZED record's completed-trials count via [completedTrialsFor], so a SAFETY_STOP or a
     * contradictory row never claims completed work.)
     */
    fun countsForAggregate(summary: EmotionSessionSummaryDto): Boolean =
        when (summary.endReason) {
            COMPLETED -> hasValidTerminalSemantics(summary)
            null -> summary.completed && !summary.abandoned
            else -> false
        }

    /**
     * The SINGLE authority (MC8o) for the completed-TRIALS a terminal implies for its write-ahead `SessionRecord`:
     * a real completion did ONE unit of work; a SAFETY_STOP or ABANDONED terminal did ZERO — even though a
     * SAFETY_STOP still reaches FINALIZED lifecycle. Derived from [countsForAggregate] (COMPLETED / legacy-completed
     * ⇒ 1; SAFETY_STOP, ABANDONED, unknown/ambiguous ⇒ 0), NEVER from the raw record status, so a FINALIZED
     * SAFETY_STOP can never look like completed work to future analytics. Fail-closed: anything not a real
     * completion is 0.
     */
    fun completedTrialsFor(summary: EmotionSessionSummaryDto): Int = if (countsForAggregate(summary)) 1 else 0

    /**
     * Whether [summary] is a VALID session for the aggregate/calibration (MC8n · PRD "valid session"): reliable
     * identity, a real completion, AND at least [MIN_SESSION_DURATION_MS] of active training. Read tolerance is not
     * scoring tolerance. (TODO(MC-scoring): also require `!hasAnomaly` when [EmotionSessionSummaryDto] exposes
     * anomaly flags — not modeled yet.)
     */
    fun isValidForAggregate(summary: EmotionSessionSummaryDto): Boolean =
        hasReliableSessionIdentity(summary) &&
            countsForAggregate(summary) &&
            summary.durationMs >= MIN_SESSION_DURATION_MS
}
