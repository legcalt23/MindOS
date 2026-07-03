package com.mindos.app.features.mindboost.language.domain.claim

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the honesty contract (§5.7, §11.6): claims are allowed only when the measured state
 * reaches their minimum, blocks always return a strong honest alternative, and calibrating
 * never permits an improvement claim.
 */
class DefaultClaimEngineTest {

    private val engine = DefaultClaimEngine()

    private fun request(claim: ClaimType, state: EvidenceState) =
        ClaimRequest(requestedClaim = claim, evidenceState = state, trait = TraitCode.INF)

    @Test
    fun trainedImprovementAllowedAtE1() {
        val decision = engine.evaluate(request(ClaimType.TRAINED_IMPROVEMENT, EvidenceState.E1_TRAINED_IMPROVEMENT))
        val allowed = assertIs<ClaimDecision.Allowed>(decision)
        assertEquals("Mejoras en este tipo de ejercicio.", allowed.safeCopy)
        assertTrue(allowed.caveats.any { it.contains("transferencia funcional") })
    }

    @Test
    fun nearTransferBlockedBelowE2FallsBackToTrainedCopy() {
        val decision = engine.evaluate(request(ClaimType.NEAR_TRANSFER, EvidenceState.E1_TRAINED_IMPROVEMENT))
        val blocked = assertIs<ClaimDecision.Blocked>(decision)
        assertSame(ClaimBlockReason.INSUFFICIENT_EVIDENCE, blocked.reason)
        // The honest fallback is the strongest claim E1 supports.
        assertEquals(ClaimType.TRAINED_IMPROVEMENT.safeCopy, blocked.safeAlternativeCopy)
    }

    @Test
    fun functionalTransferBlockedWithoutE3() {
        val decision = engine.evaluate(request(ClaimType.FUNCTIONAL_TRANSFER, EvidenceState.E2_NEAR_TRANSFER))
        val blocked = assertIs<ClaimDecision.Blocked>(decision)
        assertSame(ClaimBlockReason.NO_FUNCTIONAL_TRANSFER, blocked.reason)
    }

    @Test
    fun maintainedBlockedWithoutDelayedRetest() {
        val decision = engine.evaluate(request(ClaimType.MAINTAINED, EvidenceState.E3_FUNCTIONAL_TRANSFER))
        val blocked = assertIs<ClaimDecision.Blocked>(decision)
        assertSame(ClaimBlockReason.NO_DELAYED_RETEST, blocked.reason)
    }

    @Test
    fun calibratingBlocksEverythingWithUncertaintyCopy() {
        val decision = engine.evaluate(request(ClaimType.TRAINED_IMPROVEMENT, EvidenceState.E0_CALIBRATING))
        val blocked = assertIs<ClaimDecision.Blocked>(decision)
        assertSame(ClaimBlockReason.STILL_CALIBRATING, blocked.reason)
        assertTrue(blocked.safeAlternativeCopy.contains("calibrando", ignoreCase = true))
    }

    @Test
    fun functionalTransferAllowedAtE3CarriesMaintenanceCaveat() {
        val decision = engine.evaluate(request(ClaimType.FUNCTIONAL_TRANSFER, EvidenceState.E3_FUNCTIONAL_TRANSFER))
        val allowed = assertIs<ClaimDecision.Allowed>(decision)
        assertTrue(allowed.caveats.any { it.contains("pausa") })
    }

    @Test
    fun strongestForFollowsTheLadder() {
        assertNull(ClaimType.strongestFor(EvidenceState.E0_CALIBRATING))
        assertSame(ClaimType.TRAINED_IMPROVEMENT, ClaimType.strongestFor(EvidenceState.E1_TRAINED_IMPROVEMENT))
        assertSame(ClaimType.NEAR_TRANSFER, ClaimType.strongestFor(EvidenceState.E2_NEAR_TRANSFER))
        assertSame(ClaimType.ROBUST_GENERALIZATION, ClaimType.strongestFor(EvidenceState.E5_ROBUST_GENERALIZATION))
    }
}
