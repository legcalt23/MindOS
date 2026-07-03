package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.profile.domain.aggregation.ProfileUpdateGate
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** P1 EX4 · adapter maps paired-associates → ScoreResult; caps at CALIBRATING until a detector, INVALIDATES gaming. */
class MemoryPairsProfileAdapterTest {

    private val provenance =
        ScoreProvenance("EX4", ScoreModuleId.MEMORY, "s1", "hash", timestampMs = 0L, scoringVersion = "v1")

    /** A block of [validTrials] discriminating trials each recalling [hits]/5 targets, no lures selected. */
    private fun block(validTrials: Int, hits: Int): PairsBlockResult =
        AssociativePairsScorer.scoreBlock(
            List(validTrials) { PairsTrial(targetCount = 5, hits = hits, distractorCount = 6, falseAlarms = 0) },
        )

    private fun gamedBlock(): PairsBlockResult =
        AssociativePairsScorer.scoreBlock(
            List(10) { PairsTrial(targetCount = 5, hits = 5, distractorCount = 6, falseAlarms = 6) },
        )

    @Test
    fun perfectSession_capsAtCalibrating_whenNoDetectorCoverage() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4)) // 24 valid trials, hit rate 0.8
        val result = MemoryPairsProfileAdapter.toScoreResult(blocks, provenance) // coverage NONE (default)
        assertEquals(ScoreState.CALIBRATING, result.state)
        assertTrue(!result.updatesProfile)
    }

    @Test
    fun withAdequateCoverage_sufficientSession_isValid_withAssociativeSignal() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4))
        val result = MemoryPairsProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        assertEquals(ScoreState.VALID, result.state)
        assertTrue(result.updatesProfile)
        assertEquals(0.8, assertNotNull(result.primarySignal?.value), absoluteTolerance = 1e-9)
        assertEquals("ASSOCIATIVE_INDEX", result.primarySignal?.key?.id)
    }

    @Test
    fun belowMinN_isInsufficientData_evenWithCoverage() {
        val blocks = listOf(block(8, 4), block(8, 4)) // 16 valid trials < 20 (ACCURACY_RT_BASE nMin)
        val result = MemoryPairsProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        assertEquals(ScoreState.INSUFFICIENT_N, result.state)
        assertNull(result.primarySignal)
    }

    @Test
    fun selectEverythingSession_isInvalidated() {
        val result = MemoryPairsProfileAdapter.toScoreResult(
            listOf(gamedBlock(), gamedBlock()), provenance, DetectorCoverage.ADEQUATE,
        )
        assertEquals(ScoreState.INVALIDATED, result.state)
        assertNull(result.primarySignal)
        assertTrue(!result.updatesProfile)
    }

    @Test
    fun validSession_contributesAssociativeToMemoria_notRt() {
        val blocks = listOf(block(8, 4), block(8, 4), block(8, 4))
        val result = MemoryPairsProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        val contribution = assertNotNull(ProfileUpdateGate.contributionFor(result))
        assertEquals(CognitiveModuleId.MEMORIA, contribution.moduleId)
        assertEquals(0.8, assertNotNull(contribution.normalizedScore), absoluteTolerance = 1e-9)
    }
}
