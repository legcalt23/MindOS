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

/** EX3 deferred recall (P3) · adapter maps delayed-recall → ScoreResult; caps at CALIBRATING, INVALIDATES gaming. */
class DeferredRecallProfileAdapterTest {

    private val provenance =
        ScoreProvenance("EX3", ScoreModuleId.MEMORY, "s1", "hash", timestampMs = 0L, scoringVersion = "v1")

    /** A block of [validTrials] discriminating outcomes each recalling [hits]/8 studied words, no lures. */
    private fun block(validTrials: Int, hits: Int): DeferredRecallBlockResult =
        DeferredRecallScorer.scoreBlock(
            List(validTrials) { DeferredRecallOutcome(studiedCount = 8, hits = hits, distractorCount = 8, falseAlarms = 0) },
        )

    private fun gamedBlock(): DeferredRecallBlockResult =
        DeferredRecallScorer.scoreBlock(
            List(10) { DeferredRecallOutcome(studiedCount = 8, hits = 8, distractorCount = 8, falseAlarms = 8) },
        )

    @Test
    fun perfectSession_capsAtCalibrating_whenNoDetectorCoverage() {
        val blocks = listOf(block(8, 6), block(8, 6), block(8, 6)) // 24 valid, hit rate 0.75
        val result = DeferredRecallProfileAdapter.toScoreResult(blocks, provenance) // coverage NONE (default)
        assertEquals(ScoreState.CALIBRATING, result.state)
        assertTrue(!result.updatesProfile)
    }

    @Test
    fun withAdequateCoverage_sufficientSession_isValid_withDelayedSignal() {
        val blocks = listOf(block(8, 6), block(8, 6), block(8, 6))
        val result = DeferredRecallProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        assertEquals(ScoreState.VALID, result.state)
        assertTrue(result.updatesProfile)
        assertEquals(0.75, assertNotNull(result.primarySignal?.value), absoluteTolerance = 1e-9)
        assertEquals("DELAYED_RECALL_ACCURACY", result.primarySignal?.key?.id)
    }

    @Test
    fun belowMinN_isInsufficientData_evenWithCoverage() {
        val blocks = listOf(block(8, 6), block(8, 6)) // 16 valid < 20 (ACCURACY_RT_BASE nMin)
        val result = DeferredRecallProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        assertEquals(ScoreState.INSUFFICIENT_N, result.state)
        assertNull(result.primarySignal)
    }

    @Test
    fun selectEverythingSession_isInvalidated() {
        val result = DeferredRecallProfileAdapter.toScoreResult(
            listOf(gamedBlock(), gamedBlock()), provenance, DetectorCoverage.ADEQUATE,
        )
        assertEquals(ScoreState.INVALIDATED, result.state)
        assertNull(result.primarySignal)
        assertTrue(!result.updatesProfile)
    }

    @Test
    fun validSession_contributesDelayedToMemoria_notRt() {
        val blocks = listOf(block(8, 6), block(8, 6), block(8, 6))
        val result = DeferredRecallProfileAdapter.toScoreResult(blocks, provenance, DetectorCoverage.ADEQUATE)
        val contribution = assertNotNull(ProfileUpdateGate.contributionFor(result))
        assertEquals(CognitiveModuleId.MEMORIA, contribution.moduleId)
        assertEquals(0.75, assertNotNull(contribution.normalizedScore), absoluteTolerance = 1e-9)
    }
}
