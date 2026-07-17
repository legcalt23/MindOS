package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.SessionStatus

/**
 * Why the write-ahead `SessionRecord` could not be READ, as opposed to being cleanly absent. Kept distinct so
 * the sealing path never conflates "no draft was written" with "a draft exists but is unreadable" or "the
 * backend faulted".
 */
enum class WriteAheadFailureReason {
    /** A row exists but could not be parsed/decrypted (corrupt / key-invalidated / unsupported version). */
    UNREADABLE,

    /** A transient storage/backend fault: the read threw. */
    READ_FAILED,
}

/**
 * SYSTEM-1 slice 3 · Lifecycle EVIDENCE of the write-ahead `SessionRecord` for a cognitive run's sessionId —
 * not mere presence. Resolved by [resolveWriteAheadEvidence] from the EXISTING `SessionRecordStore` (no new
 * draft, no new scanner) and consumed by [WriteAheadEvaluator].
 *
 * When [present] is false, [failureReason] disambiguates missing (null) / unreadable / read-failed. When
 * [present] is true, [lifecycleStatus] and the timestamps describe the found record ([finalizedAtEpochMs] is
 * the record's update time only when it is FINALIZED; timestamps are copied, never fabricated).
 */
data class WriteAheadEvidence(
    val present: Boolean,
    val lifecycleStatus: SessionStatus?,
    val sessionIdMatches: Boolean,
    val startedAtEpochMs: Long?,
    val finalizedAtEpochMs: Long?,
    val failureReason: WriteAheadFailureReason?,
)

/**
 * The write-ahead check's contribution to sealing a run. [eligible] is true ONLY when the lifecycle positively
 * confirms a clean, id-matching, FINALIZED, time-coherent session; otherwise it is false (which the caller uses
 * to force profile/calibration/ranking all off). [anomalyFlags] are the diagnostic flags to append to the run.
 */
data class WriteAheadVerdict(
    val eligible: Boolean,
    val anomalyFlags: List<String>,
)

/** Canonical, stable anomaly-flag strings the write-ahead evaluator emits into a run's `anomalyFlags`. */
object WriteAheadAnomalies {
    const val MISSING = "write_ahead_missing"
    const val UNREADABLE = "write_ahead_unreadable"
    const val READ_FAILED = "write_ahead_read_failed"
    const val ABANDONED = "write_ahead_abandoned"
    const val NOT_FINALIZED = "write_ahead_not_finalized"
    const val SESSION_ID_MISMATCH = "session_id_mismatch"
    const val TIMING_INCOHERENT = "write_ahead_timing_incoherent"
}
