@file:Suppress("MagicNumber") // The specialist excellence threshold is a documented tuning default.

package com.mindos.app.features.arena.rating.engine

import com.mindos.app.features.arena.rating.model.BlockRatingState

/** Phase 03: Badge kinds. Currently only single-block specialist excellence. */
enum class BadgeType {
    SPECIALIST,
}

/** Phase 03: A badge awarded for excellence in a single block. Does NOT elevate global rank. */
data class Badge(
    val blockId: String,
    val type: BadgeType,
)

/**
 * Phase 03: Specialist badge assignment (specialist_badge_assignment.md).
 *
 * Awards a SPECIALIST badge for excellence in a single block. The plan's "top-percentile" is
 * approximated offline by an absolute excellence threshold (no population is available here). A badge
 * is purely recognition — it never raises the non-compensatory global rank.
 */
class SpecialistBadgeAssigner(
    private val specialistThreshold: Double = 1700.0,
) {
    fun assignBadges(blocks: Map<String, BlockRatingState>): List<Badge> =
        blocks.filter { it.value.ratingMu.value >= specialistThreshold }
            .map { Badge(it.key, BadgeType.SPECIALIST) }
}
