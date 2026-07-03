package com.mindos.app.features.mindboost.language.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The evidence ladder is the module's honesty contract. These tests pin its strict
 * ordering and helpers so a future edit can't silently let a higher claim through (§5.2).
 */
class EvidenceStateTest {

    @Test
    fun sixStatesOnTheLadder() {
        assertEquals(6, EvidenceState.entries.size)
    }

    @Test
    fun ranksAreStrictlyIncreasingInDeclarationOrder() {
        val ranks = EvidenceState.entries.map { it.rank }
        assertEquals(listOf(0, 1, 2, 3, 4, 5), ranks)
    }

    @Test
    fun isAtLeastIsReflexiveAndOrdered() {
        assertTrue(EvidenceState.E3_FUNCTIONAL_TRANSFER.isAtLeast(EvidenceState.E3_FUNCTIONAL_TRANSFER))
        assertTrue(EvidenceState.E4_MAINTAINED.isAtLeast(EvidenceState.E2_NEAR_TRANSFER))
        assertFalse(EvidenceState.E1_TRAINED_IMPROVEMENT.isAtLeast(EvidenceState.E2_NEAR_TRANSFER))
    }

    @Test
    fun nextWalksUpTheLadderAndStopsAtTop() {
        assertSame(EvidenceState.E1_TRAINED_IMPROVEMENT, EvidenceState.E0_CALIBRATING.next())
        assertSame(EvidenceState.E5_ROBUST_GENERALIZATION, EvidenceState.E4_MAINTAINED.next())
        assertNull(EvidenceState.E5_ROBUST_GENERALIZATION.next())
    }

    @Test
    fun lowestIsCalibrating() {
        assertSame(EvidenceState.E0_CALIBRATING, EvidenceState.LOWEST)
        assertTrue(EvidenceState.LOWEST.isCalibrating)
        assertFalse(EvidenceState.E1_TRAINED_IMPROVEMENT.isCalibrating)
    }

    @Test
    fun ofRankResolvesKnownAndRejectsUnknown() {
        assertSame(EvidenceState.E2_NEAR_TRANSFER, EvidenceState.ofRank(2))
        assertNull(EvidenceState.ofRank(99))
        assertNull(EvidenceState.ofRank(-1))
    }
}
