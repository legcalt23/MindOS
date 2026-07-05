package com.mindos.app.features.mindboost.core.session

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId

/**
 * Lifecycle of a [SessionRecord], written write-ahead so an interrupted session is never lost.
 *
 * - [STARTED]    the session began; no block of trials has been checkpointed yet.
 * - [CHECKPOINT] at least one block of trials finished and was flushed (progress is durable).
 * - [FINALIZED]  the session completed normally.
 * - [ABANDONED]  exited mid-session, or recovered as an orphan (a STARTED/CHECKPOINT that was never finalized)
 *   on a later launch. An abandoned session NEVER feeds the cognitive profile.
 *
 * Monotonic: STARTED -> CHECKPOINT* -> (FINALIZED | ABANDONED). The per-block transitions are wired in task
 * 2.2; this enum is the shared vocabulary they use.
 */
enum class SessionStatus { STARTED, CHECKPOINT, FINALIZED, ABANDONED }

/**
 * The canonical, motor-agnostic local log of ONE training session: the single source of truth for what
 * happened (which [block], when, how far it got, whether it completed or was abandoned).
 *
 * Deliberately distinct from the sync layer. [com.mindos.app.features.mindboost.core.sync.SyncQueueEntry]
 * models how a session is mirrored to the cloud (attempt/status/fingerprint) and references this record only
 * by [sessionId]; a SessionRecord never carries sync state, and the sync queue never carries session outcome.
 *
 * Privacy by design (data minimization, master plan 19.4): ONLY lifecycle + coarse engagement counters. NEVER
 * raw trials, per-item responses, free text, or a device fingerprint. The owning user is intentionally not a
 * field: the record lives in a per-user, encrypted, account-bound namespace, so identity is implicit and
 * erased with the account. It is NOT a second scoring authority: the cognitive score stays in the
 * module/profile stores, so this model carries no score value (version provenance is attached in task 2.3,
 * never a duplicate score).
 */
data class SessionRecord(
    val sessionId: String,
    val block: ScoreModuleId,
    val status: SessionStatus,
    val startedAtMs: Long,
    val updatedAtMs: Long,
    val completedTrials: Int,
    val plannedTrials: Int,
    val activeDurationMs: Long,
)
