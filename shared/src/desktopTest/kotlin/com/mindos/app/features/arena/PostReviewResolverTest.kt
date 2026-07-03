package com.mindos.app.features.arena

import com.mindos.app.features.arena.integrity.model.IntegritySignals
import com.mindos.app.features.arena.integrity.review.EvidencePacket
import com.mindos.app.features.arena.integrity.review.OpsResolutionMatrix
import com.mindos.app.features.arena.integrity.review.PostReviewResolver
import com.mindos.app.features.arena.integrity.review.RatingEffect
import com.mindos.app.features.arena.integrity.review.ResolutionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 05 / P5.3: Post-review resolution matrix, human-review enforcement, and appeal logic. */
class PostReviewResolverTest {

    private val resolver = PostReviewResolver()
    private val sevenDaysMs = 604_800_000L

    @Test
    fun matrixCoversAllTwelveResolutions() {
        assertEquals(12, OpsResolutionMatrix.MATRIX.size)
        assertTrue(ResolutionType.entries.all { OpsResolutionMatrix.MATRIX.containsKey(it) })
    }

    @Test
    fun softResolutionsApplyWithoutHumanReview() {
        val outcome = resolver.resolve(ResolutionType.NO_ACTION, humanReviewed = false)
        assertEquals(RatingEffect.NONE, outcome.policy.ratingEffect)
        assertTrue(!outcome.ratingRestored)
    }

    @Test
    fun severeResolutionRequiresHumanReview() {
        assertFailsWith<IllegalStateException> {
            resolver.resolve(ResolutionType.INVALIDATED_CONFIRMED, humanReviewed = false)
        }
        val reviewed = resolver.resolve(ResolutionType.INVALIDATED_CONFIRMED, humanReviewed = true)
        assertEquals(RatingEffect.ROLLBACK, reviewed.policy.ratingEffect)
        assertTrue(!reviewed.ratingRestored)
    }

    @Test
    fun falsePositiveRestoresRating() {
        val outcome = resolver.resolve(ResolutionType.FALSE_POSITIVE_RESTORED, humanReviewed = true)
        assertTrue(outcome.ratingRestored)
        assertEquals(RatingEffect.RESTORE, outcome.policy.ratingEffect)
    }

    @Test
    fun appealAllowedOnlyForEligibleResolutionsWithinWindowAndRateLimit() {
        // Eligible, within window, first appeal -> allowed.
        assertTrue(
            resolver.isAppealAllowed(ResolutionType.INVALIDATED_CONFIRMED, 0L, sevenDaysMs, 0),
        )
        assertTrue(
            resolver.isAppealAllowed(ResolutionType.INTEGRITY_HOLD_CONFIRMED, 0L, sevenDaysMs, 0),
        )
        // Just past the 7-day window -> denied.
        assertTrue(
            !resolver.isAppealAllowed(ResolutionType.INVALIDATED_CONFIRMED, 0L, sevenDaysMs + 1, 0),
        )
        // Rate limit reached -> denied.
        assertTrue(
            !resolver.isAppealAllowed(ResolutionType.INVALIDATED_CONFIRMED, 0L, 0L, 1),
        )
        // Non-appealable resolution -> denied even when fresh.
        assertTrue(
            !resolver.isAppealAllowed(ResolutionType.NO_ACTION, 0L, 0L, 0),
        )
    }

    @Test
    fun evidencePacketIsPseudonymizedAndValidated() {
        val packet = EvidencePacket(
            syntheticCaseId = "CASE_1",
            syntheticPlayerId = "SYNTH_PLAYER_1",
            sessionReplayReference = "REPLAY_1",
            inputEntropy = 3.2,
            geoVelocityKmh = 12.0,
            signals = IntegritySignals(collusion = true),
            createdTimestampMs = 100L,
        )
        assertTrue(packet.signals.collusion)
        assertFailsWith<IllegalArgumentException> { packet.copy(syntheticPlayerId = "") }
        assertFailsWith<IllegalArgumentException> { packet.copy(geoVelocityKmh = -1.0) }
    }
}
