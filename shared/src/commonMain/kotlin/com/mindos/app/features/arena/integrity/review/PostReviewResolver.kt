package com.mindos.app.features.arena.integrity.review

/** Phase 05: Outcome of resolving a post-review case. */
data class ResolutionOutcome(
    val resolution: ResolutionType,
    val policy: ResolutionPolicy,
    val ratingRestored: Boolean,
)

/**
 * Phase 05: Offline, deterministic post-review resolver.
 *
 * - [resolve] applies a resolution, enforcing that any policy requiring human review cannot be
 *   applied by automation alone (human_review_requirement.md); a false-positive resolution restores
 *   rating (false_positive_review.md).
 * - [isAppealAllowed] permits appeals only for appeal-eligible resolutions, within the configured
 *   window, and below the per-case rate limit (appeal_allowed_logic.md). Pure: no clock or I/O — the
 *   caller supplies `nowMs`.
 */
class PostReviewResolver(
    private val config: PostReviewConfig = PostReviewConfig.DEFAULT,
) {

    fun resolve(resolution: ResolutionType, humanReviewed: Boolean): ResolutionOutcome {
        val policy = OpsResolutionMatrix.policyFor(resolution)
        check(humanReviewed || !policy.requiresHumanReview) {
            "Resolution $resolution requires human review and cannot be applied by automation alone"
        }
        return ResolutionOutcome(
            resolution = resolution,
            policy = policy,
            ratingRestored = policy.ratingEffect == RatingEffect.RESTORE,
        )
    }

    fun isAppealAllowed(
        resolution: ResolutionType,
        resolutionTimestampMs: Long,
        nowMs: Long,
        priorAppealCount: Int,
    ): Boolean {
        val policy = OpsResolutionMatrix.policyFor(resolution)
        val withinWindow = (nowMs - resolutionTimestampMs) in 0..config.appealWindowMs
        return policy.appealAllowed && priorAppealCount < config.maxAppealsPerCase && withinWindow
    }
}
