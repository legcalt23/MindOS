package com.mindos.app.features.arena.rating.rollback

import com.mindos.app.features.arena.rating.model.PlayerRatingState
import com.mindos.app.features.arena.rating.model.Rating

/** Phase 03: Result of a rollback — the inverted delta plus its auditable [RollbackReference]. */
data class RollbackResult(
    val reference: RollbackReference,
    val invertedDelta: Double,
)

/**
 * Phase 03: Rollback engine (rollback_engine.md). Inverts a previous rating update and produces a
 * [RollbackReference] audit trail. Pure — the caller supplies synthetic ids and timestamps, and
 * [applyRollback] returns a new [PlayerRatingState]. No premium / self-report inputs.
 */
class RollbackEngine {

    fun rollback(
        sessionId: String,
        originalDelta: Double,
        syntheticPlayerId: String,
        targetTimestampMs: Long,
        executionTimestampMs: Long,
    ): RollbackResult {
        require(sessionId.isNotBlank()) { "sessionId must not be blank" }
        require(syntheticPlayerId.isNotBlank()) { "syntheticPlayerId must not be blank" }
        val reference = RollbackReference(
            rollbackId = "ROLLBACK_$sessionId",
            syntheticPlayerId = syntheticPlayerId,
            targetTimestampMs = targetTimestampMs,
            reasonCode = "INVERT_UPDATE",
            executionTimestampMs = executionTimestampMs,
        )
        return RollbackResult(reference = reference, invertedDelta = -originalDelta)
    }

    fun applyRollback(player: PlayerRatingState, invertedDelta: Double): PlayerRatingState =
        player.copy(globalRatingMu = Rating(player.globalRatingMu.value + invertedDelta))
}
