package com.mindos.app.features.mindboost.decisiones.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.profile.domain.aggregation.ProfileUpdateGate
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * E2 Reversibilidad adapter (Decisiones · P1) · blocks → universal ScoreResult through the SAME gate as
 * every module. VALID only with adequate anti-gaming coverage AND ≥20 valid trials; otherwise null
 * contribution (never a fabricated number). Maps to CognitiveModuleId.DECISIONES.
 */
class ReversibilityProfileAdapterTest {

    private val provenance = ScoreProvenance(
        exerciseId = "E2_REVERSIBILIDAD",
        moduleId = ScoreModuleId.DECISIONS,
        sessionId = "u1:reversibility-aggregate",
        pseudoUserId = "pseudo-u1",
        timestampMs = 0L,
        scoringVersion = "rev-test",
    )

    /** A VALID block of [n] cards (n/2 reversible + n/2 irreversible), all classified correctly → index 1.0. */
    private fun perfectBlock(n: Int): ReversibilityBlockResult {
        val half = n / 2
        val rev = List(half) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) }
        val irr = List(half) { ReversibilityTrial(actuallyReversible = false, selectedReversible = false) }
        return ReversibilityScorer.scoreBlock(rev + irr)
    }

    /** A constant responder (always "reversible") on both classes → INVALIDATED. */
    private fun gamedBlock(n: Int): ReversibilityBlockResult {
        val half = n / 2
        val rev = List(half) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) }
        val irr = List(half) { ReversibilityTrial(actuallyReversible = false, selectedReversible = true) }
        return ReversibilityScorer.scoreBlock(rev + irr)
    }

    @Test
    fun perfectSession_withoutDetectorCoverage_capsAtCalibrating_noProfileUpdate() {
        val result = ReversibilityProfileAdapter.toScoreResult(listOf(perfectBlock(24)), provenance)
        assertNotEquals(ScoreState.VALID, result.state, "no tested detector coverage caps below VALID")
        assertFalse(result.updatesProfile)
        assertNull(result.primarySignal, "non-VALID never carries a number")
    }

    @Test
    fun sufficientHonestSession_withAdequateCoverage_isValid_withDiscriminationSignal() {
        val blocks = listOf(perfectBlock(12), perfectBlock(12)) // 24 valid trials, 2 valid blocks
        val result = ReversibilityProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        assertEquals(ScoreState.VALID, result.state)
        assertTrue(result.updatesProfile)
        assertEquals("REVERSIBILITY_DISCRIMINATION", assertNotNull(result.primarySignal).key.id)
        assertEquals(1.0, assertNotNull(result.primarySignal?.value), absoluteTolerance = 1e-9)
    }

    @Test
    fun belowMinN_isInsufficient_noProfileUpdate() {
        val result = ReversibilityProfileAdapter.toScoreResult(
            listOf(perfectBlock(10)), // 10 valid trials < the ≥20 gate
            provenance,
            DetectorCoverage.ADEQUATE,
        )
        assertEquals(ScoreState.INSUFFICIENT_N, result.state)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun onlyGamedBlocks_isInvalidated_noProfileUpdate() {
        val result = ReversibilityProfileAdapter.toScoreResult(
            listOf(gamedBlock(24)),
            provenance,
            DetectorCoverage.ADEQUATE,
        )
        assertEquals(ScoreState.INVALIDATED, result.state)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun validSession_yieldsDecisionesContribution_throughTheGate() {
        val blocks = listOf(perfectBlock(12), perfectBlock(12))
        val result = ReversibilityProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        val contribution = assertNotNull(ProfileUpdateGate.contributionFor(result))
        assertEquals(CognitiveModuleId.DECISIONES, contribution.moduleId)
        assertEquals(1.0, assertNotNull(contribution.normalizedScore), absoluteTolerance = 1e-9)
        assertTrue(contribution.preliminary, "one-block-equivalent confidence → preliminary, never ESTABLISHED")
    }

    @Test
    fun gamedSession_yieldsNoContribution() {
        val result = ReversibilityProfileAdapter.toScoreResult(
            listOf(gamedBlock(24)),
            provenance,
            DetectorCoverage.ADEQUATE,
        )
        assertNull(ProfileUpdateGate.contributionFor(result), "a gamed session never contributes to the profile")
    }
}
