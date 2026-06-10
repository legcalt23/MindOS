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

    /** Records a completed probe run (kept apart from training sessions). */
    suspend fun recordProbe(userId: String, result: SessionResult)

    /** Probe runs recorded for a user, newest first. [limit] is mandatory. */
    suspend fun probeHistory(userId: String, limit: Int): List<SessionResult>
}
