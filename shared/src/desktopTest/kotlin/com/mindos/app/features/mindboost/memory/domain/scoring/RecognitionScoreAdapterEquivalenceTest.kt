package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization / equivalence lock: the shared [RecognitionScoreAdapter] produces a ScoreResult EQUAL to
 * BOTH existing adapters — EX4 [MemoryPairsProfileAdapter] (signal `ASSOCIATIVE_INDEX`) and EX3
 * [DeferredRecallProfileAdapter] (signal `DELAYED_RECALL_ACCURACY`) — for equivalent block inputs and the
 * same provenance/coverage/flags. This pins the precondition that makes the planned adapter delegation
 * behavior-preserving BY CONSTRUCTION; ScoreResult is a data class so equality is structural. No production
 * code touched.
 */
class RecognitionScoreAdapterEquivalenceTest {

    private val prov = ScoreProvenance("EX", ScoreModuleId.MEMORY, "s1", "hash", timestampMs = 1L, scoringVersion = "v1")

    // The SAME BankRef each leaf adapter defaults to (unregistered → identical integrity on both sides).
    private val pairsRef = BankRef(MemoryPairsProfileAdapter.BANK_ID)
    private val deferredRef = BankRef(DeferredRecallProfileAdapter.BANK_ID)

    private fun recog(state: RecognitionBlockState, index: Double?, far: Double, valid: Int) =
        RecognitionBlockResult(state, index, far, valid)

    private val scenarios: List<List<RecognitionBlockResult>> = listOf(
        emptyList(),
        listOf(recog(RecognitionBlockState.VALID, 0.6, 0.1, 10)),
        listOf(recog(RecognitionBlockState.VALID, 0.5, 0.0, 8), recog(RecognitionBlockState.VALID, 0.8, 0.0, 12)),
        listOf(recog(RecognitionBlockState.INVALIDATED, null, 1.0, 0)),
        listOf(recog(RecognitionBlockState.VALID, 0.7, 0.0, 9), recog(RecognitionBlockState.CALIBRATING, null, 0.0, 3)),
    )

    private fun List<RecognitionBlockResult>.asPairs() = map {
        PairsBlockResult(
            when (it.state) {
                RecognitionBlockState.VALID -> PairsBlockState.VALID
                RecognitionBlockState.INVALIDATED -> PairsBlockState.INVALIDATED
                RecognitionBlockState.CALIBRATING -> PairsBlockState.CALIBRATING
            },
            it.index, it.falseAlarmRate, it.validTrials,
        )
    }

    private fun List<RecognitionBlockResult>.asDeferred() = map {
        DeferredRecallBlockResult(
            when (it.state) {
                RecognitionBlockState.VALID -> DeferredRecallBlockState.VALID
                RecognitionBlockState.INVALIDATED -> DeferredRecallBlockState.INVALIDATED
                RecognitionBlockState.CALIBRATING -> DeferredRecallBlockState.CALIBRATING
            },
            it.index, it.falseAlarmRate, it.validTrials,
        )
    }

    @Test
    fun sharedAdapter_matchesMemoryPairsProfileAdapter_perScenario() {
        scenarios.forEach { blocks ->
            val shared = RecognitionScoreAdapter.toScoreResult(blocks, "ASSOCIATIVE_INDEX", prov, bankRef = pairsRef)
            val pairs = MemoryPairsProfileAdapter.toScoreResult(blocks.asPairs(), prov)
            assertEquals(pairs, shared, "ScoreResult mismatch for $blocks")
        }
    }

    @Test
    fun sharedAdapter_matchesDeferredRecallProfileAdapter_perScenario() {
        scenarios.forEach { blocks ->
            val shared = RecognitionScoreAdapter.toScoreResult(blocks, "DELAYED_RECALL_ACCURACY", prov, bankRef = deferredRef)
            val deferred = DeferredRecallProfileAdapter.toScoreResult(blocks.asDeferred(), prov)
            assertEquals(deferred, shared, "ScoreResult mismatch for $blocks")
        }
    }

    @Test
    fun sharedAdapter_matchesBoth_withAdequateCoverage_andUncleanFlag() {
        val blocks = listOf(recog(RecognitionBlockState.VALID, 0.75, 0.0, 24))
        assertEquals(
            MemoryPairsProfileAdapter.toScoreResult(blocks.asPairs(), prov, DetectorCoverage.ADEQUATE, antiGamingClean = false),
            RecognitionScoreAdapter.toScoreResult(
                blocks, "ASSOCIATIVE_INDEX", prov, bankRef = pairsRef, detectorCoverage = DetectorCoverage.ADEQUATE, antiGamingClean = false,
            ),
        )
        assertEquals(
            DeferredRecallProfileAdapter.toScoreResult(blocks.asDeferred(), prov, DetectorCoverage.ADEQUATE, antiGamingClean = false),
            RecognitionScoreAdapter.toScoreResult(
                blocks, "DELAYED_RECALL_ACCURACY", prov, bankRef = deferredRef, detectorCoverage = DetectorCoverage.ADEQUATE, antiGamingClean = false,
            ),
        )
    }
}
