package com.mindos.app.features.mindboost.focus.domain.repository

import com.mindos.app.features.mindboost.focus.domain.evaluation.ProbeAdministration
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile

/**
 * Contracts for persisting focus-training data. Implementations are local-first
 * and encrypted (10_data_persistence_plan.md). Only summaries are stored — raw
 * trials never leave the device in this design.
 */
interface FocusProfileRepository {
    suspend fun load(userId: String): UserCognitiveProfile?
    suspend fun save(profile: UserCognitiveProfile)
}

interface SessionHistoryRepository {
    suspend fun append(result: SessionResult)

    /**
     * Most recent sessions for a user, newest first.
     * [limit] is mandatory: any list read must be bounded (mirrors the Firestore
     * rules convention enforced across this repo).
     */
    suspend fun recent(userId: String, limit: Int): List<SessionResult>
}

interface BaselineRepository {
    suspend fun load(userId: String): BaselineSnapshot?
    suspend fun save(snapshot: BaselineSnapshot)
}

/**
 * Schedules and records reserved evaluation probes. Probe results are stored
 * SEPARATELY from training history ([SessionHistoryRepository]) so the two data
 * channels never mix (anti-contamination, 06_ §1).
 */
interface ProbeBankRepository {
    /** Picks the probe for the given week and rotates to its next reserved parallel form. */
    suspend fun nextProbe(userId: String, week: Int): ProbeAdministration

    /**
     * Gives back the form that the last [nextProbe] for this probe handed out, because it was never
     * administered — the user aborted, the screen was disposed, or the run threw before the first trial.
     *
     * Reserved forms are a FINITE, non-replenishable supply (see `ProbeBank`), so a burn without an
     * administration is an irreversible loss of measurement capacity, not a cosmetic counter drift.
     * Contract: this is the exact inverse of ONE [nextProbe] and is NOT idempotent — call it at most once
     * per burn, and never once the block completed (a form that produced an administration stays spent).
     *
     * DECLARED CEILING: the cut is "the block completed", not "the first stimulus was drawn", so a form
     * partially seen before a mid-block quit IS given back. That is deliberate for the probes this bank
     * holds — speeded, low-content paradigms (PVT, flanker, go/no-go) where a parallel form varies the
     * stimulus SEQUENCE rather than hiding any answer, so partial exposure carries no item-specific
     * advantage, while burning a form for a session that yielded no measurement is a certain permanent
     * loss. A content-bearing probe added later would need the stricter cut instead.
     */
    suspend fun releaseProbe(userId: String, probeId: String)

    /** Records a completed probe run (kept apart from training sessions). */
    suspend fun recordProbe(userId: String, result: SessionResult)

    /** Probe runs recorded for a user, newest first. [limit] is mandatory. */
    suspend fun probeHistory(userId: String, limit: Int): List<SessionResult>
}
