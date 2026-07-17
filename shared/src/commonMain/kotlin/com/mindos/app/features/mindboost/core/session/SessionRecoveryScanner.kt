package com.mindos.app.features.mindboost.core.session

import com.mindos.app.core.time.EpochClock

/**
 * Idempotent orphan recovery. On app/hub start it marks any [SessionStatus.STARTED] or
 * [SessionStatus.CHECKPOINT] session whose LAST UPDATE ([SessionRecord.updatedAtMs]) is older than [graceMs]
 * as [SessionStatus.ABANDONED] — its owning process died before finalizing. Pure and testable: uses the
 * injected [clock] (never a global clock); never touches FINALIZED rows; never duplicates (upsert by sessionId
 * overwrites in place); never modifies scoring or the profile; and does no remote sync (independent of the
 * sync queue).
 *
 * Recency is measured from `updatedAtMs`, NOT `startedAtMs`: a checkpoint is a heartbeat, so a legitimately
 * long session (5/10/20/30 min) that is still checkpointing stays "fresh" and is never falsely abandoned. The
 * [graceMs] window also avoids racing a just-written row from a session still live in the current process.
 *
 * PER-SUBJECT BY CONSTRUCTION (SYSTEM-1 slice 6-R3, §14 fold-in): this class has no subject concept of its own
 * — `recover()` simply folds over whatever [store] returns from [SessionRecordStore.all]. Isolation is entirely
 * inherited from [store]: when [store] is a [SecureSessionRecordStore] bound to the current authenticated
 * user's [subject][SecureSessionRecordStore], `all()` returns ONLY that user's rows, so `recover()` can no
 * longer see — let alone mutate to ABANDONED — another account's orphaned STARTED/CHECKPOINT rows sharing the
 * same physical `mindboost_sessions` namespace. A production factory that constructs this scanner over an
 * un-scoped store would silently reopen the cross-account write this fix closes.
 */
class SessionRecoveryScanner(
    private val store: SessionRecordStore,
    private val clock: EpochClock,
    private val graceMs: Long = DEFAULT_GRACE_MS,
) {
    /**
     * Marks aged orphans ABANDONED and returns how many were ACTUALLY persisted. Safe to call repeatedly.
     *
     * P2-01: counts ONLY [SessionRecordUpsertOutcome.Stored] outcomes — an orphan whose ABANDONED upsert did NOT
     * persist (revoked subject, or a transient backend/revocation fault) is NOT reported as recovered, so a
     * caller can never treat a failed recovery as a successful one. A revoked subject yields 0 (its rows are
     * being erased, not recovered); a transient fault leaves the orphan for the next scan.
     */
    suspend fun recover(): Int {
        val now = clock.nowMillis()
        val orphans = store.all().filter { it.isRecoverableOrphan(now) }
        var recovered = 0
        for (orphan in orphans) {
            val outcome = store.upsert(orphan.copy(status = SessionStatus.ABANDONED, updatedAtMs = now))
            if (outcome is SessionRecordUpsertOutcome.Stored) recovered++
        }
        return recovered
    }

    private fun SessionRecord.isRecoverableOrphan(now: Long): Boolean =
        (status == SessionStatus.STARTED || status == SessionStatus.CHECKPOINT) &&
            (now - updatedAtMs) > graceMs

    companion object {
        const val DEFAULT_GRACE_MS = 120_000L
    }
}
