package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankKind
import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.bank.BankTestFixtures
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.profile.domain.aggregation.ProfileUpdateGate
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P1 EX1 · adapter maps complex-span → ScoreResult; honestly caps at CALIBRATING until a detector exists.
 *
 * B-1: the bank integrity is DERIVED from `BankIntegrityAuthority`. The coverage/validity/anti-gaming tests
 * inject a REGISTERED + VALID generator bank ([registeredRef]) so those downstream gates are reachable;
 * the PRODUCTION reality (unregistered → SMALL_UNVALIDATED → BANK_CONTAMINATED) is pinned separately.
 */
class MemorySpanProfileAdapterTest {

    private val provenance = ScoreProvenance("EX1", ScoreModuleId.MEMORY, "s1", "hash", timestampMs = 0L, scoringVersion = "v1")

    /** A registered + valid generator bank matching the adapter's bank id (test-only; production registry is empty). */
    private val registeredRef = BankRef(
        MemorySpanProfileAdapter.BANK_ID,
        registry = listOf(BankTestFixtures.validBank(MemorySpanProfileAdapter.BANK_ID, BankKind.ALGORITHMIC_GENERATOR)),
    )

    private fun block(validTrials: Int, span: Int): SpanBlockResult =
        ComplexSpanScorer.scoreBlock(
            List(validTrials) { SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = span) },
        )

    private fun invalidBlock(): SpanBlockResult =
        ComplexSpanScorer.scoreBlock(List(10) { i -> SpanTrial(6, processingCorrect = i % 2 == 0, serialCorrectItems = 6) })

    @Test
    fun unregisteredBank_isBankContaminated_noProfileUpdate() {
        // The production reality (B-1): the span generator has no registered procedural contract, so even a
        // perfect session with an ADEQUATE detector is BANK_CONTAMINATED and never updates the profile.
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4))
        val result = MemorySpanProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        assertEquals(ScoreState.BANK_CONTAMINATED, result.state)
        assertTrue(!result.updatesProfile)
        assertNull(ProfileUpdateGate.contributionFor(result), "unregistered bank cannot feed WORKING_MEMORY")
    }

    @Test
    fun perfectSession_capsAtCalibrating_whenNoAntiGamingDetector() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4)) // 24 valid trials, span 4
        val result = MemorySpanProfileAdapter.toScoreResult(blocks, provenance, bankRef = registeredRef) // detector NONE
        assertEquals(ScoreState.CALIBRATING, result.state)
        assertTrue(!result.updatesProfile)
    }

    @Test
    fun withAdequateDetector_sufficientSession_isValid_withSpanSignal() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4))
        val result = MemorySpanProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE, bankRef = registeredRef)
        assertEquals(ScoreState.VALID, result.state)
        assertTrue(result.updatesProfile)
        assertEquals(0.5, result.primarySignal?.value) // span 4 / MAX_SPAN 8
        assertEquals("WORKING_MEMORY_SPAN", result.primarySignal?.key?.id)
    }

    @Test
    fun invalidatedSession_isInvalidated() {
        val result = MemorySpanProfileAdapter.toScoreResult(
            listOf(invalidBlock(), invalidBlock()), provenance, DetectorCoverage.ADEQUATE, bankRef = registeredRef,
        )
        assertEquals(ScoreState.INVALIDATED, result.state)
        assertNull(result.primarySignal)
    }

    @Test
    fun belowMinN_isInsufficientData_evenWithDetector() {
        val blocks = listOf(block(8, 4), block(8, 4)) // 16 valid trials < 20 (ACCURACY_RT_BASE nMin)
        val result = MemorySpanProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE, bankRef = registeredRef)
        assertEquals(ScoreState.INSUFFICIENT_N, result.state)
        assertNull(result.primarySignal)
    }

    @Test
    fun validSession_contributesSpanToMemoria_notRt() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4))
        val result = MemorySpanProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE, bankRef = registeredRef)
        val contribution = ProfileUpdateGate.contributionFor(result)
        assertTrue(contribution != null)
        assertEquals(CognitiveModuleId.MEMORIA, contribution.moduleId)
        assertEquals(0.5, contribution.normalizedScore) // span-derived, never an RT value
    }

    @Test
    fun calibratingSession_contributesNull_noProfileUpdate() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4))
        val result = MemorySpanProfileAdapter.toScoreResult(blocks, provenance, bankRef = registeredRef) // NONE → CALIBRATING
        assertNull(ProfileUpdateGate.contributionFor(result))
    }

    @Test
    fun gamedProcessing_isSuspectedGaming_noProfileUpdate() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4))
        val result = MemorySpanProfileAdapter.toScoreResult(
            blocks, provenance, DetectorCoverage.ADEQUATE, antiGamingClean = false, bankRef = registeredRef,
        )
        assertEquals(ScoreState.SUSPECTED_GAMING, result.state)
        assertTrue(!result.updatesProfile)
    }
}
