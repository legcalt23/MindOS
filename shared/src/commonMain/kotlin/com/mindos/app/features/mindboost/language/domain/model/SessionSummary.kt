// The summary is a flat aggregate record; its breadth is the data model, not a smell.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/** Local-first sync state of a persisted record (v6 §12.6 `sync_status`). */
@Serializable
enum class SyncStatus { PENDING, SYNCED, FAILED }

/**
 * Aggregated outcome of one finished session (v6 §6.1 step 10, §11.5 Coach Card source).
 *
 * Privacy by design: this stores **only aggregates** — counts and per-trait/per-error tallies —
 * never raw responses or free text (project rule: persist summaries, never high-volume raw
 * data). The granular attempt-level evidence lives in [EvidencePacket]s, which are abstractions.
 */
@Serializable
data class SessionSummary(
    val sessionId: LanguageSessionId,
    val userIdHash: UserIdHash,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val itemsAttempted: Int,
    val correctCount: Int,
    val perTraitCorrect: Map<TraitCode, Int> = emptyMap(),
    val perTraitAttempts: Map<TraitCode, Int> = emptyMap(),
    val errorCounts: Map<ErrorType, Int> = emptyMap(),
    val strongUpdateCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
) {
    init {
        require(itemsAttempted >= 0) { "itemsAttempted cannot be negative." }
        require(correctCount in 0..itemsAttempted) {
            "correctCount ($correctCount) must be in 0..itemsAttempted ($itemsAttempted)."
        }
    }

    /** Overall accuracy as a proportion, or null when nothing was attempted (never 0 silently). */
    fun accuracy(): Proportion? =
        if (itemsAttempted == 0) null else Proportion(correctCount.toDouble() / itemsAttempted)
}
