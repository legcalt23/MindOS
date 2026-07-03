@file:Suppress("MagicNumber") // The 7-day appeal window (ms) is a documented, named default.

package com.mindos.app.features.arena.integrity.review

/** Phase 05: Effect a resolution has on rating. */
enum class RatingEffect {
    NONE,
    RESTORE,
    ZERO,
    ROLLBACK,
}

/** Phase 05: The 12 ops resolutions of the resolution matrix (ops_resolution_matrix.md). */
enum class ResolutionType {
    NO_ACTION,
    WARNING_ISSUED,
    RATING_FROZEN,
    RATING_ROLLED_BACK,
    INTEGRITY_HOLD_CONFIRMED,
    INVALIDATED_CONFIRMED,
    FALSE_POSITIVE_RESTORED,
    FAST_CALIBRATION_ASSIGNED,
    TEMPORARY_SUSPENSION,
    PERMANENT_EXCLUSION,
    APPEAL_GRANTED,
    APPEAL_DENIED,
}

/** Phase 05: Per-resolution policy: rating effect, appeal eligibility, review and evidence needs. */
data class ResolutionPolicy(
    val ratingEffect: RatingEffect,
    val appealAllowed: Boolean,
    val requiresHumanReview: Boolean,
    val requiresEvidence: Boolean,
)

/** Phase 05: Appeal-window configuration (appeal_allowed_logic.md): 7-day window, rate-limited. */
data class PostReviewConfig(
    val appealWindowMs: Long = 604_800_000L,
    val maxAppealsPerCase: Int = 1,
) {
    init {
        require(appealWindowMs > 0) { "appealWindowMs must be > 0" }
        require(maxAppealsPerCase >= 0) { "maxAppealsPerCase must be >= 0" }
    }

    companion object {
        val DEFAULT: PostReviewConfig = PostReviewConfig()
    }
}

/**
 * Phase 05: The ops resolution matrix. Appeals are allowed only for INTEGRITY_HOLD_CONFIRMED and
 * INVALIDATED_CONFIRMED (appeal_allowed_logic.md). Every confirmation / sanction / restore requires
 * human review (human_review_requirement.md) — no severe outcome is final on automation alone.
 */
object OpsResolutionMatrix {

    val MATRIX: Map<ResolutionType, ResolutionPolicy> = mapOf(
        ResolutionType.NO_ACTION to ResolutionPolicy(RatingEffect.NONE, false, false, false),
        ResolutionType.WARNING_ISSUED to ResolutionPolicy(RatingEffect.NONE, false, false, false),
        ResolutionType.RATING_FROZEN to ResolutionPolicy(RatingEffect.NONE, false, false, true),
        ResolutionType.RATING_ROLLED_BACK to ResolutionPolicy(RatingEffect.ROLLBACK, false, true, true),
        ResolutionType.INTEGRITY_HOLD_CONFIRMED to ResolutionPolicy(RatingEffect.ZERO, true, true, true),
        ResolutionType.INVALIDATED_CONFIRMED to ResolutionPolicy(RatingEffect.ROLLBACK, true, true, true),
        ResolutionType.FALSE_POSITIVE_RESTORED to ResolutionPolicy(RatingEffect.RESTORE, false, true, true),
        ResolutionType.FAST_CALIBRATION_ASSIGNED to ResolutionPolicy(RatingEffect.NONE, false, false, false),
        ResolutionType.TEMPORARY_SUSPENSION to ResolutionPolicy(RatingEffect.NONE, false, true, true),
        ResolutionType.PERMANENT_EXCLUSION to ResolutionPolicy(RatingEffect.NONE, false, true, true),
        ResolutionType.APPEAL_GRANTED to ResolutionPolicy(RatingEffect.RESTORE, false, true, true),
        ResolutionType.APPEAL_DENIED to ResolutionPolicy(RatingEffect.NONE, false, true, true),
    )

    fun policyFor(type: ResolutionType): ResolutionPolicy = MATRIX.getValue(type)
}
