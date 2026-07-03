package com.mindos.app.features.mindboost.memory.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization / equivalence lock: the shared [RecognitionDiscriminationScorer] produces IDENTICAL
 * block results (state, index, false-alarm rate, valid trials) to BOTH existing scorers — EX4
 * [AssociativePairsScorer] and EX3 [DeferredRecallScorer] — for equivalent inputs, plus identical gate
 * constants. This pins the precondition that makes the planned delegation behavior-preserving BY
 * CONSTRUCTION: if the shared core already matches both, replacing their bodies with a delegation to it
 * cannot change behavior. Lives next to the scorers; no production code touched.
 */
class RecognitionScorerEquivalenceTest {

    /** One recognition trial as (positives, hits, distractors, falseAlarms) — mapped to all three types. */
    private data class Q(val pos: Int, val hits: Int, val dist: Int, val fa: Int)

    private val scenarios: List<List<Q>> = listOf(
        emptyList(),
        List(20) { Q(8, 8, 8, 8) }, // select-all → INVALIDATED
        List(5) { Q(8, 6, 8, 0) }, // 5 discriminating → CALIBRATING
        List(8) { Q(8, 6, 8, 0) }, // 8 discriminating → VALID 0.75
        List(16) { Q(8, 6, 8, 0) } + List(4) { Q(8, 8, 8, 8) }, // 16/20 = 0.80 → INVALIDATED
        List(17) { Q(8, 6, 8, 0) } + List(3) { Q(8, 8, 8, 8) }, // 17/20 = 0.85 → VALID
        List(10) { i -> Q(8, i % 9, 8, 0) }, // mixed hit rates, all discriminating
        List(8) { Q(6, 3, 0, 0) }, // degenerate: no lures, FAR 0
    )

    @Test
    fun sharedCore_matchesAssociativePairsScorer_perScenario() {
        scenarios.forEach { qs ->
            val shared = RecognitionDiscriminationScorer.scoreBlock(qs.map { it.toRecognition() })
            val pairs = AssociativePairsScorer.scoreBlock(qs.map { PairsTrial(it.pos, it.hits, it.dist, it.fa) })
            assertEquals(shared.state.name, pairs.state.name, "state mismatch for $qs")
            assertEquals(shared.index, pairs.associativeIndex, "index mismatch for $qs")
            assertEquals(shared.falseAlarmRate, pairs.falseAlarmRate, absoluteTolerance = 1e-9)
            assertEquals(shared.validTrials, pairs.validTrials, "validTrials mismatch for $qs")
        }
    }

    @Test
    fun sharedCore_matchesDeferredRecallScorer_perScenario() {
        scenarios.forEach { qs ->
            val shared = RecognitionDiscriminationScorer.scoreBlock(qs.map { it.toRecognition() })
            val deferred =
                DeferredRecallScorer.scoreBlock(qs.map { DeferredRecallOutcome(it.pos, it.hits, it.dist, it.fa) })
            assertEquals(shared.state.name, deferred.state.name, "state mismatch for $qs")
            assertEquals(shared.index, deferred.delayedRecallIndex, "index mismatch for $qs")
            assertEquals(shared.falseAlarmRate, deferred.falseAlarmRate, absoluteTolerance = 1e-9)
            assertEquals(shared.validTrials, deferred.validTrials, "validTrials mismatch for $qs")
        }
    }

    @Test
    fun sharedConstants_matchBothScorers() {
        assertEquals(RecognitionDiscriminationScorer.MAX_FALSE_ALARM_RATE, AssociativePairsScorer.MAX_FALSE_ALARM_RATE)
        assertEquals(RecognitionDiscriminationScorer.DISCRIMINATION_VALIDITY, AssociativePairsScorer.DISCRIMINATION_VALIDITY)
        assertEquals(RecognitionDiscriminationScorer.MIN_VALID_TRIALS, AssociativePairsScorer.MIN_VALID_TRIALS)
        assertEquals(RecognitionDiscriminationScorer.MAX_FALSE_ALARM_RATE, DeferredRecallScorer.MAX_FALSE_ALARM_RATE)
        assertEquals(RecognitionDiscriminationScorer.DISCRIMINATION_VALIDITY, DeferredRecallScorer.DISCRIMINATION_VALIDITY)
        assertEquals(RecognitionDiscriminationScorer.MIN_VALID_TRIALS, DeferredRecallScorer.MIN_VALID_TRIALS)
    }

    private fun Q.toRecognition() = RecognitionTrial(pos, hits, dist, fa)
}
