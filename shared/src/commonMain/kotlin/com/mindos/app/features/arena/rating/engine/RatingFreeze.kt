package com.mindos.app.features.arena.rating.engine

import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.rating.model.PlayerRatingState

/**
 * Phase 03: Rating freeze (rating_freeze.md). When integrity is REVIEW_REQUIRED or HOLD a player's
 * rating is frozen — no updates apply until resolution. Freezing sets the integrity state to a
 * non-VERIFIED value, so the Bayesian engine (which only applies a delta when integrity is VERIFIED)
 * will reject any update for a frozen player. Pure — returns a new [PlayerRatingState].
 */
class RatingFreeze {

    fun freezeRating(player: PlayerRatingState, reason: String): PlayerRatingState {
        require(reason.isNotBlank()) { "freeze reason must not be blank" }
        return player.copy(integrityState = IntegrityStateEnum.REVIEW_REQUIRED)
    }

    fun isFrozen(player: PlayerRatingState): Boolean =
        player.integrityState == IntegrityStateEnum.REVIEW_REQUIRED ||
            player.integrityState == IntegrityStateEnum.HOLD
}
