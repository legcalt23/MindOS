package com.mindos.app.features.mindboost.core.session.payload

/**
 * Outcome a replay handler reports for one entry. Drives whether the journal entry is removed or kept.
 */
enum class ReplayOutcome {
    /**
     * The payload was applied to its destination store (or is permanently unusable — e.g. undecodable — and must
     * be dropped): [PayloadReplayScanner] CONFIRMS (removes) the journal entry. Because every sink applies
     * idempotently (E8's run store dedups by runId; reversibility dedups by session id), re-applying an entry that
     * was persisted but not yet confirmed on a prior run is a no-op — so CONFIRMED here is safe even on a re-run.
     */
    CONFIRMED,

    /**
     * A TRANSIENT failure (backend threw): the entry is KEPT for the next startup replay so the payload is never
     * lost. Never used for a permanent/undecodable payload (that is CONFIRMED-to-drop, so replay cannot loop).
     */
    RETAIN,
}

/**
 * Idempotent write-ahead replay. On app/feature start it drains [PayloadJournalStore.pending] for the current
 * subject, re-applying each entry to its destination store via the [handlers] registered for its [PayloadKind] and
 * CONFIRMING (removing) it on success. This is what turns the journal from "durable intent" into "recovered
 * session": a payload journaled before a process death is re-persisted here on the next run.
 *
 * Entries whose [PayloadKind] has NO registered handler are LEFT untouched — a sibling feature's scanner (wired
 * with a different handler map over the SAME shared journal) will drain them. This lets the hub replay only
 * reversibility entries and Velocidad replay only E8 entries without either stealing the other's work.
 *
 * IDEMPOTENCE is a property of the [handlers], not this scanner: each handler must apply to a sink that dedups by
 * the payload's id, so replaying an already-persisted entry does not duplicate it. The scanner only guarantees
 * each pending entry is offered to its handler exactly once per [replay] call, and confirmed only on
 * [ReplayOutcome.CONFIRMED].
 */
class PayloadReplayScanner(
    private val journal: PayloadJournalStore,
    private val handlers: Map<PayloadKind, suspend (JournaledPayload) -> ReplayOutcome>,
) {
    /** Drains + re-applies this subject's pending entries; returns how many were confirmed. Safe to call repeatedly. */
    suspend fun replay(): Int {
        var confirmed = 0
        journal.pending().forEach { entry ->
            val handler = handlers[entry.kind] ?: return@forEach
            if (handler(entry) == ReplayOutcome.CONFIRMED) {
                journal.confirm(entry.entryId)
                confirmed += 1
            }
        }
        return confirmed
    }
}
