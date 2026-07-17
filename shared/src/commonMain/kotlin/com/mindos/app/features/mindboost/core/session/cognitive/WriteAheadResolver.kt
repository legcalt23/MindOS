package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.SessionRecord
import com.mindos.app.features.mindboost.core.session.SessionRecordRead
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.SessionStatus

/**
 * SYSTEM-1 slice 3 · Resolves [WriteAheadEvidence] for [sessionId] by READING the EXISTING lifecycle store —
 * no new draft, no new scanner, no mutation. Uses [SessionRecordStore.findEvidence] to keep absent / unreadable
 * / backend-failed rows distinct, computes whether the stored record's id matches, and copies the timestamps
 * (never fabricating one). The verdict is then produced by [WriteAheadEvaluator].
 */
suspend fun resolveWriteAheadEvidence(store: SessionRecordStore, sessionId: String): WriteAheadEvidence =
    when (val read = store.findEvidence(sessionId)) {
        is SessionRecordRead.Found -> foundEvidence(read.record, sessionId)
        SessionRecordRead.Missing -> absentEvidence(reason = null)
        SessionRecordRead.Unreadable -> absentEvidence(WriteAheadFailureReason.UNREADABLE)
        SessionRecordRead.ReadFailed -> absentEvidence(WriteAheadFailureReason.READ_FAILED)
    }

private fun foundEvidence(record: SessionRecord, requestedSessionId: String): WriteAheadEvidence = WriteAheadEvidence(
    present = true,
    lifecycleStatus = record.status,
    sessionIdMatches = record.sessionId == requestedSessionId,
    startedAtEpochMs = record.startedAtMs,
    finalizedAtEpochMs = if (record.status == SessionStatus.FINALIZED) record.updatedAtMs else null,
    failureReason = null,
)

private fun absentEvidence(reason: WriteAheadFailureReason?): WriteAheadEvidence = WriteAheadEvidence(
    present = false,
    lifecycleStatus = null,
    sessionIdMatches = false,
    startedAtEpochMs = null,
    finalizedAtEpochMs = null,
    failureReason = reason,
)
