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
 */
class SessionRecoveryScanner(
    private val store: SessionRecordStore,
    private val clock: EpochClock,
    private val graceMs: Long = DEFAULT_GRACE_MS,
) {
    /** Marks aged orphans ABANDONED and returns how many were recovered. Safe to call repeatedly. */
    suspend fun recover(): Int {
        val now = clock.nowMillis()
        val orphans = store.all().filter { it.isRecoverableOrphan(now) }
        orphans.forEach { store.upsert(it.copy(status = SessionStatus.ABANDONED, updatedAtMs = now)) }
        return orphans.size
    }

    private fun SessionRecord.isRecoverableOrphan(now: Long): Boolean =
        (status == SessionStatus.STARTED || status == SessionStatus.CHECKPOINT) &&
            (now - updatedAtMs) > graceMs

    companion object {
        const val DEFAULT_GRACE_MS = 120_000L
    }
}
