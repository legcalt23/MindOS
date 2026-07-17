package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.SessionStatus

/**
 * SYSTEM-1 slice 3 · Pure write-ahead lifecycle → eligibility/anomaly evaluator. Maps the [WriteAheadEvidence]
 * for a run's sessionId (plus the run's own status/timing) to a [WriteAheadVerdict]. A run may be [eligible]
 * ONLY when the write-ahead `SessionRecord` is present, its id matches, it is FINALIZED, and the timing is
 * coherent; every other case is FAIL-CLOSED (eligible = false) with a distinct anomaly flag.
 *
 * It REUSES the existing SessionRecord lifecycle: an ABANDONED record is produced by the existing
 * `SessionRecoveryScanner` (orphan STARTED/CHECKPOINT → ABANDONED) — this evaluator never creates a draft or a
 * scanner and never mutates any record.
 */
object WriteAheadEvaluator {

    fun evaluate(
        evidence: WriteAheadEvidence,
        runCompletionStatus: SessionCompletionStatus,
        runStartedAtEpochMs: Long,
        runCompletedAtEpochMs: Long?,
    ): WriteAheadVerdict =
        absenceVerdict(evidence)
            ?: mismatchVerdict(evidence)
            ?: presentVerdict(evidence, runCompletionStatus, runStartedAtEpochMs, runCompletedAtEpochMs)

    /** Rules 1-3: the record is not present — missing / unreadable / read-failed, each a distinct flag. */
    private fun absenceVerdict(evidence: WriteAheadEvidence): WriteAheadVerdict? =
        if (evidence.present) null else ineligible(absenceFlag(evidence.failureReason))

    private fun absenceFlag(reason: WriteAheadFailureReason?): String = when (reason) {
        WriteAheadFailureReason.UNREADABLE -> WriteAheadAnomalies.UNREADABLE
        WriteAheadFailureReason.READ_FAILED -> WriteAheadAnomalies.READ_FAILED
        null -> WriteAheadAnomalies.MISSING
    }

    /** Rule 7: a present record whose stored id does not match the run's sessionId (tampering). */
    private fun mismatchVerdict(evidence: WriteAheadEvidence): WriteAheadVerdict? =
        if (evidence.sessionIdMatches) null else ineligible(WriteAheadAnomalies.SESSION_ID_MISMATCH)

    /** Rules 4-6, 8: the record is present and id-matches — decide by lifecycle status + timing. */
    private fun presentVerdict(
        evidence: WriteAheadEvidence,
        runCompletionStatus: SessionCompletionStatus,
        runStartedAtEpochMs: Long,
        runCompletedAtEpochMs: Long?,
    ): WriteAheadVerdict = when (evidence.lifecycleStatus) {
        SessionStatus.ABANDONED -> ineligible(WriteAheadAnomalies.ABANDONED)
        SessionStatus.STARTED, SessionStatus.CHECKPOINT -> notFinalizedVerdict(runCompletionStatus)
        SessionStatus.FINALIZED -> finalizedVerdict(evidence, runStartedAtEpochMs, runCompletedAtEpochMs)
        null -> ineligible() // present but statusless: should not happen — fail-closed, no specific flag
    }

    /** Rule 5: an unfinalized record under a run that CLAIMS completion is a real inconsistency. */
    private fun notFinalizedVerdict(runCompletionStatus: SessionCompletionStatus): WriteAheadVerdict =
        if (runCompletionStatus == SessionCompletionStatus.COMPLETED) {
            ineligible(WriteAheadAnomalies.NOT_FINALIZED)
        } else {
            ineligible() // a non-completed run over an in-progress record is consistent, but not confirmed-eligible
        }

    /** Rule 6 (+ rule 8): finalized record — eligible only if the timing is coherent. */
    private fun finalizedVerdict(
        evidence: WriteAheadEvidence,
        runStartedAtEpochMs: Long,
        runCompletedAtEpochMs: Long?,
    ): WriteAheadVerdict =
        if (timingIncoherent(evidence, runStartedAtEpochMs, runCompletedAtEpochMs)) {
            ineligible(WriteAheadAnomalies.TIMING_INCOHERENT)
        } else {
            WriteAheadVerdict(eligible = true, anomalyFlags = emptyList())
        }

    /** Rule 8: only OBVIOUS impossibilities between the two sides' present timestamps (never fabricates one). */
    private fun timingIncoherent(
        evidence: WriteAheadEvidence,
        runStartedAtEpochMs: Long,
        runCompletedAtEpochMs: Long?,
    ): Boolean {
        val recordFinalizedBeforeStarted = evidence.finalizedAtEpochMs != null &&
            evidence.startedAtEpochMs != null &&
            evidence.finalizedAtEpochMs < evidence.startedAtEpochMs
        val runCompletedBeforeSessionStarted = runCompletedAtEpochMs != null &&
            evidence.startedAtEpochMs != null &&
            runCompletedAtEpochMs < evidence.startedAtEpochMs
        val runStartedAfterSessionFinalized = evidence.finalizedAtEpochMs != null &&
            runStartedAtEpochMs > evidence.finalizedAtEpochMs
        return recordFinalizedBeforeStarted || runCompletedBeforeSessionStarted || runStartedAfterSessionFinalized
    }

    private fun ineligible(vararg flags: String): WriteAheadVerdict =
        WriteAheadVerdict(eligible = false, anomalyFlags = flags.toList())
}
